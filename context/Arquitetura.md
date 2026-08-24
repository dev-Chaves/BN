# Arquitetura do backend BN

## Visão

O BN é um monólito modular síncrono em Spring MVC. Cada domínio concentra controller, service, repository, entidades e DTOs. O executável de produção é uma imagem nativa GraalVM e o banco transacional é PostgreSQL.

```text
HTTP → rate limit/log/JWT → controller + @PreAuthorize
     → service + @Transactional + guards
     → Spring Data JPA/Hibernate → PostgreSQL
```

## Stack efetiva

| Área | Tecnologia |
|---|---|
| Runtime/build | Java 25, Maven Wrapper, GraalVM Community 25 |
| Aplicação | Spring Boot 4.1.0, Spring Web MVC |
| Dados | Spring Data JPA, Hibernate bytecode enhancement |
| Banco | PostgreSQL; H2 em testes |
| Migrações | Flyway `V1`–`V12` |
| Segurança | Spring Security Resource Server, Nimbus JWT, BCrypt |
| Documentação | Springdoc 3.0.0, profile `docs` |
| Operação | Actuator, Docker, GitHub Actions, GHCR, EC2 |

## Organização e convenções

O namespace é `com.bnfix.ubm`. `domains/` contém módulos de negócio; `shared/security`, JWT e proteção multi-tenant; `shared/api`, filtros transversais; `shared/nativeimage`, hints de reflection; `api`, erros globais.

Services delimitam transações e regras. DTOs são records. Entidades mantêm invariantes por métodos de domínio. Leituras não ganham logs de endpoint porque `RequestLoggingFilter` já registra o resultado.

## Persistência

- Hibernate usa `ddl-auto: validate` e nunca cria o schema em produção.
- Flyway aplica e valida migrations ao iniciar.
- timestamps são tratados em UTC; `open-in-view` está desligado.
- PostgreSQL fornece índices parciais, triggers defensivas, GIN, `tsvector` em português e `pg_trgm`.
- o enhancement do Hibernate roda durante o build.

Toda mudança estrutural exige nova migration numerada. Migrations já aplicadas não devem ser alteradas.

## Native Image

O profile `native` gera `target/ubm`. Spring AOT cobre componentes do framework; `NativeRuntimeHints` registra entidades, DTOs, enums e value objects usados por reflection. Ao criar um desses tipos, atualize os hints. Testes JVM não detectam necessariamente hints ausentes.

## Profiles

- padrão: PostgreSQL, configuração similar à produção e Springdoc desligado;
- `docs`: OpenAPI e Swagger UI com Basic Auth;
- `test`: H2 em modo PostgreSQL, Flyway desligado, RSA em memória e Swagger habilitado.

As chaves JWT são PEM externos, nunca recursos do classpath.

## Deploy

Os Dockerfiles usam build multi-stage: GraalVM compila e Debian slim executa como usuário `spring`. Em push para `main`, o workflow compila, publica no GHCR, provisiona chaves na EC2, substitui o container e verifica `/actuator/health`.

## Limites arquiteturais atuais

- multi-tenancy lógico em schema compartilhado: isolamento depende de guards e queries;
- blocklist JWT em memória: logout não se propaga entre réplicas;
- rate limit em memória: limites são por instância;
- Swagger fica fora do profile padrão e deve ser ativado deliberadamente.
