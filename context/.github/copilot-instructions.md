# Copilot Instructions — Benefix (BN)

## O que é este repositório

Este é um **vault de documentação** (Obsidian) do projeto Benefix. Não há código-fonte aqui — os arquivos são o planejamento, arquitetura e decisões técnicas do projeto Java/Spring Boot que está em desenvolvimento separado.

Quando o usuário pedir ajuda com código, use esta documentação como contexto para gerar código coerente com as decisões já tomadas.

---

## Visão do Produto

**Benefix** é uma plataforma B2B SaaS de gestão e descoberta de benefícios corporativos:
- **Vitrine:** empresas oferecem benefícios; outras empresas os contratam via parceria
- **Gestão interna:** managers gerenciam funcionários e benefícios da própria empresa
- **Experiência do funcionário:** visualizar benefícios disponíveis e solicitar adesão (Subscription)
- **ChatBot futuro (Rômulo):** consulta em linguagem natural via API, usando GROQ + MongoDB para histórico no MongoDB Atlas

---

## Stack Técnica

| Camada | Tecnologia |
|---|---|
| Framework | Spring Boot 4 |
| Linguagem | Java |
| Banco principal | PostgreSQL (porta 5432, banco `benefix`) |
| ORM | Spring Data JPA / Hibernate (`ddl-auto: update` em dev) |
| Autenticação | JWT stateless — biblioteca `auth0/java-jwt` |
| Segurança | Spring Security 6 |
| Validação | Jakarta Validation (Bean Validation) |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI — Swagger UI em `/docs` |
| Testes | Testcontainers + PostgreSQL real (integração, não unitários) |
| ChatBot (futuro) | MongoDB Atlas para histórico de mensagens |

---

## Comandos do Projeto (código-fonte)

```bash
# Rodar em desenvolvimento
./mvnw spring-boot:run

# Build + testes de integração
./mvnw verify

# Build nativo (GraalVM) — só após estabilizar
./mvnw -Pnative native:compile
```

---

## Arquitetura

### Package by Feature

```
com.bn.benefix/
├── account/         ← entidade de autenticação
├── auth/            ← login, JWT
├── benefit/         ← benefícios
├── company/         ← empresas
├── employee/        ← funcionários
├── manager/         ← gestores
├── onboarding/      ← registro público (empresa + primeiro manager)
├── partnership/     ← parcerias B2B
├── subscription/    ← adesão de funcionários a benefícios
├── shared/
│   ├── domain/      ← value objects: CNPJ, CPF
│   └── enums/       ← Role
└── infra/
    ├── config/      ← DataInitializer (seed)
    ├── exception/   ← GlobalExceptionHandler (@ControllerAdvice)
    └── security/    ← SecurityConfig, SecurityFilter, TokenService
```

Cada pacote de domínio tem sua própria camada de controller, service, repository e DTOs. Nunca misture lógica de um domínio no pacote de outro.

### Influência de DDD

- **Construtores privados + Builder** em todas as entidades — nunca instanciar diretamente
- **Sem setters públicos** — Lombok `@Setter(AccessLevel.PRIVATE)`. Mutação só via métodos de domínio
- **Métodos de domínio** nas entidades: `activeBenefit()`, `deactivateBenefit()`, `deactivateCompany()`, `update(...)`
- **Value Objects:** `CNPJ` (embedded em Company) e `CPF` (preparado para uso futuro)

---

## Entidades e Relacionamentos

```
Company ──< Employee >── Account
   │    ──< Manager  >── Account
   │
   └──< Benefit (provider) >──< Subscription >── Employee
                │
   Partnership (clientCompany → Benefit)
```

### Regras de negócio críticas

- `Benefit` nasce **inativo** (`active = false`). Precisa de ativação explícita pelo Manager via `activeBenefit()`
- `Partnership` nasce com status **`PENDING`**. Fluxo: `PENDING → ACTIVE | REJECTED`, `ACTIVE → DISABLE`
- `Account` é a entidade de autenticação — toda pessoa (Employee ou Manager) tem uma. Role enum: `ADMIN`, `MANAGER`, `USER`
- `Subscription` só faz sentido quando existe uma `Partnership` ativa entre a empresa do funcionário e o benefício

### Enums importantes

| Enum | Valores |
|---|---|
| `Role` | `ADMIN`, `MANAGER`, `USER` |
| `EmployeeStatus` | `ACTIVE`, `DISABLE` |
| `PartnershipStatus` | `PENDING`, `ACTIVE`, `DISABLE`, `REJECTED` |

> Spring Security adiciona `ROLE_` automaticamente. O enum **não** tem o prefixo. Use `hasRole('MANAGER')` nas anotações.

---

## Segurança

- **JWT stateless** — `SecurityFilter` (`OncePerRequestFilter`) intercepta toda requisição, valida token e popula `SecurityContextHolder`
- `TokenService`: gera token com `subject = account.email`, assina com `${JWT_SECRET}` (obrigatório via env var)
- `AuthorizationService` implementa `UserDetailsService` e carrega `AccountUserDetails` pelo email
- **CSRF desabilitado** (stateless não precisa)
- `SessionCreationPolicy.STATELESS`

### Endpoints públicos
- `POST /auth/login`
- `POST /onboarding`

### Endpoints restritos por role
- `MANAGER`: `POST/PUT/DELETE /benefit`, `POST/PATCH/PUT/DELETE /partnership`
- `ADMIN`: `POST /companies`

---

## Padrão de Resposta de Erro

Todas as exceções são tratadas pelo `GlobalExceptionHandler` (`@ControllerAdvice`). Respostas de erro sempre retornam JSON com:
```json
{ "message": "...", "status": 404, "timestamp": "..." }
```
Nunca deixar stacktrace vazar para o cliente.

---

## Configuração (`application.yaml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/benefix
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update  # ⚠️ trocar por validate + Flyway antes de produção

api:
  security:
    token:
      secret: ${JWT_SECRET:my-secret-key}  # ⚠️ sempre via env var em prod

springdoc:
  swagger-ui:
    path: /docs
```

---

## API REST — Resumo

| Domínio | Endpoints principais |
|---|---|
| Auth | `POST /auth/login` |
| Onboarding | `POST /onboarding` (empresa + primeiro manager) |
| Company | CRUD em `/company` |
| Employee | CRUD em `/employee` |
| Manager | CRUD em `/manager` |
| Benefit | CRUD em `/benefit` (vitrine: `GET /benefit` lista todos ativos) |
| Partnership | CRUD + `PATCH /partnership/{id}/accept` |
| Subscription | `POST`, `GET` em `/subscription` |
| MCP/ChatBot (futuro) | `GET /api/mcp/company/{id}/context`, `GET /api/mcp/employee/{id}/benefits` |

Swagger completo em `http://localhost:8080/docs`.

---

## Testes

A estratégia é **testes de integração** com Testcontainers + PostgreSQL real — não testes unitários de lógica isolada. Fluxos prioritários a cobrir:
1. Onboarding (criação empresa + manager)
2. Parceria: criação → aprovação
3. Subscription de funcionário
4. Login e controle de acesso por role

---

## Roadmap (próximos passos priorizados)

1. **Segurança:** adicionar `@PreAuthorize` nos endpoints de Company/Employee/Manager; garantir que Manager só edita recursos da própria empresa
2. **Vitrine:** campo `category` em Benefit; filtros em `GET /benefit`
3. **Partnership:** endpoints `reject` e `disable`; validação de duplicidade
4. **Subscription:** validar se Partnership está ativa antes de criar; geração de QR Code/código individual
5. **ChatBot/MCP:** endpoints de contexto com `ROLE_BOT`; MongoDB Atlas para histórico
6. **Testes de integração** com Testcontainers
7. **Flyway** (antes de produção; obrigatório antes de GraalVM Native)
