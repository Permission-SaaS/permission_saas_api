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
  fora de escopo por falta de tempo, ver docs/clean_code_e_padroes_de_projeto/PLAN.md

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
  Fora de escopo por falta de tempo, ver docs/clean_code_e_padroes_de_projeto/PLAN.md
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
3. Qualquer sistema externo chama `POST /validate-permission` com a ApiKey + cargo + rota (`permission`) — a ApiKey é validada de verdade contra `billing`; cargo/rota são aceitos pelo contrato mas ainda não checados contra um projeto real, porque `project` não existe nesta entrega (ver `docs/clean_code_e_padroes_de_projeto/PLAN.md`)
4. ~~Cria um projeto com cargos e rotas dentro do limite do plano~~ e ~~o resultado é gravado em log de auditoria~~ — ambos fora de escopo por falta de tempo, ver "trabalho futuro" em `docs/clean_code_e_padroes_de_projeto/PLAN.md`

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
   | `PlanLimitExceededException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.addRole` — recebe o nome do projeto e o `maxRoles` |
   | `RoleAlreadyExistsException` | `project/domain/role/exception/` | `BusinessRuleException` | `Project.addRole` — recebe o nome do cargo |
   | `RouteAlreadyExistsException` | `project/domain/route/exception/` | `BusinessRuleException` | `Project.addRoute` — recebe o `httpMethod` e o `path` |
   | `ProjectAlreadyInactiveException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.deactivate` — recebe o nome do projeto |
   | `ProjectAlreadyActiveException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.activate` — recebe o nome do projeto |
   | `ProjectAlreadyDeletedException` | `project/domain/project/exception/` | `BusinessRuleException` | `Project.delete` — recebe o nome do projeto |

   **Por que não `IllegalStateException`:** transição de estado inválida é regra de negócio, não erro de programação. Uma `IllegalStateException` cai no handler genérico e vira **500** — o cliente recebe "erro interno" quando na verdade fez um pedido inválido. Herdando `BusinessRuleException`, a mesma situação vira **409** com mensagem legível, sem tocar no `GlobalExceptionHandler`.

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

> ⚠️ **Exceção temporária:** `Project`, `Role` e `Route` violam este ADR de propósito enquanto a persistência é in-memory (etapas 1-3 da disciplina de Spring Boot). Ver ADR-003 para o motivo e o checklist de reversão obrigatório na etapa 4.

---

## ADR-003: `Project`/`Role`/`Route` geram o próprio `id` — desvio temporário do ADR-001

**Status:** aceito para as etapas 1-3 da disciplina de Spring Boot. **Deve ser revertido na etapa 4**, quando entrar o JPA.

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

## Segurança do Swagger UI

`SecurityConfig` deixa todo o restante da API com `permitAll()` (autenticação real de cliente é trabalho futuro, ver `docs/clean_code_e_padroes_de_projeto/PLAN.md`), mas `/swagger-ui/**` e `/v3/api-docs/**` exigem HTTP Basic com um usuário fixo em memória (`InMemoryUserDetailsManager`), configurado via `app.swagger.username` / `app.swagger.password` (env vars `SWAGGER_USERNAME` / `SWAGGER_PASSWORD`, default `admin` / `admin123`). `/actuator/**` continua liberado.

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
├── project/               # em construção na disciplina de Spring Boot — ver docs/desenvolvimento_de_aplicacoes_java_com_spring_boot/PLAN.md
│   ├── domain/            # dividido em submódulos project/, role/, route/
│   │   ├── project/       # Project.java
│   │   ├── role/          # Role.java
│   │   └── route/         # Route.java
│   ├── application/       # CreateProjectUseCase.java (planejado)
│   └── api/               # ProjectController.java (planejado)
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
└── audit/                 # em construção na disciplina de Spring Boot — ver docs/desenvolvimento_de_aplicacoes_java_com_spring_boot/PLAN.md
    ├── domain/            # AuditEvent.java (abstrata), PermissionCheckEvent.java,
    │                      # ProjectLifecycleEvent.java, LifecycleAction.java
    ├── application/       # AuditLogListener.java (planejado)
    └── infrastructure/    # AuditEventRepositoryAdapter.java (planejado)
```

Todos os módulos de negócio são subpacotes diretos de `com.saas.permissions` (ex: `com.saas.permissions.identity`), assim como `shared`. Essa é a estrutura exigida pela detecção automática de módulos do Spring Modulith, que considera cada subpacote direto do pacote da classe `@SpringBootApplication` como um Application Module.
