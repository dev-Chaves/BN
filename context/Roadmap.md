# Roadmap técnico e de produto

Este roadmap parte do estado implementado em 24/08/2026. Priorização sugerida, não compromisso de entrega.

## P0 — Confiabilidade antes de ampliar o produto

1. Criar testes de integração PostgreSQL/Testcontainers para onboarding/login, troca de tenant, parceria, solicitação, assinatura e resgate concorrente.
2. Testar isolamento negativo: gestor/funcionário de empresa A não acessa nem altera dados da B.
3. Adicionar uma verificação nativa periódica no CI ou antes de releases, cobrindo serialização e reflection.
4. Revisar inconsistências schema-modelo acumuladas nas migrations (`usages`, comprimentos e legado) e criar migrations corretivas, sem editar as antigas.

## P1 — Operação e segurança

1. Tornar revogação JWT e rate limiting distribuídos caso existam múltiplas réplicas, por exemplo com Redis.
2. Definir estratégia de rotação de chaves RSA e identificação por `kid`.
3. Documentar backup, restore, rollback e resposta a incidentes.
4. Adicionar métricas de domínio e alertas além do health check.
5. Validar automaticamente o contrato OpenAPI e exemplos da documentação.

## P2 — Produto

1. Completar consultas de parcerias nos lados cliente e provedor, com paginação e histórico.
2. Expor histórico de resgates e uso para funcionário e gestor.
3. Adicionar dashboard de adoção, solicitações, benefícios e consumo por tenant.
4. Definir expiração/cancelamento de subscriptions e impacto de parceria/benefício desativado.
5. Evoluir pesquisa do marketplace com categorias públicas, ranking e filtros explícitos.
6. Avaliar convites e recuperação de senha em vez de senha definida pelo gestor.

## P3 — Chatbot/MCP

Só iniciar após modelo de autorização e casos de uso estarem definidos:

1. especificar quais dados o assistente pode ler/escrever por role e tenant;
2. criar endpoints de contexto mínimos e auditáveis;
3. definir retenção, consentimento e proteção do histórico;
4. implementar autenticação de serviço dedicada, sem reutilizar contas humanas;
5. testar prompt injection, exfiltração entre tenants e trilha de auditoria.

Veja [ChatBot](ChatBot.md) como material exploratório, não como arquitetura aprovada.
