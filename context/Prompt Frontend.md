# Prompt — Frontend do Benefix

> Arquivo de referência para gerar ou instruir um agente/LLM a construir o frontend completo do Benefix.

---

## Contexto do Projeto

Você é um desenvolvedor frontend sênior contratado para construir a interface web do **Benefix** — uma plataforma B2B SaaS de gestão e descoberta de benefícios corporativos.

O backend está implementado em **Spring Boot 4.1 com Java 25**, expõe uma API REST com JWT RS256 e usa cookie `jwt` httpOnly como mecanismo principal no browser. A documentação Swagger fica em `http://localhost:8080/q/swagger-ui` quando o profile `docs` está ativo e exige Basic Auth. Consulte [API](API.md) para o contrato atual antes de gerar o frontend.

---

## O que é o Benefix

O Benefix resolve dois problemas simultâneos:

1. **Para empresas (Managers/RH):** centralizar a contratação de benefícios e o fechamento de parcerias B2B — sem planilhas, sem e-mails, sem sistemas fragmentados.
2. **Para funcionários (Employees):** ter um único lugar para ver todos os benefícios disponíveis, solicitar adesão e usar o benefício via QR Code.

O principal diferencial é a **Vitrine de Benefícios**: empresas fornecedoras cadastram seus serviços; empresas contratantes descobrem e solicitam parcerias diretamente na plataforma.

---

## Design System

### Paleta de Cores ("Quite Clear")

```css
--color-grey:   #CBCBCB;
--color-white:  #F2F2F2;
--color-green:  #174D38;  /* cor primária / brand */
--color-wine:   #4D1717;  /* cor de destaque / danger */
```

### Tipografia

| Uso | Fonte | Observação |
|-----|-------|-----------|
| Títulos (`h1`, `h2`, `h3`, nomes de seção) | **Sebino** | Serif display, personalidade e identidade da marca |
| Corpo de texto, parágrafos, labels, tabelas | **Core Serif N** | Serif legível para leitura contínua |

Importe as fontes via `@font-face` no `globals.css` ou via provedor de fontes (Google Fonts / self-hosted). Configure no `tailwind.config.ts`:

```ts
theme: {
  extend: {
    fontFamily: {
      heading: ['Sebino', 'serif'],
      body:    ['Core Serif N', 'serif'],
    },
  },
}
```

Aplique por padrão no `layout.tsx` raiz:

```tsx
<body className="font-body">
  {children}
</body>
```

E use `font-heading` em todos os títulos (`<h1>`, `<h2>`, `CardTitle`, headers de seção).

### Princípios de UI

- Visual **limpo e corporativo** — não é um app de consumidor, é uma ferramenta B2B
- Sidebar de navegação lateral fixa (desktop); menu hambúrguer (mobile)
- **Títulos em Sebino** (serif display) para identidade visual forte
- **Textos e conteúdo em Core Serif N** para leitura confortável
- Cards com bordas arredondadas suaves, sombra leve
- Tabelas para listagens com paginação
- Badges coloridos para status: `PENDING` (amarelo), `ACTIVE` (verde), `DISABLE` (cinza), `REJECTED` (vinho)

---

## Autenticação e Roles

### Fluxo de Login

```
POST /auth/login  { email, password }
→ Response: { token: "eyJ..." }
→ Salvar token no localStorage (ou cookie httpOnly)
→ No browser, enviar credenciais/cookies (`credentials: "include"`). O token não deve ficar acessível ao JavaScript. Bearer token é suportado para clientes não-browser.
```

### Três Roles com Experiências Distintas

| Role | Quem é | O que vê |
|------|--------|----------|
| `ADMIN` | Superusuário da plataforma | Gestão de empresas, visão geral |
| `MANAGER` | Gestor/RH de uma empresa | Benefícios, parcerias, funcionários da sua empresa |
| `USER` | Funcionário | Vitrine, meus benefícios, QR Code de uso |

Após o login, decodifique o JWT para extrair a role e redirecione para o dashboard correto:
- `ADMIN` → `/admin/dashboard`
- `MANAGER` → `/manager/dashboard`
- `USER` → `/employee/dashboard`

### Rota Pública de Entrada

```
POST /onboarding  (empresa + primeiro manager)
```
Página `/register` pública para novas empresas se cadastrarem.

---

## Estrutura de Rotas

```
/                           → Redirect para /login ou dashboard (se autenticado)
/login                      → Página de login
/register                   → Onboarding: nova empresa + primeiro manager

/admin/
  dashboard                 → Visão geral da plataforma
  companies                 → Listar/gerenciar empresas

/manager/
  dashboard                 → Métricas rápidas da empresa
  benefits                  → Gerenciar benefícios que a empresa oferece
  benefits/new              → Criar novo benefício
  benefits/[id]/edit        → Editar benefício
  partnerships              → Parcerias da empresa (solicitadas e recebidas)
  partnerships/[id]         → Detalhe de parceria (aprovar/rejeitar/desativar)
  employees                 → Gerenciar funcionários da empresa
  employees/new             → Cadastrar funcionário
  vitrine                   → Vitrine de benefícios de outras empresas

/employee/
  dashboard                 → Resumo dos benefícios do funcionário
  vitrine                   → Todos os benefícios disponíveis para a empresa
  my-benefits               → Benefícios que o funcionário assinou (subscriptions)
  my-benefits/[id]/use      → Tela de uso: gerar QR Code para o benefício

/checkin/validate/[token]   → Página PÚBLICA que o atendente abre ao escanear QR
```

---

## Telas e Componentes por Área

### 🔐 Autenticação

**`/login`**
- Formulário: email + senha
- Link para `/register`
- Erro de credenciais inválidas inline (não alert)

**`/register` (Onboarding)**
- Formulário em duas seções: **Empresa** (nome, CNPJ) + **Gestor** (nome, email, senha)
- Validação de CNPJ no frontend
- Sucesso → redireciona para `/login` com mensagem de confirmação

---

### 👔 Manager — Área Principal

**`/manager/dashboard`**
- Cards de resumo: total de benefícios ativos, parcerias pendentes, total de funcionários
- Lista rápida de parcerias com status `PENDING` aguardando ação
- Atalhos rápidos para as principais ações

**`/manager/benefits`**
- Tabela: nome, descrição, status (ativo/inativo), data de criação
- Botão "Novo Benefício"
- Ações por linha: editar, ativar/desativar, excluir
- Badge de status: verde (ativo) / cinza (inativo)

**`/manager/benefits/new` e `/manager/benefits/[id]/edit`**
- Campos: nome, descrição, categoria (select com enum `BenefitCategory`)
- Toggle "Ativar benefício imediatamente"

**`/manager/partnerships`**
- Duas abas: **"Recebidas"** (parcerias de outras empresas solicitando benefícios seus) e **"Enviadas"** (parcerias que minha empresa solicitou)
- Tabela com: empresa, benefício, status (badge), data
- Ações: aprovar, rejeitar, desativar (conforme status e permissão)

**`/manager/partnerships/[id]`**
- Detalhe da parceria
- Informações da empresa solicitante e do benefício
- Botões de ação contextuais por status:
  - `PENDING` recebida → **Aprovar** (verde) + **Rejeitar** (vinho)
  - `ACTIVE` → **Desativar** (cinza)
  - `REJECTED` / `DISABLE` → sem ações

**`/manager/employees`**
- Tabela de funcionários com status (`ACTIVE` / `DISABLE`)
- Ações: editar, ativar/desativar

**`/manager/vitrine`**
- Grid de cards de benefícios de outras empresas (benefícios ativos de terceiros)
- Filtros: categoria, busca textual, empresa fornecedora
- Card de benefício: nome, descrição, empresa fornecedora, categoria (badge)
- Botão "Solicitar Parceria" (abre modal de confirmação)
- Se já há parceria existente: mostrar status atual no lugar do botão

---

### 👤 Employee — Área do Funcionário

**`/employee/dashboard`**
- Cards: total de benefícios assinados, notificações
- Lista resumida de "Meus Benefícios" (subscriptions ativas)

**`/employee/vitrine`**
- Grid de cards dos benefícios disponíveis para sua empresa (onde há Partnership `ACTIVE`)
- Filtros: categoria, busca
- Card mostra se funcionário já assinou o benefício
- Botão "Assinar" para criar Subscription

**`/employee/my-benefits`**
- Lista de subscriptions do funcionário autenticado
- Cada card: nome do benefício, empresa fornecedora, data de adesão, botão "Usar agora"

**`/employee/my-benefits/[id]/use`**
- Tela de uso do benefício
- Botão "Gerar QR Code" → chama `POST /checkin/start`
- Exibe QR Code gerado client-side via `qrcode.react` a partir do token retornado
- Countdown do tempo de validade (ex: 5 minutos)
- Exibe também o código alfanumérico como fallback
- Status: `Aguardando confirmação...` / `✅ Confirmado!` / `⏰ Expirado`

---

### 🔓 Página Pública de Validação

**`/checkin/validate/[token]`**
- **Sem autenticação** — acessada pelo atendente ao escanear o QR
- Faz `GET /checkin/validate/{token}`
- Exibe: nome do funcionário, empresa, benefício, validade
- Botão grande "✅ Confirmar Uso" → `POST /checkin/confirm/{token}`
- Estados: válido / expirado / já utilizado
- Design simples, funciona bem em tela de celular

---

## Endpoints da API

Base URL: `http://localhost:8080`

### Auth
```
POST   /auth/login                       → { token }
POST   /onboarding                       → criar empresa + manager
```

### Benefits
```
GET    /benefit                          → lista (vitrine)
GET    /benefit/{id}
POST   /benefit                          → MANAGER
PUT    /benefit/{id}                     → MANAGER
DELETE /benefit/{id}                     → MANAGER
PATCH  /benefit/{id}/activate            → MANAGER (ativar)
PATCH  /benefit/{id}/deactivate          → MANAGER (desativar)
```

### Partnerships
```
GET    /partnership
GET    /partnership/{id}
POST   /partnership                      → MANAGER (solicitar)
PATCH  /partnership/{id}/accept          → MANAGER (aprovar)
PATCH  /partnership/{id}/reject          → MANAGER (rejeitar) [Sprint 3]
PATCH  /partnership/{id}/disable         → MANAGER (desativar) [Sprint 3]
DELETE /partnership/{id}                 → MANAGER
```

### Employees
```
GET    /employee
GET    /employee/{id}
POST   /employee                         → MANAGER
PUT    /employee/{id}                    → MANAGER
DELETE /employee/{id}                    → MANAGER
```

### Managers
```
GET    /manager
GET    /manager/{id}
POST   /manager
PUT    /manager/{id}
DELETE /manager/{id}
```

### Subscriptions
```
GET    /subscription                     → lista geral
GET    /subscription/my                  → subscriptions do employee autenticado [Sprint 4]
GET    /subscription/{id}
POST   /subscription                     → criar (employee)
```

### CheckIn (futuro próximo)
```
POST   /checkin/start                    → gera token QR { token, expiresAt }
GET    /checkin/validate/{token}         → PÚBLICO: valida QR
POST   /checkin/confirm/{token}          → PÚBLICO: confirma uso
```

### Companies
```
GET    /company
GET    /company/{id}
POST   /company                          → ADMIN
PUT    /company/{id}
DELETE /company/{id}
```

---

## Estrutura de Arquivos Sugerida

```
src/
├── app/
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── admin/
│   │   ├── layout.tsx               ← sidebar de admin
│   │   └── dashboard/page.tsx
│   ├── manager/
│   │   ├── layout.tsx               ← sidebar de manager
│   │   ├── dashboard/page.tsx
│   │   ├── benefits/
│   │   │   ├── page.tsx
│   │   │   ├── new/page.tsx
│   │   │   └── [id]/edit/page.tsx
│   │   ├── partnerships/
│   │   │   ├── page.tsx
│   │   │   └── [id]/page.tsx
│   │   ├── employees/page.tsx
│   │   └── vitrine/page.tsx
│   ├── employee/
│   │   ├── layout.tsx               ← sidebar de employee
│   │   ├── dashboard/page.tsx
│   │   ├── vitrine/page.tsx
│   │   └── my-benefits/
│   │       ├── page.tsx
│   │       └── [id]/use/page.tsx
│   └── checkin/
│       └── validate/[token]/page.tsx ← rota pública
│
├── components/
│   ├── ui/                          ← shadcn/ui components
│   ├── layout/
│   │   ├── Sidebar.tsx
│   │   ├── Header.tsx
│   │   └── ProtectedRoute.tsx
│   ├── benefits/
│   │   ├── BenefitCard.tsx
│   │   ├── BenefitForm.tsx
│   │   └── BenefitFilters.tsx
│   ├── partnerships/
│   │   ├── PartnershipTable.tsx
│   │   └── PartnershipStatusBadge.tsx
│   ├── subscription/
│   │   └── QRCodeDisplay.tsx
│   └── shared/
│       ├── StatusBadge.tsx
│       ├── ConfirmDialog.tsx
│       └── DataTable.tsx
│
├── lib/
│   ├── api/
│   │   ├── axios.ts                 ← instância com interceptor JWT
│   │   ├── auth.ts
│   │   ├── benefits.ts
│   │   ├── partnerships.ts
│   │   ├── subscriptions.ts
│   │   └── checkin.ts
│   ├── auth/
│   │   ├── useAuth.ts               ← hook de autenticação
│   │   └── decode-token.ts          ← extrair role do JWT
│   └── utils.ts
│
└── types/
    ├── auth.ts
    ├── benefit.ts
    ├── company.ts
    ├── employee.ts
    ├── partnership.ts
    └── subscription.ts
```

---

## Tipos TypeScript Principais

```typescript
// types/auth.ts
interface LoginRequest { email: string; password: string }
interface LoginResponse { token: string }
type Role = 'ADMIN' | 'MANAGER' | 'USER'

// types/benefit.ts
type BenefitCategory = 'SAUDE' | 'EDUCACAO' | 'LAZER' | 'ALIMENTACAO' | 'OUTROS'
interface Benefit {
  id: number
  name: string
  description: string
  category: BenefitCategory
  active: boolean
  provider: { id: number; name: string }
  createdAt: string
}

// types/partnership.ts
type PartnershipStatus = 'PENDING' | 'ACTIVE' | 'DISABLE' | 'REJECTED'
interface Partnership {
  id: number
  clientCompany: { id: number; name: string }
  benefit: Benefit
  status: PartnershipStatus
  createdAt: string
}

// types/subscription.ts
interface Subscription {
  id: number
  benefit: Benefit
  employee: { id: number; name: string }
  validationCode: string
  createdAt: string
}

// types/checkin.ts
type CheckInStatus = 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED'
interface CheckInStartResponse {
  token: string
  expiresAt: string
}
interface CheckInValidateResponse {
  employeeName: string
  companyName: string
  benefitName: string
  valid: boolean
  status: CheckInStatus
}
```

---

## Configuração do Axios (interceptor JWT)

```typescript
// lib/api/axios.ts
import axios from 'axios'

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('benefix_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('benefix_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
```

---

## Comportamentos Importantes

### Controle de Acesso no Frontend

- Após login, decodifique o JWT (sem verificar assinatura — só leitura do payload) para obter `role`
- Middleware do Next.js deve proteger rotas por prefixo:
  - `/admin/*` → apenas `ADMIN`
  - `/manager/*` → apenas `MANAGER`
  - `/employee/*` → apenas `USER`
- Se role incompatível: redirecionar para `/login`

### Feedback de Erros da API

O backend retorna sempre:
```json
{ "message": "...", "status": 404, "timestamp": "..." }
```
Use o campo `message` para exibir erros ao usuário via toast/notification — nunca mostrar stacktrace.

### Badges de Status

```typescript
const statusConfig = {
  PENDING:  { label: 'Pendente',   color: 'bg-yellow-100 text-yellow-800' },
  ACTIVE:   { label: 'Ativo',      color: 'bg-green-100  text-green-800'  },
  DISABLE:  { label: 'Inativo',    color: 'bg-gray-100   text-gray-600'   },
  REJECTED: { label: 'Rejeitado',  color: 'bg-red-100    text-red-800'    },
}
```

### Vitrine — Filtros

`GET /benefit?category=SAUDE&q=dental&companyId=1`

Todos os parâmetros são opcionais. Implemente como query params gerenciados pelo TanStack Query com `keepPreviousData`.

### QR Code (tela de uso do benefício)

```typescript
import QRCode from 'qrcode.react'

// O token retornado pelo backend é usado para montar a URL de validação
const validationUrl = `${window.location.origin}/checkin/validate/${token}`

<QRCode value={validationUrl} size={256} />
```

Adicionar countdown visual dos minutos restantes até `expiresAt`. Ao expirar: desabilitar o QR e oferecer botão "Gerar novo código".

---

## Variáveis de Ambiente

```env
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## O que NÃO implementar agora (futuro)

- ChatBot (Rômulo) — será serviço separado
- Notificações em tempo real (WebSocket/SSE)
- Dashboard com métricas avançadas
- GraalVM / build nativo (só backend)
- Upload de logo de empresa
- Frontend mobile nativo (apenas responsive web por ora)

---

## Resumo do que entregar

1. **Autenticação** — login, logout, registro (onboarding), proteção de rotas por role
2. **Manager:** CRUD de benefícios, vitrine com filtros, gestão de parcerias (aprovar/rejeitar/desativar), gestão de funcionários
3. **Employee:** vitrine dos benefícios disponíveis para sua empresa, assinatura de benefícios, tela de uso com QR Code + countdown
4. **Validação pública:** página `/checkin/validate/[token]` sem auth para o atendente confirmar o uso
5. **Design system** aplicado: paleta "Quite Clear", badges de status, tabelas com paginação, toasts de feedback

---

*Contexto completo disponível em: Visão Geral · API · Domínios · Segurança · Negócio · Sprint Plan*
