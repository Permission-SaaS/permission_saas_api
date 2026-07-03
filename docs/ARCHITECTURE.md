# Arquitetura — Permission SaaS

## Ideia central

Um **monolito modular**: um único processo/deploy, mas organizado em módulos independentes por domínio de negócio. Cada módulo é um mini-sistema autocontido que não conhece os detalhes internos dos outros.

---

## Estrutura interna de cada módulo

Todo módulo tem as mesmas quatro camadas, sempre nessa ordem de dependência:

```
domain/              ← núcleo, sem dependências externas
application/         ← orquestra o domain
  command/           ← command objects (entrada dos use cases, sem anotações HTTP)
infrastructure/      ← implementa o que o domain definiu
api/                 ← expõe via HTTP
  dto/               ← request e response DTOs (com validações Jakarta)
  mapper/            ← Mapper de conversão DTO ↔ Command e Entity ↔ Response
```

**Regra de ouro:** as dependências apontam sempre para dentro. `api` conhece `application`, `application` conhece `domain`, mas `domain` não conhece nada além de si mesmo — nunca JPA, nunca HTTP, nunca outro módulo.

---

## O que cada módulo faz

```
shared ──────────────────────────────────────────────────
  Mapper<I,O> (interface genérica), configs globais
  ↑ todos os outros módulos podem usar

identity ────────────────────────────────────────────────
  POST /clients/register → RegisterClientUseCase → Client salvo
  É o ponto de entrada: sem cliente, nada mais existe

billing ─────────────────────────────────────────────────
  POST /subscriptions → SubscribeToPlanUseCase
    ├── chama PaymentGateway (interface) — simulado pelo FakeAdapter
    ├── ApiKeyFactory gera a chave de API
    └── salva Subscription + ApiKey
  Quem tem plano ativo ganha uma ApiKey para usar o sistema

project ─────────────────────────────────────────────────
  POST /projects → CreateProjectUseCase
    ├── ProjectBuilder monta Project + Roles + Routes
    └── PlanLimitValidator garante que não passa do limite do plano
  Define o que pode ser acessado e por quem (Roles/Routes)

permission ──────────────────────────────────────────────  ← núcleo
  POST /validate-permission → ValidatePermissionUseCase
    └── Chain: ApiKeyHandler → TokenHandler → RoleRouteHandler
  É a razão de existir do SaaS: valida se uma requisição
  tem permissão para acessar uma rota de um projeto

audit ───────────────────────────────────────────────────
  Ouve o evento disparado pelo módulo permission
  AuditLogListener → salva AuditLog no banco
  Ninguém chama o audit diretamente — ele reage a eventos
```

---

## Como os módulos se conectam

```
identity ──→ billing ──→ project ──→ permission
                                          │
                                       (evento)
                                          ↓
                                        audit
```

### Fluxo de negócio completo

1. Cliente se cadastra (`identity`)
2. Assina um plano e recebe uma ApiKey (`billing`)
3. Cria um projeto com cargos e rotas dentro do limite do plano (`project`)
4. Qualquer sistema externo chama `POST /validate-permission` com a ApiKey + rota + cargo (`permission`)
5. O resultado (permitido/negado) é gravado automaticamente em log (`audit`)

---

## Regras de comunicação entre módulos

Um módulo **nunca** acessa o repositório JPA de outro módulo diretamente. A comunicação acontece de exatamente duas formas:

| Forma                      | Quando usar                                               | Exemplo                                                                     |
| -------------------------- | --------------------------------------------------------- | --------------------------------------------------------------------------- |
| Chamada direta de use case | Quando um módulo precisa de dados de outro               | `CreateProjectUseCase` consulta o billing para checar os limites do plano |
| Evento de domínio         | Quando um efeito colateral deve acontecer sem acoplamento | `permission` dispara `PermissionValidated`; `audit` escuta e age      |

---

## DTOs e Commands — onde ficam e por quê

| Tipo         | Pacote                             | Responsabilidade                                                   |
| ------------ | ---------------------------------- | ------------------------------------------------------------------ |
| Request DTO  | `<módulo>/api/dto/`             | Contrato HTTP — valida entrada com`@NotBlank`, `@Email`, etc. |
| Response DTO | `<módulo>/api/dto/`             | Contrato HTTP — formato da resposta JSON                          |
| Command      | `<módulo>/application/command/` | Intenção de negócio — Java puro, sem anotações de framework  |

**Por que separar DTO de Command:** o DTO pertence ao contrato HTTP e pode ter campos exclusivos de validação (ex: `recaptchaToken`) que o use case não precisa saber. O Command pertence à intenção de negócio. Se a API mudar, só o DTO muda; se a regra de negócio mudar, só o Command muda.

**Conversão via Mapper:** o Controller não converte inline. Injeta um `Mapper<Request, Command>` e um `Mapper<Entity, Response>` — cada um é uma estratégia concreta de conversão que vive em `api/mapper/`.

```
RegisterClientRequest (api/dto/)
        ↓  RegisterClientMapper.map()     ← Strategy
RegisterClientCommand (application/command/)
        ↓  RegisterClientUseCase.execute()
Client (domain/)
        ↓  ClientResponseMapper.map()     ← Strategy
ClientResponse (api/dto/)
```

A interface `Mapper<I, O>` fica em `shared/domain/` e é reutilizada por todos os módulos.

---

## Adapter Pattern na infrastructure

Cada módulo define uma **interface de repositório** em `domain/` (a porta) e fornece um **adapter** em `infrastructure/` que a implementa usando Spring Data JPA. O use case depende apenas da interface — nunca do JPA diretamente.

```
domain/ClientRepository        ← interface (porta) — o que o use case conhece
infrastructure/
  ClientRepositoryAdapter      ← implementa ClientRepository, delega ao JPA
  JpaClientRepository          ← extends JpaRepository<Client, UUID>
```

Trocar o banco de dados exige apenas um novo adapter — o use case não muda.

---

## Estrutura de pacotes

```
src/main/java/com/saas/permissions/
├── shared/
│   ├── domain/            # Mapper<I,O> (interface genérica Strategy)
│   └── infrastructure/    # SecurityConfig
│
├── identity/
│   ├── domain/            # Client.java, ClientStatus.java, AuthProvider.java,
│   │                      # ClientRepository.java (porta)
│   ├── application/       # RegisterClientUseCase.java
│   │   └── command/       # RegisterClientCommand.java
│   ├── infrastructure/    # ClientRepositoryAdapter.java, JpaClientRepository.java
│   └── api/               # ClientController.java
│       ├── dto/           # RegisterClientRequest.java, ClientResponse.java
│       └── mapper/        # RegisterClientMapper.java, ClientResponseMapper.java
│
├── billing/
│   ├── domain/            # Plan.java, Subscription.java, ApiKey.java,
│   │                      # PaymentGateway.java (porta)
│   ├── application/       # SubscribeToPlanUseCase.java
│   ├── infrastructure/    # FakePaymentGatewayAdapter.java, ApiKeyFactory.java
│   └── api/               # SubscriptionController.java
│
├── project/
│   ├── domain/            # Project.java, Role.java, Route.java,
│   │                      # ProjectBuilder.java, PlanLimitValidator.java
│   ├── application/       # CreateProjectUseCase.java
│   └── api/               # ProjectController.java
│
├── permission/
│   ├── domain/            # PermissionValidationHandler.java (porta),
│   │                      # ApiKeyValidationHandler, TokenValidationHandler,
│   │                      # RoleRouteValidationHandler
│   ├── application/       # ValidatePermissionUseCase.java
│   └── api/               # PermissionController.java
│
└── audit/
    ├── domain/            # AuditLog.java, PermissionEventListener.java (porta)
    ├── application/       # AuditLogListener.java
    └── infrastructure/    # JpaAuditLogRepository.java
```
