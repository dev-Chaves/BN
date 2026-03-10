# Benefix – Copilot Instructions

## Project Overview

**Benefix** is a B2B SaaS platform that centralizes the management and discovery of corporate benefits — connecting provider companies (who offer benefits like gym passes, language courses, dental plans) with client companies (who contract those benefits for their employees). The main differentiator is a **Benefits Marketplace (Vitrine)** where companies can find and request partnerships directly.

## Tech Stack

- Java 21 · Spring Boot 4.0.1 · Spring MVC · Spring Data JPA · Spring Security
- PostgreSQL (schema managed via **Flyway** versioned migrations; `ddl-auto: validate` in all profiles)
- Lombok · auth0 `java-jwt` 4.4.0 · springdoc-openapi (Swagger UI at `/docs`)
- Testcontainers + PostgreSQL for integration tests
- MongoDB Atlas (planned) for ChatBot message history

## Build & Run

```bash
# Build and package
./mvnw clean install

# Run locally (requires PostgreSQL on localhost:5432)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run via Docker (app + postgres)
docker-compose up

# GraalVM Native build (planned — only after project stabilises + Flyway added)
./mvnw -Pnative native:compile
```

Set `JWT_SECRET` env var in production (defaults to `my-secret-key` in dev).

## Architecture

### Package-by-Feature

Code is organised by domain feature under `com.bn.benefix.{feature}`:

```
com.bn.benefix/
├── account/         ← auth identity entity
├── auth/            ← login, JWT
├── benefit/         ← benefits offered by companies
├── company/         ← companies (providers and clients)
├── employee/        ← employees
├── manager/         ← company managers/HR
├── onboarding/      ← public registration (company + first manager)
├── partnership/     ← B2B partnerships
├── subscription/    ← employee enrollment in benefits
├── checkin/         ← (planned) QR Code / benefit usage validation
├── shared/
│   ├── domain/      ← value objects: CNPJ, CPF
│   └── enums/       ← Role, EmployeeStatus, PartnershipStatus
└── infra/
    ├── config/      ← DataInitializer (seed)
    ├── exception/   ← GlobalExceptionHandler (@ControllerAdvice)
    └── security/    ← SecurityConfig, SecurityFilter, TokenService
```

Each feature contains: entity, repository, service, controller, and a `dto/` subpackage.  
Cross-cutting concerns live in `infra/` (security, exception handling, config) and `shared/` (value objects, enums).

### Domain Model

| Entity | Role |
|--------|------|
| `Company` | Provider or client. Central entity. |
| `Benefit` | A service offered by a provider `Company`. Starts **inactive** (`active = false`). |
| `Partnership` | B2B contract between a client `Company` and a `Benefit`. Starts as `PENDING`. |
| `Employee` | End-user who enjoys the benefits, belongs to a `Company`. |
| `Manager` | Admin of a `Company`, linked 1-to-1 with an `Account`. |
| `Account` | Auth identity (email + hashed password + `Role`). |
| `Subscription` | Employee enrollment in a benefit (requires active `Partnership`). |
| `CheckIn` | *(planned)* Single benefit usage event. Generated when employee uses a benefit; validated by the provider. |

**Entity relationship map:**
```
Company ──< Employee >── Account
   │    ──< Manager  >── Account
   │
   └──< Benefit (provider) >──< Subscription >── Employee
                │
   Partnership (clientCompany → Benefit)
                │
   CheckIn (Subscription → usage event)   ← planned
```

### Enums

| Enum | Values |
|------|--------|
| `Role` | `ADMIN`, `MANAGER`, `USER`, `BOT` *(planned)* |
| `EmployeeStatus` | `ACTIVE`, `DISABLE` |
| `PartnershipStatus` | `PENDING`, `ACTIVE`, `DISABLE`, `REJECTED` |
| `CheckInStatus` | `PENDING`, `CONFIRMED`, `EXPIRED`, `CANCELLED` *(planned)* |

> Spring Security automatically prefixes `ROLE_`. The enum does **not** include the prefix. Use `hasRole('MANAGER')` in annotations.

### Security

Stateless JWT auth (HMAC256, 2-hour expiry, `-03:00` offset).  
Token extracted from the `Authorization` header by `SecurityFilter`.  
`@EnableMethodSecurity` is active for method-level role checks.  
Public endpoints: `POST /auth/login`, `POST /onboarding`.  
`POST /companies` requires `ROLE_ADMIN`; everything else requires authentication.

**Endpoints restricted by role:**

| Role | Endpoints |
|------|-----------|
| `MANAGER` | `POST/PUT/DELETE /benefit`, `POST/PATCH/PUT/DELETE /partnership` |
| `ADMIN` | `POST /companies` |
| `BOT` *(planned)* | `GET /api/mcp/**` |

## Critical Business Rules

These rules must be respected when generating any code related to these domains:

- **`Benefit` starts inactive:** `active = false` set in `@PrePersist`. A Manager must explicitly call `activeBenefit()` to make it visible in the marketplace.
- **`Partnership` status workflow:**  
  `PENDING → ACTIVE` (provider approves) or `PENDING → REJECTED`  
  `ACTIVE → DISABLE` (deactivated)  
  A `Partnership` is always created with status `PENDING`.
- **`Subscription` requires active `Partnership`:** Before creating a `Subscription`, validate that there is an `ACTIVE` `Partnership` between the employee's company and the benefit.
- **Manager authorization:** A Manager can only create/edit/delete resources (benefits, partnerships) that belong to their own company. Always validate via `validateManagerAuthorization(accountId, companyId)`.
- **`Benefit.category`** *(planned):* Enum for benefit category (e.g. `HEALTH`, `EDUCATION`, `LEISURE`, `FOOD`). Will be used for marketplace filters.
- **Contracts have expiry dates** *(planned):* `Partnership` will gain a `validUntil` date. Expired partnerships auto-transition to `DISABLE`.

## Key Conventions

### Entity Builder Pattern

All entities use a **static inner `Builder` class** with required fields as constructor parameters. Direct construction and public setters are forbidden:

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PRIVATE)
public class Company {
    public static Builder builder(String name, CNPJ cnpj) { ... }

    public static class Builder {
        public Builder(String name, CNPJ cnpj) { ... }
        public Company build() { return new Company(this); }
    }
}
```

### Bidirectional Relationship Management

Builders and private helper methods maintain both sides of a relationship:

```java
private void addEmployee(Employee val) {
    this.employees.add(val);
    val.defineCompany(this); // sets the back-reference
}
```

### Soft Delete

Entities are never physically deleted. Use domain methods: `deactivateCompany()`, `deactivateManager()`, `deactivateEmployee()`, `deactivateBenefit()`, etc.

### Value Objects

`CPF` and `CNPJ` are `@Embeddable` value objects in `shared/domain/`. Always create via their static factory:

```java
CNPJ.of("06990590000123")  // 14 raw digits, validated with check-digit algorithm
CPF.of("52998224725")      // 11 raw digits, validated with check-digit algorithm
```

### DTOs

All DTOs are Java **records**, located in the feature's `dto/` subpackage.  
Naming: `{Entity}CreationRequestDTO`, `{Entity}CreationResponseDTO`, `{Entity}UpdateRequestDTO`.

### Authorization Pattern

Write operations validate that the authenticated manager belongs to the same company as the resource:

```java
private void validateManagerAuthorization(UUID accountId, Long companyId) {
    Manager manager = managerRepository.findByAccountId(accountId)
        .orElseThrow(...);
    if (!manager.getCompany().getId().equals(companyId))
        throw new SecurityException("...");
}
```

### Exception Handling

`GlobalExceptionHandler` maps exceptions to HTTP responses:
- `EntityNotFoundException` → 404
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400 with per-field error map
- Uncaught `Exception` → 500

All error responses follow this JSON format:
```json
{ "message": "...", "status": 404, "timestamp": "..." }
```
Never let a stacktrace leak to the client.

### `@PrePersist`

Entities set `createdAt = LocalDateTime.now()` and default boolean flags in `@PrePersist`.  
**Important:** `Benefit.active` defaults to `false` (not `true`) — it must be explicitly activated.

### Dev Data Seeding

`DataInitializer` runs at startup (skipped on `test` profile) and seeds companies, managers, employees, and partnerships when the DB is empty. Default credentials are logged to stdout.

### Onboarding Flow

`POST /onboarding` atomically creates a `Company` + its first `Manager` in a single `@Transactional` operation via `OnboardingService`.

## API REST — Summary

| Domain | Main Endpoints |
|--------|----------------|
| Auth | `POST /auth/login` |
| Onboarding | `POST /onboarding` (company + first manager, public) |
| Company | CRUD at `/company` |
| Employee | CRUD at `/employee` |
| Manager | CRUD at `/manager` |
| Benefit | CRUD at `/benefit`; `GET /benefit` = marketplace listing |
| Partnership | CRUD + `PATCH /partnership/{id}/accept`; planned: `/reject`, `/disable` |
| Subscription | `POST`, `GET` at `/subscription` |
| CheckIn *(planned)* | `POST /checkin/start`, `GET /checkin/validate/{token}`, `POST /checkin/confirm/{token}` |
| MCP/ChatBot *(planned)* | `GET /api/mcp/company/{id}/context`, `GET /api/mcp/employee/{id}/benefits` |

Swagger UI: `http://localhost:8080/docs`

## CheckIn / QR Code (Planned Feature)

When an employee wants to use a benefit in person, the system generates a time-limited token — displayed as a QR Code on the frontend — which the provider's attendant scans to confirm usage.

### CheckIn entity (planned)

```
CheckIn {
  id             Long
  subscription   → Subscription
  benefit        → Benefit
  employee       → Employee
  validationToken  String (UUID — stored in DB, used as QR Code payload)
  qrCodeToken      String (short-lived)
  status           CheckInStatus
  expiresAt        LocalDateTime
  checkedInAt      LocalDateTime
  confirmedAt      LocalDateTime? (null until provider confirms)
  confirmedBy      String?        (optional attendant identifier)
}
```

**Status flow:** `PENDING → CONFIRMED` (attendant confirms) or `PENDING → EXPIRED` (time runs out)

### QR Code architecture — token in DB, image on client

The QR Code image is **never stored**. The backend stores only the UUID token. The frontend generates the image from a URL:

```
https://benefix.com/checkin/validate/{uuid}
```

**Flow:**
1. `POST /checkin/start` → server creates `CheckIn(status=PENDING, token=UUID, expiresAt=now+5min)` → returns `{ token, expiresAt }`
2. Frontend generates QR Code image from the URL (zero server calls)
3. Attendant scans → `GET /checkin/validate/{token}` → returns employee name, company, benefit, validity (public endpoint)
4. Attendant clicks Confirm → `POST /checkin/confirm/{token}` → `status = CONFIRMED`

> Security: short expiry (5–15 min) + single-use (token expires after confirmation). UUID in DB allows instant invalidation unlike JWT.

## ChatBot / MCP Integration (Planned)

An external chatbot service (Rômulo) will consume Benefix's API using a dedicated `ROLE_BOT` token. The LLM stack uses **GROQ** (free tier). Conversation history is stored in **MongoDB Atlas** (separate from PostgreSQL).

### What Benefix must provide

- `GET /api/mcp/company/{id}/context` — lightweight JSON summary (rules + active benefits) for the LLM
- `GET /api/mcp/employee/{id}/benefits` — list of benefits the employee has access to
- These endpoints are restricted to `ROLE_BOT`
- The chatbot runs as a **separate service** — not part of this monolith
- The chatbot writes **only to MongoDB** (message history), never to PostgreSQL directly

### MongoDB message document (planned)

```json
{
  "accountId": "123",
  "conversationId": "uuid",
  "role": "user | assistant",
  "content": "Quais benefícios eu tenho?",
  "createdAt": "2026-03-06T20:00:00Z",
  "metadata": { "companyId": "42" }
}
```

## Tests

Strategy: **integration tests only**, using **Testcontainers + real PostgreSQL**. Unit tests are not the focus.

Priority flows to cover:
1. Onboarding (company + manager creation)
2. Partnership: creation → approval (`PENDING → ACTIVE`)
3. Employee subscription to a benefit
4. Login and role-based access control

## Technical Plans (Future)

| Item | Description |
|------|-------------|
| **GraalVM Native** | `./mvnw -Pnative native:compile`. Requires: `@RegisterReflectionForBinding` on entities, Flyway in place, verify `auth0/java-jwt` AOT compatibility. |
| **Virtual Threads** | Project Loom — enable with one line in `application.yaml`. Comparable throughput to reactive without rewriting code. |
| **`Benefit.category`** | Add `BenefitCategory` enum (HEALTH, EDUCATION, LEISURE, FOOD) + filter in `GET /benefit`. |
| **Partnership expiry** | Add `validUntil: LocalDate` to `Partnership`; auto-disable on expiry. |
| **Pagination** | Add `Pageable` to all list endpoints. |
