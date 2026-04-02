# Copilot Instructions for BN Repository

## Build, test, and run commands

```bash
# Start local infrastructure (PostgreSQL)
docker compose up -d

# Run API in dev mode (hot reload)
./mvnw quarkus:dev

# Unit/Quarkus tests
./mvnw test

# Single test class
./mvnw test -Dtest=CriticalIntegrationTest

# Single test method
./mvnw test -Dtest=CriticalIntegrationTest#shouldOnboardAndLoginManager

# Full pipeline (includes failsafe verify phase)
./mvnw verify

# Build artifacts
./mvnw package
./mvnw package -Dquarkus.package.jar.type=uber-jar
./mvnw package -Dnative
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

There is no dedicated lint task configured in `pom.xml` (no Checkstyle/PMD/Spotless plugin).

## High-level architecture

This is a Quarkus reactive REST API organized by business domain under `org.acme.domains.*` (`account`, `auth`, `benefit`, `company`, `employee`, `manager`, `onboarding`, `partnership`, `subscription`).

Each domain follows a layered flow:

- `*Resource`: HTTP endpoints and role checks (`@RolesAllowed`), usually implementing `BaseResource` for standard status mapping.
- `*Service`: orchestration and business rules with Mutiny `Uni`, tenant checks, and transactional/session boundaries.
- `*Repository`: Panache reactive persistence.
- `dto/`: request/response payloads.

Cross-cutting pieces to understand first:

- `BaseResource` centralizes response wrapping (`toCreated`, `toOk`, `delete`).
- `GlobalExceptionMapper` maps domain/runtime exceptions to a standard `ErrorResponse` JSON payload.
- `TenantGuard` enforces tenant isolation (manager-company, manager-employee, partnership/provider, employee-benefit visibility).

Authentication/authorization model:

- Login is cookie-based JWT (`/auth/login` sets `jwt` HttpOnly cookie with `SameSite=Strict`).
- API expects token from cookie (`mp.jwt.token.header=Cookie`, cookie name `jwt`) rather than `Authorization: Bearer`.
- Role-based access uses JWT groups and `@RolesAllowed`.

Configuration model:

- `application.yaml` holds shared + production-oriented defaults, including `%prod` docs protection.
- `application-dev.yaml` provides local DB/JWT config for development.
- Be careful with `.env` overrides (`QUARKUS_PROFILE` and datasource vars can fully change active profile/config source).

## Key conventions for this codebase

- Prefer constructor injection in resources/services.
- Services are reactive end-to-end: return `Uni<T>`, compose with `flatMap`/`map`, and use `@WithSession` or `@WithTransaction` where appropriate.
- Entities commonly use a builder factory pattern (`Entity.builder(...).<field>(...).build()`) and domain methods (`activate`, `update`, etc.) instead of exposing broad setters.
- Tenant authorization must be enforced in service methods via `TenantGuard` before mutating or exposing tenant-scoped data.
- Error handling should surface meaningful exceptions (`NotFoundException`, `SecurityException`, `IllegalStateException`) and rely on `GlobalExceptionMapper` for HTTP mapping.
- Tests use `@QuarkusTest` + RestAssured; integration-style flows are centralized in `CriticalIntegrationTest`; local DB overrides are provided by `LocalDatabaseTestProfile`.
