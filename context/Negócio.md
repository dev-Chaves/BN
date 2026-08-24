# Negócio — Modelo de Negócio e Questões Empresariais

> Voltar ao rascunho principal: [[Benefix]]

---

## Modelo de Negócio

### SAAS vs Contrato B2B

Ainda em aberto. Duas hipóteses:

| Modelo | Descrição | Pros | Contras |
|---|---|---|---|
| **SaaS** | Assinatura mensal/anual por empresa | Recorrência, escalável, simples | Precisa de infra sólida |
| **Contrato B2B** | Contrato personalizado por empresa | Flexível, ticket alto | Difícil de escalar |

> ⚠️ Pesquisar legislação sobre contratos entre empresas no Brasil — o Benefix **facilita** a conexão mas **não gerencia** o contrato entre as partes.

---

## Monetização

Ideias para geração de receita:

1. **Taxa de plataforma:** porcentagem sobre cada parceria ativada
2. **Assinatura por empresa:** plano mensal baseado no número de funcionários ou benefícios ativos
3. **Plano freemium:** vitrine gratuita, funcionalidades avançadas pagas
4. **Destaque na vitrine:** empresas pagam para aparecer primeiro nos resultados

---

## Fluxo Comercial das Parcerias

**Importante:** O Benefix **não gerencia** o contrato legal entre empresas — apenas facilita a conexão.

```
Empresa A descobre Benefício da Empresa B na Vitrine
        ↓
Manager da Empresa A solicita parceria (PENDING)
        ↓
Manager da Empresa B aprova (ACTIVE)
        ↓
[Fora do sistema] As empresas assinam contrato diretamente
        ↓
Funcionários da Empresa A passam a ter acesso ao benefício
```

**Contratos na plataforma:**
- Contratos simples com **data de validade** (ex: parceria válida por 12 meses)
- Após expirar → status `DISABLE` automaticamente
- Renovação pode ser solicitada novamente

---

## Check-in — Validação e Registro de Uso do Benefício

> 🧠 **Discussão em aberto** — este é o maior problema de UX da plataforma.

### O problema real (caso concreto)

Na faculdade, existe uma parceria com o estacionamento: estudantes pagam menos. A "validação" hoje é:
- O estudante **mostra a carteirinha** ou
- Simplesmente **fala que é estudante** (boca a boca)

Não há registro, não há controle, não há auditoria. O estacionamento confia na palavra. A faculdade não sabe quantos alunos usaram o benefício nem quando.

**O Benefix resolve exatamente isso** — mas precisa fazer isso de um jeito que o estacionamento (ou qualquer outro fornecedor de benefício) consiga adotar **sem precisar de equipe técnica, sem app, sem integração**.

---

### Referências do mercado

#### Wellhub (Gympass)
- Empresa contrata o plano e funcionários têm acesso a academias parceiras
- Check-in: **o funcionário faz check-in pelo app do Gympass** — a academia recebe a confirmação
- A academia precisa ter o **app da academia integrado ao Gympass** ou usar um **tablet/totem na recepção**
- Registra data, hora, duração
- ✅ Controle total para ambos os lados
- ❌ Exige que a academia tenha o sistema integrado — barreira técnica para parceiros pequenos

#### TotalPass
- Modelo similar ao Gympass
- Check-in via **QR Code gerado no app** — recepcionista escaneia
- Algumas academias usam **código verbal** informado na recepção
- Relatórios mensais de uso por empresa contratante
- ✅ QR Code é mais simples de adotar
- ❌ Ainda exige algum processo na recepção do parceiro

#### Ticket Refeição / VR
- Benefício de alimentação com cartão físico
- Validação é feita pela **maquininha de cartão** — zero fricção para o estabelecimento
- ✅ Infraestrutura já existente (maquininha)
- ❌ Modelo não se aplica para benefícios não-monetários

---

### Nossa proposta: facilitar ao MÁXIMO o lado do fornecedor

O ponto central é: **o fornecedor do benefício (ex: estacionamento) não deve precisar instalar nada, criar conta ou aprender um sistema**.

#### Opção A — QR Code gerado pelo usuário ✅ (favorita)

```
Funcionário abre o app → aba "Meus Benefícios"
        ↓
Seleciona o benefício (ex: Estacionamento da Faculdade)
        ↓
Clica em "Usar agora" → gera QR Code com validade curta (ex: 5 minutos)
        ↓
Mostra o QR Code para o atendente do estacionamento
        ↓
Atendente escaneia com celular pessoal → abre link no navegador → confirma uso
        ↓
Sistema registra: quem usou, quando, qual benefício
```

**Por que funciona para o fornecedor:**
- Não precisa de app instalado
- Não precisa de conta no Benefix
- Qualquer celular com câmera e navegador serve
- O link de validação é uma **página web simples** — mostra nome do funcionário, empresa, benefício e botão "Confirmar"

#### Opção B — Código alfanumérico (fallback)

Para situações onde o QR Code não é viável (ex: validação por telefone, atendimento remoto):

```
Funcionário → "Usar agora" → recebe código: BN-4F8X2
        ↓
Fala o código para o atendente
        ↓
Atendente acessa portal.benefix.com/validar → digita o código → confirma
```

- Código de uso único, validade curta (ex: 15 minutos)
- Mesmo fluxo de registro no sistema

#### Opção C — Sem validação pelo fornecedor (modo confiança)

Para benefícios onde não faz sentido confirmar presencialmente (ex: desconto em compra online):

```
Funcionário declara uso no app → sistema registra
```

- Sem interação do fornecedor
- Útil para benefícios de baixo risco de fraude
- Relatórios de uso ficam disponíveis para o Manager

---

### Entidade CheckIn (esboço)

```
CheckIn {
  id
  subscription       → Subscription (quem está usando)
  benefit            → Benefit
  employee           → Employee
  checkedInAt        → LocalDateTime  (momento do uso)
  confirmedAt        → LocalDateTime? (quando o fornecedor confirmou — pode ser null)
  confirmedBy        → String?        (identificação do atendente — opcional)
  validationCode     → String         (código único gerado para esse check-in)
  qrCodeToken        → String         (token do QR Code, com expiração)
  status             → CheckInStatus
  expiresAt          → LocalDateTime  (validade do QR/código)
}

CheckInStatus: PENDING | CONFIRMED | EXPIRED | CANCELLED
```

**Fluxo de status:**
```
[Funcionário clica "Usar"] → PENDING (QR/código válido por X minutos)
        ↓ atendente confirma
CONFIRMED (registro permanente)
        ↓ tempo esgota sem confirmação
EXPIRED
```

---

### ⚙️ Arquitetura — Como armazenamos o QR Code?

> ❓ **Problema:** Se o QR Code é uma imagem, onde ela fica? Banco de dados? Arquivo? S3?

**Resposta:** o QR Code **nunca é armazenado**. Ele é gerado dinamicamente no frontend a partir de uma URL — e essa URL contém o token que sim, fica no banco.

#### Por que não armazenar a imagem?

| Abordagem | Problema |
|---|---|
| BLOB/Base64 no PostgreSQL | Banco incha absurdamente, performance péssima em queries, impossível regenerar sem atualizar o registro |
| Arquivo no servidor (disco) | Sem escalabilidade, difícil de limpar tokens expirados |
| Object Storage (S3/MinIO) | Overengineered demais para este caso — e ainda teria que limpar os arquivos expirados |

#### A solução correta: token no banco, imagem no cliente

O QR Code é apenas **a representação visual de uma string**. Essa string é uma URL:

```
https://benefix.com/checkin/validate/550e8400-e29b-41d4-a716-446655440000
```

O que armazenamos no banco é só o UUID. A imagem é gerada no frontend com uma biblioteca (ex: `qrcode.react` no React, ou `ZXing` se quiser gerar server-side em Java). Nenhuma imagem trafega na API, nenhuma imagem é salva.

#### Fluxo completo

```
1. Funcionário clica "Usar agora"
        ↓
2. POST /checkin/start
   → Servidor cria CheckIn(status=PENDING, validationToken=UUID, expiresAt=agora+5min)
   → Retorna: { token: "550e8400-...", expiresAt: "2026-03-06T20:47:00" }
        ↓
3. Frontend recebe o token e gera a IMAGEM QR localmente
   (lib JS gera a imagem a partir da URL — zero chamada ao servidor)
        ↓
4. Funcionário mostra o QR no celular para o atendente
        ↓
5. Atendente escaneia → abre https://benefix.com/checkin/validate/550e8400-...
   → GET /checkin/validate/{token}
   → Servidor busca CheckIn pelo UUID, verifica status + expiresAt
   → Retorna: { employee: "João", company: "Empresa X", benefit: "Estacionamento", valid: true }
        ↓
6. Atendente clica "Confirmar"
   → POST /checkin/confirm/{token}
   → CheckIn.status = CONFIRMED, confirmedAt = agora
```

#### UUID no banco vs JWT Stateless — qual usar?

| | UUID no banco | JWT Stateless |
|---|---|---|
| **Storage** | Um registro no `CheckIn` | Nenhum |
| **Invalidação antecipada** | ✅ Só mudar o status | ❌ Impossível (token é auto-suficiente) |
| **Audit trail** | ✅ Nativo (o `CheckIn` já é o registro) | Precisa criar registro separado ao confirmar |
| **Infraestrutura** | PostgreSQL (já temos) | JWT já temos (`TokenService`) |
| **Segurança se QR for fotografado** | ✅ Invalida por tempo/status | ⚠️ Válido até expirar, sem controle |
| **Complexidade** | Baixa | Baixa |

**✅ Recomendação: UUID no banco.** O `CheckIn` já é a entidade de registro — o token é parte dela. Qualquer problema (QR capturado, funcionário mudou de ideia) → basta expirar o registro. E o projeto já usa PostgreSQL.

> 💡 O JWT Stateless seria válido se quiséssemos zero storage e aceitarmos que a única proteção é o tempo de expiração curto. Para um projeto com audit trail como requisito, UUID ganha.

---

### Endpoint de validação pública

Este é o endpoint que o atendente acessa ao escanear o QR Code:

```
GET /checkin/validate/{token}
→ Retorna: nome do funcionário, empresa, benefício, validade
→ Público (sem autenticação) — o token em si é a autorização

POST /checkin/confirm/{token}
→ Confirma o uso (marca como CONFIRMED)
→ Público — qualquer pessoa com o token pode confirmar
```

> ⚠️ A segurança aqui é o **tempo curto de validade** do token (5-15 minutos) + **uso único** (token expira após confirmação).

---

### Perguntas em aberto

- [ ] Quanto tempo de validade do QR Code? (sugestão: 5 min para uso presencial)
- [ ] O fornecedor precisa de algum cadastro mínimo ou é totalmente anônimo na confirmação?
- [ ] Registro de quantas vezes o mesmo funcionário pode usar o mesmo benefício por período (ex: 1x por dia, ilimitado)?
- [ ] Limite de uso por parceria (ex: empresa contratou até 50 usos/mês)?
- [ ] Notificar o Manager quando um funcionário usa um benefício?
- [ ] Relatório de uso para o Manager: quantos check-ins por benefício, por funcionário, por período?

---

## Notificações

| Evento | Quem notificar |
|---|---|
| Funcionário solicita adesão a benefício | Manager da empresa |
| Empresa solicita parceria | Manager da empresa fornecedora |
| Parceria aprovada/rejeitada | Manager da empresa solicitante |
| Parceria próxima do vencimento | Ambos os Managers |

---

## Questões Legais (pesquisar)

- [ ] Qual a responsabilidade do Benefix quando uma parceria dá errado?
- [ ] LGPD: quais dados pessoais armazenamos? CPF, email, nome — exige consentimento
- [ ] Contrato de uso da plataforma (ToS) para empresas
- [ ] Emissão de nota fiscal: quando a plataforma cobra, precisa emitir NF

---

## Paleta de Cores

**Quite Clear**

| Nome | Hex |
|---|---|
| Grey | `#CBCBCB` |
| Branco | `#F2F2F2` |
| Verde | `#174D38` |
| Vinho | `#4D1717` |

---

*Ver também: [[Visão Geral]] · [[Roadmap]] · [[Requisitos]]*
