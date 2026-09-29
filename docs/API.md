# API — Permission SaaS

Cada endpoint implementado: método, path, request/response e um exemplo de `curl`. As seções por módulo descrevem a aplicação principal (`permission-service`, porta 8080); o serviço extraído tem sua própria seção no fim, [`audit-service`](#audit-service--serviço-independente-porta-8081).

Uma coleção Postman com todos os endpoints, encadeados por variáveis (`clientId` → `planId` → `apiKey` → `projectId`) e com asserções de status, está versionada em `docs/postman/permission-saas.postman_collection.json`. Para rodar a coleção inteira sem abrir o Postman:

```bash
npx newman run docs/postman/permission-saas.postman_collection.json
```

## Formato padrão de erro

Todo erro (em qualquer endpoint) é tratado por `shared/api/GlobalExceptionHandler` e devolvido no mesmo formato (`ErrorResponse`):

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Client not found",
  "timestamp": "2026-07-10T12:00:00Z"
}
```

| Situação | Status |
|---|---|
| Recurso não encontrado por id — categoria `ResourceNotFoundException`, lançada como `ClientNotFoundException`/`PlanNotFoundException` | `404 Not Found` |
| Regra de negócio violada — categoria `BusinessRuleException`, lançada como `EmailAlreadyInUseException`/`PaymentDeclinedException`/`ActiveSubscriptionExistsException` | `409 Conflict` |
| Falha de validação `@Valid` no request DTO | `400 Bad Request` |
| Qualquer erro não mapeado | `500 Internal Server Error` |

Ver `docs/ARCHITECTURE.md` → "Tratamento de exceções" para detalhes de implementação.

---

## `identity`

### `POST /clients/register`

Cadastra um novo `Client`.

**Request** (`RegisterClientRequest`):
```json
{
  "name": "Jairo Neto",
  "email": "jairo@example.com",
  "phone": "11999999999",
  "rawPassword": "senha123",
  "provider": "local",
  "providerId": null
}
```

**Response** `201 Created` (`ClientResponse`):
```json
{
  "id": "8f14e45f-ceea-4c72-8a13-000000000000",
  "name": "Jairo Neto",
  "email": "jairo@example.com",
  "phone": "11999999999",
  "provider": "local",
  "providerId": null,
  "status": "active",
  "emailVerified": false,
  "blocked": false,
  "loginAttempts": 0,
  "blockExpiresAt": null,
  "emailVerifiedAt": null,
  "lastLoginAt": null,
  "createdAt": "2026-07-04T12:00:00Z",
  "updatedAt": "2026-07-04T12:00:00Z",
  "deletedAt": null
}
```

**Erros:** `409 Conflict` se `email` já estiver em uso (`existsByEmail` → `EmailAlreadyInUseException`).

```bash
curl -X POST http://localhost:8080/clients/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Jairo Neto","email":"jairo@example.com","phone":"11999999999","rawPassword":"senha123"}'
```

**Importante:** o `id` retornado aqui (ou em `GET /clients`) é o `clientId` usado em `POST /subscriptions` — não há outra forma de obtê-lo pela API.

---

### `GET /clients`

Lista todos os `Client` cadastrados.

**Response** `200 OK` — array de `ClientResponse` (mesmo formato do registro acima).

```bash
curl http://localhost:8080/clients
```

---

### `GET /clients/{clientId}`

Busca um `Client` pelo id.

**Response** `200 OK` (`ClientResponse`, mesmo formato acima). **Erros:** `404 Not Found` se `clientId` não existir (`FindClientByIdUseCase` → `ClientNotFoundException`).

```bash
curl http://localhost:8080/clients/8f14e45f-ceea-4c72-8a13-000000000000
```

---

## `billing`

### `POST /plans`

Cadastra um `Plan`.

**Request** (`RegisterPlanRequest`): `name` (obrigatório), `description`, `maxProjects`, `maxUsersPerProject`, `price` (obrigatório, positivo).

**Response** `201 Created` (`PlanResponse`).

> Até a disciplina de Spring Boot este era o único endpoint de criação que devolvia **200**, e a divergência estava registrada aqui como conhecida. Corrigida na disciplina de microsserviços: agora os quatro endpoints de criação (`POST /clients/register`, `POST /subscriptions`, `POST /projects` e este) devolvem **201 Created**, e a coleção Postman assere 201.

**Erros:** `500 Internal Server Error` se o `name` já existir — a constraint `uq_plans_name` estoura no banco e não há exceção de domínio correspondente, então cai no handler genérico. O correto seria `409 Conflict` via uma `PlanNameAlreadyInUseException`; registrado como pendência.

```bash
curl -X POST http://localhost:8080/plans \
  -H "Content-Type: application/json" \
  -d '{"name":"Pro","description":"Plano padrão","maxProjects":10,"maxUsersPerProject":50,"price":99.90}'
```

---

### `GET /plans/{planId}`

Busca um `Plan` pelo id.

**Response** `200 OK` (`PlanResponse`):
```json
{
  "id": "3f2504e0-4f89-11d3-9a0c-000000000000",
  "name": "pro",
  "maxProjects": 10,
  "maxUsersPerProject": 50,
  "price": 99.90,
  "active": true
}
```

**Erros:** `404 Not Found` se `planId` não existir (`FindPlanByIdUseCase` → `PlanNotFoundException`).

```bash
curl http://localhost:8080/plans/3f2504e0-4f89-11d3-9a0c-000000000000
```

---

### `POST /subscriptions`

Assina um `Plan` para um `Client` já cadastrado. Cobra via `PaymentGateway` (simulado); se aprovado, ativa a `Subscription` e gera uma `ApiKey`.

**Request** (`SubscribeToPlanRequest`):
```json
{
  "clientId": "8f14e45f-ceea-4c72-8a13-000000000000",
  "planId": "3f2504e0-4f89-11d3-9a0c-000000000000"
}
```

**Response** `201 Created` (`SubscriptionResponse`):
```json
{
  "subscriptionId": "b1a2c3d4-0000-0000-0000-000000000000",
  "status": "active",
  "startsAt": "2026-07-04T12:00:00Z",
  "expiresAt": "2026-08-04T12:00:00Z",
  "apiKey": "sk_1a2b3c4d5e6f..."
}
```

**Importante:** `apiKey` só aparece em texto puro nesta resposta. Depois disso só o hash (`keyHash`) é persistido — não há endpoint para recuperar a chave em claro novamente.

**Erros:** `404 Not Found` se `clientId` ou `planId` não existirem (`FindClientByIdUseCase` → `ClientNotFoundException`, `FindPlanByIdUseCase` → `PlanNotFoundException`); `409 Conflict` se o pagamento for recusado — marca a `Subscription` como `canceled`/rejeitada e lança `PaymentDeclinedException` (sem gerar `ApiKey`); `409 Conflict` se o `Client` já possuir uma `Subscription` `active` (dentro do prazo) para o **mesmo** `planId` (`ActiveSubscriptionExistsException`, mensagem `"Client already has an active subscription to this plan..."`).

> **Pendência — `402 Payment Required` para pagamento recusado.** Hoje os dois conflitos deste
> endpoint devolvem **409**, porque `ActiveSubscriptionExistsException` e `PaymentDeclinedException`
> estendem a mesma `BusinessRuleException` e o `GlobalExceptionHandler` mapeia a família inteira
> para `409 Conflict`.
>
> Só um dos dois é realmente um conflito. `409` significa que a requisição conflita com o **estado
> atual do recurso** — é exatamente o caso de "já existe assinatura ativa para este plano". Um
> pagamento recusado não conflita com estado nenhum: é uma operação externa que falhou, e o código
> que existe para isso é `402 Payment Required`.
>
> A correção é um handler específico, que passa na frente do handler da família por ser mais
> específico:
>
> ```java
> @ExceptionHandler(PaymentDeclinedException.class)
> ResponseEntity<ErrorResponse> handlePaymentDeclined(PaymentDeclinedException ex) {
>     return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
> }
> ```
>
> Alcance da mudança: `shared/api/GlobalExceptionHandler.java`, o parágrafo de erros acima e o
> `@ApiResponse` do `SubscriptionController` (o exemplo "Pagamento recusado" sairia do 409 para um
> 402 próprio). A coleção Postman **não** é afetada — nenhum request dela exercita pagamento
> recusado nem assinatura duplicada.

**Troca de plano:** se o `Client` já possuir uma `Subscription` `active` para um plano **diferente**, ela é automaticamente marcada `canceled` (e sua `ApiKey` revogada) antes de ativar a nova — ver invariante em `docs/DOMAIN.md`.

```bash
curl -X POST http://localhost:8080/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"clientId":"8f14e45f-ceea-4c72-8a13-000000000000","planId":"3f2504e0-4f89-11d3-9a0c-000000000000"}'
```

---

## `permission`

### `POST /validate-permission`

Valida se uma ApiKey pode acessar uma rota (`httpMethod` + `route`) de um projeto com um determinado cargo.

> **Nota de contrato.** O nome deste recurso destoa do resto da API, que é organizada por
> substantivo no plural (`/clients`, `/plans`, `/projects`). O formato coerente seria
> `POST /permissions/validate`. A renomeação está registrada como trabalho futuro e **não foi
> aplicada de propósito**: esta URL é citada como evidência entregue nos documentos congelados
> das duas disciplinas anteriores, que não podem ser editados. Trocá-la agora tornaria aquela
> evidência incorreta sem render nada em troca. Roda a `Chain of Responsibility` descrita em `docs/PATTERNS.md`: `ApiKeyValidationHandler` → `TokenValidationHandler` → `RoleRouteValidationHandler`. A chain para no primeiro handler que negar.

**Request** (`ValidatePermissionRequest`):
```json
{
  "apiKey": "sk_78dad83cea33462aba966523f184ddce",
  "projectId": "0d2b1f9c-1111-2222-3333-444455556666",
  "role": "ADMIN",
  "httpMethod": "GET",
  "route": "/orders"
}
```

| Campo | Regra (Bean Validation) |
|---|---|
| `apiKey` | `@NotBlank` |
| `projectId` | `@NotNull` — identifica o projeto dono da rota |
| `role` | `@NotBlank`, até 80 caracteres |
| `httpMethod` | `@NotBlank` e um de `GET`, `POST`, `PUT`, `PATCH`, `DELETE` (case-insensitive) |
| `route` | `@NotBlank`, até 255 caracteres, precisa começar com `/` |

**Response** `200 OK` (`PermissionValidationResponse`):
```json
{
  "granted": true,
  "reason": "granted"
}
```

**Erros:** `400 Bad Request` quando algum campo acima falha na validação. Note que uma negativa de permissão **não** é erro: devolve `200` com `granted: false`.

**Motivos de negativa possíveis**, na ordem em que a chain os produz:

| `reason` | Handler | Significado |
|---|---|---|
| `invalid or inactive api key` | `ApiKeyValidationHandler` | a ApiKey não existe ou foi revogada em `billing` |
| `project not found or inactive` | `RoleRouteValidationHandler` | o `projectId` não existe, está inativo ou foi excluído |
| `route not registered in the project` | `RoleRouteValidationHandler` | não há rota com esse `httpMethod` + `route` no projeto |
| `route is inactive` | `RoleRouteValidationHandler` | a rota existe mas está desativada |
| `role not registered in the project` | `RoleRouteValidationHandler` | não há cargo com esse nome no projeto |
| `role is inactive` | `RoleRouteValidationHandler` | o cargo existe mas está desativado |
| `role has no active grant on this route` | `RoleRouteValidationHandler` | cargo e rota existem no projeto, mas não há concessão (`RoleRoute`) ativa ligando os dois |

`TokenValidationHandler` continua concedendo sempre: depende de um 2º fator de autenticação (JWT/login), fora do escopo — ver `docs/PATTERNS.md`.

> **Ordem das checagens:** projeto → rota → cargo → concessão. O último motivo é o mais comum em operação: o cargo e a rota existem, mas ninguém concedeu o acesso. Conceda com `POST /projects/{projectId}/roles/{roleId}/routes/{routeId}`.

```bash
curl -X POST http://localhost:8080/validate-permission \
  -H "Content-Type: application/json" \
  -d '{"apiKey":"sk_78dad...","projectId":"0d2b1f9c-1111-2222-3333-444455556666","role":"ADMIN","httpMethod":"GET","route":"/orders"}'
```

---

## `project`

Servidos por quatro controllers, um por sub-recurso: `ProjectController` (`/projects`), `RoleController` (`/projects/{projectId}/roles`), `RouteController` (`/projects/{projectId}/routes`) e `RoleRouteController` (`/projects/{projectId}/roles/{roleId}/routes`). Todos operam sobre PostgreSQL via `ProjectRepositoryAdapter` (Spring Data JPA) — nas etapas 2-3 o mesmo contrato era servido por um `Map` em memória, sem que a API mudasse.

**Exclusão é lógica.** `DELETE` marca `deletedAt`; a partir daí o projeto responde `404` em qualquer leitura, como se não existisse. Não há endpoint para restaurar. Para apagar de vez existe `DELETE /projects/{projectId}/purge`, que remove a linha e, em cascata, seus cargos e rotas.

### `GET /projects`

Lista os projetos não excluídos, em ordem alfabética.

**Query params** (todos opcionais, `SearchProjectsRequest`): `name` (trecho do nome, ignora maiúsculas, máx. 120 caracteres) e `onlyActive` (`true`/`false`).

**Response** `200 OK` — array de `ProjectResponse`:
```json
[
  {
    "id": "0d2b1f9c-0000-0000-0000-000000000000",
    "clientId": "8f14e45f-ceea-4c72-8a13-000000000000",
    "name": "Portal Interno",
    "description": "Portal administrativo",
    "maxRoles": 5,
    "active": true,
    "createdAt": "2026-08-29T22:00:00Z",
    "updatedAt": null,
    "roles": [
      {
        "id": "1a1a1a1a-0000-0000-0000-000000000000",
        "projectId": "0d2b1f9c-0000-0000-0000-000000000000",
        "name": "ADMIN",
        "description": "Acesso total",
        "active": true,
        "createdAt": "2026-08-29T22:00:00Z",
        "updatedAt": null
      }
    ],
    "routes": [
      {
        "id": "2b2b2b2b-0000-0000-0000-000000000000",
        "projectId": "0d2b1f9c-0000-0000-0000-000000000000",
        "name": "Listar usuários",
        "httpMethod": "GET",
        "path": "/users",
        "description": "Listagem paginada",
        "active": true,
        "createdAt": "2026-08-29T22:00:00Z",
        "updatedAt": null
      }
    ]
  }
]
```

**Serialização do 1-N:** o pai embute os filhos; cada filho referencia o pai por `projectId` (UUID), nunca por objeto. É o que evita referência circular sem precisar de `@JsonIgnore`.

```bash
curl "http://localhost:8080/projects?name=portal&onlyActive=true"
```

---

### `GET /projects/{projectId}`

**Response** `200 OK` (`ProjectResponse`). **Erros:** `404 Not Found` se o projeto não existir ou estiver excluído (`ProjectNotFoundException`).

```bash
curl http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000
```

---

### `POST /projects`

Cria um projeto.

**Request** (`CreateProjectRequest`):
```json
{
  "clientId": "8f14e45f-ceea-4c72-8a13-000000000000",
  "name": "Portal do Cliente",
  "description": "Portal de autoatendimento",
  "maxRoles": 5
}
```

Validações: `clientId` obrigatório; `name` obrigatório, máx. 120; `description` máx. 500; `maxRoles` obrigatório, mínimo 1.

**Response** `201 Created` (`ProjectResponse`) + header `Location: /projects/{id}`. **Erros:** `400 Bad Request` em dados inválidos.

```bash
curl -X POST http://localhost:8080/projects \
  -H "Content-Type: application/json" \
  -d '{"clientId":"8f14e45f-ceea-4c72-8a13-000000000000","name":"Portal do Cliente","description":"Portal de autoatendimento","maxRoles":5}'
```

---

### `PUT /projects/{projectId}`

Alteração **parcial**: campo ausente ou nulo mantém o valor atual; string em branco é recusada.

**Request** (`UpdateProjectRequest`): `name`, `description`, `maxRoles` — todos opcionais.

**Response** `200 OK` (`ProjectResponse`). **Erros:** `400` em dados inválidos; `404` se o projeto não existir ou estiver excluído; `409 Conflict` se `maxRoles` for menor que a quantidade de cargos já cadastrados (`PlanLimitExceededException`).

```bash
curl -X PUT http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000 \
  -H "Content-Type: application/json" \
  -d '{"name":"Portal do Cliente v2","maxRoles":8}'
```

---

### `DELETE /projects/{projectId}`

Exclusão lógica (`deletedAt`).

**Response** `204 No Content`. **Erros:** `404 Not Found` se o projeto não existir **ou já tiver sido excluído** — o segundo `DELETE` devolve 404, não 409.

```bash
curl -X DELETE http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000 -i
```

---

### `DELETE /projects/{projectId}/purge`

Remoção definitiva (hard delete). Apaga a linha de `projects` e, em cascata, todos os cargos e rotas do projeto — pela FK `ON DELETE CASCADE` no banco e pelo `orphanRemoval = true` do `@OneToMany`.

Diferente do `DELETE` simples, funciona também sobre um projeto **já excluído logicamente**: é o caminho para tirar do banco o que a exclusão lógica apenas escondeu.

**Response** `204 No Content`. **Erros:** `404 Not Found` se não houver linha com esse id.

```bash
curl -X DELETE http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000/purge -i
```

---

### `POST /projects/{projectId}/roles`

Adiciona um cargo ao projeto. O cargo nasce **sem nenhuma rota liberada** — quem libera é `POST /projects/{projectId}/roles/{roleId}/routes/{routeId}`.

`RoleResponse` traz o campo `permissions` com as concessões **ativas** do cargo, o que faz o `GET /projects/{projectId}` mostrar o grafo completo `Project → Role → RoleRoute` numa única resposta.

**Request** (`AddRoleRequest`): `name` (obrigatório, máx. 80), `description` (máx. 255).

**Response** `201 Created` (`RoleResponse`). **Erros:** `400` em dados inválidos; `404` se o projeto não existir ou estiver excluído; `409 Conflict` se já existir cargo com o mesmo nome — comparação ignora maiúsculas (`RoleAlreadyExistsException`) — ou se o limite `maxRoles` do projeto já tiver sido atingido (`PlanLimitExceededException`).

```bash
curl -X POST http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000/roles \
  -H "Content-Type: application/json" \
  -d '{"name":"ADMIN","description":"Acesso total"}'
```

---

### `POST /projects/{projectId}/routes`

Adiciona uma rota protegida ao projeto.

**Request** (`AddRouteRequest`): `name` (obrigatório, máx. 80), `path` (obrigatório, precisa começar com `/`, máx. 255), `httpMethod` (obrigatório, um de `GET|POST|PUT|PATCH|DELETE`, ignora maiúsculas), `description` (máx. 255).

**Response** `201 Created` (`RouteResponse`). **Erros:** `400` em dados inválidos; `404` se o projeto não existir ou estiver excluído; `409 Conflict` se já existir a mesma combinação `httpMethod` + `path` (`RouteAlreadyExistsException`).

```bash
curl -X POST http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000/routes \
  -H "Content-Type: application/json" \
  -d '{"name":"Listar usuários","path":"/users","httpMethod":"GET","description":"Listagem paginada"}'
```

---

### `GET /projects/{projectId}/routes`

Lista as rotas do projeto, ordenadas por `path`.

**Query param** (opcional): `httpMethod` — filtra por método, ignorando maiúsculas.

**Response** `200 OK` — array de `RouteResponse`. **Erros:** `404` se o projeto não existir ou estiver excluído.

```bash
curl "http://localhost:8080/projects/0d2b1f9c-0000-0000-0000-000000000000/routes?httpMethod=GET"
```

---

### `POST /projects/{projectId}/roles/{roleId}/routes/{routeId}`

Concede ao cargo o acesso a uma rota do projeto. É o que faz o `POST /validate-permission` responder `granted: true` para o par.

Sem corpo — os três identificadores estão no caminho.

**Response** `201 Created` (`RolePermissionResponse`):
```json
{
  "id": "9186e7bd-432a-421f-85f5-fca210cb6ef1",
  "roleId": "c83819ba-ff47-4c37-b365-8c3de198a6f9",
  "routeId": "24eb7f5f-1962-4abb-bac7-0e70a143347f",
  "active": true,
  "grantedAt": "2026-08-31T21:41:19.514367458-03:00",
  "revokedAt": null
}
```

**Erros:**

| Status | Quando |
|---|---|
| `404` | o projeto, o cargo ou a rota não existe — ou a rota/cargo pertence a outro projeto |
| `409` | o cargo já tem uma concessão **ativa** nessa rota |

```bash
curl -X POST http://localhost:8080/projects/$PROJ/roles/$ROLE/routes/$ROUTE
```

---

### `DELETE /projects/{projectId}/roles/{roleId}/routes/{routeId}`

Revoga o acesso. **Não apaga a linha** — grava `revokedAt`, preservando o registro de que aquele cargo teve acesso e até quando. É o que sustenta a pergunta de auditoria "quando o cargo X deixou de poder acessar a rota Y".

Depois de revogar, conceder de novo cria uma **nova** linha; as duas convivem no histórico, e só a última fica ativa.

**Response** `204 No Content`. **Erros:** `404 Not Found` se não houver concessão ativa — inclusive num segundo `DELETE` seguido.

```bash
curl -X DELETE http://localhost:8080/projects/$PROJ/roles/$ROLE/routes/$ROUTE -i
```

---

### `GET /projects/{projectId}/roles/{roleId}/routes`

Lista as concessões do cargo, da mais recente para a mais antiga.

| Query param | Default | Efeito |
|---|---|---|
| `includeRevoked` | `false` | com `true`, devolve também as concessões já revogadas — é a visão de histórico |

**Response** `200 OK` — lista de `RolePermissionResponse`. **Erros:** `404 Not Found` se o projeto ou o cargo não existir.

```bash
# só o que vale agora
curl "http://localhost:8080/projects/$PROJ/roles/$ROLE/routes"

# histórico completo
curl "http://localhost:8080/projects/$PROJ/roles/$ROLE/routes?includeRevoked=true"
```

---

## `audit`

### `GET /audit-events`

Lista a trilha de auditoria, do evento mais recente para o mais antigo. Os eventos são gravados pelo Observer `AuditLogListener`, que reage ao `PermissionValidatedEvent` publicado a cada `POST /validate-permission` — ver `docs/PATTERNS.md`.

**Query params** (todos opcionais, `SearchAuditEventsRequest`): `type` (`PERMISSION_CHECK` ou `PROJECT_LIFECYCLE`, sem diferenciar maiúsculas), `projectId` (UUID), `onlyDenied` (`true` devolve só as validações negadas), `from` e `to` (ISO-8601 com fuso, ex.: `2026-09-01T00:00:00Z`; os dois limites entram no resultado). Um limite sozinho vale como "a partir de" ou "até". `onlyDenied=true` com `type=PROJECT_LIFECYCLE` devolve lista vazia — evento de ciclo de vida nunca é negado.

Os filtros são aplicados no banco, por consulta JPQL (ver `docs/ARCHITECTURE.md` → ADR-009).

**Response** `200 OK` — array de `AuditEventResponse`:
```json
[
  {
    "id": "e24dd619-da77-46c7-b8ab-956d90d0ad71",
    "type": "PERMISSION_CHECK",
    "projectId": null,
    "occurredAt": "2026-08-29T23:04:05.881829269-03:00",
    "description": "NEGADO null /admin para o cargo 'GUEST' — invalid or inactive api key (74.42 ms)"
  }
]
```

> ⚠️ O `null` na descrição é o `httpMethod`: `ValidatePermissionRequest` carrega só `route`, sem o método HTTP, então o evento não tem o que registrar nesse campo. Fecha na etapa 4, junto com a regra real do `RoleRouteValidationHandler`, que vai precisar do método para casar a rota com o projeto.

**Um DTO para toda a hierarquia:** `AuditEvent` é abstrata e tem duas subclasses (`PermissionCheckEvent`, `ProjectLifecycleEvent`). O que as distingue aparece em `type` e `description`, ambos polimórficos (`type()` e `describe()`), então a API expõe a herança sem precisar de um DTO por subclasse.

**Erros:** `400 Bad Request` se `type` não for um dos valores conhecidos, se `from` estiver no futuro ou se `from` for posterior a `to` (`InvalidAuditPeriodException`).

```bash
curl "http://localhost:8080/audit-events?onlyDenied=true"
curl "http://localhost:8080/audit-events?onlyDenied=true&from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z"
```

> Use `Z` ou um fuso negativo (`-03:00`) na URL. Um `+` sem codificação (`%2B`) chega ao servidor como espaço e a data não é reconhecida.

---

## `audit-service` — serviço independente (porta 8081)

A trilha de auditoria extraída como aplicação Spring Boot própria, com banco próprio (`audit_db`, porta 5433). Na etapa 2 da disciplina de microsserviços quem chama estes endpoints é a aplicação principal, via OpenFeign; até lá eles são exercitados direto, pelo Postman (pasta `audit-service (8081)`) ou pelo Swagger UI em `http://localhost:8081/swagger-ui/index.html`.

Erros seguem o mesmo [formato padrão](#formato-padrão-de-erro) da aplicação principal. O serviço não tem autenticação: é chamado pela rede interna dos serviços, não por clientes.

### `GET /audit-events`

Mesmo contrato do [`GET /audit-events`](#get-audit-events) da aplicação principal — filtros `type`, `projectId`, `onlyDenied`, `from` e `to`, resposta `AuditEventResponse[]` do mais recente para o mais antigo e os mesmos `400`. As consultas JPQL são as do ADR-009, copiadas para o serviço.

```bash
curl "http://localhost:8081/audit-events?onlyDenied=true&from=2026-09-01T00:00:00Z"
```

### `POST /audit-events/permission-checks`

Registra uma validação de permissão na trilha. O caminho é específico do tipo de evento porque o corpo só serve para validações de permissão; `GET /audit-events` continua listando todos os tipos juntos.

**Request** (`RegisterPermissionCheckRequest`):
```json
{
  "projectId": "11111111-1111-1111-1111-111111111111",
  "occurredAt": "2026-09-28T20:00:00Z",
  "routePath": "/produtos",
  "httpMethod": "GET",
  "roleName": "ADMIN",
  "granted": false,
  "reason": "invalid or inactive api key",
  "durationMs": 3.5
}
```

| Campo | Regra |
|---|---|
| `projectId` | obrigatório, UUID |
| `occurredAt` | obrigatório, ISO-8601 com fuso. **Sem** `@PastOrPresent`: a data é gerada por outro serviço, com outro relógio, e uma diferença de milissegundos entre eles recusaria eventos legítimos |
| `routePath` | obrigatório, começa com `/`, até 255 caracteres |
| `httpMethod` | obrigatório, `GET`/`POST`/`PUT`/`PATCH`/`DELETE` sem diferenciar maiúsculas; gravado em maiúsculas |
| `roleName` | obrigatório, até 80 caracteres |
| `granted` | obrigatório. É `Boolean`, não `boolean`: um campo ausente vira `400`, e não "negado" em silêncio |
| `reason` | opcional (só as negadas têm motivo), até 255 caracteres |
| `durationMs` | obrigatório, `>= 0` |

Os limites de tamanho seguem as colunas da migration `V1`, para que um valor grande demais responda `400` em vez de estourar no banco.

**Response** `201 Created` — o `AuditEventResponse` gravado:
```json
{
  "id": "3848fce2-7590-4fbc-b802-ff41b7085232",
  "type": "PERMISSION_CHECK",
  "projectId": "11111111-1111-1111-1111-111111111111",
  "occurredAt": "2026-09-28T20:00Z",
  "description": "NEGADO GET /produtos para o cargo 'ADMIN' — invalid or inactive api key (3.5 ms)"
}
```

Cada evento gravado também é acrescentado como uma linha em `logs/audit-events.txt`, relativo à pasta de onde o serviço sobe.

**Erros:** `400 Bad Request` com os campos inválidos na mensagem (ex.: `"routePath: must start with /; granted: must not be null"`), ou `"Malformed request body"` para JSON quebrado ou UUID/data em formato inválido.

```bash
curl -X POST http://localhost:8081/audit-events/permission-checks \
  -H "Content-Type: application/json" \
  -d '{"projectId":"11111111-1111-1111-1111-111111111111","occurredAt":"2026-09-28T20:00:00Z","routePath":"/produtos","httpMethod":"GET","roleName":"ADMIN","granted":false,"reason":"invalid or inactive api key","durationMs":3.5}'
```
