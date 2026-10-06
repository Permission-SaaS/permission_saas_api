# Arquitetura — `permission-service`

## Ideia central

Um **monolito modular**: um único processo/deploy, mas organizado em módulos independentes por domínio de negócio. Cada módulo é um mini-sistema autocontido que não conhece os detalhes internos dos outros.

Este documento descreve o interior do `permission-service`: camadas, módulos, regras entre módulos, conversões e tratamento de erros. A visão do sistema inteiro (os outros serviços e como eles conversam por OpenFeign, RabbitMQ e Config Server) e o log de decisões (ADR-001 … ADR-015) ficam no [`docs/ARCHITECTURE.md` do repositório guarda-chuva](https://github.com/Permission-SaaS/permission_saas/blob/main/docs/ARCHITECTURE.md). Os números de ADR citados aqui apontam para lá.

---

## Estrutura interna de cada módulo

Todo módulo tem as mesmas quatro camadas, sempre nessa ordem de dependência:

```
domain/              ← núcleo, sem dependências externas
application/         ← orquestra o domain
  command/           ← command objects (entrada dos use cases que alteram estado, sem anotações HTTP)
  query/             ← query objects (entrada dos use cases de leitura com filtros, sem anotações HTTP)
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

project ─────────────────────────────────────────────────  ← implementado na disciplina de Spring Boot
  CRUD completo em /projects, mais os sub-recursos /roles e /routes
    ├── Project é o agregado: addRole/addRoute mantêm os invariantes
    │   (maxRoles, duplicidade de cargo, duplicidade de httpMethod+path)
    ├── exclusão é lógica (deletedAt) — projeto excluído responde 404;
    │   DELETE /projects/{id}/purge remove em definitivo, em cascata
    ├── RoleRoute liga cargo a rota com histórico (grantedAt/revokedAt):
    │   é o que define quais rotas cada cargo pode acessar
    ├── persistência JPA: Project @OneToMany Role/Route,
    │   Role @OneToMany RoleRoute @ManyToOne Route
    ├── CheckRouteAccessUseCase responde se um cargo alcança uma rota —
    │   é o que o permission consulta pelo RouteAccessChecker
    └── POST /projects/{id}/routes/import importa rotas de um CSV com
        Spring Batch, em lotes de 10 (ADR-014, desde 04/10/2026)
  Define o que pode ser acessado e por quem (Roles/Routes)

permission ──────────────────────────────────────────────  ← núcleo, implementado
  POST /validate-permission → ValidatePermissionUseCase
    └── Chain: ApiKeyValidationHandler → TokenValidationHandler → RoleRouteValidationHandler
  ApiKeyValidationHandler valida a ApiKey contra billing (via use case).
  RoleRouteValidationHandler checa projeto, rota, cargo e a concessão
  RoleRoute que liga os dois (via RouteAccessChecker →
  project.CheckRouteAccessUseCase).
  TokenValidationHandler segue concedendo sempre: depende de um 2º fator
  de autenticação, fora do escopo — ver docs/PATTERNS.md

audit ───────────────────────────────────────────────────  ← implementado na disciplina de Spring Boot;
                                                            desde 30/09/2026, cliente do audit-service
  GET /audit-events → SearchAuditEventsUseCase (filtros type/projectId/onlyDenied/from/to),
  repassado ao audit-service pela porta AuditTrail (OpenFeign); serviço fora do ar → 503
  AuditLogListener (@Async) escuta PermissionValidatedEvent e publica a validação
  na fila audit.events do RabbitMQ (porta AuditEventPublisher), consumida pelo
  audit-service — desde 03/10/2026 (ADR-013)
  sem banco próprio: a hierarquia AuditEvent completa, a tabela e o arquivo
  texto vivem no audit-service (ADR-010); aqui ficam só AuditEvent e
  PermissionCheckEvent, o evento que o listener monta e envia
```

---

## Como os módulos se conectam

```
identity ──→ billing ──→ permission ──→ project
                            │
                     PermissionValidatedEvent
                            ↓
                          audit
```

### Fluxo de negócio completo (implementado nesta entrega)

1. Cliente se cadastra (`identity`)
2. Assina um plano e recebe uma ApiKey (`billing`)
3. Cria um projeto com cargos e rotas dentro do limite do plano e concede a cada cargo as rotas que ele pode acessar (`project`) — implementado na disciplina de Spring Boot
4. Qualquer sistema externo chama `POST /validate-permission` com ApiKey + `projectId` + cargo + método HTTP + rota (`permission`) — a ApiKey é validada contra `billing` e o par cargo/rota é checado contra o projeto real em `project`
5. Cada validação de permissão é gravada na trilha de auditoria (`audit`), via evento — implementado na disciplina de Spring Boot

A cadeia está fechada: o `RoleRouteValidationHandler` deixou de conceder sempre e passou a consultar o `project` pela porta `RouteAccessChecker`. Os motivos de negativa que ele devolve, na ordem em que são checados, são `project not found or inactive`, `route not registered in the project`, `route is inactive`, `role not registered in the project`, `role is inactive` e `role has no active grant on this route`.

O último é o que responde à regra de negócio do produto — *o cargo X pode acessar a rota Y no projeto Z?* —, e vem da entidade associativa `RoleRoute`. Antes dela a validação parava em "cargo e rota existem no mesmo projeto", o que liberaria qualquer cargo para qualquer rota.

---

## Regras de comunicação entre módulos

Um módulo **nunca** acessa o repositório JPA de outro módulo diretamente. A comunicação acontece de exatamente duas formas:

| Forma                      | Quando usar                                               | Exemplo                                                                     |
| -------------------------- | --------------------------------------------------------- | --------------------------------------------------------------------------- |
| Chamada direta de use case | Quando um módulo precisa de dados de outro               | `permission/infrastructure/BillingApiKeyValidator` chama `billing.application.subscription.FindActiveApiKeyByPlainKeyUseCase` para validar a ApiKey; `permission/infrastructure/ProjectRouteAccessChecker` chama `project.application.project.CheckRouteAccessUseCase` para checar cargo × rota (ambos implementados); `CreateProjectUseCase` consultaria o billing para checar os limites do plano (planejado) |
| Evento de domínio         | Quando um efeito colateral deve acontecer sem acoplamento | `permission/application/ValidatePermissionUseCase` publica `PermissionValidatedEvent` (pacote `permission/domain/event`, anotado `@NamedInterface("events")`); `audit/application/AuditLogListener` consome com `@EventListener` e grava a trilha. Ver `docs/PATTERNS.md` → Observer, inclusive o motivo de ainda não ser `@ApplicationModuleListener` |

---

## DTOs e Commands — onde ficam e por quê

| Tipo         | Pacote                             | Responsabilidade                                                   |
| ------------ | ---------------------------------- | ------------------------------------------------------------------ |
| Request DTO  | `<módulo>/api/dto/`             | Contrato HTTP — valida entrada com`@NotBlank`, `@Email`, etc. |
| Response DTO | `<módulo>/api/dto/`             | Contrato HTTP — formato da resposta JSON                          |
| Command      | `<módulo>/application/command/` | Intenção de negócio — Java puro, sem anotações de framework  |
| Query        | `<módulo>/application/query/`   | Pergunta de leitura (filtros) — Java puro, sem anotações de framework |

**Command e Query têm a mesma forma, e pacotes diferentes.** Os dois são *Parameter Objects* — records passivos que carregam a entrada de um use case. O sufixo e o pacote dizem a intenção sem abrir o use case: `Command` altera estado, `Query` só lê (ver `PATTERNS.md` → "Command Object"). Até a disciplina de microsserviços as duas Queries do projeto moravam em `command/`; foram separadas quando o `audit` passou a ter os dois tipos lado a lado no serviço extraído.

**Quando criar um objeto de entrada.** Só quando a entrada vem de um DTO — um corpo de requisição ou um conjunto de filtros opcionais (`CreateProjectCommand`, `SearchProjectsQuery`). Identificadores de caminho e um filtro isolado vão como parâmetros simples (`FindPlanByIdUseCase.execute(UUID)`, `GrantRouteToRoleUseCase.execute(projectId, roleId, routeId)`, `FindProjectRoutesUseCase.execute(projectId, httpMethod)`): um record que só embrulha um `UUID` é cerimônia sem ganho.

**Não confundir com o `@Query` do Spring Data.** A Query de `application/query/` é a *pergunta* do caso de uso e não sabe que existe banco; o `@Query` é a *forma* de responder em JPQL e vive só nas interfaces `Jpa*Repository` de `infrastructure/`, atrás da porta do domínio.

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

### Convenções de DTO, Command e Mapper nos módulos `project` e `audit`

**Campo nulo significa ausência de critério.** Vale para os filtros de leitura — `SearchProjectsRequest`, `SearchProjectsQuery`, `SearchAuditEventsRequest` e `SearchAuditEventsQuery`: nulo quer dizer "não filtrar por este critério", o que permite a mesma consulta servir a chamadas com e sem filtro. Em `UpdateProjectRequest` a mesma convenção significa "manter o valor atual", por ser uma alteração parcial.

**Por que `UpdateProjectRequest` não tem `@NotBlank`.** O domínio precisa distinguir "não informado" (nulo, mantém) de "informado vazio" (string em branco, é erro). Um `@NotBlank` no DTO recusaria os dois casos na borda e tiraria do agregado a decisão — `Project.update()` é quem lança `InvalidProjectDataException` — que responde **400**, e não 409, desde que passou a herdar `InvalidDataException`. O `docs/API.md` já descrevia esse endpoint como "`400` em dados inválidos": a mudança alinhou o código ao contrato que estava documentado.

**Nem todo mapper implementa `Mapper<I, O>`.** A interface tem uma entrada só, e `UpdateProjectMapper`, `AddRoleToProjectMapper` e `AddRouteToProjectMapper` montam o command a partir de duas origens: o identificador vem do caminho e o restante do corpo. Eles ficam fora da interface, mas continuam em `api/mapper/` — o controller segue apenas repassando dados, sem montar command inline. Os demais (`CreateProjectMapper`, `SearchProjectsMapper`, `SearchAuditEventsMapper` e todos os de resposta) implementam `Mapper<I, O>` normalmente.

**Serialização do 1-N sem referência circular.** `ProjectResponse` embute os filhos, e `RoleResponse`/`RouteResponse` expõem o pai como `projectId` (UUID), não como objeto. O ciclo se fecha do lado do filho, o que dispensa `@JsonIgnore` ou `@JsonManagedReference` — a decisão está no formato do DTO, não em anotação de serialização.

**Um DTO para toda a hierarquia de `AuditEvent`.** No `audit-service`, `AuditEventResponse` serve `PermissionCheckEvent` e `ProjectLifecycleEvent`: o que distingue as subclasses sai por `type()` e `describe()`, ambos polimórficos. A API expõe a herança sem um DTO por subclasse, e uma subclasse nova não muda o controller nem o mapper. O monolito recebe `type` e `description` prontos e só os repassa (`AuditTrailEntry`).

---

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

**A porta é burra; filtro e ordenação ficam no use case.** `ProjectRepository` expõe só `save`, `findById`, `findAll` e `existsById`. Quem filtra por `deletedAt`, por trecho de nome, por método HTTP e quem ordena é o use case (`SearchProjectsUseCase`, `FindProjectRoutesUseCase`). Isso mantém o adapter in-memory e o adapter JPA com a mesma superfície, e concentra a regra de leitura onde ela é testável sem banco. A contrapartida é que consultas que precisem descer para o SQL (paginação, agregação) exigem ampliar a porta — na etapa 4 as consultas derivadas do `JpaRepository` entram por baixo do adapter, sem mudar o use case.

**Exceção: a trilha de auditoria filtra no banco.** Ela cresce sem limite, e filtrar em memória carregava a tabela inteira a cada consulta. No `audit-service`, a porta `AuditEventRepository` expõe `search` e `searchDenied` com os filtros como parâmetros, e o adapter os resolve em JPQL — ver ADR-009. O módulo `audit` do monolito só repassa os filtros ao serviço pela porta `AuditTrail` (ADR-010). O `project` continua na regra geral porque o volume por cliente é pequeno.

`ProjectRepository` também não expõe `deleteById`: a remoção é soft delete e é comportamento do agregado (`Project.delete()`); uma remoção física na porta permitiria contorná-lo por fora do domínio.

**Mesma regra vale para comunicação entre módulos, não só para JPA:** `permission/domain/ApiKeyValidator` é uma porta que `permission` define para si mesmo; quem a implementa é `permission/infrastructure/BillingApiKeyValidator`, que por dentro chama o use case `billing.application.subscription.FindActiveApiKeyByPlainKeyUseCase`. Isso mantém `permission/domain` sem importar nada de `billing` — só `permission/infrastructure` conhece a existência do outro módulo, exatamente como só `infrastructure` conhece o JPA.

---

### Mapper de persistência × adapter — quem faz o quê

No `project`, a conversa domínio ↔ JPA está dividida em duas responsabilidades, e a linha entre elas é
nítida:

| | Onde | Responsabilidade |
|---|---|---|
| **Mapper de persistência** | `infrastructure/<submódulo>/XxxJpaMapper` | traduzir **um** objeto entre os dois mundos: `toJpa`, `toDomain` e `copy` (aplicar numa entidade gerenciada só o que pode mudar depois da criação) |
| **Adapter** | `infrastructure/project/ProjectRepositoryAdapter` | orquestrar o **agregado**: decidir quais filhos são novos, alterados ou removidos, aplicar isso na coleção gerenciada pelo EntityManager e cuidar da transação |

Cada mapper mora no submódulo da sua própria entidade (`RoleJpaMapper` em `infrastructure/role`,
`RouteJpaMapper` em `infrastructure/route`, `RoleRouteJpaMapper` em `infrastructure/roleroute`), como
manda o ADR-006. `ProjectJpaMapper` delega aos dois primeiros, e `RoleJpaMapper` delega ao terceiro —
o grafo de dependência acompanha o grafo do agregado e não tem ciclo.

Em uma frase: **o mapper constrói um objeto a partir de outro; o adapter decide o que construir.**

**Por que a decisão de "novo, alterado ou removido" não desce para o mapper:** ela depende de comparar
a lista do domínio com a coleção que o Hibernate está gerenciando, e isso só o dono do agregado
enxerga. Um mapper que recebesse a entidade gerenciada para mexer na coleção deixaria de ser tradutor
e viraria um segundo orquestrador.

**É o mesmo desenho que a camada `api` já usa na entrada.** `ProjectController` não monta o JSON de
resposta — quem monta é `ProjectResponseMapper`, e o controller continua sendo o responsável pela
resposta. `ProjectJpaMapper` é esse mesmo par espelhado na saída para o banco.

> **Só o `project` segue este desenho hoje.** `identity` e `billing` mantêm `toJpa`/`toDomain` dentro
> do próprio adapter (ver `ClientRepositoryAdapter`, 91 linhas). A diferença é deliberada e tem duas
> razões: `Client`, `Plan` e `Subscription` não têm coleção filha, então o adapter não incha; e os dois
> módulos são evidência congelada da disciplina de Clean Code — reescrevê-los agora apagaria o retrato
> do que foi entregue lá. **Estender adapter + mapper aos demais módulos é trabalho futuro planejado**,
> não pendência esquecida: quando um deles ganhar um agregado com filhos, ele vem junto.

**Por que esses mappers não implementam `Mapper<I,O>`:** a interface tem um método de um argumento só.
Aqui `toDomain` precisa do id do pai (é ele que quebra a referência circular no JSON) e o `toJpa` de
`RoleRoute` precisa da `RouteJpaEntity` já gerenciada. É a mesma exceção, pelo mesmo motivo, que já
vale para `UpdateProjectMapper` e companhia na camada `api`.

---

## Tratamento de exceções — GlobalExceptionHandler

Duas camadas de tipos, para conciliar "handler genérico por categoria HTTP" com "exceção legível e concentrada por módulo":

1. **Categorias abstratas em `shared/domain/exception/`** — mapeiam 1:1 para um status HTTP. Nenhuma delas é instanciada diretamente (construtor `protected`), só estendida:
   - `DomainException` — superclasse abstrata de tudo.
   - `ResourceNotFoundException extends DomainException` → `404`.
   - `BusinessRuleException extends DomainException` → `409`.
   - `InvalidDataException extends DomainException` → `400` (acrescentada na disciplina de microsserviços).
2. **Exceções concretas por módulo**, uma para cada erro de negócio real do sistema, cada uma já carregando sua própria mensagem — quem lança nunca monta uma `String` na hora do `throw`:

   | Exceção | Módulo/pacote | Extends | Uso |
   |---|---|---|---|
   | `ClientNotFoundException` | `identity/domain/exception/` | `ResourceNotFoundException` | `FindClientByIdUseCase` |
   | `EmailAlreadyInUseException` | `identity/domain/exception/` | `BusinessRuleException` | `RegisterClientUseCase` — recebe o email no construtor e monta a mensagem internamente |
   | `PlanNotFoundException` | `billing/domain/plan/exception/` | `ResourceNotFoundException` | `FindPlanByIdUseCase` |
   | `PaymentDeclinedException` | `billing/domain/subscription/exception/` | `BusinessRuleException` | `SubscribeToPlanUseCase` — recebe o `subscriptionId` |
   | `ActiveSubscriptionExistsException` | `billing/domain/subscription/exception/` | `BusinessRuleException` | `SubscribeToPlanUseCase.handleExistingSubscription` — recebe o `expiresAt` |
   | `PlanLimitExceededException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.addRole` — recebe o nome do projeto e o `maxRoles` |
   | `RoleAlreadyExistsException` | `project/domain/role/exception/` | `BusinessRuleException` | `Project.addRole` — recebe o nome do cargo |
   | `RouteAlreadyExistsException` | `project/domain/route/exception/` | `BusinessRuleException` | `Project.addRoute` — recebe o `httpMethod` e o `path` |
   | `ProjectAlreadyInactiveException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.deactivate` — recebe o nome do projeto |
   | `ProjectAlreadyActiveException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.activate` — recebe o nome do projeto |
   | `ProjectAlreadyDeletedException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.delete` — recebe o nome do projeto |
   | `InvalidProjectDataException` | `project/domain/project/exception/` | `InvalidDataException` | `Project.update` — recebe o campo e o motivo (`"must not be blank"`, `"must be greater than zero"`) |
   | `RoleNotFoundException` | `project/domain/role/exception/` | `ResourceNotFoundException` | busca de cargo dentro do projeto |
   | `RouteNotFoundException` | `project/domain/route/exception/` | `ResourceNotFoundException` | busca de rota dentro do projeto |
   | `RouteAccessNotFoundException` | `project/domain/roleroute/exception/` | `ResourceNotFoundException` | revogação de concessão inexistente |
   | `RouteAccessAlreadyGrantedException` | `project/domain/roleroute/exception/` | `BusinessRuleException` | `Role.grantAccessTo` |
   | `RouteAccessAlreadyRevokedException` | `project/domain/roleroute/exception/` | `BusinessRuleException` | `Role.revokeAccessTo` |
   | `ProjectNotFoundException` | `project/domain/project/exception/` | `ResourceNotFoundException` | `FindProjectByIdUseCase` |

   **Por que não `IllegalStateException`:** transição de estado inválida é regra de negócio, não erro de programação. Uma `IllegalStateException` cai no handler genérico e vira **500** — o cliente recebe "erro interno" quando na verdade fez um pedido inválido. Herdando `BusinessRuleException`, a mesma situação vira **409** com mensagem legível, sem tocar no `GlobalExceptionHandler`.

   Exemplo: `throw new ClientNotFoundException();` em vez de `throw new ResourceNotFoundException("Client not found")` — a mensagem some do call site porque já é responsabilidade da própria exceção. Quando há dado relevante para a mensagem (email, id, data), ele entra como parâmetro do construtor (ex.: `new EmailAlreadyInUseException(command.email())`) — nunca a mensagem pronta.

`shared/api/GlobalExceptionHandler` (`@RestControllerAdvice`) continua enxergando só as categorias abstratas — não precisa conhecer `ClientNotFoundException` nem qualquer outra folha, porque toda folha herda de uma das duas categorias:

- `ResourceNotFoundException` (e qualquer subclasse) → `404 Not Found`
- `BusinessRuleException` (e qualquer subclasse) → `409 Conflict`
- `InvalidDataException` (e qualquer subclasse) → `400 Bad Request`
- `MethodArgumentNotValidException` (falha de `@Valid` nos DTOs de request) → `400 Bad Request`, mensagem concatena `campo: motivo` de cada erro de validação
- `HttpMessageNotReadableException` (corpo ilegível: JSON malformado, UUID que não converte) → `400 Bad Request` com mensagem fixa `"Malformed request body"`. **Não** devolve `ex.getMessage()`: o Jackson expõe nome de classe, campo e posição no JSON, que é exatamente o detalhe interno que a Etapa 1 manda não vazar
- Qualquer outra `Exception` não mapeada → `500 Internal Server Error`, logada via `@Slf4j` (nunca vaza stacktrace pro cliente)

**Por que `HttpMessageNotReadableException` precisa de handler próprio.** `GlobalExceptionHandler` não estende `ResponseEntityExceptionHandler`, e o `@ExceptionHandler(Exception.class)` deste advice é resolvido antes do `DefaultHandlerExceptionResolver` do Spring. Sem a entrada específica, todo corpo malformado caía no catch-all e voltava **500**. Verificado em 25/09/2026: `{"projectId":"nao-sou-uuid"}` e `{"apiKey": ` agora respondem `400`.

**Ordem dos handlers não importa.** `ResourceNotFoundException`, `BusinessRuleException` e `InvalidDataException` são irmãs sob `DomainException`, sem sobreposição; o Spring escolhe o `@ExceptionHandler` mais próximo na hierarquia da exceção lançada. Uma exceção concreta só precisa de handler próprio se divergir da categoria que herda.

**Consideração registrada — `402` para pagamento recusado.** `PaymentDeclinedException` herda `BusinessRuleException` e portanto responde `409`, junto com os conflitos de estado. Só um dos dois é conflito de verdade: pagamento recusado é operação externa que falhou, e o código próprio seria `402 Payment Required`. Separar exigiria um `@RestControllerAdvice` só do `billing` — `shared` não pode importar de um módulo de negócio sem inverter a dependência e quebrar o `verifiesModularStructure()`. Custo maior que o ganho nesta entrega; o raciocínio completo está em `docs/API.md`, na seção do `POST /subscriptions`.

Formato de resposta único: `shared/api/dto/ErrorResponse` (`status`, `error`, `message`, `timestamp`).

**Por que duas camadas:** se cada módulo lançasse `BusinessRuleException("texto solto")` diretamente, o texto ficaria espalhado pelos use cases e duplicado sempre que o mesmo erro fosse lançado de dois lugares. Concentrar cada exceção concreta no módulo do domínio que ela descreve (seguindo a mesma regra de ouro de `ARCHITECTURE.md` — `domain` não conhece HTTP) deixa o `throw` autoexplicativo e a mensagem con­sistente em um único lugar; a categoria abstrata em `shared` é só o que o `GlobalExceptionHandler` precisa para decidir o status HTTP, sem precisar de um `@ExceptionHandler` por exceção concreta.

**Como estender:**
- Novo erro dentro de uma categoria já existente (404 ou 409) → criar uma nova classe em `<módulo>/domain/[<agregado>/]exception/` estendendo `ResourceNotFoundException` ou `BusinessRuleException`; o `GlobalExceptionHandler` não muda.
- Nova categoria de status HTTP → criar uma nova subclasse abstrata de `DomainException` em `shared/domain/exception/` e um `@ExceptionHandler` correspondente no `GlobalExceptionHandler`.

---

---

## Segurança do Swagger UI

`SecurityConfig` deixa todo o restante da API com `permitAll()` (autenticação real de cliente é trabalho futuro, ver o [`PLAN.md` da disciplina de Clean Code](https://github.com/Permission-SaaS/permission_saas/blob/main/docs/clean_code_e_padroes_de_projeto/PLAN.md)), mas `/swagger-ui/**` e `/v3/api-docs/**` exigem HTTP Basic com um usuário fixo em memória (`InMemoryUserDetailsManager`), configurado via `app.swagger.username` / `app.swagger.password` (env vars `SWAGGER_USERNAME` / `SWAGGER_PASSWORD`; default `admin` / `admin123` só no profile `dev`, obrigatórias no `prod` — ADR-011). `/actuator/**` continua liberado.

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
├── project/               # implementado na disciplina de Spring Boot
│   │                      # cada camada é dividida em project/, role/ e route/, como no billing
│   ├── domain/
│   │   ├── project/       # Project.java, ProjectRepository.java (porta)
│   │   │   └── exception/ # ProjectNotFoundException, PlanLimitExceededException,
│   │   │                  # InvalidProjectDataException, ProjectAlready{Active,Inactive,Deleted}Exception
│   │   ├── role/          # Role.java + exception/RoleAlreadyExistsException, RoleNotFoundException
│   │   ├── route/         # Route.java, RouteImporter (porta da importação em lote),
│   │   │                  # RouteImportResult + exception/RouteAlreadyExistsException, RouteNotFoundException
│   │   └── roleroute/     # RoleRoute.java (entidade associativa com histórico)
│   │       └── exception/ # RouteAccessAlreadyGranted/AlreadyRevoked/NotFoundException
│   ├── application/
│   │   ├── project/       # Create/Update/Delete/Purge/FindById/FindAll/Search ProjectUseCase,
│   │   │   │              # CheckRouteAccessUseCase, RouteAccessResult,
│   │   │   │              # package-info.java com @NamedInterface("application.project")
│   │   │   ├── command/   # CreateProjectCommand, UpdateProjectCommand
│   │   │   └── query/     # SearchProjectsQuery
│   │   ├── role/          # AddRoleToProjectUseCase
│   │   │   └── command/   # AddRoleToProjectCommand
│   │   ├── route/         # AddRouteToProjectUseCase, FindProjectRoutesUseCase, ImportRoutesUseCase
│   │   │   └── command/   # AddRouteToProjectCommand
│   │   └── roleroute/     # GrantRouteToRoleUseCase, RevokeRouteFromRoleUseCase,
│   │                      # FindRolePermissionsUseCase
│   ├── infrastructure/
│   │   ├── project/       # ProjectJpaEntity (@OneToMany roles/routes), ProjectJpaMapper,
│   │   │                  # JpaProjectRepository, ProjectRepositoryAdapter, ProjectDemoRunner
│   │   ├── role/          # RoleJpaEntity (@ManyToOne project, @OneToMany permissions), RoleJpaMapper
│   │   ├── route/         # RouteJpaEntity (@ManyToOne project, @OneToMany permissions inverso),
│   │   │   │              # RouteJpaMapper
│   │   │   └── batch/     # job de importação (ADR-014): ImportRoutesJobConfig (reader, step, job),
│   │   │                  # RouteCsvLine, RouteImportProcessor, RouteImportWriter, BatchRouteImporter
│   │   └── roleroute/     # RoleRouteJpaEntity (@ManyToOne role + @ManyToOne route), RoleRouteJpaMapper
│   └── api/
│       ├── project/       # ProjectController.java
│       │   ├── dto/       # Create/Update/Search ProjectRequest, ProjectResponse
│       │   └── mapper/    # CreateProjectMapper, UpdateProjectMapper, SearchProjectsMapper,
│       │                  # ProjectResponseMapper
│       ├── role/          # RoleController.java
│       │   ├── dto/       # AddRoleRequest, RoleResponse
│       │   └── mapper/    # AddRoleToProjectMapper, RoleResponseMapper
│       ├── route/         # RouteController.java
│       │   ├── dto/       # AddRouteRequest, RouteImportRequest, RouteResponse, RouteImportResponse
│       │   └── mapper/    # AddRouteToProjectMapper, RouteResponseMapper, RouteImportResponseMapper
│       └── roleroute/     # RoleRouteController.java
│           ├── dto/       # RolePermissionResponse
│           └── mapper/    # RolePermissionResponseMapper
│
├── permission/            # implementado — Chain of Responsibility (docs/PATTERNS.md)
│   ├── domain/            # PermissionValidationHandler.java (Handler abstrato),
│   │                      # ApiKeyValidationHandler, TokenValidationHandler,
│   │                      # RoleRouteValidationHandler (ConcreteHandlers),
│   │                      # ApiKeyValidator.java e RouteAccessChecker.java (portas),
│   │                      # dto/PermissionCheckRequest.java, dto/PermissionCheckResult.java
│   │   └── event/         # PermissionValidatedEvent.java + package-info @NamedInterface("events")
│   ├── application/       # ValidatePermissionUseCase.java
│   ├── infrastructure/    # BillingApiKeyValidator.java (implementa ApiKeyValidator chamando
│   │                      # billing.FindActiveApiKeyByPlainKeyUseCase),
│   │                      # ProjectRouteAccessChecker.java (implementa RouteAccessChecker
│   │                      # chamando project.CheckRouteAccessUseCase)
│   └── api/               # PermissionController.java
│       ├── dto/           # ValidatePermissionRequest.java, PermissionValidationResponse.java
│       └── mapper/        # ValidatePermissionMapper.java, PermissionValidationResponseMapper.java
│
└── audit/                 # implementado na disciplina de Spring Boot; desde 30/09/2026 é
    │                      # cliente do audit-service, sem banco próprio (ADR-010)
    ├── domain/            # AuditEvent.java (abstrata) e PermissionCheckEvent.java (o evento
    │                      # enviado), AuditEventPublisher.java (porta de gravação, pela fila),
    │                      # AuditTrail.java (porta de consulta no audit-service),
    │                      # AuditTrailEntry.java (modelo de leitura)
    │   └── exception/     # InvalidAuditPeriodException.java, AuditTrailUnavailableException.java,
    │                      # AuditEventNotPublishedException.java
    ├── application/       # AuditLogListener.java (Observer, @Async), SearchAuditEventsUseCase.java
    │   └── query/         # SearchAuditEventsQuery.java
    ├── infrastructure/
    │   ├── messaging/     # RabbitAuditEventPublisher.java, AuditMessagingConfig.java (fila e conversor)
    │   │   └── dto/       # AuditMessage.java (envelope), PermissionCheckPayload.java (contrato)
    │   └── client/        # AuditClient.java (@FeignClient, só a consulta), AuditTrailClientAdapter.java
    │       └── dto/       # AuditEventResponse.java (contrato)
    └── api/               # AuditEventController.java
        ├── dto/           # SearchAuditEventsRequest.java, AuditEventResponse.java
        └── mapper/        # SearchAuditEventsMapper.java, AuditEventResponseMapper.java
```

Todos os módulos de negócio são subpacotes diretos de `com.saas.permissions` (ex: `com.saas.permissions.identity`), assim como `shared`. Essa é a estrutura exigida pela detecção automática de módulos do Spring Modulith, que considera cada subpacote direto do pacote da classe `@SpringBootApplication` como um Application Module.
