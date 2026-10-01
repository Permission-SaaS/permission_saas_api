# Arquitetura — Permission SaaS

## Ideia central

Um **monolito modular**: um único processo/deploy, mas organizado em módulos independentes por domínio de negócio. Cada módulo é um mini-sistema autocontido que não conhece os detalhes internos dos outros.

Desde a disciplina de microsserviços, o sistema é **um monolito modular mais um serviço extraído dele**: o `permission-service/` (a aplicação principal, com `identity`, `billing`, `project`, `permission` e `audit`) e o `audit-service/`. A extração segue o padrão *Strangler Fig* — uma capacidade de cada vez, começando pela mais desacoplada — em vez de reescrever o monolito inteiro em serviços. Ver "O `audit-service`" abaixo e o ADR-008.

---

## O `audit-service` — o primeiro serviço extraído

Aplicação Spring Boot própria (`audit-service/`, porta 8081), com as mesmas quatro camadas de um módulo do monolito — sem subpacote de módulo, porque o serviço inteiro **é** o módulo:

```
com.saas.audit/
  domain/          AuditEvent e subclasses, AuditEventRepository e AuditEventJournal (portas), exception/
  application/     SearchAuditEventsUseCase (query/), RegisterPermissionCheckUseCase (command/)
  infrastructure/  entidades JPA SINGLE_TABLE, consultas JPQL do ADR-009, AuditEventFileWriter
  api/             AuditEventController, DTOs, mappers, GlobalExceptionHandler
```

- **Dono dos próprios dados:** banco `audit_db` num container Postgres próprio (`audit-postgres`, porta 5433), com usuário próprio — a aplicação principal nem tem credencial para entrar nele. O schema é da migration `V1` do serviço, cópia da `V8` do monolito.
- **Contrato pela rede, não por código:** `GET /audit-events` e `POST /audit-events/permission-checks` (ver `docs/API.md`). O que os dois lados têm em comum — `Mapper`, `ErrorResponse`, as exceções base — foi **copiado**, não compartilhado por biblioteca (ADR-008).
- **O que ficou de fora:** Spring Security (o serviço é chamado pela rede interna, não por clientes), Spring Modulith (é um módulo só), `AuditDemoRunner` e `AuditLogListener` — no serviço, o evento chega pelo `POST`, não por evento em memória.

**Extração concluída (30/09/2026).** A gravação e a consulta passam pelo serviço, e o monolito não guarda mais nada da trilha. Na gravação, o `AuditLogListener` do monolito traduz o `PermissionValidatedEvent` em `PermissionCheckEvent` e o entrega à porta `AuditTrail`, implementada por `AuditTrailClientAdapter` sobre o cliente OpenFeign `AuditClient` (`audit/infrastructure/client/`). O listener roda na thread da validação de permissão, então duas proteções impedem que a auditoria derrube a validação: timeout curto no cliente `audit-service` (1s para conectar, 2s para responder, no `application.yml`; o default do Feign é 60s) e um `try/catch` no listener. O adapter só traduz a `FeignException` para a `AuditTrailUnavailableException` da porta; quem decide que perder o evento é aceitável e quebrar a validação não é, é a camada `application`. Com o serviço fora do ar, a validação responde normalmente e o evento **se perde**, com um `WARN` no log — a limitação que a etapa 4 resolve trocando o Feign da gravação por uma fila no RabbitMQ. Na consulta, o `GET /audit-events` do monolito valida os filtros e o período (os `400` saem daqui, sem chamar a rede) e repassa ao serviço pela mesma porta `AuditTrail`, que devolve o modelo de leitura `AuditTrailEntry` — a resposta do serviço não traz dados para reconstruir um `PermissionCheckEvent`. Serviço fora do ar vira `503`: a `AuditTrailUnavailableException` estende a `ServiceUnavailableException` do `shared`, mapeada pelo `GlobalExceptionHandler` (o `shared` não pode importar do `audit`, que depende dele). A persistência local do módulo — entidades JPA, consultas do ADR-009, arquivo texto, `AuditDemoRunner` — foi apagada e a migration `V10` removeu a tabela `audit_events` do banco principal (ADR-010).

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
    └── CheckRouteAccessUseCase responde se um cargo alcança uma rota —
        é o que o permission consulta pelo RouteAccessChecker
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
  repassado ao audit-service pela porta AuditTrail; serviço fora do ar → 503
  AuditLogListener escuta PermissionValidatedEvent e registra a validação
  no audit-service (porta AuditTrail → cliente OpenFeign AuditClient)
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

## ADR-001: quem gera o ID da entidade — domínio ou Hibernate

**Decisão:** métodos de fábrica no domínio (`Subscription.pendingFor()`, etc.) **não** atribuem `id` manualmente quando a entidade JPA correspondente usa `@GeneratedValue(strategy = GenerationType.UUID)`. O `id` fica `null` até o primeiro `save()`; o adapter lê o valor gerado de volta (`toDomain(saved)`) e o use case reatribui a variável (`subscription = subscriptionRepository.save(subscription)`).

**Por quê:** o Spring Data `SimpleJpaRepository.save()` decide entre `persist()` e `merge()` checando se o `id` está `null` (`isNew()`). Se o domínio já atribui um UUID antes do primeiro save, o repositório assume que a entidade **já existe** e chama `merge()` — que faz um `SELECT` pra achar a linha, não encontra (ela ainda não existe) e o Hibernate lança `StaleObjectStateException` (`ObjectOptimisticLockingFailureException`), mesmo sem `@Version` na entidade. Foi exatamente o bug corrigido em `SubscribeToPlanUseCase`/`Subscription.pendingFor()` — `Client` e `ApiKey` nunca tiveram esse problema porque já seguiam essa regra.

**Como aplicar:** qualquer entidade nova com `@GeneratedValue(strategy = GenerationType.UUID)` (ex: futuras entidades de `project`, `permission`, `audit`) deve deixar o Hibernate gerar o `id` — nunca pré-atribuir no domínio. Se um fluxo salvar a mesma entidade mais de uma vez na mesma transação (como a subscription: pending → paid/rejected → active), sempre reatribuir a variável local ao retorno de `save()`.

> ⚠️ **Exceção deliberada:** `Project`, `Role` e `Route` continuam gerando o próprio `id` no domínio. O ADR-003 explica por quê isso deixou de conflitar com este ADR quando a persistência JPA entrou na etapa 4. `AuditEvent` segue este ADR: quem atribui o `id` é o Hibernate (`@GeneratedValue(strategy = GenerationType.UUID)` na `AuditEventJpaEntity`).

---

## ADR-003: `Project`/`Role`/`Route` geram o próprio `id` — desvio do ADR-001

**Status:** aceito nas etapas 1-3 como temporário; **mantido em definitivo na etapa 4** — ver "Resolução na etapa 4" no fim deste ADR.

**Contexto:** `Project.addRole()` e `Project.addRoute()` fecham a referência de volta do filho para o pai (`role.setProjectId(this.id)`) — é o que materializa o relacionamento 1-N exigido pela rubrica. Isso só funciona se `this.id` já existir no momento da chamada.

Nas etapas 1-3 não há banco: a persistência é um `Map` in-memory e a demonstração monta o grafo em memória. Se o `id` só fosse atribuído no `save()`, todo `Role`/`Route` sairia com `projectId = null` — o relacionamento não apareceria nem no `toString()` da demo, nem no arquivo de auditoria.

**Decisão:** enquanto a persistência for in-memory, as três entidades do módulo `project` inicializam `id` com `UUID.randomUUID()` via `@Builder.Default`.

**Conflito conhecido com o ADR-001:** o ADR-001 determina o oposto — o domínio **não** deve pré-atribuir `id` quando a entidade JPA usa `@GeneratedValue(strategy = GenerationType.UUID)`, porque o `SimpleJpaRepository.save()` decide entre `persist()` e `merge()` checando `id == null`. Um `id` pré-atribuído faz o Spring Data chamar `merge()` em uma entidade que ainda não existe, e o Hibernate lança `ObjectOptimisticLockingFailureException`. Foi exatamente o bug corrigido em `SubscribeToPlanUseCase`.

Ou seja: **manter o `@Builder.Default` do `id` na etapa 4 reintroduz um bug já corrigido neste projeto.**

### Consequência para o `@EqualsAndHashCode(of = "id")`

`Project`, `Role` e `Route` são anotadas com `@EqualsAndHashCode(of = "id")` — igualdade por identidade, não por valor. Dois objetos são a mesma entidade se têm o mesmo `id`, independentemente dos demais campos. É a semântica correta para uma **entidade** (ao contrário de um value object, que se compara por conteúdo), e é o padrão seguido por todo o domínio do projeto: `Client`, `Plan`, `Subscription` e `ApiKey` usam a mesma anotação.

Hoje isso é seguro **por causa do ADR-003**, e não por acaso. O `@Builder.Default` garante duas propriedades das quais o `equals`/`hashCode` depende:

| Propriedade | Por que importa |
|---|---|
| `id` nunca é `null` | duas entidades distintas nunca colidem como "ambas nulas" |
| `id` nunca muda durante a vida do objeto | o `hashCode()` é estável, então o objeto continua achável dentro de um `HashSet`/`HashMap` |

**O item 1 do checklist abaixo destrói as duas.** Com `private UUID id;` sem default, o `id` fica `null` até o primeiro `save()`, e aí:

1. **Duas entidades novas diferentes passam a ser "iguais".** Ambas têm `id == null`, então `equals()` devolve `true`. Dois `Role` recém-criados num `Set` viram um só, silenciosamente — sem exceção, sem log. O risco é concreto na etapa 4: se a coleção `@OneToMany` for mapeada como `Set` (item 3), o Hibernate vai usar `equals`/`hashCode` para gerenciá-la.
2. **O `hashCode()` muda depois do `save()`.** De `null` para um UUID. Se o objeto já estava dentro de um `HashSet` ou era chave de um `HashMap`, ele passa a morar no bucket errado e não é mais encontrado — nem por `contains()`, nem por `remove()`. É a armadilha clássica de `equals`/`hashCode` com entidades JPA.

**As três saídas possíveis na etapa 4** (decidir antes de escrever a `ProjectJpaEntity`):

| Opção | Como | Custo |
|---|---|---|
| **A — `Persistable`** | manter o UUID gerado no domínio e implementar `Persistable<UUID>.isNew()` para o Spring Data saber que a entidade é nova sem consultar o `id` | resolve o conflito ADR-001 × ADR-003 na raiz; `equals`/`hashCode` continuam válidos sem alteração |
| **B — chave natural** | comparar por chave de negócio em vez de `id`. `Role` já tem uma: `projectId` + `name` (o invariante de unicidade). `Route`: `projectId` + `httpMethod` + `path` | dispensa Lombok, exige `equals`/`hashCode` à mão; `Project` não tem chave natural óbvia |
| **C — `equals` null-safe** | `id != null && id.equals(other.id)`, com `hashCode()` retornando constante (`getClass().hashCode()`) | o Lombok não expressa isso — `equals`/`hashCode` passam a ser escritos à mão nas três classes |

A opção **A** é a preferida: ela preserva tudo que já está escrito e elimina o desvio em vez de administrá-lo. As opções B e C existem aqui para o caso de a `ProjectJpaEntity` acabar exigindo `@GeneratedValue`.

> Os eventos do módulo `audit` seguem regra diferente e proposital: `AuditEvent` usa `@EqualsAndHashCode(of = "id")`, mas `PermissionCheckEvent` e `ProjectLifecycleEvent` usam `@EqualsAndHashCode(callSuper = true)`, incluindo os campos próprios de cada subclasse. Revisar essa escolha ao mapear a herança `SINGLE_TABLE` na etapa 4 — herança e igualdade por identidade interagem mal quando duas subclasses diferentes podem compartilhar o mesmo `id` de tabela única.

**O que fazer na etapa 4 (checklist obrigatório):**

1. Remover o `@Builder.Default` de `id` em `Project`, `Role` e `Route` — voltar a `private UUID id;`. **Só fazer isto junto com o item 2.**
2. Escolher e aplicar uma das três opções de `equals`/`hashCode` acima. Um `id` nulo com `@EqualsAndHashCode(of = "id")` é bug silencioso, não erro de compilação — nenhum teste existente falha por isso.
3. Mapear a coleção como `@OneToMany(mappedBy = "project", cascade = ALL, orphanRemoval = true)` na `ProjectJpaEntity`, deixando o JPA ser dono da FK. `Role.projectId`/`Route.projectId` no domínio passam a ser valor derivado, preenchido pelo adapter em `toDomain()`.
4. Remover as chamadas `role.setProjectId(this.id)` / `route.setProjectId(this.id)` de `addRole`/`addRoute`, que deixam de ter função.
5. Conferir que `ProjectRepositoryAdapter` reatribui a variável ao retorno do `save()` (`project = projectRepository.save(project)`), como manda o ADR-001.

**Por que aceitar o desvio em vez de evitá-lo:** a alternativa seria a rotina de demonstração atribuir os `id` manualmente antes de montar o grafo, deixando o domínio limpo. Isso empurraria uma responsabilidade de infraestrutura para dentro da demo e tornaria o `addRole` inseguro por padrão (silenciosamente gravando `null` se alguém esquecesse). Como a troca para JPA já está isolada atrás da porta `ProjectRepository` — o use case não muda —, o custo de reverter é o checklist acima, contido em três arquivos de domínio e um adapter.

---

### Resolução na etapa 4

O desvio **não foi revertido** — deixou de ser um desvio. O que mudou é que a persistência JPA usa
classes próprias (`ProjectJpaEntity`, `RoleJpaEntity`, `RouteJpaEntity`), separadas das classes de
domínio. O conflito descrito acima só existiria se a entidade de domínio *fosse* a entidade JPA.

**Por que o conflito com o ADR-001 não se materializa:**

| Condição do bug original | Situação na etapa 4 |
|---|---|
| entidade JPA com `@GeneratedValue` recebendo `id` pré-atribuído | `ProjectJpaEntity` usa `@Id` **atribuído**, sem `@GeneratedValue` — a coluna tem `DEFAULT gen_random_uuid()` só para quem inserir por SQL |
| `save()` decidindo `persist`/`merge` às cegas | o `ProjectRepositoryAdapter` faz `findById` antes de gravar: se existe, muta a entidade **gerenciada**; se não, monta uma nova |
| `@Version` disparando `ObjectOptimisticLockingFailureException` | nenhuma das três entidades tem `@Version`, então `merge()` sobre uma linha inexistente resolve para `SELECT` + `INSERT`, não para um `UPDATE` de zero linhas |

**Sobre a opção A (`Persistable`), que este ADR elegia como preferida:** foi implementada, avaliada e
removida. Ela só economiza o `SELECT` que o `merge()` faz antes de inserir, e cobra por isso um campo
`@Transient isNew` mais callbacks `@PostLoad`/`@PostPersist` — ou seja, um segundo lugar guardando
"esta linha já existe?", que o adapter já sabe porque acabou de consultar. Estado duplicado em troca
de uma consulta: não compensa. Se um dia o custo do `SELECT` extra pesar (inserção em lote, por
exemplo), `Persistable` volta como otimização localizada na entidade JPA, sem tocar em domínio nem
use case.

**Consequência para `equals`/`hashCode`:** o risco descrito acima **desapareceu**, porque o item 1 do
checklist (remover o `@Builder.Default` do `id`) não foi executado — e não precisa ser. O `id` do
domínio continua nunca sendo nulo e nunca mudando, que são exatamente as duas propriedades de que o
`@EqualsAndHashCode(of = "id")` depende.

**Checklist original, item a item:**

| Item | Situação |
|---|---|
| 1. remover `@Builder.Default` do `id` | ❌ não executado — deliberadamente, ver acima |
| 2. escolher uma das três opções de `equals`/`hashCode` | ✅ nenhuma foi necessária; a semântica atual segue válida |
| 3. mapear `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)` | ✅ feito na `ProjectJpaEntity` |
| 4. remover `role.setProjectId(this.id)` de `addRole`/`addRoute` | ❌ mantido de propósito: `Role.projectId`/`Route.projectId` são o que a resposta JSON usa para referenciar o pai sem referência circular. O adapter repreenche esse campo em `toDomain()` a partir de `entity.getProject().getId()`, então o valor nunca diverge do dono real da FK |
| 5. adapter reatribuindo a variável ao retorno de `save()` | ✅ `save()` devolve `toDomain(jpa.save(entity))` |

> A ressalva sobre herança e igualdade no `audit` também se resolveu: as subclasses de
> `AuditEventJpaEntity` compartilham a tabela, mas não o `id` — cada linha é um evento distinto, e o
> `id` é gerado pelo banco, nunca pelo domínio.

---

## ADR-005: sem `Map` in-memory, sem seed e sem loader de arquivo texto no código final

**Status:** aceito na etapa 4 da disciplina de Spring Boot.

**Contexto:** as etapas 1-3 pediam explicitamente um `Map` simulando o banco (itens 7 e 8 da rubrica) e
classes *loader* lendo arquivos texto (itens 4 e 5). O enunciado da Etapa 4 autoriza a remoção do
`Map`: *"A implementação com Map não precisa permanecer no código final, pois estará preservada no
marco etapa-3."*

**Decisão:** removidos do código final o `InMemoryProjectRepository`, o `InMemoryAuditEventRepository`,
o `ProjectFileLoader`, o `SeedFileException`, o `ProjectSeedRunner` e os arquivos
`src/main/resources/data/*.txt`. A aplicação passa a depender exclusivamente do banco.

**Motivo:** o seed automático fazia a subida da aplicação depender de três arquivos de classpath e
gravava dados de demonstração em qualquer ambiente onde a tabela `projects` estivesse vazia —
inclusive produção. Como a arquitetura final é `Controller → Service → Repository → Banco`, um
carregador de texto no caminho de inicialização é um segundo dono do estado inicial, sem dono claro.

**Consequência a registrar:** os itens 4, 5, 7 e 8 da rubrica passam a ser evidenciados **apenas pelas
tags** `etapa-1`, `etapa-2` e `etapa-3`, não pelo `HEAD`. Isso é coerente com a estrutura de avaliação
por marcos combinada com o professor, mas é uma escolha consciente: quem olhar só a versão final não
encontra o `Map` nem os loaders.

Para inspecionar essas evidências:

```bash
git show etapa-3:src/main/java/com/saas/permissions/project/infrastructure/InMemoryProjectRepository.java
git show etapa-3:src/main/java/com/saas/permissions/project/infrastructure/ProjectFileLoader.java
git show etapa-3:src/main/resources/data/projects.txt
```

---

## ADR-006: submódulos por entidade dentro de cada camada do `project`

**Status:** aceito na etapa 4 da disciplina de Spring Boot.

**Contexto:** o `billing` já dividia cada camada em `plan/` e `subscription/`. O `project` tinha essa
divisão apenas no `domain` (`project/`, `role/`, `route/`); `application`, `infrastructure` e `api`
eram planos, com 40 classes misturadas.

**Decisão:** replicar a divisão do `domain` nas outras três camadas. Cada camada do `project` tem
`project/`, `role/` e `route/`, e `ProjectController` deixou de acumular os sub-recursos: `/roles` e
`/routes` passaram para `RoleController` e `RouteController`, com as mesmas URLs de antes.

**Consequências:**

- As URLs e os contratos não mudaram — a coleção Postman roda igual, sem edição.
- `RoleJpaEntity`, `RouteJpaEntity` e `ProjectJpaEntity` tiveram de virar `public`: elas se referenciam
  entre subpacotes. `JpaProjectRepository` e `ProjectRepositoryAdapter` seguem restritos ao subpacote.
- O `@NamedInterface` do `project` desceu de `project.application` para `project.application.project`,
  que é o subpacote de onde o `permission` consome o `CheckRouteAccessUseCase`. Subpacote de um pacote
  anotado **não** herda a anotação no Spring Modulith — por isso a anotação precisa estar exatamente
  onde está a classe consumida.

---

## ADR-007: `RoleRoute` é entidade associativa com histórico, não tabela de junção

**Status:** aceito na etapa 4 da disciplina de Spring Boot (migration `V9`).

**Contexto:** até então o modelo tinha `Project 1─N Role` e `Project 1─N Route`, sem nada ligando cargo a rota. A validação de permissão conseguia dizer no máximo "cargo e rota existem no mesmo projeto", o que na prática libera qualquer cargo para qualquer rota — o oposto do que um sistema de permissões vende.

**Decisão:** modelar

```
Project 1 ──── N Role  1 ──── N RoleRoute
Project 1 ──── N Route 1 ──── N RoleRoute
```

com `RoleRoute` carregando `grantedAt` e `revokedAt` além das duas FKs.

**Por que não `@ManyToMany` com `@JoinTable`:** o `@ManyToMany` esconde a tabela de junção e não deixa espaço para atributos próprios. Revogar viraria `role.getRoutes().remove(route)` — um `DELETE`, e a informação de que aquele cargo já teve acesso some. Com entidade associativa explícita, revogar é preencher `revokedAt`: a linha permanece e a trilha de auditoria consegue responder **quando** o cargo perdeu o acesso, que é o motivo pelo qual a associação existe.

**Decisões de detalhe:**

| Decisão | Motivo |
|---|---|
| índice único **parcial** `WHERE revoked_at IS NULL` | garante no banco no máximo uma concessão ativa por par, e ao mesmo tempo permite empilhar histórico: conceder → revogar → conceder gera duas linhas |
| `isActive()` derivado de `revokedAt == null`, sem coluna booleana | um booleano mais a data poderiam divergir; com um campo só, o estado é sempre consistente |
| `@OneToMany` com cascata no `Role`, inverso somente leitura no `Route` | dois caminhos de cascata gravariam a mesma linha; o `Role` é o dono porque é por ele que o agregado navega. A relação `Route 1─N RoleRoute` continua existindo no mapeamento e no banco (FK com `ON DELETE CASCADE`) |
| `Project` é quem concede e revoga, não `Role` | só o agregado raiz enxerga cargos e rotas ao mesmo tempo, e é isso que permite validar que ambos pertencem ao mesmo projeto antes de criar a concessão |

**Consequência no adapter — `save()` em duas etapas.** `RoleRouteJpaEntity.route` é um `@ManyToOne` **sem cascata**: a rota precisa já estar gerenciada pelo `EntityManager` quando a concessão a referencia. Num projeto recém-criado a rota ainda é transiente, e o Hibernate tentava resolvê-la por id no banco, onde a linha ainda não existia — `ObjectRetrievalFailureException`. Por isso `ProjectRepositoryAdapter.save()` grava primeiro o esqueleto (projeto + cargos + rotas) e só então aplica as concessões, num segundo `save()` dentro da mesma transação. O caso só aparece na primeira gravação de um agregado que já nasce com concessões — foi um teste de integração que o pegou, não o teste manual pela API, onde rota e concessão vêm em requisições separadas.

---

## ADR-002: `@Data` no domínio quebra o encapsulamento — refactor planejado

**Status:** aceito como dívida técnica consciente. Refactor não agendado (ver "trabalho futuro").

**Contexto:** todas as entidades de domínio do projeto (`Client`, `Plan`, `Subscription`, `Project`, `Role`, `Route`, `AuditEvent`) usam `@Data` do Lombok, que gera getter **e setter públicos para todo campo**. Isso entrou no projeto pela conveniência de escrever entidades rápido, antes de qualquer entidade ter regra de negócio própria.

**Problema:** quando o domínio passa a guardar invariantes, o `@Data` os torna opcionais. `Project.addRole()` valida `maxRoles`, mas o `@Data` também expõe `getRoles()` e `setRoles()` — então:

```java
project.addRole(role);           // valida o limite
project.getRoles().add(role);    // fura o limite, mesma classe, mesmo efeito
project.setRoles(outraLista);    // troca a coleção inteira
```

O encapsulamento hoje é **convenção, não garantia**: o invariante só vale se todo chamador lembrar de usar o método certo. É a mesma classe de problema que o ADR-001 descreve — comportamento correto dependendo de disciplina do chamador em vez de estar imposto pelo tipo.

**Decisão para agora:** manter `@Data` e documentar a limitação. Trocar em todas as entidades é um refactor transversal que atinge use cases, adapters e mappers dos cinco módulos; fazer isso no meio da disciplina de Spring Boot competiria com as etapas e não tem item de rubrica correspondente.

**Refactor planejado (trabalho futuro):** por entidade que tenha invariante, substituir `@Data` por:

1. `@Getter` + `@EqualsAndHashCode(of = "id")` — sem `@Setter` de classe.
2. Coleções expostas como cópia imutável (`List.copyOf(roles)`) e mutáveis só pelos métodos de domínio (`addRole`, `removeRole`).
3. Setters pontuais só onde a infraestrutura exigir (mappers JPA), preferencialmente substituídos por construtor/builder.

Entidades sem regra de negócio própria (`Plan`, hoje) podem continuar com `@Data` — o critério é ter ou não invariante a proteger, não uniformidade.

**Ordem sugerida:** `Project` primeiro (é quem tem o invariante mais claro, `maxRoles`), depois `Subscription` (máquina de estados), depois as demais.

---

## ADR-004: schema dos módulos `project` e `audit` — fronteiras de FK e herança

**Status:** aceito na etapa 4 da disciplina de Spring Boot (migrations `V5`–`V8`).

> Desde 30/09/2026 (ADR-010), a tabela `audit_events`, a herança `SINGLE_TABLE` e os `CHECK` abaixo existem só no `audit_db` do `audit-service` (a `V1` dele é cópia da `V8`). A `V10` do monolito apagou a tabela do banco principal.

### Onde há FK e onde não há

> A migration `V9` acrescentou `role_routes`, com FK para `roles` **e** para `routes`, ambas `ON DELETE CASCADE` — as duas pontas são do mesmo agregado, então valem as mesmas razões de `roles` e `routes`. Ver ADR-007.

| Referência | FK no banco | Por quê |
|---|---|---|
| `roles.project_id` → `projects` | ✅ com `ON DELETE CASCADE` | mesmo módulo e mesmo agregado; acompanha o `cascade = ALL` / `orphanRemoval = true` do `@OneToMany` da `ProjectJpaEntity` |
| `routes.project_id` → `projects` | ✅ com `ON DELETE CASCADE` | idem |
| `projects.client_id` → `clients` | ❌ só índice | ver abaixo |
| `audit_events.project_id` → `projects` | ❌ só índice | ver abaixo |

`subscriptions` (V3) referencia `clients` e `plans` com FK, então a ausência delas aqui é desvio consciente do precedente, por dois motivos distintos:

- **`projects.client_id`** — `CreateProjectUseCase` não valida o cliente contra o módulo `identity`: a FK transformaria um `clientId` inexistente em erro 500 do driver, sem exceção de domínio mapeada pelo `GlobalExceptionHandler`. Validar o cliente de verdade exigiria o `identity` expor um `@NamedInterface` de consulta — trabalho futuro; aí a FK passa a fazer sentido.

  > Quando esta decisão foi escrita havia um segundo motivo: o seed de `ProjectFileLoader` referenciava clientes fictícios inexistentes em `clients`, e a FK quebraria o carregamento. Esse motivo caiu com a remoção do seed (ADR-005); o primeiro continua valendo sozinho.
- **`audit_events.project_id`** — a trilha de auditoria precisa sobreviver ao projeto que descreve, e o módulo `audit` não é dono da tabela `projects`. A coluna é **nullable** porque `PermissionValidatedEvent` ainda não carrega o projeto (mesma lacuna do `httpMethod`, achado 3 de 29/08).

### Herança do `audit`: `SINGLE_TABLE`

`AuditEvent` → `PermissionCheckEvent` / `ProjectLifecycleEvent` foi mapeada como tabela única discriminada por `event_type`, com valores iguais aos devolvidos por `AuditEvent.type()`. A alternativa `JOINED` foi descartada porque a trilha é *append-only* e sempre lida pela superclasse (`SearchAuditEventsUseCase` devolve `List<AuditEvent>`) — pagar um JOIN por leitura não se justifica.

O preço é que as colunas de cada subclasse precisam ser `NULL`-áveis. A obrigatoriedade volta como `CHECK` condicionado ao discriminador (`audit_events_permission_check_fields`, `audit_events_lifecycle_fields`), o que também protege `granted` e `duration_ms`, primitivos no domínio (`boolean`/`double`) que não aceitam `null`.

### O schema espelha os invariantes do domínio

As regras que hoje só existem em Java foram repetidas como constraint, para que dados inseridos fora da API — o seed dos `.txt`, por exemplo — respeitem a mesma regra:

| Constraint | Regra de domínio correspondente |
|---|---|
| `uq_roles_project_name` (`project_id`, `LOWER(name)`) | `Project.addRole()` → `RoleAlreadyExistsException` |
| `uq_routes_project_method_path` (`project_id`, `UPPER(http_method)`, `LOWER(path)`) | `Project.addRoute()` → `RouteAlreadyExistsException` |
| `projects_max_roles_check` | `Project.update()` → `InvalidProjectDataException`; `@Min(1)` de `CreateProjectRequest` |
| `routes_http_method_check`, `routes_path_check` | `@Pattern` de `AddRouteRequest` |

Dois índices são parciais, cobrindo exatamente os filtros que os use cases aplicam: `idx_projects_active` (`WHERE deleted_at IS NULL`) para o soft delete que `FindAllProjectsUseCase`/`FindProjectByIdUseCase` tratam como inexistência, e `idx_audit_events_denied` (`WHERE granted = FALSE`) para o filtro `onlyDenied` de `SearchAuditEventsUseCase`.

`updated_at` é *nullable* nas três tabelas novas, ao contrário de `clients`/`subscriptions`, que usam `DEFAULT NOW()`: `Project`, `Role` e `Route` só preenchem `updatedAt` na primeira alteração, e um default reescreveria essa semântica.

**Verificado em 30/08/2026:** as oito migrations aplicam em sequência num Postgres 16 limpo; o seed dos três `.txt` entra íntegro (3 projetos, 8 cargos, 10 rotas) e 16 casos de constraint se comportam como o domínio.

---

## ADR-008: projetos irmãos no mesmo repositório, não multi-módulo Maven

**Status:** aceito na etapa 1 da disciplina de microsserviços; **revisado na etapa 2 (28/09/2026)**
— a aplicação principal saiu da raiz para `permission-service/`. A versão original e o motivo da revisão estão no
fim deste ADR.

**Contexto:** a disciplina exige extrair o `audit` para uma aplicação Spring Boot independente e,
depois, acrescentar um Config Server. Passam a existir três `pom.xml` onde havia um. O caminho
idiomático do Maven seria converter a raiz em um `pom` agregador (`<packaging>pom</packaging>`) com
`permission-service/`, `audit-service/` e `config-server/` como módulos.

Há uma restrição que vem de fora do Maven: este repositório é a evidência avaliada de três
disciplinas da Pós-Graduação, e as tags `etapa-1` … `etapa-4` apontam para o código como ele foi
entregue em 31/08/2026.

**Decisão:** um único repositório, com **cada aplicação em uma pasta irmã** e a raiz reservada para
o que é de todas elas — orquestração e documentação. Cada aplicação tem seu próprio `pom.xml`,
`mvnw` e `Dockerfile`; não há `pom` agregador:

```
permission_saas/
├── permission-service/  aplicação principal (monolito modular)
├── audit-service/       serviço extraído
├── config-server/       Spring Cloud Config Server
├── docker-compose.yml   orquestra todos
└── docs/  README.md
```

Cada projeto compila sozinho (`./mvnw` dentro da pasta) e o `docker-compose.yml` da raiz é o que
integra os três.

**Alternativa descartada — `pom` agregador na raiz.** Um agregador só se paga quando há build único
ou código compartilhado entre os módulos, e aqui não há nenhum dos dois (ver "Duplicação
consciente" abaixo). Acrescentaria um `pom.xml` na raiz e a tentação de um módulo `common`, que
voltaria a acoplar os deploys.

**Consequências:**

- **Não há build único.** Compilar tudo é rodar `./mvnw` em cada pasta, ou `docker compose build`.
  Aceitável porque os projetos não compartilham código-fonte — o que é comum entre eles (DTOs de
  contrato, `Mapper`, `GlobalExceptionHandler`) é copiado deliberadamente, não extraído para uma
  biblioteca compartilhada.
- **Duplicação consciente.** Uma lib compartilhada voltaria a acoplar os dois deploys: mudar o
  contrato exigiria versionar e publicar a lib antes de subir qualquer um dos lados. Para dois
  serviços com um contrato pequeno, copiar custa menos do que acoplar.
- O `verifiesModularStructure()` do Spring Modulith continua valendo só para a aplicação principal.
  A fronteira entre ela e o `audit-service` deixa de ser verificada por teste e passa a ser
  verificada pela rede: não há como importar o pacote do outro.
- Se uma disciplina futura precisar de build único, basta acrescentar um `pom` agregador na raiz
  listando as pastas como módulos — o layout já é o de um multi-módulo.
- **Qualquer serviço pode virar repositório próprio depois**, sem perder o histórico:
  `git subtree split --prefix=audit-service` gera um branch só com a história daquela pasta. Um
  repositório por serviço desde já foi descartado porque separaria as tags `etapa-*` e
  `arq-etapa-*` — a evidência das disciplinas — em repositórios diferentes.

**Revisão de 28/09/2026 — a aplicação principal saiu da raiz.** A versão original deste ADR mantinha
a aplicação principal na raiz (`src/`, `pom.xml`, `Dockerfile`) e acrescentava o `audit-service`
como pasta dentro dela. O argumento era que mover `src/` para `permission-service/src/` faria o diff da entrega ser
"dominado por milhares de linhas de arquivo movido". O argumento não se sustenta: o Git registra a
mudança como renomeação (`{src => permission-service/src}/…`, sem linha alterada), e as tags antigas continuam
apontando para o layout antigo, intactas.

Na prática, o layout original teve dois custos que não estavam previstos:

- **Parecia que o serviço fazia parte do monolito.** Com o `audit-service/` dentro da pasta da
  aplicação principal, quem abre o repositório vê um serviço aninhado em outro — o oposto do que a
  etapa 2 quer demonstrar, duas aplicações independentes.
- **A IDE concordava com essa leitura.** A extensão Java do VS Code importava o `pom.xml` da raiz e
  tratava o `audit-service/` como uma pasta do projeto principal, sem classpath próprio. Com a raiz
  sem `pom.xml`, cada pasta é importada como projeto independente.

A mudança foi feita antes da etapa 2 ganhar Feign, Dockerfiles e Config Server, quando só o
`docker-compose.yml` e os comandos do `README.md` precisavam de ajuste.

---

## ADR-009: a consulta da trilha de auditoria desce para o banco, em JPQL

**Status:** aceito na etapa 1 da disciplina de microsserviços. Desde 30/09/2026 as consultas existem só no `audit-service`, copiadas na extração; o monolito as removeu junto com a tabela (ADR-010).

**Contexto:** até aqui, `GET /audit-events` carregava todos os eventos (ou todos os de um projeto)
e o use case filtrava `type` e `onlyDenied` num stream — a regra geral "a porta é burra" da seção
*Adapter Pattern na infrastructure*. Para o `audit` essa regra custa caro: a trilha é *append-only*,
ganha uma linha a cada validação de permissão e nunca é podada. A etapa 1 também pede consultas
Spring Data coerentes com o domínio, e a busca por período era a que faltava.

**Decisão:** a porta ganha dois métodos com os filtros como parâmetros explícitos, e o adapter os
resolve em duas consultas `@Query` (JPQL):

| Porta | Consulta | Filtros |
|---|---|---|
| `search(type, projectId, from, to)` | `JpaAuditEventRepository.search` sobre `AuditEventJpaEntity` | tipo, projeto, período |
| `searchDenied(projectId, from, to)` | `JpaPermissionCheckEventRepository.searchDenied` sobre `PermissionCheckEventJpaEntity` | `granted = false`, projeto, período |

Cada filtro segue o padrão `(:param IS NULL OR campo = :param)`: parâmetro nulo significa "não
filtrar", a mesma convenção dos DTOs de busca. O use case `SearchAuditEventsUseCase` fica só com a
regra de negócio: rejeita período invertido (`InvalidAuditPeriodException`, 400), normaliza o tipo
para maiúsculas e escolhe entre `search` e `searchDenied`.

Três detalhes de implementação que não são óbvios:

- **`searchDenied` tem repositório próprio.** O JPQL navega por classes, não por tabelas: `granted`
  é declarado só na subclasse, então a consulta precisa partir de `PermissionCheckEventJpaEntity`,
  mesmo que as duas classes gravem na mesma tabela (ADR-004, `SINGLE_TABLE`).
- **O discriminador é mapeado como atributo somente leitura** (`eventType`, com
  `insertable = false, updatable = false`), para o filtro de tipo comparar texto. Quem grava a coluna
  continua sendo o `@DiscriminatorValue`. A alternativa `TYPE(e) IN :types` foi descartada porque
  exigiria passar uma coleção de `Class` como parâmetro.
- **As datas levam `cast(:from as OffsetDateTime)` no lado do `IS NULL`.** Para uma data nula, o
  driver do PostgreSQL não informa o tipo do parâmetro — `timestamp` e `timestamptz` são ambos
  candidatos —, e o banco recusa `$5 is null` com *could not determine data type of parameter*.
  `String` e `UUID` não precisam do `cast` porque o driver informa o tipo mesmo com valor nulo. O H2
  dos testes infere o tipo sozinho, então esse erro só aparece no PostgreSQL.

**Alternativas descartadas:**

- **Consultas derivadas, uma por combinação** (`findByGrantedFalseAndOccurredAtBetween…`): cinco
  filtros opcionais geram dezenas de combinações, e cada uma viraria um método.
- **`Specification` (Criteria API):** é o caminho idiomático para filtros realmente dinâmicos, mas
  com cinco filtros fixos o JPQL fica legível numa tela e não exige montar predicados em código.
  Continua sendo a saída se os filtros crescerem.

**Consequências:**

- O adapter JPA e um eventual adapter em memória deixam de ter o mesmo custo de implementação:
  filtrar passou a ser responsabilidade do adapter. Aceitável porque o `audit` não tem adapter em
  memória desde o ADR-005.
- O índice parcial `idx_audit_events_denied` (`occurred_at DESC WHERE granted = FALSE`), criado na
  `V8`, passa a ser usado de fato — antes existia, mas a filtragem acontecia no Java.
- **Sem teste automatizado das consultas por enquanto.** Foram verificadas contra o PostgreSQL real
  com a aplicação no ar (sem filtro, período, tipo em minúsculas, `onlyDenied` com
  `PROJECT_LIFECYCLE` e período invertido). O teste de integração ficou para o `audit-service`, que é
  onde esse código passa a morar na etapa 2.

---

## ADR-010: a persistência do `audit` sai do monolito

**Status:** aceito em 30/09/2026, na etapa 2 da disciplina de microsserviços (migration `V10`).

**Contexto:** com a gravação e a consulta da trilha passando pelo `audit-service`, a tabela
`audit_events` do banco principal parou de receber eventos e de ser lida. O código que a servia virou
código morto:
- as entidades JPA da herança `SINGLE_TABLE`;
- os repositórios JPQL do ADR-009 e o adapter;
- as portas `AuditEventRepository` e `AuditEventJournal`;
- o `AuditEventFileWriter` e o `AuditDemoRunner`.

**Decisão:** apagar esse código e a tabela. É o último passo do Strangler Fig para o `audit`. Enquanto o
caminho antigo existir, não fica claro qual dos dois vale, e quem mexer depois pode religá-lo sem
perceber. No monolito fica só o necessário para falar com o serviço:
- o `AuditLogListener`;
- a consulta (`SearchAuditEventsUseCase` e o controller);
- a porta `AuditTrail`, com o modelo de leitura `AuditTrailEntry`;
- o cliente OpenFeign.

- A migration é nova (`V10__drop_audit_events_table.sql`). A `V8` não é editada nem apagada: o Flyway
  guarda o checksum de cada migration aplicada e acusaria a diferença em todo banco que já a rodou.
- `AuditEvent` e `PermissionCheckEvent` ficam, porque são o evento que o listener monta e a porta
  recebe. `ProjectLifecycleEvent` e `LifecycleAction` saem do monolito, porque só o `AuditDemoRunner`
  os criava. Continuam no `audit-service`.

**Os eventos antigos não foram copiados para o `audit_db`.** O banco principal só tinha dados de
desenvolvimento: 6 validações de teste na máquina do autor. Num sistema em produção, a ordem seria
outra: primeiro copiar as linhas para o banco do serviço, com um job de migração (o Spring Batch da
etapa 4 serve para isso), e só depois rodar o `DROP`.

**O que a disciplina de Spring Boot entregou continua acessível.** A herança `SINGLE_TABLE`, o
`AuditDemoRunner` e o arquivo texto foram entregas daquela disciplina. A evidência dela é a tag
`etapa-4`, que continua apontando para o código como foi entregue. A herança JPA e o arquivo texto
continuam funcionando no `audit-service`, para onde foram copiados na extração.

**Consequências:**

- O `permission-service` não guarda dado de auditoria nenhum. Toda consulta depende do `audit-service`
  no ar e responde `503` quando ele não está.
- O índice parcial do ADR-009 e os `CHECK` do ADR-004 passam a existir só no `audit_db`.
- O `docs/DER.pdf` ainda mostra `audit_events` no banco principal; precisa ser corrigido quando o
  diagrama for refeito.

---

## Segurança do Swagger UI

`SecurityConfig` deixa todo o restante da API com `permitAll()` (autenticação real de cliente é trabalho futuro, ver `docs/clean_code_e_padroes_de_projeto/PLAN.md`), mas `/swagger-ui/**` e `/v3/api-docs/**` exigem HTTP Basic com um usuário fixo em memória (`InMemoryUserDetailsManager`), configurado via `app.swagger.username` / `app.swagger.password` (env vars `SWAGGER_USERNAME` / `SWAGGER_PASSWORD`, default `admin` / `admin123`). `/actuator/**` continua liberado.

**Por quê:** a documentação interativa expõe todos os endpoints e facilita descoberta/abuso se ficar pública; como login/JWT de cliente está fora de escopo desta entrega, HTTP Basic com um usuário fixo é a menor solução que já impede acesso não autenticado ao Swagger sem implementar um fluxo de autenticação completo.

O grupo `public` do `SwaggerConfig` (`GroupedOpenApi`) é só rotulagem de agrupamento do OpenAPI — não tem relação com controle de acesso, que é feito inteiramente pelo `SecurityFilterChain`.

---

## Estrutura de pacotes

```
permission-service/src/main/java/com/saas/permissions/
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
│   │   ├── route/         # Route.java + exception/RouteAlreadyExistsException, RouteNotFoundException
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
│   │   ├── route/         # AddRouteToProjectUseCase, FindProjectRoutesUseCase
│   │   │   └── command/   # AddRouteToProjectCommand
│   │   └── roleroute/     # GrantRouteToRoleUseCase, RevokeRouteFromRoleUseCase,
│   │                      # FindRolePermissionsUseCase
│   ├── infrastructure/
│   │   ├── project/       # ProjectJpaEntity (@OneToMany roles/routes), ProjectJpaMapper,
│   │   │                  # JpaProjectRepository, ProjectRepositoryAdapter, ProjectDemoRunner
│   │   ├── role/          # RoleJpaEntity (@ManyToOne project, @OneToMany permissions), RoleJpaMapper
│   │   ├── route/         # RouteJpaEntity (@ManyToOne project, @OneToMany permissions inverso),
│   │   │                  # RouteJpaMapper
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
│       │   ├── dto/       # AddRouteRequest, RouteResponse
│       │   └── mapper/    # AddRouteToProjectMapper, RouteResponseMapper
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
    │                      # enviado), AuditTrail.java (porta de gravação e consulta no
    │                      # audit-service), AuditTrailEntry.java (modelo de leitura)
    │   └── exception/     # InvalidAuditPeriodException.java, AuditTrailUnavailableException.java
    ├── application/       # AuditLogListener.java (Observer), SearchAuditEventsUseCase.java
    │   └── query/         # SearchAuditEventsQuery.java
    ├── infrastructure/
    │   └── client/        # AuditClient.java (@FeignClient), AuditTrailClientAdapter.java
    │       └── dto/       # RegisterPermissionCheckRequest.java, AuditEventResponse.java (contrato)
    └── api/               # AuditEventController.java
        ├── dto/           # SearchAuditEventsRequest.java, AuditEventResponse.java
        └── mapper/        # SearchAuditEventsMapper.java, AuditEventResponseMapper.java
```

Todos os módulos de negócio são subpacotes diretos de `com.saas.permissions` (ex: `com.saas.permissions.identity`), assim como `shared`. Essa é a estrutura exigida pela detecção automática de módulos do Spring Modulith, que considera cada subpacote direto do pacote da classe `@SpringBootApplication` como um Application Module.
