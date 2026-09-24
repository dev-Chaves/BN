# Domínios e regras de negócio

## Modelo relacional

```text
Account 1 ── N Manager N ── 1 Company
Account 1 ── 1 Employee N ── 1 Company
Company 1 ── N Benefit N ── N Category
Company(client) 1 ── N Partnership N ── 1 Benefit(provider)
Employee 1 ── N RedemptionToken N ── 1 Benefit
RedemptionToken 1 ── 0..1 BenefitRedemption
Company 1 ── N Announcement 1 ── N AnnouncementRecipient
```

## Account e identidades

`Account` é a identidade global: UUID, nome, e-mail normalizado, senha BCrypt, CPF e role. E-mail e CPF são únicos. `Manager` representa o vínculo da conta com uma empresa; a mesma conta pode gerir várias empresas, sem duplicar o vínculo. `Employee` representa um funcionário de uma empresa e possui estado `ACTIVE` ou `DISABLED`.

## Company

Tenant com nome, CNPJ único, flag ativa e criação. Cada empresa tem exatamente um vínculo de gestor proprietário. O proprietário pode desativá-la após confirmar a senha. Managers só operam empresas às quais pertencem.

## Benefit e Category

Um benefício pertence a uma empresa provedora e pode ter várias categorias. Possui nome, descrição, ativo, visibilidade pública, janela de validade, limite de usos por usuário, termos e criação.

Um benefício está disponível quando provedor e benefício estão ativos e a data atual está dentro da janela. O marketplace privado exclui benefícios do próprio tenant; o público inclui somente benefícios publicamente visíveis. Categorias iniciais: Saúde, Educação, Alimentação, Transporte, Lazer e Bem-estar.

## Partnership

Conecta uma empresa cliente a um benefício de outra provedora. Não é permitido solicitar benefício próprio ou duplicar a relação cliente/benefício.

```text
PENDING ──→ ACTIVE ──→ DISABLED
    └─────→ REJECTED
```

Somente o tenant provedor revisa a solicitação. Uma parceria ativa torna o benefício elegível para resgate pelos funcionários da empresa cliente.

## Enrollment (auto-cadastro de evento)

O funcionário pode ser criado por um caminho público de evento: `POST /companies/event/enroll` cria a conta (`USER`) e o vínculo `Employee` **já ativo** na empresa configurada em `app.event.company-id`. Diferente do fluxo de gestão, não passa por gestor nem por ativação manual — é o único caminho público de criação de funcionário. Quando `app.event.company-id` é `0` ou a empresa está inativa, o endpoint fica indisponível.

## Redemption

O funcionário emite token aleatório para um benefício elegível; apenas o hash é persistido. O token expira em `REDEMPTION_TOKEN_TTL_MINUTES` (padrão três minutos), pode ser pré-visualizado e é consumido uma vez pelo gestor da empresa provedora. Só existe um token ativo por par (funcionário, benefício): emitir novamente revoga o anterior. O consumo registra `BenefitRedemption` e respeita disponibilidade e `maxUsesPerUser`.

Estados do token: `ACTIVE`, `CONSUMED`, `EXPIRED`.

## Announcement

Um gestor publica comunicado no tenant. O sistema cria destinatário para cada funcionário ativo naquele momento. Cada destinatário guarda `readAt`; por isso existem histórico individual, contador e marcação de leitura sem alterar o comunicado original.

## Value objects e integridade

CPF e CNPJ são value objects embutidos. O banco complementa validações de aplicação com FKs, checks, índices únicos/condicionais e triggers para instalações que ainda possuam dados legados duplicados.

## Regras transversais

- operações de tenant usam o `companyId` do JWT e guards;
- conta, vínculo e empresa precisam estar ativos;
- mutações são transacionais e registradas em log sem dados sensíveis;
- paginação é limitada globalmente a 50 itens;
- tipos de domínio expostos ou instanciados por reflection precisam constar em `NativeRuntimeHints`.
