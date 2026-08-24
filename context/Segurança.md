# Segurança

## Autenticação

O BN emite JWT RS256 com issuer `bn-api` e validade padrão de três horas. Chaves privada e pública vêm de PEMs externos. Claims relevantes: `sub`, `email`, `accountId`, `groups`, `jti` e, quando existe empresa ativa, `companyId`.

O login retorna o perfil no body e grava o token no cookie `jwt`: `HttpOnly`, `SameSite=Strict`, `Secure` e `Domain` configuráveis, `Path=/` e `Max-Age=10800`. Clientes não-browser também podem usar Bearer token. Logout revoga o `jti` em memória até a expiração e remove o cookie.

## Roles

| Role | Escopo |
|---|---|
| `ADMIN` | criação administrativa de gestor |
| `MANAGER` | gestão de empresa e operação do provedor |
| `USER` | funcionário e uso dos benefícios |

O claim `groups` vira authority `ROLE_*`. Controllers usam `@PreAuthorize`; qualquer rota não pública exige autenticação.

Rotas públicas: login, onboarding, health check e `GET /benefits/public/**`.

## Multi-tenancy

Tenants compartilham schema. O `companyId` do JWT identifica a empresa ativa. `TenantGuard` confirma vínculo e escopo do recurso; `AccessStatusGuard` bloqueia entidades inativas.

Um gestor pode possuir vários vínculos. `POST /auth/switch-company` valida o vínculo e emite novo JWT. IDs enviados no payload identificam recursos, mas nunca concedem autorização.

## Proteções

- BCrypt para senhas;
- CORS com allowlist;
- cookie `SameSite=Strict` e API stateless;
- rate limit em login, onboarding e resgates;
- Jakarta Validation e tratamento global de erros;
- token de resgate guardado apenas como SHA-256, válido por três minutos e consumido atomicamente;
- logs não devem conter senha, JWT ou token bruto.

## Limitações conhecidas

- blocklist e rate limit não são distribuídos;
- Swagger depende de Basic Auth configurado no profile `docs`;
- mudanças em cookie, subdomínios ou CORS devem reavaliar a proteção CSRF, hoje desativada.
