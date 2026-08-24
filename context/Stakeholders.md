# Stakeholders — Personas do Benefix

> Voltar ao rascunho principal: [[Benefix]]

---

## O que são Proto-Personas
	
Proto-personas representam os tipos de usuários reais do produto com base no conhecimento atual do time — sem pesquisa formal ainda, mas ancoradas nas dores e contextos que motivaram o Benefix. Elas guiam decisões de produto, priorização de features e linguagem de comunicação.

---

## Personas

### Persona 1 — Ana Paula · Gestora de RH

> **Role no sistema:** `MANAGER` · **Prioridade: #1**

---

**Quem é**

Ana Paula tem 32 anos, é Coordenadora de RH em uma empresa de médio porte com 80 funcionários. Formada em Administração, trabalha há 4 anos nessa posição e é a única pessoa do time que cuida de benefícios.

---

**O que faz**

- Negocia contratos de benefícios (plano de saúde, academia, vale-refeição, cursos) com fornecedores diferentes
- Comunica novos benefícios para os funcionários por e-mail e WhatsApp
- Renova contratos manualmente, controlando tudo em planilha
- Responde as mesmas perguntas repetidas dos funcionários: *"tenho direito à academia?"*, *"como acesso o plano?"*

---

**Suas dores**

- **Fragmentação:** cada benefício é um sistema, um login, um processo diferente
- **Invisibilidade de uso:** contratou academia para 80 pessoas, não sabe se alguém usa
- **Descoberta manual:** para encontrar novos benefícios, depende de indicação ou cold call de vendedores
- **Retrabalho:** funcionários chegam sem saber o que têm, ela explica individualmente
- **Parceria travada:** fechar um benefício com outra empresa é e-mail, reunião, contrato no Word — leva semanas

---

**Como o Benefix ajuda**

- Vitrine de benefícios: encontra novas opções sem depender de prospecção manual
- Fluxo de parceria centralizado: solicita, acompanha status (PENDING → ACTIVE), sem e-mails
- Funcionários gerenciam a própria adesão — ela para de responder as mesmas perguntas
- Dashboard de uso: sabe quantos funcionários usam cada benefício

---

### Persona 2 — Rafael · Funcionário

> **Role no sistema:** `USER` · **Prioridade: #2**

---

**Quem é**

Rafael tem 26 anos, é Analista de TI na empresa da Ana Paula. Está sempre no celular, gosta de academia e séries. Não pensa muito em benefícios até perceber que estava pagando por algo que poderia ser gratuito.

---

**O que faz**

- Trabalha das 9h às 18h no escritório
- Usa transporte corporativo e vale-refeição; ignora os outros benefícios porque não sabe exatamente o que tem
- Descobriu que tinha desconto numa escola de inglês só quando um colega mencionou

---

**Suas dores**

- **Desconhecimento:** não sabe o que tem direito sem perguntar pro RH
- **Fricção no uso:** quando vai usar um benefício presencialmente, não tem como provar que é da empresa sem ligar pro RH ou mostrar o crachá (que o atendente não sabe verificar)
- **Dispersão:** cada benefício tem um app, um portal, um processo diferente

---

**Como o Benefix ajuda**

- Um só lugar para ver todos os benefícios disponíveis para ele
- Solicitação de adesão simples: clica, pede, aguarda (ou acessa direto)
- QR Code gerado no momento do uso — mostra no celular, o atendente escaneia, sem depender de ninguém

---

### Persona 3 — Camila · Gestora da Empresa Fornecedora

> **Role no sistema:** `MANAGER` (lado fornecedor) · **Prioridade: #3**

---

**Quem é**

Camila tem 38 anos, é Diretora Comercial de uma rede de escolas de idiomas com 3 unidades. Quer expandir a base corporativa da escola — empresas que compram planos de inglês para os funcionários como benefício.

---

**O que faz**

- Prospecta empresas para fechar acordos B2B (a escola vende planos em grupo para empresas)
- Negocia condições, assina contratos individuais com cada empresa contratante
- Não tem visibilidade de quantos funcionários de cada empresa estão usando o plano

---

**Suas dores**

- **Prospecção cara e lenta:** achar empresas interessadas depende de indicação, LinkedIn ou cold call
- **Processo manual:** cada parceria nova é uma negociação separada, sem padronização
- **Sem dados:** não sabe o ROI de ter 5 empresas parceiras vs 10 — não mede uso real
- **Sem vitrine:** quando uma empresa quer benefício de idioma, Camila não aparece na busca delas

---

**Como o Benefix ajuda**

- Cadastra os planos da escola na vitrine — aparece para todos os Managers buscando benefícios de idioma
- Recebe solicitações de parceria em vez de ter que prospectar ativamente
- Aprova/rejeita parcerias pelo sistema, sem e-mails
- Relatório de uso: sabe quantos funcionários de cada empresa parceira estão ativos

---

## Priorização

Quem atacar primeiro e por quê:

| # | Persona | Papel no negócio | Raciocínio |
|---|---|---|---|
| **1** | Ana Paula · Manager | Compra e opera a plataforma | Sem ela, o produto não existe. É quem assina o contrato com o Benefix, cadastra a empresa, convida funcionários e fecha parcerias. O produto deve resolver a vida dela acima de tudo. |
| **2** | Rafael · Funcionário | Usa diariamente | A experiência dele é o termômetro de retenção da Ana Paula. Se os funcionários adoram o Benefix, ela renova. Se reclamam, ela cancela. Ele não paga, mas tem poder de veto indireto. |
| **3** | Camila · Fornecedora | Alimenta a vitrine de benefícios | Sem fornecedoras, a vitrine está vazia. Mas o Benefix pode começar com parceiros âncora recrutados manualmente — a vitrine não precisa ser grande para entregar valor inicial. |

---

## Necessidades Cruzadas

O Benefix é um produto de dois lados (marketplace). As personas dependem umas das outras para que o produto funcione:

```
Ana Paula precisa de Camila  → vitrine só tem valor se houver benefícios cadastrados
Camila precisa de Ana Paula  → sem empresas contratantes, não há ROI em estar na vitrine
Rafael precisa de Ana Paula  → ela que contrata os benefícios que ele vai poder usar
Ana Paula precisa de Rafael  → adoção pelos funcionários justifica o investimento no Benefix
```

> **Implicação prática para o lançamento:** o Benefix precisa resolver o problema de pelo menos 1 Ana Paula com benefícios reais para usar — mesmo que sejam cadastrados manualmente, sem Camila. A Camila pode entrar depois.

---

*Ver também: [[Visão Geral]] · [[Requisitos]] · [[Roadmap]] · [[Negócio]]*
