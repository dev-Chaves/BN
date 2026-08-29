# Experimento — concorrência no consumo de tokens de resgate

Artefatos do estudo de controle de concorrência no fluxo `POST /redemptions/provider/consume`.
Objetivo: submeter o **mesmo token** a requisições simultâneas e verificar quantos resgates são
confirmados, quantos são rejeitados e quantos registros são persistidos — com e sem os mecanismos
de proteção (lock pessimista, atualização condicional e restrição de unicidade).

## Arquivos

| Arquivo | Papel |
|---|---|
| `seed.sh` | Popula o ambiente via API pública (onboarding → benefício → parceria → employee) e escreve `seed-result.json` |
| `redemption-concurrency.js` | Script k6: emite um token fresco no `setup()` e dispara `VUS` requisições concorrentes de consumo |
| `verify.sql` | Queries pós-ensaio contra o PostgreSQL (resgates por token, duplicações) |
| `results/` | Sumários JSON exportados por rodada (`RUN_ID` nomeia cada arquivo) |

Requisitos: `k6`, `jq`, `curl`, `psql` (para verificação) e Docker acessível ao host de execução.

## 1. Preparar o ambiente

Suba o container da API com os rate limits de teste elevados — o filtro é in-process e limita
`POST /redemptions/*` a 10 req/s por IP, o que contaminaria o burst do k6 com respostas 429
(`RateLimitFilter` + `application.yaml`):

```bash
docker run -d --name bn-api-test --env-file /opt/bn/.env \
  -e REDEMPTION_RATE_LIMIT_USES_SECOND=1000 \
  -e REDEMPTION_RATE_LIMIT_USES_MINUTE=10000 \
  -e AUTH_RATE_LIMIT_USES_SECOND=50 \
  -e AUTH_RATE_LIMIT_USES_MINUTE=1000 \
  -v /opt/bn/secrets:/opt/bn/secrets:ro \
  -p 127.0.0.1:8081:8080 \
  ghcr.io/dev-chaves/bn:<tag>
```

As variáveis `-e` sobrepõem o `--env-file`. Confirme a saúde: `curl http://127.0.0.1:8081/actuator/health`.

## 2. Popular o ambiente

```bash
cd experiments
BASE_URL=http://127.0.0.1:8081 ./seed.sh
```

O seed cria fornecedor, cliente, benefício (`maxUsesPerUser=1000`), parceria ativa, employee e
executa um smoke test completo (emissão → preview → consumo). Ao final escreve `seed-result.json`.

Reexecutar o seed cria novos CPFs/CNPJs válidos e novos e-mails — é seguro rodar de novo.

## 3. Rodar o ensaio

Uma rodada = uma invocação do k6 = um token fresco consumido por `VUS` VUs simultaneamente.
O JWT de login é limitado por IP (elevado acima), então os logins ficam no `setup()`.

```bash
mkdir -p results
cd experiments

# Baseline: 1 VU
VUS=1 RUN_ID=lock-1-1 k6 run redemption-concurrency.js > results/lock-1-1.log 2>&1

# Níveis de concorrência, com repetições
for vus in 2 10 50; do
  for rep in 1 2 3 4 5; do
    VUS=$vus RUN_ID=lock-${vus}-${rep} k6 run redemption-concurrency.js > results/lock-${vus}-${rep}.log 2>&1
  done
done
```

O redirecionamento captura o summary em texto, os checks e — importante para o estudo — uma linha
`BN-RESP <base64>` por requisição, com status, duração e corpo da resposta individual
(ver seção 6 para extrair).

Saída esperada por rodada (variante atual, com proteções):

```
confirmados (200):         1
rejeitados CAS/lock (400): VUS - 1
conflito unicidade (409):  0
rate limit (429):          0
inesperados:               0
```

- **400** "Invalid state" = rejeição válida: o CAS/lock identificou token já consumido ou expirado.
- **409** = a restrição `UNIQUE` de `benefit_redemptions.token_id` precisou agir — indício de que
  alguma camada anterior falhou (esperado 0 na variante com proteções; pode aparecer na variante sem lock).
- **429/5xx** invalidam a rodada (repetir).

## 4. Verificar o estado no banco

O k6 imprime no stdout o token bruto emitido no `setup()`. Para as queries 1 e 2, calcule o hash:

```bash
# macOS (shasum) — no Linux, use sha256sum
printf '%s' '<token-bruto>' | shasum -a 256 | cut -d' ' -f1
psql "$DATABASE_URL" -v token_hash=<hash> -v recent=50 -f verify.sql
```

As queries críticas: **(2)** resgates do token (esperado 1) e **(3)** duplicações globais
(esperado 0 linhas). O `seed.sh` também consome 1 token no smoke test — considere-o ao interpretar
a query 4.

## 5. Variante sem lock (comparação)

Branch dedicada removendo **duas** das três proteções; restará apenas a `UNIQUE` de
`benefit_redemptions.token_id`:

1. `git checkout -b experiment/no-lock`
2. Em `RedemptionTokenRepository`: remover `@Lock(LockModeType.PESSIMISTIC_WRITE)` e o import.
3. Em `RedemptionTokenRepository`: substituir `consumeIfActive` por uma atualização **incondicional**
   (mesma assinatura, sem as condições `status`/`expiresAt`):
   ```java
   @Modifying
   @Query("update RedemptionToken t set t.status = :consumed, t.consumedAt = :now where t.id = :id")
   int consumeIfActive(UUID id, LocalDateTime now, RedemptionTokenStatus consumed);
   ```
   (ajustar o `default` correspondente).
4. `./mvnw spotless:apply && ./mvnw compile && ./mvnw test`
5. Build/deploy no ambiente de teste (mesmo comando do passo 1) e repetir a bateria do passo 3
   usando `RUN_ID=nolock-...`.

Diferença esperada: as rejeições migram de **400** (CAS recusou) para **409** (violação de unicidade
detectada pelo banco, `DataIntegrityViolationException`), com impacto observável em latência
p95/p99 — é esse trade-off que o experimento documenta.

## 6. Consolidar

Os sumários em `results/<RUN_ID>.json` contêm os contadores (`redemption_confirmed`,
`redemption_rejected_consumed`, `redemption_unique_conflict`, `redemption_rate_limited`,
`redemption_unexpected`), as latências por classe (`redemption_confirmed_duration`,
`redemption_rejected_duration` — p50/p95/p99 dos confirmados e das rejeições separadamente) e
`http_req_duration` (todas as respostas). Exemplo de extração:

```bash
for f in results/*.json; do
  jq -r --arg run "$(basename "$f" .json)" '
    [$run,
     (.metrics["redemption_confirmed"].values.count // 0),
     (.metrics["redemption_rejected_consumed"].values.count // 0),
     (.metrics["redemption_unique_conflict"].values.count // 0),
     (.metrics["redemption_rate_limited"].values.count // 0),
     (.metrics["redemption_unexpected"].values.count // 0),
     ((.metrics["redemption_confirmed_duration"].values["p(95)"] // 0) | floor),
     ((.metrics["redemption_confirmed_duration"].values["p(99)"] // 0) | floor),
     ((.metrics["redemption_rejected_duration"].values["p(95)"] // 0) | floor),
     ((.metrics["redemption_rejected_duration"].values["p(99)"] // 0) | floor)]
    | @tsv' "$f"
done | column -t
```

Colunas: `run | confirmados | rejeitados(400) | 409 | 429 | inesperados | p95 confirmados | p99 confirmados | p95 rejeitados | p99 rejeitados` (ms).
Com VUS=1 só existe a série de confirmados; nas demais, compare a latência dos **confirmados**
entre níveis de concorrência (efeito da fila do lock) e entre variantes (com lock vs. sem lock).

### Estudar as respostas individuais

Cada requisição de consumo gera uma linha `BN-RESP <base64>` no log da rodada (o logger do k6
escapa `console.log` em logfmt, então o JSON é codificado em base64 para extração confiável).
A linha decodificada contém status, duração, VU/iteração e corpo da resposta. Para trabalhar com elas:

```bash
# Extrair as respostas de uma rodada para NDJSON
grep -oE 'msg="BN-RESP [A-Za-z0-9+/=]+"' results/lock-50-1.log \
  | sed -E 's/msg="BN-RESP //; s/"$//' \
  | base64 --decode > results/lock-50-1-responses.ndjson

# Contagem por status
jq -s 'group_by(.status) | map({status: .[0].status, total: length})' results/lock-50-1-responses.ndjson

# Distribuição de latência por classe
jq -s '
  group_by(.status == 200) |
  map({classe: (if .[0].status == 200 then "confirmados" else "rejeitados" end),
       n: length,
       p50: (map(.durationMs) | sort | .[length/2 | floor]),
       max: (map(.durationMs) | max)})' results/lock-50-1-responses.ndjson

# Corpos das respostas inesperadas (409/429/5xx) — devem ser 0 linhas numa rodada válida
jq -c 'select(.status != 200 and .status != 400)' results/lock-50-1-responses.ndjson
```

Os corpos são fixos por classe (por exemplo, `{"message":"Invalid state","status":400}` nas
rejeições do CAS/lock e `{"message":"Resource conflicts with existing data","status":409}` quando a
unicidade atua) — o valor do NDJSON está em auditar exceções e correlacionar VU/iteração com
latência.

### Amostras de métricas por request (opcional)

Para histogramas por requisição no artigo, grave também o stream de amostras do k6:

```bash
VUS=50 RUN_ID=lock-50-1 k6 run --out json=results/lock-50-1-samples.json redemption-concurrency.js
```

Cada linha do arquivo é uma métrica com timestamp; filtre por `http_req_duration` (e pelos
contadores customizados) para reconstruir a série temporal da rodada.

### Nota metodológica sobre latência

A latência medida pelo k6 (`res.timings.duration`) inclui a **rede WAN** entre a origem do
tráfego e a EC2. Isso não afeta a contagem de confirmados/rejeitados/duplicações, mas contamina
a comparação de latência entre variantes. Recomendações:

- Compare variantes sempre com o **mesmo ponto de origem** (mesma máquina/rede, janelas próximas).
- Para latência pura de servidor, rode o k6 na própria EC2 via SSH:

  ```bash
  # na EC2, com o repositório/clonagem disponível:
  BASE_URL=http://127.0.0.1:8081 VUS=50 RUN_ID=lock-50-ec2-1 k6 run experiments/redemption-concurrency.js
  ```

- Registre no resumo expandido qual origem foi usada para os números de latência reportados.
