# API — Permission SaaS

Cada endpoint implementado: método, path, request/response e um exemplo de `curl`. Endpoints ainda não implementados (`project`, `audit`) não aparecem aqui — ver `CLAUDE.md` e `docs/PLAN.md` para o que falta.

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

**Troca de plano:** se o `Client` já possuir uma `Subscription` `active` para um plano **diferente**, ela é automaticamente marcada `canceled` (e sua `ApiKey` revogada) antes de ativar a nova — ver invariante em `docs/DOMAIN.md`.

```bash
curl -X POST http://localhost:8080/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"clientId":"8f14e45f-ceea-4c72-8a13-000000000000","planId":"3f2504e0-4f89-11d3-9a0c-000000000000"}'
```

---

## `permission`

### `POST /validate-permission`

Valida se uma ApiKey pode acessar uma `route` com um `role`. Roda a `Chain of Responsibility` descrita em `docs/PATTERNS.md`: `ApiKeyValidationHandler` → `TokenValidationHandler` → `RoleRouteValidationHandler`. A chain para no primeiro handler que negar.

**Request** (`ValidatePermissionRequest`):
```json
{
  "apiKey": "sk_78dad83cea33462aba966523f184ddce",
  "role": "admin",
  "route": "/orders"
}
```

**Response** `200 OK` (`PermissionValidationResponse`):
```json
{
  "granted": true,
  "reason": "granted"
}
```

**Importante:** só `ApiKeyValidationHandler` checa uma regra real hoje (a ApiKey precisa existir e estar `active` em `billing`). `TokenValidationHandler` e `RoleRouteValidationHandler` sempre concedem nesta entrega — dependem do módulo `project` (Role/Route) e de um 2º fator de autenticação, nenhum implementado por falta de tempo (ver `docs/PLAN.md`, "trabalho futuro"). Uma ApiKey inválida ou revogada é negada antes de chegar aos outros handlers:
```json
{
  "granted": false,
  "reason": "invalid or inactive api key"
}
```

```bash
curl -X POST http://localhost:8080/validate-permission \
  -H "Content-Type: application/json" \
  -d '{"apiKey":"sk_78dad83cea33462aba966523f184ddce","role":"admin","route":"/orders"}'
```
