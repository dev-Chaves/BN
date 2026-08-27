# Mudanças na API — comparação com os últimos 3 commits

> **Base de comparação:** commit `9174e16` (feature: add tests containers integration with search by similarity) → working tree atual.
> **Commits incluídos:** `617252e`, `d3b65d6`, `975b745` + mudanças não commitadas.
> **Data:** 27/08/2026 · **Audiência:** time frontend

---

## TL;DR — Breaking changes

| # | Mudança | Impacto no frontend |
|---|---------|---------------------|
| 1 | Domínio **Subscriptions removido** | `POST /subscriptions` não existe mais |
| 2 | Domínio **Benefit Access Requests removido** | Todos os endpoints `/benefit-requests/*` não existem mais |
| 3 | Domínio **Shared Benefits removido** | `/shared-benefits/available` e `/shared-benefits/me` não existem mais |
| 4 | Emissão de token de resgate **mudou de path** | `POST /redemptions/subscriptions/{subscriptionId}/token` → **`POST /redemptions/benefits/{benefitId}/token`** |
| 5 | **Novo endpoint** `GET /benefits/me` | Lista os benefícios disponíveis para o funcionário logado (substitui subscriptions + shared-benefits + benefit-requests) |
| 6 | **Novo campo** `availableToProviderEmployees` | Em `BenefitResponse`, `CreateBenefitRequest` e `UpdateBenefitRequest` |
| 7 | **Tokens ativos antigos foram revogados** (migração V13) | Funcionários precisam emitir token novamente |

---

## 1. Endpoints removidos

Não faça mais chamadas a estes endpoints — retornarão **404**:

| Método | Path | Papel | Substituto |
|--------|------|-------|------------|
| `POST` | `/subscriptions` | `USER` | `GET /benefits/me` + `POST /redemptions/benefits/{benefitId}/token` |
| `POST` | `/benefit-requests` | `USER` | Não é mais necessário (acesso derivado de parcerias) |
| `GET` | `/benefit-requests/me` | `USER` | `GET /benefits/me` |
| `GET` | `/benefit-requests/provider` | `MANAGER` | Não é mais necessário |
| `PUT` | `/benefit-requests/{id}/approve` | `MANAGER` | Não é mais necessário |
| `PUT` | `/benefit-requests/{id}/reject` | `MANAGER` | Não é mais necessário |
| `GET` | `/shared-benefits/available` | `USER` | `GET /benefits/me` |
| `GET` | `/shared-benefits/me` | `USER` | `GET /benefits/me` |

### Modelo de negócio

O fluxo anterior era: funcionário solicita acesso (`/benefit-requests`) → provider aprova → cria subscription → resgata por subscription.

O novo fluxo **elimina a etapa de solicitação/aprovação**: o acesso do funcionário é **derivado automaticamente** das parcerias ativas da empresa dele (ver seção 5).

---

## 2. Endpoints alterados

### `POST /redemptions/benefits/{benefitId}/token` — Emitir token de resgate

- **Papel:** `USER`
- **Antes:** `POST /redemptions/subscriptions/{subscriptionId}/token`
- **Path variable:** `benefitId` (era `subscriptionId`)

**Request:** sem body.

**Response 201** (inalterado):

```json
{
  "token": "<raw-token>",
  "redemptionUrl": "https://<public-url>/resgatar/<raw-token>",
  "expiresAt": "2026-08-27T14:35:00"
}
```

**Regras (novas):**
- Elegibilidade validada no momento da emissão (ver seção 5). Sem elegibilidade → erro.
- Apenas **1 token ativo por par (funcionário, benefício)**. Emitir novamente **revoga** o token ativo anterior.
- Token expira em **3 minutos**.
- Limite de usos: `maxUsesPerUser` por funcionário/benefício.

**Erros:**
- `404` — benefício não encontrado
- `403` — benefício não disponível para o funcionário (`SecurityException`)
- `409/400` — limite de uso atingido (`Benefit usage limit reached`)

### `GET /benefits/public/search?termo=` — Busca de benefícios

- Sem mudança de contrato, mas **correção de bug**: o campo `providerName` agora vem populado (antes vinha `null` porque a query não o selecionava).

```json
{
  "content": [
    {
      "id": 1,
      "name": "Benefício X",
      "description": "Descrição...",
      "providerName": "Empresa Provider"
    }
  ],
  "pageable": { "..." : "..." }
}
```

---

## 3. Endpoints novos

### `GET /benefits/me` — Benefícios disponíveis para o funcionário logado

- **Papel:** `USER`
- **Descrição:** lista todos os benefícios que o funcionário pode resgatar agora (elegibilidade avaliada em tempo real — ver seção 5).

**Request:** sem body, sem params.

**Response 200** — `List<EmployeeBenefitResponse>`:

```json
[
  {
    "benefitId": 1,
    "benefitName": "Benefício X",
    "description": "Descrição do benefício",
    "providerName": "Empresa Provider",
    "categories": [
      { "id": 3, "name": "Alimentação" }
    ],
    "validUntil": "2026-12-31T23:59:59",
    "maxUsesPerUser": 2,
    "usedCount": 1,
    "remainingUses": 1,
    "terms": "Termos de uso..."
  }
]
```

> Este endpoint substitui o fluxo de subscriptions: o frontend deve renderizar a lista de benefícios a partir daqui e emitir o token com `benefitId`.

---

## 4. Campos novos em DTOs

### `availableToProviderEmployees: boolean`

Novo campo em:

- **`BenefitResponse`** — resposta de `POST /benefits`, `PUT /benefits/{id}`, etc.
- **`CreateBenefitRequest`** — request de `POST /benefits` (opcional, default `false`)
- **`UpdateBenefitRequest`** — request de `PUT /benefits/{id}` (opcional; se `null`, não altera)

**Significado:** quando `true`, os **funcionários do próprio provider** também podem resgatar o benefício (além das empresas com parceria ativa). Default: `false`.

---

## 5. Nova regra de elegibilidade (`BenefitAccessPolicy`)

Um funcionário pode resgatar um benefício **somente se todas** as condições forem verdadeiras **no momento da emissão e no consumo do token**:

1. Funcionário com conta **ativa** (`AccessStatusGuard`);
2. Benefício **operacional** na data atual (`validFrom`/`validUntil` + ativo);
3. **Uma** das condições:
   - Existe **parceria ativa** (`Partnership` status `ACTIVE`) entre a empresa do funcionário e o benefício; **ou**
   - O benefício é do **próprio provider** do funcionário **e** `availableToProviderEmployees = true`.

**Consequência prática:** se uma parceria for desativada, o benefício deixa de aparecer em `GET /benefits/me` e os tokens em aberto falham no consumo.

---

## 6. Mudanças de comportamento internas (sem quebra de contrato)

### Parcerias (`PUT /partnerships/{id}/status` — transições mais rígidas)
- Transições válidas: `PENDING → ACTIVE`, `PENDING → REJECTED`, `ACTIVE → DISABLED`.
- Qualquer outra transição agora lança erro (`Only a pending partnership can be reviewed` / `Only an active partnership can be disabled`). Antes transições inválidas podiam passar silenciosamente em alguns casos.

### Resgate pelo provider (`POST /redemptions/provider/preview` e `/provider/consume`)
- Contrato de request/response **inalterado** (`RedemptionTokenResponse`, `RedemptionPreviewResponse`, `RedemptionResponse`).
- Internamente o token agora referencia `employee` + `benefit` (não mais subscription); a elegibilidade é revalidada no consumo — parceria desativada após emissão do token **bloqueia** o resgate.

### Desativação de empresa (`DELETE`/deactivate company)
- Comportamento equivalente, mas benefícios agora são desativados via query no repositório (sem tocar em `BenefitAccessRequest`, que não existe mais).

### Migração de banco `V13__derive_employee_benefit_access.sql`
- `redemption_tokens` e `benefit_redemptions` agora referenciam `employee_id`/`benefit_id` diretamente (coluna `subscription_id` removida);
- `benefit_redemptions` ganhou `beneficiary_company_id`;
- `benefits` ganhou `available_to_provider_employees`;
- **Todos os tokens ativos antigos foram revogados** (`status = 'REVOKED'`);
- Tabelas `subscriptions` e `benefit_access_requests` dropadas.

---

## 7. Fixes já commitados (últimos 3 commits)

| Commit | Fix | Impacto |
|--------|-----|---------|
| `617252e` | `LazyInitializationException` na busca — usa projection | Interno; corrige 500 na busca |
| `d3b65d6` | Query de busca não selecionava `providerName` | **`providerName` agora preenchido** em `GET /benefits/public/search` |
| `975b745` | Ajuste fino da mesma query | Interno |
| `9174e16` | Testcontainers + busca por similaridade | Infra de testes; sem impacto de API |

---

## 8. Checklist de migração do frontend

- [ ] Remover todas as chamadas a `/subscriptions`, `/benefit-requests/*` e `/shared-benefits/*`
- [ ] Substituir listagem de benefícios do funcionário por `GET /benefits/me`
- [ ] Trocar emissão de token para `POST /redemptions/benefits/{benefitId}/token` (usar `benefitId` de `/benefits/me`)
- [ ] Tratar erro de elegibilidade (403) na emissão/consumo — pode acontecer se a parceria for desativada
- [ ] Remover telas de solicitação/aprovação de acesso (fluxo eliminado)
- [ ] Adicionar campo `availableToProviderEmployees` nos formulários de criar/editar benefício (MANAGER) e exibi-lo onde `BenefitResponse` é usado
- [ ] Refletir que tokens antigos foram revogados: funcionários devem emitir novo token (tokens expiram em 3 min; 1 ativo por benefício)
- [ ] Opcional: tratar `providerName` na busca (agora sempre populado)
