# GEMINI.md - BN API

This project is a reactive REST API for corporate benefits management ("BN"), built with **Quarkus 3.32.2** and **Java 21**. It uses a reactive stack to ensure high performance and scalability.

## Project Overview

- **Purpose:** Management of corporate benefits, companies, employees, managers, partnerships, and subscriptions.
- **Architecture:** Domain-driven structure located in `src/main/java/org/acme/domains`.
    - **Resource:** JAX-RS endpoints handling HTTP requests.
    - **Service:** Business logic implementation.
    - **Repository:** Data access using Hibernate Reactive with Panache.
    - **DTO:** Data transfer objects for API requests and responses.
- **Reactivity:** Heavily uses **Mutiny** (`Uni`, `Multi`) for asynchronous and non-blocking operations.
- **Security:** 
    - **JWT:** Authentication via SmallRye JWT, using a `jwt` cookie for token storage.
    - **Basic Auth:** Specifically for Swagger UI and OpenAPI endpoints in production (`%prod` profile).
- **Persistence:** PostgreSQL (Reactive Client) with **Flyway** for database migrations.
- **Rate Limiting:** Implemented for sensitive endpoints (Auth, Onboarding) using **Bucket4j**.

## Key Technologies

- **Framework:** Quarkus 3.32.2
- **Language:** Java 21
- **Reactive Engine:** Mutiny / Hibernate Reactive
- **Auth:** SmallRye JWT (Cookie-based)
- **Database:** PostgreSQL + Flyway
- **Documentation:** OpenAPI / Swagger UI
- **Testing:** JUnit 5, REST-assured, Mockito

## Building and Running

### Development
```bash
# Start infrastructure (PostgreSQL)
docker compose up -d

# Run in dev mode (hot reload)
./mvnw quarkus:dev
```

### Testing
```bash
# Run all tests
./mvnw test

# Run critical integration tests
./mvnw -Dtest=CriticalIntegrationTest test

# Full pipeline (unit + integration)
./mvnw verify
```

### Production Build
```bash
# Fast-jar build
./mvnw package

# Native executable build
./mvnw package -Dnative
```

## Development Conventions

- **Reactivity:** Always prefer reactive patterns (`Uni<Response>`) over blocking ones. Use `BaseResource` interface for consistent response wrapping.
- **Validation:** Use Jakarta Bean Validation annotations in DTOs.
- **Native Support:** When adding new DTOs, register them in `org.acme.domains.shared.api.NativeJsonReflectionConfig` to ensure proper serialization in native images.
- **Database Migrations:** Add new SQL scripts to `src/main/resources/db/migration` following the Flyway naming convention (`V<N>__description.sql`).
- **Auth Guarding:** Use `Role` enum and Quarkus security annotations (e.g., `@RolesAllowed`) for endpoint protection.
- **Tenant Isolation:** Note the use of `TenantGuard` in `shared/security` (check implementation details if modifying multi-tenant logic).

## Key Files and Locations

- `pom.xml`: Project dependencies and build configuration.
- `src/main/resources/application.yaml`: Central configuration file.
- `src/main/java/org/acme/domains/`: Domain-specific logic.
- `src/main/resources/db/migration/`: Database schema evolution.
- `src/main/java/org/acme/domains/shared/api/NativeJsonReflectionConfig.java`: Native image reflection registry.
