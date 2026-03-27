# Plano de execução (prioridade máxima)

Este plano é focado em **começar agora** e fechar as regras de negócio mais críticas primeiro.

## Ordem de implementação (não pular etapas)

1. **Partnership lifecycle**
   - Implementar `accept`, `reject`, `disable`.
   - Validar transições permitidas de status.
   - Garantir ownership: só manager da empresa fornecedora altera parceria.

2. **Subscription com regra correta**
   - Permitir subscription apenas com `PartnershipStatus.ACTIVE`.
   - Bloquear subscription duplicada ativa (`employee + benefit`).

3. **Gestão completa de benefícios**
   - Implementar `update`, `activate`, `deactivate`, `delete`.
   - Garantir tenant ownership.
   - Implementar vitrine com filtros (categoria, texto, empresa).

4. **Gestão completa de funcionários**
   - Implementar `activate`, `update`, listagem por tenant.
   - Reforçar regras de acesso por manager da própria empresa.

5. **Contrato de erro padronizado**
   - Padronizar respostas de erro com `message`, `status`, `timestamp`.
   - Cobrir validação, autorização e regras de negócio.

6. **Testes de integração críticos**
   - Fluxos obrigatórios:
     - onboarding + login
     - lifecycle de partnership
     - subscription com parceria ativa
     - autorização tenant/role

7. **Check-in / QR (depois do core)**
   - Criar domínio `CheckIn` com token temporário e expiração.
   - Endpoints para iniciar, validar e confirmar uso.
   - Registro auditável de uso.

## Critério de fechamento

- Regras críticas de negócio implementadas e validadas por teste.
- Fluxos principais funcionando ponta a ponta sem quebra de tenant.
- API com comportamento de erro consistente.
