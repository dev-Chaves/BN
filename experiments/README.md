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
# macOS (shasum) — no Linux, use sha256sum.
# ATENÇÃO: a linha de log termina com `" source=console` — extraia só o token
# (43 caracteres base64url), sem o sufixo do logfmt:
token=$(grep -o 'token bruto.*' results/lock-50-1.log | head -1 | sed 's/.*hash): //; s/" source=console$//')
printf '%s' "$token" | shasum -a 256 | cut -d' ' -f1
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
5. Deploy temporário na produção **sem tocar na main** (o `deploy.yaml` tem `workflow_dispatch`
   e o checkout usa o ref disparado):
   ```bash
   gh workflow run deploy.yaml --ref experiment/no-lock   # builda a branch e promove na prod
   gh run watch                                           # ~10–15 min
   ```
6. Repetir a bateria do passo 3 com `RUN_ID=nolock-...` contra a produção. Critério de rodada
   válida muda: `confirmados == 1`, `429 == 0`, `5xx == 0` — **400 e 409 são resultados
   esperados** (a distribuição entre eles é dado do experimento).
7. Rollback imediato (segundos, não depende de rebuild):
   ```bash
   docker stop bn-api && docker rm bn-api
   docker stop bn-api-previous && docker rename bn-api-previous bn-api && docker start bn-api
   ```

Diferença esperada: sem lock/CAS, a leitura vê o token `ACTIVE` em corridas simultâneas e o
`UPDATE` incondicional não recusa ninguém — a rejeição passa a ocorrer na **inserção do
resgate** (`409`, `DataIntegrityViolationException` → mapeado pelo `GlobalExceptionHandler`)
quando dois requests colidem na `UNIQUE`, ou no `400` quando o perdedor lê o token já
`CONSUMED`. Impacto observável em latência p95/p99 e na proporção 400/409 — é esse trade-off
que o experimento documenta. Bônus: sem `FOR UPDATE`, o `preview` volta a funcionar
(read-only não é mais violado), confirmando a causa raiz do bug 500.

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
# (o base64 do macOS descarta as quebras de linha do grep — normalize com jq -c)
grep -oE 'msg="BN-RESP [A-Za-z0-9+/=]+"' results/lock-50-1.log \
  | sed -E 's/msg="BN-RESP //; s/"$//' \
  | base64 --decode | jq -c . > results/lock-50-1-responses.ndjson

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

## 7. Resultados — execução de 2026-08-29 (variante com proteções)

Ambiente: `https://api.bnfix.com.br` (produção, EC2 + Postgres RDS), origem do tráfego =
máquina local do autor (WAN). Rate limits de `/redemptions/*` elevados via env no container
(sem 429 nas 16 rodadas). Seed: benefitId 352, sufixo `1788010364`. Validação por rodada:
`confirmados == 1` **e** `409 + 429 + inesperados == 0`; rodadas inválidas são repetidas
(nenhuma precisou de repetição).

| run | conf | 400 | 409 | 429 | 5xx | p95 conf (ms) | med conf (ms) | p95 rej (ms) | med rej (ms) |
|---|---|---|---|---|---|---|---|---|---|
| lock-1-1   | 1 | 0  | 0 | 0 | 0 | 169 | 169 | —   | —   |
| lock-2-1…5 | 1 | 1  | 0 | 0 | 0 | 135–182 | 135–155 | 141–161 | 141–161 |
| lock-10-1…5| 1 | 9  | 0 | 0 | 0 | 134–143 | 134–143 | 152–159 | 144–149 |
| lock-50-1…5| 1 | 49 | 0 | 0 | 0 | 135–179 | 135–179 | 196–311 | 173–268 |

### Estatísticas agregadas por nível (todas as repetições, via NDJSON)

| nível | classe | n | p50 | p95 | min | max |
|---|---|---|---|---|---|---|
| 2 | confirmados | 5 | 142 | 182 | 135 | 182 |
| 2 | rejeitados | 5 | 152 | 161 | 142 | 161 |
| 10 | confirmados | 5 | 137 | 143 | 134 | 143 |
| 10 | rejeitados | 45 | 149 | 157 | 133 | 160 |
| 50 | confirmados | 5 | 140 | 179 | 136 | 179 |
| 50 | rejeitados | 245 | 197 | 294 | 135 | 320 |

### Leitura dos dados

- **Unicidade: 16/16 rodadas com exatamente 1 confirmado e 0 duplicações.** A restrição de uso
  único se manteve em todos os níveis testados (até 50 consumidores simultâneos).
- `409` nunca ocorreu: nas rejeições, o **lock pessimista e/ou o CAS** sempre atuou antes da
  restrição `UNIQUE` — a restrição é a última linha de defesa, não o mecanismo de rejeição.
- Latência dos **confirmados** é estável entre níveis (p50 ~137–142 ms): o vencedor do lock é
  atendido no primeiro turno. A latência dos **rejeitados** cresce com a concorrência
  (152 → 149 → 197 ms de p50; p95 161 → 157 → 294 ms): efeito da fila no `SELECT … FOR UPDATE`
  somado à espera na fila de requisições do Tomcat.
- Latência absoluta inclui WAN (ver nota metodológica acima); para o artigo, comparar apenas
  entre classes/variantes com a mesma origem, ou reexecutar na EC2 para números de servidor.

### Pendências documentadas

1. **Verificação no banco** (`verify.sql`) com os hashes de `results/token-hashes.csv`
   (16 tokens): confirmar 1 redemption por token e 0 duplicações globais. Requer cliente psql
   (não instalado na máquina local — usar DBeaver/pgAdmin ou instalar psql).
2. **Variante sem lock** (seção 5): branch `experiment/no-lock`, reexecutar a bateria e
   comparar (rejeições devem migrar de 400 para 409).
3. **Bug de produção — `preview` 500**: `POST /redemptions/provider/preview` retorna 500 com
   payload e permissões corretos (reproduzido 3×, incl. smoke test do seed). Hipótese:
   `@Transactional(readOnly = true)` + `@Lock(PESSIMISTIC_WRITE)` no `findByHashWithRelations`
   — PostgreSQL rejeita `SELECT … FOR UPDATE` em transação read-only; nos testes JVM o preview
   entra na transação read-write já aberta pelo teste, mascarando o erro. Corrigir (remover
   `readOnly` ou usar consulta sem lock no preview) e revalidar o smoke do seed.
