# AGENTS.md

Benefix BN API (`com.bnfix.ubm`, artifact `ubm`). Spring Boot **4.1.0** / Java **25**, packaged as a **GraalVM native image** (`graalvm-community:25`). Single Maven module; no README.

## Commands (always `./mvnw`, never system `mvn`)

- Format: `./mvnw spotless:apply` (palantir-java-format 2.97.0)
- Verify formatting: `./mvnw spotless:check`
- Compile: `./mvnw compile`
- Tests: `./mvnw test` (3 tests, ~10s, H2 in-memory)
- Native build: `./mvnw -B -DskipTests -Pnative native:compile` — slow (minutes), run only when asked

Order matters for submission: `spotless:apply` → `compile` → `test`. `spotless:apply` may rewrite formatting in files you didn't touch if the committed code drifted from 2.97.0; keep those formatting-only changes.

## GraalVM native constraints (critical)

- The production artifact is the native binary (see `Dockerfile` and CI). Everything must be native-compatible.
- Reflection/hints live in `src/main/java/com/bnfix/ubm/shared/nativeimage/NativeRuntimeHints.java` (`RuntimeHintsRegistrar`). **When adding an entity, DTO, or enum, register it there** or it will fail only at native runtime (works in JVM tests).
- Lombok `@Slf4j`/builders are compile-time only — safe in native. No runtime reflection.
- Springdoc/OpenAPI is **disabled by default** (`springdoc.api-docs.enabled=false`); native image doesn't include it unless you add AOT hints.

## Architecture

- Domains under `domains/<domain>/`: `*Controller` (REST) → `*Service` (business + `@Transactional`) → `*Repository` + JPA entities. DTOs in `<domain>/dto/`. Controllers use Spring Security `@PreAuthorize("hasRole(...)")` with roles `ADMIN`/`MANAGER`/`USER`.
- `domains/shared/` = value types (`CPF`, `CNPJ`) + `Role`. `shared/` (repo root) = cross-cutting infra: `api/RateLimitFilter`, `api/RequestLoggingFilter`, `security/*` (JWT, `TenantGuard`, `TenantContext`), `nativeimage/`.
- **Multi-tenancy is security-enforced, not schema-based**: every manager/employee call resolves the tenant from the JWT `companyId` claim, and `TenantGuard`/`AccessStatusGuard` verify the user's company matches the target row. New endpoints that touch company-scoped data must go through `TenantGuard`; don't bypass it.
- Auth: JWT RS256 with RSA keypair **read from PEM files** at runtime (`app.jwt.public-key`/`private-key`, default `/opt/bn/secrets/*.pem`). `AuthService`/`SwitchCompanyService` are `@Profile("!test")`; tests generate their own key via `testJwtKey()`.
- In-process rate limiting via `RateLimitFilter` for `/auth/login`, `/onboarding`, `/redemptions/`.

## Data layer

- Flyway migrations in `src/main/resources/db/migration/` (`V1__...`, `V10__add_usages.sql`). `spring.jpa.hibernate.ddl-auto: validate` + `validate-on-migrate`. **Schema changes need a new numbered `V<n>` migration**, not `ddl-auto: create`.
- `hibernate-maven-plugin` `enhance` goal runs at build (bytecode enhancement) — entities rely on it for dirty checking/immutable fields.
- Local run expects a PostgreSQL at `jdbc:postgresql://localhost:5432/benefix`. Tests use H2 in PostgreSQL mode via `application-test.yaml` (flyway disabled).

## Profiles & config

- `test` — H2, no JWT key files needed, MockMvc, swagger enabled. Used by tests.
- `docs` — enables springdoc at `/q/openapi`, `/q/swagger-ui` (basic-auth protected).
- default — prod-ish config in `application.yaml`.
- `.env`/`.env.example` map env vars; copy `.env.example` → `.env` for local dev. **`src/main/resources/privateKey.pem` and `publicKey.pem` are gitignored** — without them the default profile fails at startup.

## Logging (SLF4J + Lombok `@Slf4j`)

- Mutating ops (create/update/delete, login, redeem, approve/reject, etc.) → `log.info` with entity IDs and actor email; never log passwords, raw tokens, or the full JWT.
- Expected failures → `log.warn`; unhandled exceptions → `GlobalExceptionHandler` generic `Exception` handler logs `log.error` with stack trace (5xx).
- `RequestLoggingFilter` logs every request by outcome: `debug` 2xx, `warn` 4xx, `error` 5xx. **Don't add per-endpoint logs to GET/read endpoints** — the filter already covers them; adding duplicates noise.
- Keep messages in English.

## CI / deploy

- `.github/workflows/deploy.yaml`: on push to `main` → native compile → push `ghcr.io/dev-chaves/bn` → SSH deploy to EC2 (health check `/actuator/health`). Native build has `skipTests`.
- Two nearly identical Dockerfiles: `Dockerfile` (root, used by `docker-compose`) and `src/main/docker/Dockerfile.native-micro` (used by CI). Change both if build steps change.

## Tests

- Only 2 test classes: `UbmApplicationTests` (context loads) and `SwaggerDocsSecurityTest` (MockMvc). Full `./mvnw test` is the complete suite.
- Tests run under `@ActiveProfiles("test")`; no external services needed.
