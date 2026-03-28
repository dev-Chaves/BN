# bn

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Local DEV environment (API + integration tests)

### Prerequisites

- Java 21
- Docker Desktop or OrbStack running
- Port `5432` available

### 1) Start PostgreSQL locally

```shell
docker compose up -d
docker compose ps
```

The project expects:
- database: `benefix`
- user: `postgres`
- password: `password`
- host/port: `localhost:5432`

### 2) Run API in dev mode

```shell
./mvnw quarkus:dev
```

Useful URLs:
- API base: `http://localhost:8080`
- Dev UI: `http://localhost:8080/q/dev`
- Swagger UI: `http://localhost:8080/q/swagger-ui`

The project uses local JWT key files in `src/main/resources`:
- `privateKey.pem` (token signing)
- `publicKey.pem` (token verification)

### 3) Run tests

```shell
# Unit tests
./mvnw test

# Critical integration test
./mvnw -Dtest=CriticalIntegrationTest test

# Full pipeline
./mvnw verify
```

### 4) Quick API smoke (optional)

```shell
curl -i http://localhost:8080/q/health
```

### 5) Test accounts for manual API testing (dev local)

Use these fixed credentials:

- Manager
  - email: `manager.dev@bn.local`
  - password: `manager-pass-123`
- Employee
  - email: `employee.dev@bn.local`
  - password: `employee-pass-123`

Bootstrap them in local dev:

```shell
# 1) Onboard tenant + manager
curl -i -X POST http://localhost:8080/onboarding \
  -H "Content-Type: application/json" \
  -d '{
    "company": {
      "name": "BN Dev Company",
      "cnpj": "10000000700120"
    },
    "manager": {
      "name": "Manager Dev",
      "cpf": "00000700177",
      "email": "manager.dev@bn.local",
      "password": "manager-pass-123"
    }
  }'

# 2) Login manager (token no body + cookie jwt HttpOnly)
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "manager.dev@bn.local",
    "password": "manager-pass-123"
  }'

# 2.1) Opcional: salvar cookie JWT em arquivo para chamadas autenticadas por cookie
curl -i -c cookies.txt -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "manager.dev@bn.local",
    "password": "manager-pass-123"
  }'

# 3) Fetch company id via Authorization header (replace <MANAGER_TOKEN>)
curl -i http://localhost:8080/companies/me \
  -H "Authorization: Bearer <MANAGER_TOKEN>"

# 3.1) Fetch company id via cookie JWT (sem Authorization header)
curl -i -b cookies.txt http://localhost:8080/companies/me

# 4) Create employee (replace <MANAGER_TOKEN> and <COMPANY_ID>)
curl -i -X POST http://localhost:8080/employees \
  -H "Authorization: Bearer <MANAGER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Employee Dev",
    "cpf": "00000700258",
    "email": "employee.dev@bn.local",
    "password": "employee-pass-123",
    "companyId": <COMPANY_ID>
  }'

# 5) Activate employee (replace <MANAGER_TOKEN> and <EMPLOYEE_ID>)
curl -i -X PUT "http://localhost:8080/employees/activate?employeeId=<EMPLOYEE_ID>" \
  -H "Authorization: Bearer <MANAGER_TOKEN>"
```

If records already exist (email/CPF/CNPJ already used), reset local DB:

```shell
docker compose down -v && docker compose up -d
```

### 5) Stop local infra

```shell
docker compose down
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/bn-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Validate object properties (field, getter) and method parameters for your beans (REST, CDI, Jakarta Persistence)
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- SmallRye JWT ([guide](https://quarkus.io/guides/security-jwt)): Secure your applications with JSON Web Token
- Reactive PostgreSQL client ([guide](https://quarkus.io/guides/reactive-sql-clients)): Connect to the PostgreSQL database using the reactive pattern
- SmallRye JWT Build ([guide](https://quarkus.io/guides/security-jwt-build)): Create JSON Web Token with SmallRye JWT Build API

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
