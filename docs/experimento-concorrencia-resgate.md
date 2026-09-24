# Resumo da sessão — experimento de concorrência no resgate de tokens

**Data:** 29–30/08/2026 · **Objetivo:** coletar dados experimentais para o TCC sobre controle de
concorrência no fluxo de resgate do Benefix (uso único de token sob requisições simultâneas).

## 1. O que foi estudado

Duas configurações do mesmo fluxo (`RedemptionService.consume`):

| Configuração | Proteções ativas | Branch |
|---|---|---|
| **lock** | bloqueio pessimista (`SELECT … FOR UPDATE` em `findByHashWithRelations`) + CAS (`consumeIfActive` com condições de status/expiração) + `UNIQUE` de `benefit_redemptions.token_id` | `main` (produção) |
| **no-lock** | apenas a `UNIQUE` — lock e CAS removidos (`consumeIfActive` incondicional) | `experiment/no-lock` (deploy temporário) |

Trocas de variante na produção via `workflow_dispatch` do `deploy.yaml` na branch
(sem tocar na `main`) e rollback instantâneo pelo container `bn-api-previous`.

## 2. Ambiente e metodologia

- Produção controlada: `https://api.bnfix.com.br` (EC2 1 GB + RDS PostgreSQL), dados sintéticos
  (seeds benefitId 306/352/402), rate limits elevados via env no container.
- Instrumento: k6 v2.2.0 — bursts de 1 a 200 usuários virtuais consumindo **o mesmo token**;
  classificação por classe de status (200 confirmado · 400 rejeição de negócio · 409 conflito de
  unicidade · 429 rate limit · demais inesperados); latência p50/p95 por classe; registro
  individual por request (`BN-RESP` base64 → NDJSON).
- Validação por rodada: `confirmados == 1` e `429 == 0` e `5xx == 0` (400/409 são resultados
  esperados); rodadas inválidas repetidas (nenhuma precisou).
- Todas as comparações com **mesma origem de carga** (mesma máquina, mesmo host, mesmo proxy).

## 3. Matriz executada (44 rodadas · 2.108 requisições de consumo)

| # | Teste | Níveis | Rodadas | Artefatos |
|---|---|---|---|---|
| 1 | lock, bateria | 1/2/10/50 × 5 | 16 | `results/lock-*` (§7 README) |
| 2 | no-lock, bateria | 1/2/10/50 × 5 | 16 | `results-nolock/nolock-*` (§8) |
| 3 | no-lock, saturação | 100/200 × 3 | 6 | `results-nolock/nolock-100/200-*` (§9) |
| 4 | lock, saturação | 100/200 × 3 | 6 | `results/lock-100/200-*` (§9) |

## 4. Dados extraídos (agregados por nível, via NDJSON)

### 4.1 Bateria (1–50 VUs)

| nível | variante | classe | n | p50 | p95 |
|---|---|---|---|---|---|
| 2 | lock | confirmados | 5 | 142 | 182 |
| 2 | lock | rejeitados 400 | 5 | 152 | 161 |
| 2 | no-lock | confirmados | 5 | 148 | 165 |
| 2 | no-lock | rejeitados 400 | 5 | 157 | 164 |
| 10 | lock | confirmados | 5 | 137 | 143 |
| 10 | lock | rejeitados 400 | 45 | 149 | 157 |
| 10 | no-lock | confirmados | 5 | 136 | 141 |
| 10 | no-lock | rejeitados 400 | 41 | 145 | 157 |
| 10 | no-lock | conflitos 409 | 4 | 142 | 145 |
| 50 | lock | confirmados | 5 | 140 | 179 |
| 50 | lock | rejeitados 400 | 245 | **197** | **294** |
| 50 | no-lock | confirmados | 5 | 140 | 161 |
| 50 | no-lock | rejeitados 400 | 239 | 168 | 230 |
| 50 | no-lock | conflitos 409 | 6 | 146 | 169 |

### 4.2 Saturação (100–200 VUs)

| nível | variante | conf | 400 | 409 | 429/5xx | p50 rej 400 | p95 rej 400 |
|---|---|---|---|---|---|---|---|
| 100 | lock | 1 | 99 | 0 | 0 | 300 | 482 |
| 100 | no-lock | 1 | 98 | 1 | 0 | **246** | **312** |
| 200 | lock | 1 | 199 | 0 | 0 | **322** | **494** |
| 200 | no-lock | 1 | 198 | 1 | 0 | 450 | 578 |

Pico de latência geral observado: p95 ≈ 611 ms (`nolock-200-2`), sem timeouts.

## 5. Achados principais

1. **Unicidade: 44/44 rodadas com exatamente 1 confirmado e 0 duplicações** — nas duas
   configurações e em todos os níveis. A `UNIQUE` é garantia estrutural do banco; nenhuma carga
   a quebra.
2. **Composição das rejeições**: com lock/CAS, 100% `400` (serialização determinística); sem
   lock, majoritariamente `400` (perdedor lê token já `CONSUMED`) + 409 na corrida real até o
   `INSERT` (17 no total do no-lock; ~3,4% das rejeições a 50 VUs).
3. **Trade-off de latência**: a fila do `SELECT … FOR UPDATE` é o custo dominante do lock
   (p95 de rejeição 161→294 ms entre 2 e 50 VUs); o fallback restritivo rejeita ~22% mais
   rápido a 50 VUs (p95 230 ms), com confirmados estáveis (~140 ms nos dois).
4. **Crossover na saturação**: no-lock vence a 100 VUs (p50 246 vs 300; p95 312 vs 482) mas
   perde a 200 (p50 450 vs 322; p95 578 vs 494) — sob contenção extrema a rejeição com lock é
   uma leitura que falha cedo, enquanto sem lock cada rejeitado executa `UPDATE` + tentativa de
   `INSERT` e paga rollbacks de colisão. Gargalo comum a 200 VUs: fila do Hikari (10 conexões).
5. **409 não escala com a carga** (3,4% @50 → ~0,8% @200): a fila escalona a chegada ao banco,
   estreitando a janela read→INSERT.
6. **Bug do `preview` diagnosticado e confirmado**: `POST /redemptions/provider/preview`
   retornava 500 em produção por `FOR UPDATE` dentro de transação `readOnly` (rejeitado pelo
   PostgreSQL; nos testes JVM a transação read-write do teste mascarava o erro). Na versão
   no-lock o preview retornou 200 no smoke test — causa raiz confirmada. Correção pendente na
   `main` (remover `readOnly` ou usar consulta sem lock no preview).
7. **Mapeamento de exceções**: `400` = `IllegalStateException` (rejeição de negócio);
   `409` = `DataIntegrityViolationException` (constraint), ambos tratados no
   `GlobalExceptionHandler`. Sem lock, parte das rejeções muda de classe semântica — relevante
   para monitoramento e semântica de cliente.

## 6. Problemas encontrados e corrigidos no caminho

- `seed.sh` quebrado no bash 3.2 (macOS): payload dividido e chaves removidas por brace
  expansion → 500 no login (corrigido com padrão variável-antes-da-chamada).
- DTOs exigem `companyId` no body (benefício e employee); benefício e employee nascem inativos
  (passos de ativação adicionados ao seed).
- Extração do token bruto do log k6 capturava o sufixo logfmt (`" source=console`) → hashes
  errados (corrigido; `token-hashes.csv` regenerado).
- `base64 --decode` do macOS descarta quebras de linha → NDJSON normalizado com `jq -c`.

## 7. Decisões de escopo (para o TCC)

- Documentar apenas: lock pessimista vs constraints, fallback restritivo mais rápido, mapeamento
  de exceções e trade-offs de latência.
- **Sem** experimento multi-instância (2 servidores) e **sem** variante no-protection (sem
  `UNIQUE`): o contrafactual entra qualitativamente — a correção vive no banco e vale para
  qualquer número de instâncias; mecanismos em memória (não usados) é que quebrariam com
  escala horizontal.
- Formato: TCC, ~2 páginas (docx); terminologia **"ambiente controlado na AWS"** (não
  "homologação"); resumo em passado (trabalho concluído). Rascunho da continuação do resumo já
  redigido na sessão (abertura mantida pelo autor).

## 8. Estado e pendências

- Prod na versão **com lock** (rollback executado); `main` intocada; branch
  `experiment/no-lock` preservada (commits `58efe8f`, `eb0cda1`, `d72f941`).
- Verificação no banco (DBeaver): queries 1–4 do `verify.sql` com os hashes de
  `results/token-hashes.csv` e `results-nolock/token-hashes.csv` (22 cada); query 3
  (duplicações globais) deve permanecer com **0 linhas**.
- Escrever o documento final do TCC (quando autorizar a redação/geração do docx).
- Higienização futura da prod: reverter rate limits do `/opt/bn/.env`, limpar dados sintéticos
  dos seeds e (opcional) re-disparar deploy da `main` para alinhar 100% o estado; corrigir o
  bug do `preview` na `main`.
