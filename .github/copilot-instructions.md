# Copilot Instructions for BN Repository

## Project Overview
This is a **Quarkus-based REST API** (v3.32.2) for a benefits management system. The application uses reactive programming with Panache ORM, PostgreSQL, JWT-based authentication, and Flyway migrations.

## Build & Test Commands

### Run the application in dev mode
```bash
./mvnw quarkus:dev
```
- Enables live coding (hot reload)
- Dev UI available at http://localhost:8080/q/dev/
- Requires PostgreSQL running on localhost:5432

### Run tests
```bash
# Run all unit and integration tests
./mvnw verify

# Run only unit tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=GreetingResourceTest

# Run a specific test method
./mvnw test -Dtest=GreetingResourceTest#testHelloEndpoint
```

### Build application
```bash
# Standard JAR build
./mvnw package

# Generates quarkus-run.jar in target/quarkus-app/

# Über-JAR build (all dependencies included)
./mvnw package -Dquarkus.package.jar.type=uber-jar

# Native executable build (requires GraalVM)
./mvnw package -Dnative

# Native build in container (no GraalVM required)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Architecture

### Domain-Driven Design
The application follows a **domain-driven structure** with each business domain in its own package under `src/main/java/org/acme/domains/`:

- **Domains**: account, auth, benefit, company, employee, manager, onboarding, partnership, subscription
- Each domain contains:
  - **Entity** (extends `PanacheEntity`) - JPA entity with Panache builder pattern
  - **Repository** (implements Panache reactive repository) - Database queries
  - **Service** (annotated with `@ApplicationScoped`) - Business logic, returns `Uni<T>`
  - **Resource** (REST endpoint) - Implements `BaseResource` interface
  - **DTOs** - Separated in `dto/` package for requests/responses

### Reactive Programming
- Uses **Smallrye Mutiny** (`Uni<T>`) throughout the stack
- All I/O operations are non-blocking (database, HTTP)
- Services return `Uni<T>` which are transformed by Resources
- No blocking operations in service/repository layers

### REST API Pattern
- Resources implement `BaseResource` interface for consistent response handling
- Base methods: `toCreated()` (201), `toOk()` (200), `delete()` (204)
- Dependency injection via constructor
- Security via JWT with `@JsonWebToken` and `@RolesAllowed` annotations

### Database & Migrations
- **Reactive PostgreSQL client** for async connections
- **Hibernate Reactive + Panache** for ORM
- **Flyway** for schema migrations (`src/main/resources/db/migration/`)
- Migrations run automatically on startup (`migrate-at-start: true`)
- Entities use builder pattern for immutability

## Key Conventions

### Entity Builder Pattern
```java
public static Builder builder(String name, Company provider) {
    return new Builder(name, provider);
}
private Entity(Builder builder) { /* set fields */ }
```

### Service Layer Pattern
```java
@ApplicationScoped
public class MyService {
    // Constructor injection of repositories
    public MyService(DependencyRepository repo) { ... }
    
    // Methods return Uni<DTO>
    public Uni<ResponseDTO> doSomething(RequestDTO req, String email) {
        return repository.findBy...()
            .onItem().ifNull().failWith(new NotFoundException(...))
            .flatMap(result -> /* chain operations */)
            .call(repository::persist) // execute side-effects
            .onItem().transform(entity -> new ResponseDTO(...));
    }
}
```

### Resource Pattern
```java
@ApplicationScoped
@Path("/benefits")
public class BenefitResource implements BaseResource {
    private final BenefitService service;
    private final JsonWebToken jwt;
    
    public BenefitResource(BenefitService service, JsonWebToken jwt) { ... }
    
    @POST
    @RolesAllowed("MANAGER")
    public Uni<Response> create(CreateRequest request) {
        return toCreated(service.doSomething(request, jwt.getName()));
    }
}
```

### Configuration
- Application properties in `src/main/resources/application.yaml`
- JWT enabled by default (`smallrye-jwt.enabled: true`)
- Swagger UI always included (`:always-include: true`)
- Hibernate SQL logging enabled in dev

## Testing
- Unit tests use `@QuarkusTest` annotation
- REST tests use `RestAssured` library
- Integration tests follow `*IT.java` naming convention
- Tests automatically get dev database (PostgreSQL must be running)

## Common Tasks

### Adding a new domain
1. Create folder: `src/main/java/org/acme/domains/{domain}/`
2. Create Entity extending `PanacheEntity` with builder pattern
3. Create Repository with reactive methods
4. Create Service with business logic (return `Uni<T>`)
5. Create Resource implementing `BaseResource`
6. Add DTOs in `dto/` subfolder
7. Create migrations in `src/main/resources/db/migration/`

### Adding an endpoint
1. Add method to Resource
2. Use `@GET`, `@POST`, `@PUT`, `@DELETE` annotations
3. Protect with `@RolesAllowed("ROLE")` if needed
4. Return `Uni<Response>` using `toCreated()`, `toOk()`, or `delete()`
5. Call service method with JWT principal: `jwt.getName()`

### Debugging reactive chains
- Use `.log()` on Uni for detailed tracing
- Check Quarkus Dev UI at http://localhost:8080/q/dev/
- Review Flyway migrations if database issues occur
