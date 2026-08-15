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

**Limitação conhecida desta entrega:** `TokenValidationHandler` e `RoleRouteValidationHandler` sempre devolvem `allow()`, porque o módulo `project` (Role/Route) e um 2º fator de autenticação (Token) não foram implementados por falta de tempo — só `ApiKeyValidationHandler` aplica uma regra real hoje (a `ApiKey` precisa existir e estar `active`, consultando `billing`). Ver "trabalho futuro" em `docs/clean_code_e_padroes_de_projeto/PLAN.md`.

---

## `project`

### Project

Projeto de um cliente. Agrega os cargos (`Role`) e as rotas protegidas (`Route`) que o módulo `permission` consulta ao validar um acesso.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `clientId` | UUID | FK → `Client` |
| `name` | String | — |
| `description` | String | — |
| `maxRoles` | Integer | limite de cargos; **`null` = sem limite** |
| `roles` | `List<Role>` | 1-N |
| `routes` | `List<Route>` | 1-N |
| `isActive` | boolean | desativação — o projeto existe, mas não valida permissões |
| `createdAt`, `updatedAt` | timestamp | — |
| `deletedAt` | timestamp | **soft delete** — `null` enquanto o projeto existe |

**Invariante:** um `Project` não pode ter mais que `maxRoles` cargos — aplicado em `Project.addRole()`, lança `PlanLimitExceededException` (409). Quando `maxRoles` é `null`, não há limite.

**Invariante:** dois cargos do mesmo projeto não podem ter o mesmo `name`, comparação **case-insensitive** (`Admin` e `admin` são o mesmo cargo) — aplicado em `Project.addRole()`, lança `RoleAlreadyExistsException` (409).

**Invariante:** duas rotas do mesmo projeto não podem ter a mesma combinação `path` + `httpMethod`, comparação **case-insensitive** — aplicado em `Project.addRoute()`, lança `RouteAlreadyExistsException` (409). A chave é a combinação, não o `path` sozinho: `GET /users` e `POST /users` são rotas distintas e legítimas.

**Desativação ≠ exclusão.** São operações independentes, com campos próprios:

| Operação | Campo | Significado |
|---|---|---|
| `deactivate()` | `isActive = false` | projeto existe e é listado, mas está suspenso |
| exclusão (soft delete) | `deletedAt = now()` | projeto deixa de existir para o resto do sistema |

**Invariante:** um projeto excluído (`deletedAt != null`) não aceita **nenhuma** alteração de estado — `addRole()`, `addRoute()`, `activate()`, `deactivate()` e `delete()` lançam `ProjectAlreadyDeletedException` (409). A guarda está centralizada em `Project.ensureNotDeleted()`, chamada como primeira linha de cada mutador. Em particular, `activate()` não ressuscita um projeto excluído.

**Invariante:** `deactivate()` num projeto já inativo lança `ProjectAlreadyInactiveException` (409); `activate()` num projeto já ativo lança `ProjectAlreadyActiveException` (409). São transições de estado inválidas, não erros de programação — por isso herdam `BusinessRuleException` e viram 409, nunca 500.

**Consequência para as consultas:** toda leitura de `Project` filtra `deletedAt IS NULL`. No adapter in-memory das etapas 2-3 isso é um `filter` no stream; na etapa 4 vira `findByDeletedAtIsNull()`. Sem esse filtro, um projeto excluído continua aparecendo no `GET /projects` depois de um `DELETE` bem-sucedido.

O `toString()` de `Project` lista `roles` e `routes` — é a representação usada pela rotina de demonstração da Etapa 1.

### Role

Cargo dentro de um projeto. Referencia o projeto pai por `projectId` (UUID), não por objeto.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `projectId` | UUID | FK → `Project` |
| `name`, `description` | String | — |
| `isActive` | boolean | — |
| `createdAt`, `updatedAt` | timestamp | — |

### Route

Rota protegida de um projeto.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `projectId` | UUID | FK → `Project` |
| `name`, `description` | String | — |
| `path` | String | caminho protegido |
| `httpMethod` | String | — |
| `isActive` | boolean | — |
| `createdAt`, `updatedAt` | timestamp | — |

---

## `audit`

### AuditEvent (abstrata)

Raiz da trilha de auditoria. É abstrata porque a trilha guarda tipos heterogêneos de evento, cada um com campos próprios — a herança descreve o domínio, não existe só para atender a requisito.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `projectId` | UUID | FK → `Project`, guardado por id (o módulo `audit` não importa classes de `project`) |
| `occurredAt` | timestamp | default `now()` |

Contrato abstrato: `describe()` (resumo legível, usado no console e no arquivo de auditoria) e `type()` (discriminador — vira a `@DiscriminatorColumn` do mapeamento `SINGLE_TABLE`).

### PermissionCheckEvent

Resultado de uma validação executada pela chain do módulo `permission`. Evento de maior volume da trilha.

| Campo | Tipo | Observação |
|---|---|---|
| `routePath`, `httpMethod`, `roleName` | String | dados da tentativa de acesso |
| `granted` | boolean | resultado |
| `reason` | String | motivo da recusa; `null` quando concedida |
| `durationMs` | double | tempo gasto pela chain |
| `ipAddress`, `country` | String | origem; `country` enriquecido via OpenFeign |

`type()` = `PERMISSION_CHECK`.

### ProjectLifecycleEvent

Mudança estrutural em um projeto.

| Campo | Tipo | Observação |
|---|---|---|
| `action` | `LifecycleAction` enum | `CREATED`, `UPDATED`, `DELETED` |
| `projectName` | String | nome no momento do evento |
| `performedBy` | UUID | FK → `Client` responsável |

`type()` = `PROJECT_LIFECYCLE`.

---

## Planejado (ainda não modelado em código)

- **RoleRoute** (`project`) — associação N:N entre `Role` e `Route`.
- **EndUser** (`project` ou `permission`) — usuário final de um projeto, vinculado a um `Role`.

Ver estrutura completa em `docs/DER.pdf`.
