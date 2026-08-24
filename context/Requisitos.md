# Requisitos e cobertura atual

## Funcionais implementados

- onboarding público cria empresa, gestor proprietário e conta;
- login, logout, restauração de sessão e troca de empresa;
- gestores administram empresas vinculadas, funcionários e o próprio perfil;
- benefícios possuem categorias, validade, termos, visibilidade e limite de uso;
- marketplace público, busca textual e marketplace autenticado;
- solicitação, aprovação, rejeição e desativação de parceria B2B;
- funcionários consultam benefícios, assinam os elegíveis e solicitam acesso individual;
- provedores aprovam/rejeitam solicitações individuais;
- funcionário emite token e provedor pré-valida/consome o resgate;
- gestores publicam comunicados e funcionários controlam leitura.

## Não funcionais implementados

- monólito modular e package by feature;
- autenticação JWT RS256 e autorização por role;
- isolamento lógico multi-tenant;
- PostgreSQL com Flyway e validação do schema;
- binário nativo GraalVM em produção;
- validação de entrada, erros padronizados, logs e health check;
- CORS configurável, rate limit e Swagger protegido;
- Docker e CI/CD para GHCR/EC2.

## Parciais ou pendentes

- cobertura de testes ainda pequena frente aos fluxos críticos;
- a busca de benefícios usa PostgreSQL 18/Testcontainers, mas os fluxos HTTP e multi-tenant ainda não têm cobertura equivalente;
- não há chatbot, MCP ou persistência de conversas;
- não há dashboard agregado de métricas/auditoria de negócio;
- revogação JWT e rate limit não são distribuídos;
- não há processo documentado de rollback de migration/deploy;
- OpenAPI depende de ativação do profile `docs` e não é validado no pipeline atual.

## Regras de qualidade para novas mudanças

- nova entidade/DTO/enum deve entrar em `NativeRuntimeHints`;
- toda alteração de schema requer migration Flyway nova;
- todo acesso company-scoped deve usar o tenant autenticado e os guards;
- logs de mutação devem registrar IDs e ator, nunca segredo;
- validar com `spotless:apply`, `compile` e `test`, nessa ordem.
