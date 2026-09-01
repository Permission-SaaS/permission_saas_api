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

Value object de entrada da `Chain of Responsibility` (`permission/domain/dto`). Carrega os cinco dados que qualquer sistema externo envia para `POST /validate-permission`: `apiKey` (String), `projectId` (UUID), `role` (String), `httpMethod` (String) e `route` (String).

`projectId` e `httpMethod` entraram na etapa 4: sem o primeiro não há como saber em qual projeto procurar a rota, e sem o segundo não dá para distinguir `GET /users` de `POST /users`, que são rotas diferentes pelo invariante do `Project`. Antes disso o evento de auditoria gravava `httpMethod = null`.

### PermissionCheckResult

Value object de saída da chain (`permission/domain/dto`). Carrega `granted` (boolean) e `reason` (String — motivo da negação, ou `"granted"` quando aprovado). Criado só pelos factory methods `allow()` / `deny(reason)`, nunca pelo construtor do record diretamente.

**Invariante:** a chain para no primeiro handler que devolver `granted = false` — os handlers seguintes nunca são chamados (ver `docs/PATTERNS.md`, Chain of Responsibility).

**Regras aplicadas hoje:** `ApiKeyValidationHandler` exige que a `ApiKey` exista e esteja `active` (consultando `billing`); `RoleRouteValidationHandler` exige, consultando `project`, que (1) o projeto exista e esteja ativo, (2) a rota `httpMethod` + `route` exista e esteja ativa nele, (3) o cargo exista e esteja ativo nele e (4) **exista uma concessão ativa daquele cargo naquela rota** (`RoleRoute`). `TokenValidationHandler` continua devolvendo `allow()` sempre — o 2º fator de autenticação está fora do escopo do projeto.

É o item (4) que responde à pergunta de negócio do produto: *o cargo X pode acessar a rota Y no projeto Z?* Sem ele, qualquer cargo alcançaria qualquer rota do projeto.

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

**Consequência para as consultas:** toda leitura de `Project` filtra `deletedAt IS NULL`. Nas etapas 2-3 isso era um `filter` no stream sobre o `Map`; desde a etapa 4 o filtro desceu para o banco, nas consultas derivadas `findByDeletedAtIsNullOrderByCreatedAtAsc()` e `findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc()`. Sem esse filtro, um projeto excluído continua aparecendo no `GET /projects` depois de um `DELETE` bem-sucedido.

**Exclusão definitiva.** A porta `ProjectRepository` ganhou `deleteById(UUID)` na etapa 4, usado só pelo `PurgeProjectUseCase` (`DELETE /projects/{id}/purge`). O soft delete continua sendo comportamento do agregado (`Project.delete()`); a remoção física é uma operação administrativa separada, que apaga cargos e rotas em cascata. Até a etapa 3 a porta não tinha esse método justamente para impedir que a exclusão lógica fosse contornada — a exceção agora é explícita e tem um único chamador.

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

> `Role` carrega `List<RoleRoute> permissions` — as concessões daquele cargo, incluindo as já revogadas. `activePermissions()` devolve só as vigentes e `hasActiveAccessTo(routeId)` é o que o `CheckRouteAccessUseCase` consulta.

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

### RoleRoute

Entidade associativa entre `Role` e `Route` (`project/domain/roleroute`). É ela que diz **quais rotas cada cargo pode acessar** e, por ter vida própria, também **desde quando** e **até quando**.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK |
| `roleId` | UUID | FK → `Role` |
| `routeId` | UUID | FK → `Route` |
| `grantedAt` | timestamp | quando o acesso foi concedido |
| `revokedAt` | timestamp | `null` enquanto a concessão está válida |

**Por que entidade associativa e não uma tabela de junção simples.** Uma tabela `(role_id, route_id)` responde "pode agora?" e nada mais: revogar seria apagar a linha, e a informação de que aquele cargo já teve acesso desapareceria. Com `grantedAt`/`revokedAt`, revogar é **fechar** a linha, não removê-la — e a trilha de auditoria passa a conseguir responder *quando o cargo X deixou de poder acessar a rota Y*, que é exatamente o tipo de pergunta que um sistema de permissões precisa sustentar.

**Invariante:** um cargo tem no máximo **uma** concessão ativa por rota — aplicado em `Project.grantRouteToRole()`, lança `RouteAccessAlreadyGrantedException` (409), e garantido no banco pelo índice único **parcial** `uq_role_routes_active ON role_routes (role_id, route_id) WHERE revoked_at IS NULL`. O "parcial" é o que permite empilhar histórico: conceder → revogar → conceder de novo gera duas linhas para o mesmo par, e só a última fica ativa.

**Invariante:** só se concede o que pertence ao projeto. `Project.grantRouteToRole()` valida que o cargo e a rota são do próprio agregado antes de criar a concessão — `RoleNotFoundException` / `RouteNotFoundException` (404). O `Project` é o agregado raiz justamente porque é o único lugar que enxerga cargos e rotas ao mesmo tempo.

**Invariante:** revogar exige uma concessão ativa. `Project.revokeRouteFromRole()` lança `RouteAccessNotFoundException` (404) se não houver, e `RoleRoute.revoke()` lança `RouteAccessAlreadyRevokedException` (409) se a linha já estiver fechada.

**`isActive()` é derivado**, não armazenado: `revokedAt == null`. Guardar um booleano *e* a data permitiria os dois divergirem; com um campo só, o estado é sempre consistente.

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

- **EndUser** (`project` ou `permission`) — usuário final de um projeto, vinculado a um `Role`.

### `userId` em `PermissionCheckEvent` — saber *quem* tentou, não só com qual cargo

Hoje a trilha registra **o cargo** que tentou o acesso (`roleName`), mas não a pessoa. Duas tentativas
negadas do mesmo cargo são indistinguíveis, e a pergunta mais comum numa investigação — *quem tentou
acessar isso?* — não tem resposta.

**Feature:** acrescentar `userId` ao `PermissionCheckEvent`, preenchido a partir da requisição de
validação.

**Decisões a tomar antes de implementar:**

| Questão | Encaminhamento proposto |
|---|---|
| De quem é esse usuário? | É o **usuário final do sistema cliente**, não o `Client` do nosso `identity`. Quem sabe quem ele é é quem chama a API; portanto o valor vem no corpo de `POST /validate-permission`, não de uma sessão nossa |
| Qual o tipo da coluna? | `VARCHAR`, não `UUID`. Cada cliente identifica seus usuários como quiser — id numérico, e-mail, UUID, login. Um `UUID` obrigaria todos ao nosso formato |
| Tem FK? | Não, pelo mesmo motivo de `projects.client_id` e `audit_events.project_id` (ADR-004): a referência aponta para fora do nosso banco |
| É obrigatório? | Não no primeiro momento. Torná-lo `@NotBlank` quebra todo cliente já integrado; entra opcional e a obrigatoriedade vira uma decisão de versionamento do contrato |
| E o `performedBy` que já existe? | É outro conceito e fica como está: em `ProjectLifecycleEvent` ele identifica o **`Client`** que mexeu no projeto. `userId` é o usuário final de quem sofreu a validação |

**Onde a mudança bate:**

1. Migration `V10` — `ALTER TABLE audit_events ADD COLUMN user_id VARCHAR(120)`, mais um índice
   (`user_id`, `occurred_at DESC`), que é como a consulta de investigação vai filtrar.
2. `PermissionCheckEvent.userId` e `describe()` passando a citá-lo.
3. `PermissionCheckEventJpaEntity` e o `AuditEventRepositoryAdapter` nos dois sentidos.
4. `ValidatePermissionRequest` → `PermissionCheckRequest` → `PermissionValidatedEvent` →
   `AuditLogListener`: é o mesmo caminho que `projectId` e `httpMethod` percorreram na etapa 4, e serve
   de roteiro.
5. `AuditEventQuery` + `GET /audit-events?userId=` para consultar, com consulta derivada
   `findByUserIdOrderByOccurredAtDesc`.
6. Coleção Postman e `docs/API.md`.

**Ressalva de privacidade:** o campo passa a guardar um identificador de pessoa vindo de outro sistema.
Vale registrar por quanto tempo a trilha é retida e evitar aceitar ali dados que identifiquem além do
necessário — um id opaco basta, e-mail já é dado pessoal.

Ver estrutura completa em `docs/DER.pdf`.
