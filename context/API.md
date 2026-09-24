# API REST

Base local: `http://localhost:8080`. Datas e horas são serializadas em ISO-8601. Endpoints paginados pelo Spring aceitam `page`, `size` (máximo global 50) e `sort`. O inventário abaixo reflete os controllers em 23/09/2026.

## Autenticação

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `POST /auth/login` | público | `{email,password}`; perfil no body e cookie JWT |
| `GET /auth/me` | autenticado | perfil da sessão |
| `POST /auth/logout` | autenticado | revoga sessão; `204` |
| `POST /auth/switch-company` | `MANAGER` | `{companyId}`; novo JWT e perfil |

O perfil contém `accountId`, `email`, `role`, `companyId`, `companyName` e `name`.

## Onboarding e empresas

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `POST /onboarding` | público | `{company:{name,cnpj},manager:{name,cpf,email,password}}`; `201` |
| `POST /companies/event/enroll` | público | `{name,cpf,email,password}`; auto-cadastro na empresa do evento; `201` |
| `GET /companies` | `MANAGER` | empresas vinculadas ao gestor |
| `POST /companies` | `MANAGER` | `{name,cnpj}`; cria nova empresa vinculada; `201` |
| `GET /companies/me` | `MANAGER` | empresa ativa |
| `PUT /companies/me` | `MANAGER` | `{name}` |
| `PUT /companies/me/deactivate` | `MANAGER` proprietário | `{password}`; desativa tenant |

## Inscrição de evento

`POST /companies/event/enroll` é público e cria, na empresa configurada em `app.event.company-id` (`EVENT_COMPANY_ID`; `0` desabilita o endpoint), uma conta `USER` e um `Employee` já `ACTIVE`. Não há gestor no fluxo: é o único caminho público de criação de funcionário.

```json
{ "name": "Maria Silva", "cpf": "12345678909", "email": "maria@exemplo.com", "password": "senha-forte-1" }
```

Resposta `201`:

```json
{ "employeeId": 1, "name": "Maria Silva", "companyId": 5, "companyName": "Evento", "status": "ACTIVE" }
```

Erros: `400` para corpo inválido ou CPF inválido (`CPF is invalid`), `404` quando o evento não está configurado ou a empresa está inativa (`Enrollment not available`), `409` para e-mail/CPF já usados (`Email already in use` / `CPF already in use`) e `429` ao exceder o rate limit.

## Gestores e funcionários

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `POST /managers` | `ADMIN` | `{name,cpf,email,password,companyId}`; `201` |
| `GET /managers/me` | `MANAGER` | perfil do gestor no tenant |
| `PUT /managers/me/email` | `MANAGER` | `{email,currentPassword}` |
| `PUT /managers/me/password` | `MANAGER` | `{currentPassword,newPassword}` |
| `POST /employees` | `MANAGER` | `{name,cpf,email,password,companyId}`; `201` |
| `GET /employees` | `MANAGER` | `page`, `size`; funcionários do tenant |
| `PUT /employees/{employeeId}` | `MANAGER` | `{name}` |
| `PUT /employees/activate?employeeId=` | `MANAGER` | ativa funcionário |
| `PUT /employees/disable?employeeId=` | `MANAGER` | desativa funcionário |

## Benefícios e categorias

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `GET /benefits/public` | público | marketplace ativo e público, paginado |
| `GET /benefits/public/search?termo=` | público | busca por similaridade na descrição, paginada |
| `POST /benefits` | `MANAGER` | benefício; `201` |
| `GET /benefits/tenant` | `MANAGER` | próprios; `categoryId` opcional e paginação |
| `GET /benefits/marketplace` | `MANAGER` | benefícios externos; filtro e paginação |
| `PUT /benefits/{id}` | `MANAGER` provedor | atualização parcial |
| `PUT /benefits/{id}/activate` | `MANAGER` provedor | ativa |
| `PUT /benefits/{id}/deactivate` | `MANAGER` provedor | desativa |
| `DELETE /benefits/{id}` | `MANAGER` provedor | remove; `204` |
| `GET /categories` | `MANAGER` | catálogo fixo |

Criação de benefício:

```json
{
  "name": "Academia",
  "description": "Acesso mensal às unidades",
  "companyId": 1,
  "categoryIds": [6],
  "publiclyVisible": true,
  "validFrom": "2026-08-01T00:00:00",
  "validUntil": "2026-12-31T23:59:59",
  "maxUsesPerUser": 4,
  "terms": "Apresente o token no atendimento."
}
```

`UpdateBenefitRequest` aceita os mesmos campos, exceto `companyId`; campos omitidos permanecem inalterados.

## Parcerias

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `POST /partnerships` | `MANAGER` cliente | `{benefitId}`; cria `PENDING` |
| `GET /partnerships/provider/pending` | `MANAGER` provedor | pendências do tenant |
| `PUT /partnerships/accept?partnershipId=` | `MANAGER` provedor | `PENDING → ACTIVE` |
| `PUT /partnerships/reject?partnershipId=` | `MANAGER` provedor | `PENDING → REJECTED` |
| `PUT /partnerships/disable?partnershipId=` | `MANAGER` provedor | `ACTIVE → DISABLED` |

## Experiência do funcionário

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `GET /benefits/me` | `USER` | benefícios elegíveis para resgate, derivados das parcerias ativas da empresa |

O acesso do funcionário é derivado automaticamente das parcerias ativas da sua empresa — não há assinatura nem solicitação/aprovação individual. Benefícios do próprio provedor entram apenas quando `availableToProviderEmployees = true`.

## Resgates

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `POST /redemptions/benefits/{benefitId}/token` | `USER` titular | token e URL; `201` |
| `POST /redemptions/provider/preview` | `MANAGER` provedor | `{token}`; valida sem consumir |
| `POST /redemptions/provider/consume` | `MANAGER` provedor | `{token}`; registra uso |

O token dura `REDEMPTION_TOKEN_TTL_MINUTES` (padrão três minutos), só há um token ativo por par (funcionário, benefício) — emitir novamente revoga o anterior — e apenas a empresa provedora pode consumi-lo. O limite `maxUsesPerUser` é verificado no consumo.

## Comunicados

| Método e path | Acesso | Entrada / resultado |
|---|---|---|
| `POST /announcements` | `MANAGER` | `{title,content}`; publica para funcionários ativos; `201` |
| `GET /announcements/company` | `MANAGER` | `page`, `size`; histórico do tenant |
| `GET /announcements/me` | `USER` | `page`, `size`; comunicados recebidos |
| `GET /announcements/me/unread-count` | `USER` | contador |
| `PUT /announcements/{id}/read` | `USER` | marca um como lido |
| `PUT /announcements/me/read-all` | `USER` | marca todos como lidos |

As páginas de comunicado usam `{items,page,size,hasMore}`.

## Erros, rate limit e docs

Erros são normalizados por `GlobalExceptionHandler` em `ApiError`; validação tende a `400`, autenticação a `401`, autorização/tenant a `403`, ausência a `404`, conflitos de estado a `409` conforme o caso e falhas inesperadas a `500`.

Rate limit excedido retorna `429` em login, onboarding, inscrição de evento ou resgates. A documentação executável está em `/q/openapi` e `/q/swagger-ui` somente no profile `docs`, com Basic Auth.
