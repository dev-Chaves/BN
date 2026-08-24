# Benefix BN API

Backend da Benefix, plataforma B2B para empresas publicarem, compartilharem e operarem benefícios corporativos. A API atende gestores, funcionários e administradores, com isolamento por empresa, marketplace, solicitações de acesso, comunicados internos e resgates por token.

## Stack e estado atual

| Item | Implementação |
|---|---|
| Linguagem e framework | Java 25, Spring Boot 4.1.0, Spring MVC |
| Persistência | Spring Data JPA, Hibernate e PostgreSQL |
| Schema | Flyway `V1`–`V12`; Hibernate em `validate` |
| Segurança | Spring Security, JWT RS256, cookie `httpOnly` e Bearer token |
| Produção | GraalVM Native Image 25, Docker, GHCR e EC2 |
| API docs | Springdoc, profile `docs`, protegido por Basic Auth |
| Observabilidade | Actuator, request logging e rate limit em memória |
| Testes | JUnit, Spring Boot Test, H2 e PostgreSQL via Testcontainers |

O projeto implementa onboarding, gestores multiempresa, gestão de funcionários, catálogo próprio, marketplace público e privado, categorias, parcerias B2B, assinatura e solicitação de acesso, comunicados e resgates de uso único.

## Arquitetura

Monólito modular organizado por feature, com fluxo `Controller → Service → Repository/Entity`. Services concentram transações e regras; controllers aplicam autorização; `TenantGuard` e `AccessStatusGuard` validam vínculo, tenant e estado.

```text
src/main/java/com/bnfix/ubm/
├── api/                 # erros HTTP globais
├── domains/             # módulos de negócio e DTOs
└── shared/
    ├── api/             # rate limit e request logging
    ├── nativeimage/     # hints explícitos do GraalVM
    └── security/        # JWT, CORS, tenant e revogação
```

Veja [Arquitetura](context/Arquitetura.md), [Domínios](context/Domínios.md) e [Segurança](context/Segurança.md).

## Pré-requisitos

- JDK 25 e PostgreSQL;
- OpenSSL para gerar as chaves RSA;
- Docker, opcional;
- GraalVM Community 25 somente para build nativo local.

Use sempre o Maven Wrapper (`./mvnw`).

## Configuração local

1. Crie o banco `benefix`.
2. Copie `.env.example` para `.env` e ajuste banco, cookie e CORS.
3. Gere chaves RSA:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out privateKey.pem
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

4. Aponte `JWT_PRIVATE_KEY_PATH` e `JWT_PUBLIC_KEY_PATH` para esses arquivos. Eles não devem ser versionados.
5. Exporte o `.env` no shell ou configure as variáveis na IDE:

```bash
./mvnw spring-boot:run
```

A API usa `http://localhost:8080`; health check: `GET /actuator/health`. Flyway migra e Hibernate valida o schema na inicialização.

## Configuração principal

| Variável | Padrão | Uso |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/benefix` | conexão PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | vazio | senha do banco |
| `JWT_PUBLIC_KEY_PATH` | `/opt/bn/secrets/publicKey.pem` | validação JWT |
| `JWT_PRIVATE_KEY_PATH` | `/opt/bn/secrets/privateKey.pem` | assinatura JWT |
| `COOKIE_SECURE` | `true` | atributo `Secure` |
| `COOKIE_DOMAIN` | `.bnfix.com.br` | deixe vazio em desenvolvimento |
| `CORS_ALLOWED_ORIGINS` | domínios BN | allowlist separada por vírgula |
| `APP_PUBLIC_URL` | `http://localhost:3000` | link de resgate |
| `PORT` | `8080` | porta HTTP |

Veja [`.env.example`](.env.example) para limites e variáveis adicionais.

## Autenticação e multi-tenancy

`POST /auth/login` devolve o perfil no body e grava o JWT no cookie `jwt`, `HttpOnly`, `SameSite=Strict`, com duração de três horas. A API também aceita Bearer token. O JWT usa RS256 e contém `groups`, `accountId`, `email`, `jti` e, quando aplicável, `companyId`.

- `ADMIN`: criação administrativa de gestores;
- `MANAGER`: gestão da empresa ativa;
- `USER`: funcionário, acesso e uso de benefícios.

Uma conta MANAGER pode pertencer a várias empresas. `POST /auth/switch-company` emite token para outro vínculo. O `companyId` do payload nunca substitui a validação do claim e dos guards.

## API

| Base path | Público/role | Finalidade |
|---|---|---|
| `/auth` | login público; demais autenticados | sessão, perfil, logout e tenant |
| `/onboarding` | público | empresa e gestor proprietário |
| `/companies` | `MANAGER` | empresas e tenant atual |
| `/managers` | `ADMIN`/`MANAGER` | criação e autogestão |
| `/employees` | `MANAGER` | gestão de funcionários |
| `/benefits/public` | público | vitrine e busca textual |
| `/benefits` | `MANAGER` | catálogo e marketplace |
| `/categories` | `MANAGER` | categorias |
| `/partnerships` | `MANAGER` | parceria B2B |
| `/shared-benefits` | `USER` | benefícios do funcionário |
| `/subscriptions` | `USER` | assinatura |
| `/benefit-requests` | `USER`/`MANAGER` | solicitação e revisão |
| `/redemptions` | `USER`/`MANAGER` | emissão, preview e consumo |
| `/announcements` | `USER`/`MANAGER` | comunicados |

O contrato completo está em [`context/API.md`](context/API.md).

## Swagger/OpenAPI

O Springdoc fica desligado por padrão. Para documentação JVM local:

```bash
export SPRING_PROFILES_ACTIVE=docs
export SWAGGER_BASIC_AUTH_USERNAME=bnadmin
export SWAGGER_BASIC_AUTH_PASSWORD='troque-esta-senha'
./mvnw spring-boot:run
```

- OpenAPI: `http://localhost:8080/q/openapi`
- Swagger UI: `http://localhost:8080/q/swagger-ui`

Ambos exigem Basic Auth.

## Desenvolvimento

Execute nesta ordem antes de submeter:

```bash
./mvnw spotless:apply
./mvnw compile
./mvnw test
```

A suíte inclui integração PostgreSQL 18 via Testcontainers; portanto, `./mvnw test` requer um daemon Docker acessível.

Build nativo, lento e necessário apenas quando solicitado ou antes de mudanças críticas de runtime:

```bash
./mvnw -B -DskipTests -Pnative native:compile
```

Ao criar entidade, DTO ou enum, registre-o em `NativeRuntimeHints`. Mudanças de schema exigem uma nova migration `V<n>__descricao.sql`.

## Containers e deploy

`docker compose up --build` compila o binário nativo e o executa como usuário sem privilégios. O host precisa de `.env` válido e chaves em `/opt/bn/secrets`.

Pushes em `main` acionam compilação nativa, publicação de `ghcr.io/dev-chaves/bn` e deploy em EC2 com health check. Os dois Dockerfiles devem continuar equivalentes.

## Documentação

O índice completo está em [`context/Benefix.md`](context/Benefix.md). Em caso de conflito, código e migrations são a fonte de verdade.

## Licença

O repositório ainda não declara licença; trate o código como proprietário.
