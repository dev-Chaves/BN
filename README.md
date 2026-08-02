# BN API

API REST reativa para gestão de benefícios corporativos, construída com Quarkus.

## Stack e versões

- Java 21
- Quarkus 3.32.2
- Hibernate Reactive + Panache
- PostgreSQL (JDBC + Reactive Client)
- Flyway (migração automática no startup)
- JWT com SmallRye (cookie `jwt`)
- OpenAPI/Swagger UI

## Arquitetura do projeto

Estrutura de domínio em `src/main/java/org/acme/domains`:

- `account`, `auth`, `benefit`, `company`, `employee`, `manager`, `onboarding`, `partnership`, `subscription`
- `shared` para componentes comuns (por exemplo, `BaseResource`)

Padrão arquitetural predominante:

- `Resource`: contrato HTTP (JAX-RS)
- `Service`: regra de negócio
- `Repository`: acesso a dados com Panache reativo
- `dto/`: payloads de entrada e saída

## Pré-requisitos para desenvolvimento local

- Java 21
- Docker Desktop ou OrbStack
- Porta `5432` livre para PostgreSQL

## Configuração de ambiente

O ambiente local está definido em `application-dev.yaml` e `docker-compose.yml`.

Parâmetros locais:

- Banco: `benefix`
- Usuário: `postgres`
- Senha: definida por `POSTGRES_PASSWORD`
- JDBC URL: `jdbc:postgresql://localhost:5432/benefix`
- Reactive URL: `postgresql://localhost:5432/benefix`

JWT:

- Chave privada: `src/main/resources/privateKey.pem`
- Chave pública: `src/main/resources/publicKey.pem`
- Issuer esperado: `bn-api`
- Cookie de autenticação: `jwt`

## Subindo infraestrutura local

```bash
export POSTGRES_PASSWORD='uma-senha-local-forte'
export SWAGGER_BASIC_AUTH_USERNAME='admin-local'
export SWAGGER_BASIC_AUTH_PASSWORD='uma-senha-local-forte'
docker compose up -d
docker compose ps
```

Parar infraestrutura:

```bash
docker compose down
```

Reset completo do banco local:

```bash
docker compose down -v && docker compose up -d
```

## Executando a aplicação

Modo desenvolvimento com hot reload:

```bash
./mvnw quarkus:dev
```

URLs úteis:

- API base: `http://localhost:8080`
- Quarkus Dev UI: `http://localhost:8080/q/dev`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- OpenAPI: `http://localhost:8080/q/openapi`
- Health: `http://localhost:8080/q/health`

## Fluxo manual de autenticação e onboarding (curl)

### 1) Criar tenant e manager

```bash
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
```

### 2) Login manager (token no body + cookie HttpOnly)

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "manager.dev@bn.local",
    "password": "manager-pass-123"
  }'
```

Salvar cookie para chamadas autenticadas:

```bash
curl -i -c cookies.txt -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "manager.dev@bn.local",
    "password": "manager-pass-123"
  }'
```

### 3) Buscar dados da empresa do manager (via cookie JWT)

```bash
curl -i -b cookies.txt http://localhost:8080/companies/me
```

### 4) Criar funcionário

```bash
curl -i -X POST http://localhost:8080/employees \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Employee Dev",
    "cpf": "00000700258",
    "email": "employee.dev@bn.local",
    "password": "employee-pass-123",
    "companyId": <COMPANY_ID>
  }'
```

### 5) Ativar funcionário

```bash
curl -i -X PUT "http://localhost:8080/employees/activate?employeeId=<EMPLOYEE_ID>" \
  -b cookies.txt
```

## Principais endpoints

- `POST /onboarding` (público)
- `POST /auth/login` (público)
- `POST /auth/switch-company` (`MANAGER`; troca o tenant do JWT)
- `POST /auth/logout` (público; remove o cookie de sessão)
- `GET /companies` (`MANAGER`; lista as empresas ativas da conta)
- `POST /companies` (`MANAGER`; cria outra empresa para a mesma conta)
- `GET /companies/me` (`MANAGER`)
- `PUT /companies/me/deactivate` (`MANAGER` proprietário; desativação lógica confirmada por senha)
- `GET /managers/me` (`MANAGER`)
- `POST /managers` (`ADMIN`)
- `POST /employees` (`MANAGER`)
- `PUT /employees/activate` (`MANAGER`)
- `PUT /employees/disable` (`MANAGER`)
- `GET /employees` (`MANAGER`)
- `POST /benefits` (`MANAGER`)
- `GET /benefits/tenant` (`MANAGER`)
- `GET /benefits/marketplace` (`MANAGER`)
- `PUT /benefits/{benefitId}` (`MANAGER`)
- `PUT /benefits/{benefitId}/activate` (`MANAGER`)
- `PUT /benefits/{benefitId}/deactivate` (`MANAGER`)
- `DELETE /benefits/{benefitId}` (`MANAGER`)
- `POST /partnerships` (`MANAGER`)
- `PUT /partnerships/accept` (`MANAGER`)
- `PUT /partnerships/reject` (`MANAGER`)
- `PUT /partnerships/disable` (`MANAGER`)
- `POST /subscriptions` (`USER`)

## Testes

```bash
# Todos os testes unitários + cenários Quarkus test
./mvnw test

# Teste crítico de integração
./mvnw -Dtest=CriticalIntegrationTest test

# Pipeline completo (inclui failsafe/verify)
./mvnw verify
```

Observações:

- Testes `@QuarkusTest` usam PostgreSQL efêmero via Dev Services; Docker Desktop ou OrbStack precisa estar ativo.
- O banco de teste é descartável e não reutiliza a base local `benefix`.
- Evite injetar variáveis de produção (`QUARKUS_DATASOURCE_*`) ao executar os testes.
- As listagens de funcionários e benefícios aceitam `page` e `size`; `size` é limitado a 100 e o padrão é 50.
- CPF e e-mail identificam uma única conta. A mesma conta pode possuir um vínculo de gestor em várias empresas; desativar uma empresa preserva a identidade e o histórico das demais.
- O criador da empresa é seu gestor proprietário. Somente esse vínculo pode encerrar o tenant; a operação desativa gestores, colaboradores, benefícios, parcerias e tokens, além de cancelar solicitações pendentes.

## Build e execução em produção

Pacote padrão (`fast-jar`):

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

JAR:

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

Native build:

```bash
./mvnw package -Dnative
```

Native build em container:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

### Observação sobre serialização JSON em native image

Este projeto usa um registro central de reflection para payloads JSON da API em:

- `src/main/java/org/acme/domains/shared/api/NativeJsonReflectionConfig.java`

Sempre que adicionar novo DTO de request/response exposto por endpoint, inclua o tipo nesse registro para evitar falhas em runtime nativo (ex.: `InvalidDefinitionException: No serializer found ...`).

### Swagger protegido com Basic Auth em produção

Em produção (`%prod`), a documentação exige Basic Auth nos endpoints:

- `GET /q/swagger-ui/*`
- `GET /q/openapi*`

Configurar no `.env` da EC2 (ou variáveis do container):

```bash
SWAGGER_BASIC_AUTH_USERNAME=seu_usuario_docs
SWAGGER_BASIC_AUTH_PASSWORD=sua_senha_docs_forte
```

Exemplo de acesso ao OpenAPI em produção:

```bash
curl -i -u "$SWAGGER_BASIC_AUTH_USERNAME:$SWAGGER_BASIC_AUTH_PASSWORD" \
  https://SEU_HOST/q/openapi
```

Notas operacionais:

- A proteção Basic Auth é aplicada apenas na documentação; a API de negócio continua com JWT via cookie.
- Se `SWAGGER_BASIC_AUTH_USERNAME`/`SWAGGER_BASIC_AUTH_PASSWORD` não estiverem definidas em produção, o bootstrap falha por configuração ausente.
- Use HTTPS no ambiente público para proteger credenciais Basic Auth em trânsito.

## Documentação e guias Quarkus relacionados

- Site oficial: <https://quarkus.io/>
- Maven tooling: <https://quarkus.io/guides/maven-tooling>
- Flyway: <https://quarkus.io/guides/flyway>
- Validation: <https://quarkus.io/guides/validation>
- OpenAPI/Swagger UI: <https://quarkus.io/guides/openapi-swaggerui>
- REST + Jackson: <https://quarkus.io/guides/rest#json-serialisation>
- JWT: <https://quarkus.io/guides/security-jwt>
- Reactive SQL Client: <https://quarkus.io/guides/reactive-sql-clients>

*...*
