# Domínio — Permission SaaS

Glossário das entidades, value objects e invariantes de negócio. Fonte de referência: `docs/DER.pdf`.

---

## `identity`

### Client

Cliente da plataforma — dono de projetos e assinante de um plano.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `name` | String | — |
| `email` | String | único |
| `phone` | String | opcional |
| `passwordHash` | String | nulo quando o login é via `provider` OAuth |
| `provider` / `providerId` | `AuthProvider` enum / String | `local`, `google`, `facebook`, `github` |
| `status` | `ClientStatus` enum | `active`, `inactive`, `blocked`, `deleted`, `pending` |
| `emailVerified`, `blocked`, `loginAttempts` | boolean / boolean / int | controle de acesso |
| `blockExpiresAt`, `emailVerifiedAt`, `lastLoginAt` | timestamp | — |
| `createdAt`, `updatedAt`, `deletedAt` | timestamp | soft delete via `deletedAt` |

---

## `billing`

### Plan

Plano contratável. Define os limites que `project` deve respeitar ao criar projetos/cargos.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `name` | String | — |
| `maxProjects` | int | limite de projetos simultâneos do cliente |
| `maxUsersPerProject` | int | limite de `EndUser` por projeto |
| `price` | BigDecimal | valor simulado (sem gateway real) |
| `active` | boolean | planos inativos não podem ser assinados |
| `createdAt` | timestamp | — |

### Subscription

Vínculo entre um `Client` e um `Plan`. Gera a `ApiKey` usada pelo módulo `permission`.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `clientId` | UUID | FK → `Client` |
| `planId` | UUID | FK → `Plan` |
| `status` | `SubscriptionStatus` enum | `pending`, `active`, `canceled`, `expired` |
| `startsAt`, `expiresAt` | timestamp | período de vigência |
| `createdAt` | timestamp | — |

**Invariante:** uma `ApiKey` só é gerada por uma `Subscription` com `status = active` — aplicado em `SubscribeToPlanUseCase`.

**Invariante:** um `Client` não pode ter duas `Subscription` com `status = active` ao mesmo tempo — aplicado em `SubscribeToPlanUseCase.handleExistingSubscription`:
- Já existe uma `active` para o **mesmo** plano e ainda dentro do prazo (`expiresAt` no futuro) → rejeita com erro (`Client already has an active subscription to this plan`).
- Já existe uma `active` para **outro** plano e ainda dentro do prazo → a antiga é marcada `canceled` (troca de plano) e sua `ApiKey` é revogada; a nova `Subscription`/`ApiKey` seguem o fluxo normal.
- Já existe uma `active` mas o prazo já passou → a antiga é marcada `expired` (dado que nenhum job periódico faz essa varredura ainda) e sua `ApiKey` é revogada; a nova assinatura é criada normalmente.

### ApiKey

Credencial usada pelos sistemas externos para chamar `POST /validate-permission`.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `subscriptionId` | UUID | FK → `Subscription` |
| `keyHash` | String | hash da chave (via `PasswordEncoder`), nunca a chave em texto puro |
| `plainKey` | String | **transiente** — existe só no instante da criação (retorno ao cliente); não é persistido, não tem coluna na migration |
| `active` | boolean | `false` após revogação |
| `createdAt`, `revokedAt` | timestamp | — |

Criada via Factory Method — ver `docs/PATTERNS.md`.

### PaymentRequest / PaymentResult

Value objects de entrada/saída do port `PaymentGateway` (`billing/domain/dto`). `PaymentRequest` carrega `subscriptionId` + `amount`; `PaymentResult` carrega `approved` + `transactionId`. Existem para que `SubscribeToPlanUseCase` nunca dependa do formato de um gateway específico — ver `docs/PATTERNS.md` (Adapter).

### SubscriptionResult

Value object de saída do `SubscribeToPlanUseCase` (`billing/domain/dto`). Agrupa a `Subscription` já ativada + a `ApiKey` recém-gerada (com `plainKey` em texto puro) — é o único ponto do sistema em que a chave em claro existe fora do `ApiKeyFactory`. O controller/`SubscriptionResponseMapper` extrai o `plainKey` para devolver ao cliente; depois disso só o `keyHash` sobrevive no banco.

---

## `permission`

### PermissionCheckRequest

Value object de entrada da `Chain of Responsibility` (`permission/domain/dto`). Carrega os três dados que qualquer sistema externo envia para `POST /validate-permission`: `apiKey` (String), `role` (String) e `route` (String).

### PermissionCheckResult

Value object de saída da chain (`permission/domain/dto`). Carrega `granted` (boolean) e `reason` (String — motivo da negação, ou `"granted"` quando aprovado). Criado só pelos factory methods `allow()` / `deny(reason)`, nunca pelo construtor do record diretamente.

**Invariante:** a chain para no primeiro handler que devolver `granted = false` — os handlers seguintes nunca são chamados (ver `docs/PATTERNS.md`, Chain of Responsibility).

**Limitação conhecida desta entrega:** `TokenValidationHandler` e `RoleRouteValidationHandler` sempre devolvem `allow()`, porque o módulo `project` (Role/Route) e um 2º fator de autenticação (Token) não foram implementados por falta de tempo — só `ApiKeyValidationHandler` aplica uma regra real hoje (a `ApiKey` precisa existir e estar `active`, consultando `billing`). Ver "trabalho futuro" em `docs/PLAN.md`.

---

## Planejado (ainda não modelado em código)

- **Project / Role / Route** (`project`) — Projeto do cliente, cargos e rotas protegidas, respeitando `Plan.maxProjects` / `Plan.maxUsersPerProject`.
- **RoleRoute** (`project`) — associação N:N entre `Role` e `Route`.
- **EndUser** (`project` ou `permission`) — usuário final de um projeto, vinculado a um `Role`.
- **AuditLog** (`audit`) — registro de cada validação de permissão (ação, rota, resultado, IP).

Ver estrutura completa em `docs/DER.pdf`.
