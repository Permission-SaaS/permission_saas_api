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

**Submódulos dentro de um módulo:** quando um módulo tem mais de um agregado com ciclo de vida próprio, cada camada ganha uma subpasta por agregado (ex.: `billing/domain/plan/` e `billing/domain/subscription/`), replicada em `application/`, `infrastructure/` e `api/`. Isso mantém a regra de ouro (dependências sempre para dentro) e ainda separa visualmente o que pertence a `Plan` do que pertence a `Subscription`. Classes que só existem por causa de um agregado (ex.: `ApiKey` e `PaymentGateway`, criados dentro do fluxo de `SubscribeToPlanUseCase`) entram na subpasta do agregado que as orquestra, mesmo sem levar o nome dele — não viram uma terceira subpasta "genérica". Cruzar submódulos do mesmo módulo é permitido via import direto (ex.: `SubscribeToPlanUseCase` importa `application.plan.FindPlanByIdUseCase`) — a regra de "nunca outro módulo" vale entre módulos (`billing` → `project`), não entre submódulos do mesmo módulo.

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

project ─────────────────────────────────────────────────  ← não implementado nesta entrega
  POST /projects → CreateProjectUseCase (planejado)
    ├── ProjectBuilder monta Project + Roles + Routes
    └── PlanLimitValidator garante que não passa do limite do plano
  Define o que pode ser acessado e por quem (Roles/Routes) —
  fora de escopo por falta de tempo, ver docs/PLAN.md

permission ──────────────────────────────────────────────  ← núcleo, implementado
  POST /validate-permission → ValidatePermissionUseCase
    └── Chain: ApiKeyValidationHandler → TokenValidationHandler → RoleRouteValidationHandler
  Valida a ApiKey de verdade (chama billing via use case). Os dois
  últimos handlers sempre concedem porque dependem de project
  (Role/Route) e de um 2º fator de auth, nenhum implementado ainda —
  decisão documentada, ver docs/PATTERNS.md

audit ───────────────────────────────────────────────────  ← não implementado nesta entrega
  Ouviria o evento disparado pelo módulo permission
  AuditLogListener → salva AuditLog no banco (planejado)
  Fora de escopo por falta de tempo, ver docs/PLAN.md
```

---

## Como os módulos se conectam

```
identity ──→ billing ──→ permission
                            │
                         (evento, planejado)
                            ↓
                          audit

project (planejado, não integrado nesta entrega)
```

### Fluxo de negócio completo (implementado nesta entrega)

1. Cliente se cadastra (`identity`)
2. Assina um plano e recebe uma ApiKey (`billing`)
3. Qualquer sistema externo chama `POST /validate-permission` com a ApiKey + cargo + rota (`permission`) — a ApiKey é validada de verdade contra `billing`; cargo/rota são aceitos pelo contrato mas ainda não checados contra um projeto real, porque `project` não existe nesta entrega (ver `docs/PLAN.md`)
4. ~~Cria um projeto com cargos e rotas dentro do limite do plano~~ e ~~o resultado é gravado em log de auditoria~~ — ambos fora de escopo por falta de tempo, ver "trabalho futuro" em `docs/PLAN.md`

---

## Regras de comunicação entre módulos

Um módulo **nunca** acessa o repositório JPA de outro módulo diretamente. A comunicação acontece de exatamente duas formas:

| Forma                      | Quando usar                                               | Exemplo                                                                     |
| -------------------------- | --------------------------------------------------------- | --------------------------------------------------------------------------- |
| Chamada direta de use case | Quando um módulo precisa de dados de outro               | `permission/infrastructure/BillingApiKeyValidator` chama `billing.application.subscription.FindActiveApiKeyByPlainKeyUseCase` para validar a ApiKey (implementado); `CreateProjectUseCase` consultaria o billing para checar os limites do plano (planejado) |
| Evento de domínio         | Quando um efeito colateral deve acontecer sem acoplamento | `permission` dispararia `PermissionValidated`; `audit` escutaria e agiria (planejado, não implementado nesta entrega) |

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

**Mesma regra vale para comunicação entre módulos, não só para JPA:** `permission/domain/ApiKeyValidator` é uma porta que `permission` define para si mesmo; quem a implementa é `permission/infrastructure/BillingApiKeyValidator`, que por dentro chama o use case `billing.application.subscription.FindActiveApiKeyByPlainKeyUseCase`. Isso mantém `permission/domain` sem importar nada de `billing` — só `permission/infrastructure` conhece a existência do outro módulo, exatamente como só `infrastructure` conhece o JPA.

---

## Tratamento de exceções — GlobalExceptionHandler

Duas camadas de tipos, para conciliar "handler genérico por categoria HTTP" com "exceção legível e concentrada por módulo":

1. **Categorias abstratas em `shared/domain/exception/`** — mapeiam 1:1 para um status HTTP. Nenhuma delas é instanciada diretamente (construtor `protected`), só estendida:
   - `DomainException` — superclasse abstrata de tudo.
   - `ResourceNotFoundException extends DomainException` → `404`.
   - `BusinessRuleException extends DomainException` → `409`.
2. **Exceções concretas por módulo**, uma para cada erro de negócio real do sistema, cada uma já carregando sua própria mensagem — quem lança nunca monta uma `String` na hora do `throw`:

   | Exceção | Módulo/pacote | Extends | Uso |
   |---|---|---|---|
   | `ClientNotFoundException` | `identity/domain/exception/` | `ResourceNotFoundException` | `FindClientByIdUseCase` |
   | `EmailAlreadyInUseException` | `identity/domain/exception/` | `BusinessRuleException` | `RegisterClientUseCase` — recebe o email no construtor e monta a mensagem internamente |
   | `PlanNotFoundException` | `billing/domain/plan/exception/` | `ResourceNotFoundException` | `FindPlanByIdUseCase` |
   | `PaymentDeclinedException` | `billing/domain/subscription/exception/` | `BusinessRuleException` | `SubscribeToPlanUseCase` — recebe o `subscriptionId` |
   | `ActiveSubscriptionExistsException` | `billing/domain/subscription/exception/` | `BusinessRuleException` | `SubscribeToPlanUseCase.handleExistingSubscription` — recebe o `expiresAt` |

   Exemplo: `throw new ClientNotFoundException();` em vez de `throw new ResourceNotFoundException("Client not found")` — a mensagem some do call site porque já é responsabilidade da própria exceção. Quando há dado relevante para a mensagem (email, id, data), ele entra como parâmetro do construtor (ex.: `new EmailAlreadyInUseException(command.email())`) — nunca a mensagem pronta.

`shared/api/GlobalExceptionHandler` (`@RestControllerAdvice`) continua enxergando só as categorias abstratas — não precisa conhecer `ClientNotFoundException` nem qualquer outra folha, porque toda folha herda de uma das duas categorias:

- `ResourceNotFoundException` (e qualquer subclasse) → `404 Not Found`
- `BusinessRuleException` (e qualquer subclasse) → `409 Conflict`
- `MethodArgumentNotValidException` (falha de `@Valid` nos DTOs de request) → `400 Bad Request`, mensagem concatena `campo: motivo` de cada erro de validação
- Qualquer outra `Exception` não mapeada → `500 Internal Server Error`, logada via `@Slf4j` (nunca vaza stacktrace pro cliente)

Formato de resposta único: `shared/api/dto/ErrorResponse` (`status`, `error`, `message`, `timestamp`).

**Por que duas camadas:** se cada módulo lançasse `BusinessRuleException("texto solto")` diretamente, o texto ficaria espalhado pelos use cases e duplicado sempre que o mesmo erro fosse lançado de dois lugares. Concentrar cada exceção concreta no módulo do domínio que ela descreve (seguindo a mesma regra de ouro de `ARCHITECTURE.md` — `domain` não conhece HTTP) deixa o `throw` autoexplicativo e a mensagem con­sistente em um único lugar; a categoria abstrata em `shared` é só o que o `GlobalExceptionHandler` precisa para decidir o status HTTP, sem precisar de um `@ExceptionHandler` por exceção concreta.

**Como estender:**
- Novo erro dentro de uma categoria já existente (404 ou 409) → criar uma nova classe em `<módulo>/domain/[<agregado>/]exception/` estendendo `ResourceNotFoundException` ou `BusinessRuleException`; o `GlobalExceptionHandler` não muda.
- Nova categoria de status HTTP → criar uma nova subclasse abstrata de `DomainException` em `shared/domain/exception/` e um `@ExceptionHandler` correspondente no `GlobalExceptionHandler`.

---

## ADR-001: quem gera o ID da entidade — domínio ou Hibernate

**Decisão:** métodos de fábrica no domínio (`Subscription.pendingFor()`, etc.) **não** atribuem `id` manualmente quando a entidade JPA correspondente usa `@GeneratedValue(strategy = GenerationType.UUID)`. O `id` fica `null` até o primeiro `save()`; o adapter lê o valor gerado de volta (`toDomain(saved)`) e o use case reatribui a variável (`subscription = subscriptionRepository.save(subscription)`).

**Por quê:** o Spring Data `SimpleJpaRepository.save()` decide entre `persist()` e `merge()` checando se o `id` está `null` (`isNew()`). Se o domínio já atribui um UUID antes do primeiro save, o repositório assume que a entidade **já existe** e chama `merge()` — que faz um `SELECT` pra achar a linha, não encontra (ela ainda não existe) e o Hibernate lança `StaleObjectStateException` (`ObjectOptimisticLockingFailureException`), mesmo sem `@Version` na entidade. Foi exatamente o bug corrigido em `SubscribeToPlanUseCase`/`Subscription.pendingFor()` — `Client` e `ApiKey` nunca tiveram esse problema porque já seguiam essa regra.

**Como aplicar:** qualquer entidade nova com `@GeneratedValue(strategy = GenerationType.UUID)` (ex: futuras entidades de `project`, `permission`, `audit`) deve deixar o Hibernate gerar o `id` — nunca pré-atribuir no domínio. Se um fluxo salvar a mesma entidade mais de uma vez na mesma transação (como a subscription: pending → paid/rejected → active), sempre reatribuir a variável local ao retorno de `save()`.

---

## Segurança do Swagger UI

`SecurityConfig` deixa todo o restante da API com `permitAll()` (autenticação real de cliente é trabalho futuro, ver `docs/PLAN.md`), mas `/swagger-ui/**` e `/v3/api-docs/**` exigem HTTP Basic com um usuário fixo em memória (`InMemoryUserDetailsManager`), configurado via `app.swagger.username` / `app.swagger.password` (env vars `SWAGGER_USERNAME` / `SWAGGER_PASSWORD`, default `admin` / `admin123`). `/actuator/**` continua liberado.

**Por quê:** a documentação interativa expõe todos os endpoints e facilita descoberta/abuso se ficar pública; como login/JWT de cliente está fora de escopo desta entrega, HTTP Basic com um usuário fixo é a menor solução que já impede acesso não autenticado ao Swagger sem implementar um fluxo de autenticação completo.

O grupo `public` do `SwaggerConfig` (`GroupedOpenApi`) é só rotulagem de agrupamento do OpenAPI — não tem relação com controle de acesso, que é feito inteiramente pelo `SecurityFilterChain`.

---

## Estrutura de pacotes

```
src/main/java/com/saas/permissions/
├── shared/
│   ├── domain/
│   │   ├── Mapper.java         # interface genérica Strategy
│   │   └── exception/          # DomainException (abstrata), ResourceNotFoundException (abstrata, 404),
│   │                          # BusinessRuleException (abstrata, 409) — só categorias, nunca lançadas direto
│   ├── infrastructure/        # SecurityConfig
│   └── api/                   # PingController, GlobalExceptionHandler (@RestControllerAdvice)
│       └── dto/                # ErrorResponse.java
│
├── identity/
│   ├── domain/            # Client.java, ClientStatus.java, AuthProvider.java,
│   │                      # ClientRepository.java (porta)
│   │   └── exception/     # ClientNotFoundException, EmailAlreadyInUseException
│   ├── application/       # RegisterClientUseCase.java
│   │   └── command/       # RegisterClientCommand.java
│   ├── infrastructure/    # ClientRepositoryAdapter.java, JpaClientRepository.java
│   └── api/               # ClientController.java
│       ├── dto/           # RegisterClientRequest.java, ClientResponse.java
│       └── mapper/        # RegisterClientMapper.java, ClientResponseMapper.java
│
├── billing/               # dividido em submódulos plan/ e subscription/ dentro de cada camada
│   ├── domain/
│   │   ├── plan/          # Plan.java, PlanRepository.java (porta)
│   │   │   └── exception/ # PlanNotFoundException.java
│   │   └── subscription/  # Subscription.java, ApiKey.java, PaymentGateway.java (porta)
│   │       ├── dto/       # PaymentRequest.java, PaymentResult.java, SubscriptionResult.java
│   │       └── exception/ # PaymentDeclinedException.java, ActiveSubscriptionExistsException.java
│   ├── application/
│   │   ├── plan/          # FindPlanByIdUseCase.java
│   │   └── subscription/  # SubscribeToPlanUseCase.java
│   │       └── command/   # SubscribeToPlanCommand.java
│   ├── infrastructure/
│   │   ├── plan/          # PlanJpaEntity.java, PlanJpaRepository.java, PlanRepositoryAdapter.java
│   │   └── subscription/  # FakePaymentGatewayAdapter.java, ApiKeyFactory.java, BillingConfig.java, ...
│   └── api/
│       ├── plan/          # PlanController.java
│       │   ├── dto/       # PlanResponse.java
│       │   └── mapper/    # PlanResponseMapper.java
│       └── subscription/  # SubscriptionController.java
│           ├── dto/       # SubscribeToPlanRequest.java, SubscriptionResponse.java
│           └── mapper/    # SubscribeToPlanMapper.java, SubscriptionResponseMapper.java
│
├── project/               # planejado, não implementado nesta entrega — ver docs/PLAN.md
│   ├── domain/            # Project.java, Role.java, Route.java,
│   │                      # ProjectBuilder.java, PlanLimitValidator.java
│   ├── application/       # CreateProjectUseCase.java
│   └── api/               # ProjectController.java
│
├── permission/            # implementado — Chain of Responsibility (docs/PATTERNS.md)
│   ├── domain/            # PermissionValidationHandler.java (Handler abstrato),
│   │                      # ApiKeyValidationHandler, TokenValidationHandler,
│   │                      # RoleRouteValidationHandler (ConcreteHandlers),
│   │                      # ApiKeyValidator.java (porta), dto/PermissionCheckRequest.java,
│   │                      # dto/PermissionCheckResult.java
│   ├── application/       # ValidatePermissionUseCase.java
│   ├── infrastructure/    # BillingApiKeyValidator.java (implementa ApiKeyValidator
│   │                      # chamando billing.FindActiveApiKeyByPlainKeyUseCase)
│   └── api/               # PermissionController.java
│       ├── dto/           # ValidatePermissionRequest.java, PermissionValidationResponse.java
│       └── mapper/        # ValidatePermissionMapper.java, PermissionValidationResponseMapper.java
│
└── audit/                 # planejado, não implementado nesta entrega — ver docs/PLAN.md
    ├── domain/            # AuditLog.java, PermissionEventListener.java (porta)
    ├── application/       # AuditLogListener.java
    └── infrastructure/    # JpaAuditLogRepository.java
```

Todos os módulos de negócio são subpacotes diretos de `com.saas.permissions` (ex: `com.saas.permissions.identity`), assim como `shared`. Essa é a estrutura exigida pela detecção automática de módulos do Spring Modulith, que considera cada subpacote direto do pacote da classe `@SpringBootApplication` como um Application Module.
