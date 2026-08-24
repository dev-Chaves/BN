# Sprint Plan — Benefix (BN)

> **Documento histórico.** O estado corrente e as prioridades revisadas estão em [Roadmap](Roadmap.md); vários itens abaixo já foram implementados de outra forma.

> Voltar ao rascunho principal: [[Benefix]]
> Estado atual: **CRUDs prontos · JWT funcional · Onboarding implementado · Flyway + Migrations ✅**

---

## 📌 Visão Geral do Kanban

```
DONE                          | BACKLOG (sprints abaixo)
──────────────────────────────|────────────────────────
✅ CRUDs (todos os domínios)  | Sprint 1 → Sprint 7
✅ JWT Auth                   |
✅ Onboarding público         |
✅ GlobalExceptionHandler     |
✅ Flyway + Migrations        |
```

---

## 🔑 Ordem de Execução (dependências)

```
Sprint 1 (Segurança)
    ↓
Sprint 2 (Vitrine/Filtros)    Sprint 3 (Partnership)
                                    ↓
                              Sprint 4 (Subscription)
                                    ↓
                              Sprint 5 (Testes)
                                    ↓
                              Sprint 6 (Melhorias Técnicas)
                                    ↓
                              Sprint 7 (ChatBot/MCP)
```

---

## 🏃 Sprint 1 — Segurança & Autorização

> **Objetivo:** Nenhuma operação sensível sem controle de acesso. Fundação obrigatória antes de ir a produção.

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S1-01 | `@PreAuthorize` em Company | Adicionar `@PreAuthorize("hasRole('ADMIN')")` no `POST /company`; revisar demais endpoints | Criar empresa sem role ADMIN retorna **403** |
| S1-02 | `@PreAuthorize` em Employee | `POST`, `PUT`, `DELETE` em `/employee` exigem `MANAGER` | Funcionário (USER) recebe **403** ao tentar criar/editar/deletar |
| S1-03 | `@PreAuthorize` em Manager | `POST`, `PUT`, `DELETE` em `/manager` exigem `MANAGER` ou `ADMIN` | USER recebe **403** |
| S1-04 | Ownership: Manager × Benefit | `PUT /benefit/{id}` e `DELETE /benefit/{id}`: validar se benefício pertence à empresa do Manager autenticado | Manager de empresa A não edita benefício de empresa B (**403**) |
| S1-05 | Ownership: Manager × Partnership | `PATCH /partnership/{id}/accept`: validar que Manager é da empresa **fornecedora** | Manager cliente não aprova a própria solicitação (**403**) |
| S1-06 | Restringir CORS | Trocar `allowedOrigins("*")` por lista de origens específicas (`localhost` + URL de produção futura) | Requisição de origem não listada retorna erro CORS |

---

## 🏃 Sprint 2 — Vitrine de Benefícios

> **Objetivo:** Managers encontram benefícios por categoria, nome e empresa fornecedora.

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S2-01 | Enum `BenefitCategory` | Criar enum com valores: `SAUDE`, `EDUCACAO`, `LAZER`, `ALIMENTACAO`, `OUTROS`; adicionar campo `category` em `Benefit` + migration Flyway | Campo persiste e aparece na resposta da API |
| S2-02 | Filtro por categoria | `GET /benefit?category=SAUDE` usando Specification ou JPQL dinâmico | Retorna apenas benefícios ativos da categoria informada |
| S2-03 | Filtro textual | `GET /benefit?q=dental` busca por `ILIKE` em nome e descrição | Retorna benefícios cujo nome ou descrição contém o termo |
| S2-04 | Filtro por empresa | `GET /benefit?companyId=X` | Retorna apenas benefícios da empresa especificada |
| S2-05 | Auth da vitrine | Definir e implementar: vitrine (`GET /benefit`) é pública ou exige autenticação? | Comportamento definido, implementado e documentado no Swagger |

---

## 🏃 Sprint 3 — Partnership: Fluxo Completo

> **Objetivo:** Ciclo de vida completo — `PENDING → ACTIVE | REJECTED`, `ACTIVE → DISABLE`.

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S3-01 | `PATCH /partnership/{id}/reject` | Manager (empresa fornecedora) rejeita parceria `PENDING` → `REJECTED` | Tentar rejeitar uma `ACTIVE` retorna **400** |
| S3-02 | `PATCH /partnership/{id}/disable` | Manager desativa parceria `ACTIVE` → `DISABLE` | Tentar desativar `PENDING` retorna **400** |
| S3-03 | Validação de duplicidade | Impedir que a mesma empresa solicite o mesmo benefício duas vezes enquanto há parceria ativa/pendente | Segunda solicitação retorna **409 Conflict** |
| S3-04 | Ownership no reject/disable | Apenas Manager da empresa **fornecedora** pode rejeitar ou desativar | Manager cliente recebe **403** |

---

## 🏃 Sprint 4 — Subscription: Fluxo do Funcionário

> **Objetivo:** Subscription válida só com Partnership ativa; funcionário recebe código de validação.

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S4-01 | Validar Partnership ativa | Antes de criar Subscription, verificar Partnership `ACTIVE` entre empresa do employee e o benefício | Sem partnership ativa retorna **422** com mensagem clara |
| S4-02 | Validar employee pertence à empresa cliente | Employee autenticado deve ser da empresa que tem a Partnership | Employee de empresa A não assina benefício de empresa B (**403**) |
| S4-03 | Impedir Subscription duplicada | Funcionário não pode assinar o mesmo benefício duas vezes enquanto ativo | Segunda tentativa retorna **409 Conflict** |
| S4-04 | Gerar `validationCode` | UUID ou código alfanumérico único gerado ao criar Subscription; persiste no banco via migration | Campo `validationCode` presente na entidade e na resposta da API |
| S4-05 | `GET /subscription/my` | Endpoint para o employee autenticado listar suas próprias subscriptions | Retorna apenas subscriptions do employee logado |

---

## 🏃 Sprint 5 — Testes de Integração

> **Objetivo:** Cobertura dos fluxos críticos com banco real (Testcontainers).

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S5-01 | Setup Testcontainers | Configurar `@SpringBootTest` com PostgreSQL Testcontainer; criar `AbstractIntegrationTest` base | `./mvnw verify` sobe banco real e executa testes |
| S5-02 | Teste: Onboarding | `POST /onboarding` cria empresa + manager + account; login retorna token válido | Fluxo completo passa end-to-end |
| S5-03 | Teste: Parceria (criação → aprovação) | Manager A cria benefício → Manager B solicita → Manager A aprova → status `ACTIVE` | Transições de estado corretas |
| S5-04 | Teste: Subscription | Setup de parceria ativa → employee cria subscription → `validationCode` gerado | Subscription persiste com código no banco |
| S5-05 | Teste: Controle de acesso | USER tenta criar benefício → 403; Manager tenta aprovar parceria da empresa errada → 403 | Regras de segurança funcionam end-to-end |

---

## 🏃 Sprint 6 — Melhorias Técnicas

> **Objetivo:** Qualidade de produção: paginação, auditoria e preparação para deploy.

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S6-01 | Paginação nos GETs | Adicionar `Pageable` em `GET /benefit`, `/partnership`, `/employee`, `/subscription` | `?page=0&size=20` funciona; resposta inclui `totalPages` e `totalElements` |
| S6-02 | Auditoria JPA | `@CreatedBy`, `@CreatedDate`, `@LastModifiedDate` nas entidades principais via Spring Data Auditing | Campos populados automaticamente ao salvar/atualizar |
| S6-03 | `ddl-auto: validate` | Trocar `update` por `validate` no `application.yaml` — Flyway é o dono do schema | App inicia e Hibernate valida sem alterar o banco |
| S6-04 | Melhorar GlobalExceptionHandler | Distinguir `EntityNotFoundException`, `ConstraintViolationException`, `DataIntegrityViolationException` com mensagens úteis | Cada erro retorna status HTTP e mensagem adequados (sem stacktrace) |

---

## 🏃 Sprint 7 — ChatBot / MCP (Rômulo)

> **Objetivo:** Endpoints de contexto para o LLM; histórico de conversas no MongoDB Atlas.

| ID | Card | O que fazer | Critério de aceite |
|----|------|------------|-------------------|
| S7-01 | `GET /api/mcp/company/{id}/context` | JSON resumido: dados da empresa + benefícios ativos + parcerias | Payload estruturado e consumível por LLM |
| S7-02 | `GET /api/mcp/employee/{id}/benefits` | Lista de benefícios ativos do funcionário com `validationCode` | Retorna apenas benefícios com subscription ativa |
| S7-03 | Role `BOT` para MCP | Criar role `BOT` (ou token especial) para o chatbot acessar endpoints MCP | Endpoints MCP recusam acesso sem role/token correto |
| S7-04 | MongoDB — histórico de chat | Adicionar `spring-boot-starter-data-mongodb`; entidade `ChatMessage` com `sessionId`, `role`, `content`, `timestamp` | Histórico persiste no MongoDB Atlas; `POST /chat/{sessionId}/message` funciona |

---

*Ver também: [[Roadmap]] · [[Requisitos]] · [[Segurança]] · [[ChatBot]]*
