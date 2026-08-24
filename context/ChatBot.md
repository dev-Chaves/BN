# ChatBot — Integração com o ChatBot do Rômulo

> Voltar ao rascunho principal: [[Benefix]]

---

## Visão Geral

O Benefix terá integração com um chatbot desenvolvido pelo **Rômulo** (dev do time). O chatbot permitirá que funcionários e gestores consultem informações sobre benefícios, parcerias e regras da empresa usando linguagem natural.

A integração entre o Benefix e o chatbot será feita via **API REST** — o chatbot consome endpoints do Benefix para buscar contexto. O protocolo e os endpoints **ainda não estão definidos** e precisam ser acordados com o Rômulo.

---

## O que precisamos definir (com o Rômulo)

- [x] Quais endpoints o chatbot vai consumir do Benefix?

* **Reposta:** Criaremos um endpoint que só poderá acessado pela ROLE_BOT.

- [ ] Como o chatbot vai se autenticar na API? (API Key dedicada? JWT com role especial? Token de serviço?)
- [ ] O chatbot poderá escrever dados no Benefix ou apenas ler? (ex: criar uma Subscription em nome do usuário)

* **Resposta:** Ele irá gravar apenas no banco de dados não relacional, **MongoDB**.

- [ ] Qual protocolo de integração? REST puro? MCP (Model Context Protocol)?
- [x] O chatbot vai rodar junto com o Benefix ou em serviço separado?

* **Resposta:** Irá rodar em serviço separado

- [x] Qual LLM será usado? (OpenAI, Gemini, Ollama local?)
* **Resposta:** Iremos usar GROQ Free tier

---

## Armazenamento de conversas — MongoDB

As conversas do chatbot serão armazenadas em **MongoDB**, separado do PostgreSQL do Benefix.

**Por que MongoDB aqui:**
- Mensagens são documentos sem schema fixo (podem variar por tipo de conversa)
- Volume de escrita potencialmente alto (cada mensagem é um insert)
- Não faz sentido relacional — não há joins entre mensagens
- Fácil de consultar por `accountId`, por período ou por conversa

**Finalidade:**
- **Auditoria** — saber o que foi perguntado, quando e por quem
- **Controle** — identificar usos indevidos ou perguntas fora do escopo
- **Histórico** — o chatbot pode usar mensagens anteriores como contexto

**Estrutura de documento (esboço):**
```json
{
  "_id": "ObjectId",
  "accountId": "123",
  "conversationId": "uuid-da-conversa",
  "role": "user | assistant",
  "content": "Quais benefícios eu tenho?",
  "createdAt": "2026-03-06T20:00:00Z",
  "metadata": {
    "companyId": "42",
    "benefitIds": []
  }
}
```

**Perguntas em aberto:**
- [ ] Retenção: quantos dias de histórico guardar?
- [ ] Quem pode consultar o histórico? Só ADMIN? Manager vê as conversas dos funcionários da empresa?
- [x] O MongoDB fica no mesmo servidor ou em serviço separado (Atlas)?
- **Resposta:** Atlasi

---

## O que o Benefix precisa preparar

Independente dos endpoints definitivos, o Benefix precisa estar pronto para:

- [x] Springdoc disponível em `/q/openapi` e `/q/swagger-ui` no profile `docs`, com Basic Auth; ainda falta definir um contrato MCP específico
- [ ] Autenticação por API Key ou token de serviço para o chatbot (sem login humano)
- [ ] Endpoints de contexto leves (a definir com Rômulo) — respostas simples e rápidas, sem dados desnecessários
- [ ] Definir o que o chatbot pode ou não fazer (apenas leitura por enquanto?)

---

## Stack (parcial)

| Componente | Tecnologia | Status |
|---|---|---|
| API do Benefix | Spring Boot 4 | ✅ Em desenvolvimento |
| ChatBot | A cargo do Rômulo | 🔄 A definir |
| Histórico de conversas | MongoDB | 📋 Planejado |
| LLM | A definir | ❓ Em aberto |
| Autenticação chatbot↔API | A definir com Rômulo | ❓ Em aberto |

---

*Ver também: [[API]] · [[Requisitos]] · [[Roadmap]]*
