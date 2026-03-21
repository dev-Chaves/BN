# Plano de Sprint — Entrega servidor funcional

## Problema e abordagem
Você quer um roteiro de execução para deixar o backend funcional para entrega na janela desta semana, com priorização objetiva (PO/Scrum Master/Senior Engineer), e confirmou que o objetivo mínimo é **API pronta + deploy em servidor/staging**.

Abordagem: focar primeiro no **MVP funcional de servidor** (autenticação, fluxos críticos de negócio, persistência correta, segurança e testes de integração), depois avançar para robustez e acabamento.

## Estado atual (resumo técnico)
- Estrutura Quarkus + Reactive + Flyway está montada.
- Existem domínios com boa base (ex.: `PartnershipService`), mas há lacunas críticas:
  - serviços vazios/incompletos (`AuthService`, `ManagerService`);
  - onboarding sem persistência completa;
  - inconsistências de transação/erros em alguns services;
  - resources essenciais ainda ausentes para cobertura completa de API.

## Prioridades de entrega

### P0 — Obrigatório para “servidor funcional”
1. Implementar autenticação JWT ponta a ponta (`AuthService` + `AuthResource`).
2. Completar `ManagerService` e expor endpoints mínimos de manager.
3. Corrigir `OnboardingService` para persistir entidades necessárias no fluxo.
4. Padronizar services críticos (`SubscriptionService`, `BenefitService`, `EmployeeService`):
   - transação onde necessário;
   - mensagens/erros corretos e consistentes;
   - validações em padrão reativo claro.
5. Criar resources faltantes para fluxos essenciais:
   - partnership,
   - subscription,
   - company (mínimo necessário ao fluxo real).
6. Fechar configuração de segurança em runtime (JWT config + autorização por role).
7. Garantir teste de integração dos fluxos críticos.
8. Realizar deploy em servidor/staging com smoke test final.

### P1 — Alta relevância (logo após P0)
1. Padronizar tratamento global de exceções HTTP.
2. Endurecer validações de domínio (duplicidade CPF/CNPJ, regras de associação).
3. Cobrir cenários negativos principais nos testes.

### P2 — Acabamento
1. Melhorias de ergonomia de API (mensagens, payloads, consistência de respostas).
2. Revisão final de documentação operacional para entrega.

## Sequência de execução (sem datas)
1. **Fundação de acesso**: autenticação JWT completa.
2. **Cadastro/gestão base**: manager + onboarding persistente.
3. **Consistência do core**: refatorar services críticos para padrão único.
4. **Exposição de API**: resources faltantes para fechar jornadas.
5. **Confiabilidade**: testes de integração e correções.
6. **Hardening**: erro global, validações adicionais e checklist final de release.
7. **Entrega operacional**: deploy em staging, validação de saúde e endpoints críticos.

## Roteiro diário executável (Dia 1 a Dia 7)

### Dia 1 — Fundação de acesso (trabalho em paralelo)
- Você: `AuthService` (login, validação de senha, geração de JWT).
- Parceiro/time: `AuthResource` + contrato de request/response.
- Checkpoint do dia:
  - `POST /auth/login` retorna token;
  - endpoint protegido responde `401/403` sem token.

### Dia 2 — Cadastro base (manager + onboarding)
- Você: completar `ManagerService` (fluxos mínimos de criação/consulta).
- Parceiro/time: `ManagerResource` + ajuste de permissões.
- Em seguida: corrigir `OnboardingService` para persistência transacional.
- Checkpoint do dia:
  - onboarding cria e persiste Account/Company/Manager;
  - manager criado aparece em consulta.

### Dia 3 — Hardening de services críticos
- Refatorar `SubscriptionService`, `BenefitService` e `EmployeeService` no padrão do `PartnershipService`:
  - `@WithTransaction` onde há múltiplos passos de escrita;
  - exceções/mensagens corretas;
  - validações separadas e legíveis.
- Checkpoint do dia:
  - build sem regressão;
  - cenários de erro principais com resposta coerente.

### Dia 4 — Fechamento de API essencial
- Criar/fechar `PartnershipResource`, `SubscriptionResource` e `CompanyResource` (mínimo funcional).
- Revisar autenticação/autorização por role em todos endpoints críticos.
- Checkpoint do dia:
  - jornadas principais completas via API autenticada.

### Dia 5 — Testes de integração + correções
- Priorizar testes E2E dos fluxos:
  - auth/login;
  - onboarding;
  - employee;
  - partnership;
  - subscription.
- Corrigir bugs encontrados no mesmo dia.
- Checkpoint do dia:
  - `./mvnw verify` verde (ou com falhas não críticas mapeadas e corrigíveis).

### Dia 6 — Deploy staging + smoke tests
- Subir app em staging com config realista (DB/JWT por ambiente).
- Executar smoke tests:
  - healthcheck;
  - login;
  - 2-3 fluxos críticos completos.
- Checkpoint do dia:
  - API acessível em staging e validada.

### Dia 7 — Buffer de estabilização e handoff
- Fechar pendências, regressões e observabilidade mínima.
- Revisar checklist final e preparar handoff da entrega.
- Checkpoint do dia:
  - sem blockers abertos;
  - pacote pronto para demonstração/entrega.

## Modelo de trabalho diário (para você e time)
- Início: 15 min de definição de objetivo do dia.
- Meio do dia: checkpoint rápido (status + bloqueios).
- Fim: validação objetiva de pronto (evidência com comando/teste/endpoint).
- Regra de ouro: não abrir escopo novo sem fechar o P0 corrente.

## Board de execução — primeiros 3 passos (ordem exata)

### 1) `auth-login-flow` (PRONTO PARA INICIAR AGORA)
- Objetivo: autenticação JWT ponta a ponta (`AuthService` + `AuthResource`).
- Definição de pronto:
  - `POST /auth/login` devolve token em caso válido;
  - credencial inválida retorna erro consistente;
  - rota protegida sem token retorna `401/403`.
- Validação:
  - `./mvnw test -Dtest=*Auth*`
  - `./mvnw verify` (ao final do dia).

### 2) `manager-domain-api` (INICIA APÓS PASSO 1)
- Objetivo: completar `ManagerService` e expor endpoints mínimos.
- Definição de pronto:
  - criação e consulta de manager funcionando;
  - autorização por role aplicada;
  - respostas de erro coerentes.
- Dependência: concluir `auth-login-flow`.

### 3) `onboarding-persistence` (INICIA APÓS PASSO 2)
- Objetivo: onboarding persistir `Account`, `Company` e `Manager` em fluxo transacional.
- Definição de pronto:
  - entidades persistidas no banco ao final da operação;
  - rollback em caso de erro intermediário;
  - endpoint refletindo estado persistido real.
- Dependência: concluir `manager-domain-api`.

## Regras operacionais do board
- WIP máximo: 1 tarefa P0 por vez.
- Só puxar próxima tarefa quando a anterior estiver com evidência de pronto.
- Evidência mínima por tarefa: teste/verify + chamada de endpoint.

## Checklist de pronto para entrega
- Login retorna token válido e protegido por regras de role.
- Fluxos críticos persistem corretamente no banco (onboarding, employee, partnership, subscription).
- Endpoints essenciais disponíveis e testados.
- Build e verify passam sem regressão.
- Configuração de execução documentada e reproduzível.
- API publicada em staging com smoke tests aprovados.

## Dependências e riscos
- Dependência operacional: banco PostgreSQL disponível e configurado.
- Dependência de segurança: chave/config JWT definida por ambiente.
- Risco principal: expandir escopo antes de fechar P0.
  - Mitigação: bloquear P1/P2 até P0 estar concluído e validado por testes.

## Todos estruturados no SQL
Os itens do plano já foram refletidos em `todos`/`todo_deps` para acompanhamento de execução e dependências.
