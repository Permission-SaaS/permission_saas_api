# Padrões de Projeto — Permission SaaS

Este arquivo documenta cada padrão de projeto (GoF) usado no sistema: onde vive, por que foi escolhido, e como estender.

**Status atual:** `identity`, `shared`, `billing` (Factory Method `ApiKeyFactory`, Adapter `FakePaymentGatewayAdapter`, Command+Mapper de `SubscribeToPlanUseCase`) e `permission` (Chain of Responsibility) vieram da disciplina de Clean Code. O **Observer** (`audit/application/AuditLogListener`) foi implementado na disciplina de Spring Boot e está descrito abaixo. O **Builder** de `project` segue planejado — o agregado é montado por `Project.builder()` do Lombok mais `addRole`/`addRoute`, que já resolvem o caso sem uma classe de builder própria.

---

## Implementados

### Adapter — porta de repositório (`identity`)

- **Onde:** `identity/domain/ClientRepository.java` (interface/porta) + `identity/infrastructure/ClientRepositoryAdapter.java` (implementação) + `identity/infrastructure/JpaClientRepository.java` (`extends JpaRepository`)
- **Por que:** o `RegisterClientUseCase` depende só da interface `ClientRepository`, nunca do Spring Data JPA diretamente. Isso mantém a camada `domain` livre de qualquer dependência de framework (regra de ouro da arquitetura, ver `ARCHITECTURE.md`).
- **Como estender:** para trocar de banco ou adicionar cache, crie um novo `infrastructure/XxxRepositoryAdapter` que implemente `ClientRepository` — o use case não muda.

### Command Object (estilo CQRS) + Mapper — não é o GoF Command

- **Onde:** `identity/application/command/RegisterClientCommand.java` (o "Command"), `identity/api/mapper/RegisterClientMapper.java` e `ClientResponseMapper.java` (conversão), `shared/domain/Mapper.java` (interface genérica `Mapper<I, O>`)
- **Fluxo:**
  ```
  RegisterClientRequest (api/dto)
          ↓ RegisterClientMapper.map()
  RegisterClientCommand (application/command)
          ↓ RegisterClientUseCase.execute()
  Client (domain)
          ↓ ClientResponseMapper.map()
  ClientResponse (api/dto)
  ```
- **Por que NÃO é o GoF Command pattern:** o GoF Command exige um objeto que sabe **se executar** (método `execute()` no próprio objeto), com um Invoker que dispara a chamada e um Receiver que a lógica de fato invoca — pensado para viabilizar fila de execução, undo/redo e log de comandos. Aqui, `RegisterClientCommand` é passivo: só carrega dados. Quem executa é `RegisterClientUseCase.execute(command)`, de fora. Ou seja, é um **Parameter Object** (agrupa parâmetros relacionados num único objeto) batizado de "Command" na convenção **CQRS** (Command = intenção de escrita; Query = intenção de leitura), não o padrão estrutural do GoF.
- **Por que essa escolha e não o GoF Command "de verdade":** o escopo do projeto (ver `CLAUDE.md` → Scope boundaries) não tem requisito de undo/redo, fila de comandos ou execução assíncrona desacoplada do chamador. Introduzir Invoker/Receiver acrescentaria indireção sem ganho — contra a diretriz de não adicionar abstração além do necessário.
- **Convenção de nomenclatura ao criar novos objetos de entrada:**
  - Operação **muda** o estado do domínio (cria/atualiza/deleta) → sufixo `Command` (ex.: `SubscribeToPlanCommand`, `CreateProjectCommand`)
  - Operação só **lê**, sem alterar nada → sufixo `Query` (ex.: `FindClientByEmailQuery`, `ListPlansQuery`)
  - Efeitos colaterais automáticos do sistema (como o log de auditoria disparado via Observer) não contam para essa decisão — o que importa é a intenção de quem chama a operação, não o que acontece nos bastidores.
- **Como estender:** para um novo endpoint, crie `<Nome>Request` (dto) → `<Nome>Mapper implements Mapper<Request, Command>` → `<Nome>Command` (application/command) → `<Nome>UseCase.execute(command)`. Não escreva conversão de campos dentro do controller.
- **Segundo exemplo (`billing/subscription`):** `SubscribeToPlanRequest` (`api/subscription/dto`) → `SubscribeToPlanMapper` (`api/subscription/mapper`) → `SubscribeToPlanCommand` (`application/subscription/command`) → `SubscribeToPlanUseCase.execute()` (`application/subscription`) → `SubscriptionResult` (`domain/subscription/dto`, agrupa `Subscription` + `ApiKey`) → `SubscriptionResponseMapper` → `SubscriptionResponse` (`api/subscription/dto`). Mesmo fluxo do `identity`, confirmando que o padrão é reutilizável entre módulos sem alterar `Mapper<I, O>`.
- **`billing` tem dois agregados (`Plan` e `Subscription`), cada camada é subdividida em `plan/` e `subscription/`** — ver `ARCHITECTURE.md` → "Submódulos dentro de um módulo". `ApiKey` e `PaymentGateway` ficam em `domain/subscription/` porque só existem dentro do fluxo de `SubscribeToPlanUseCase`, mesmo sem levar "Subscription" no nome.
- **Terceiro exemplo, só leitura (`billing/plan`):** `GET /plans/{id}` não usa Command (é uma Query, sem mudança de estado — ver convenção de nomenclatura acima): `PlanController` → `FindPlanByIdUseCase.execute(UUID)` (`application/plan`) → `Plan` (`domain/plan`) → `PlanResponseMapper` (`api/plan/mapper`) → `PlanResponse` (`api/plan/dto`). Sem Mapper de entrada porque não há DTO de request além do `@PathVariable`.

### Factory Method — criação da API Key (`billing`)

- **Onde:** `billing/infrastructure/subscription/ApiKeyFactory.java` (Creator abstrato) + `billing/infrastructure/subscription/UuidApiKeyFactory.java` (ConcreteCreator) + `billing/domain/subscription/ApiKey.java` (Product)
- **Estrutura GoF aplicada:**
  - `ApiKeyFactory.create(UUID)` é a operação **fixa**: sempre chama o factory method para obter o produto, e sempre aplica hash (`PasswordEncoder`) + `createdAt` sobre ele — isso não varia entre implementações.
  - `ApiKeyFactory.newApiKey(UUID)` é o **factory method** propriamente dito (`protected abstract`, devolve o Product `ApiKey`) — é a decisão que fica com a subclasse.
  - `UuidApiKeyFactory` é o único ConcreteCreator hoje: decide o formato da chave em texto puro (UUID prefixado com `sk_`).
- **Por que a classe base é `abstract` e não `@Component`:** se a base fosse uma classe concreta anotada `@Component` e ao mesmo tempo existisse uma subclasse também `@Component`, o Spring registraria dois beans candidatos para o tipo `ApiKeyFactory`, quebrando qualquer injeção com `NoUniqueBeanDefinitionException`. Como classes abstratas são automaticamente ignoradas no component scan, só o ConcreteCreator (`UuidApiKeyFactory`) vira bean, e quem consome injeta o tipo `ApiKeyFactory` sem saber qual implementação recebeu.
- **Por que só existe um ConcreteCreator:** o formato alternativo mais óbvio seria JWT, mas "JWT authentication" está explicitamente fora de escopo no `CLAUDE.md` (Scope boundaries). Além disso, o domínio já modela `ApiKey.active`/`revokedAt` como revogação via consulta ao banco — um modelo incompatível com token JWT autocontido (que não pode ser revogado sem manter uma blacklist, o que anularia a vantagem stateless do JWT). Por isso, o Factory Method aqui existe para permitir uma futura variação **dentro do mesmo modelo opaco+hash** (ex.: trocar o gerador aleatório por um mais forte), não para acomodar JWT.
- **Como estender:** criar uma nova subclasse de `ApiKeyFactory`, sobrescrever `newApiKey(UUID)` devolvendo um `ApiKey` com o novo formato de `plainKey`, e anotá-la `@Component` (com `@Primary`/`@Qualifier` se coexistir com `UuidApiKeyFactory`). `create(UUID)` não muda.
- **Quem consome:** `SubscribeToPlanUseCase` injeta o tipo abstrato `ApiKeyFactory` e chama `create(subscription.getId())` só depois que a `Subscription` foi ativada (ver invariante em `docs/DOMAIN.md`).

### Adapter — gateway de pagamento (`billing`)

- **Onde:** `billing/domain/subscription/PaymentGateway.java` (Target) + `billing/domain/subscription/dto/PaymentRequest.java` / `PaymentResult.java` (VOs do Target) + `billing/infrastructure/subscription/ExternalPaymentGatewaySimulator.java` (Adaptee) + `billing/infrastructure/subscription/FakePaymentGatewayAdapter.java` (Adapter)
- **Estrutura GoF aplicada:**
  - **Target:** `PaymentGateway.process(PaymentRequest)` — único método (ISP), é só isso que `SubscribeToPlanUseCase` vai conhecer.
  - **Adaptee:** `ExternalPaymentGatewaySimulator.charge(long amountInCents, String reference)` — simula uma lib de terceiro com formato incompatível de propósito (valor em centavos, retorno `LegacyChargeResponse(statusCode, legacyTransactionRef)` com vocabulário próprio).
  - **Adapter:** `FakePaymentGatewayAdapter` implementa `PaymentGateway` e por dentro converte `PaymentRequest` → `(amountInCents, reference)`, chama o Adaptee, e traduz `LegacyChargeResponse` de volta para `PaymentResult` (`statusCode == "OK"` → `approved = true`).
- **Por que:** `SubscribeToPlanUseCase` depende só de `PaymentGateway` — trocar pelo gateway real no futuro não toca no use case (DIP/OCP).
- **Como estender:** para um provedor real, criar outro `infrastructure/XxxPaymentGatewayAdapter implements PaymentGateway` que converse com o SDK real e trocar o bean via `@Primary`/`@Qualifier` — `PaymentGateway` e `SubscribeToPlanUseCase` não mudam.
- **Wiring do Adaptee:** `ExternalPaymentGatewaySimulator` **não** é `@Component` de propósito — ele simula uma lib de terceiro, e libs de terceiro não usam o component scan da aplicação. O bean é registrado manualmente em `billing/infrastructure/subscription/BillingConfig` (`@Configuration` + `@Bean`), que é o equivalente a como se declararia o bean de um SDK real (ex.: `StripeClient`) na configuração do Spring.

### Chain of Responsibility — validação de permissão (`permission`)

- **Onde:** `permission/domain/PermissionValidationHandler.java` (Handler abstrato) + três ConcreteHandlers no mesmo pacote — `ApiKeyValidationHandler`, `TokenValidationHandler`, `RoleRouteValidationHandler` — + `permission/domain/dto/PermissionCheckRequest.java` / `PermissionCheckResult.java` (VOs que trafegam pela chain) + `permission/application/ValidatePermissionUseCase.java` (monta e dispara a chain) + `permission/api/PermissionController.java` (`POST /validate-permission`).
- **Estrutura GoF aplicada:**
  - **Handler:** `PermissionValidationHandler.handle(PermissionCheckRequest)` é o método **fixo** (`final`) — chama `check()` (que cada subclasse implementa) e só repassa para o próximo handler (`linkWith`) se o atual concedeu (`result.granted()`); se algum handler nega, a chain para ali e devolve o motivo (`reason`), sem chamar os handlers seguintes.
  - **ConcreteHandler 1 — `ApiKeyValidationHandler`:** só sabe validar a ApiKey, via porta `permission/domain/ApiKeyValidator` (implementada por `permission/infrastructure/BillingApiKeyValidator`, que chama `billing.FindActiveApiKeyByPlainKeyUseCase` — comunicação entre módulos via use case, nunca via repositório de outro módulo direto, ver `ARCHITECTURE.md`).
  - **ConcreteHandler 2 — `TokenValidationHandler`** e **ConcreteHandler 3 — `RoleRouteValidationHandler`:** sempre concedem (`PermissionCheckResult.allow()`) nesta entrega. Existem estruturalmente na chain (provando que um novo handler se pluga sem alterar os demais — OCP), mas sua regra de negócio real depende do módulo `project` (Role/Route) e de um segundo fator de autenticação, nenhum dos dois implementado por falta de tempo. Isso está documentado como decisão consciente, não como bug — ver comentário Javadoc em cada classe e `docs/clean_code_e_padroes_de_projeto/PLAN.md`, "trabalho futuro".
  - **Client:** `ValidatePermissionUseCase` monta a chain uma vez no construtor (`apiKeyValidationHandler.linkWith(tokenValidationHandler).linkWith(roleRouteValidationHandler)`) e sempre entra pelo primeiro handler (`chain.handle(request)`).
- **Por que Chain of Responsibility e não uma sequência de `if`s no use case:** cada handler não sabe quantos handlers vêm depois dele nem o que eles fazem — só sabe repassar. Adicionar uma quarta validação (ex.: rate limiting) é criar uma quarta classe e religar a chain no construtor do use case; nenhum handler existente muda (OCP). Um método único com vários `if` aninhados violaria isso — qualquer nova regra exigiria editar a mesma função.
- **Como estender:** criar uma nova subclasse de `PermissionValidationHandler`, implementar `check()`, anotar `@Component`, injetar no construtor de `ValidatePermissionUseCase` e incluir no `linkWith(...)` — na ordem que fizer sentido (ex.: handler mais barato primeiro, para short-circuit cedo).

### Observer — trilha de auditoria (`permission` → `audit`)

- **Onde:** `permission/domain/event/PermissionValidatedEvent.java` (o evento) + `permission/application/ValidatePermissionUseCase.java` (Subject, publica) + `audit/application/AuditLogListener.java` (Observer, consome) + `audit/domain/AuditEventRepository.java` (porta) + `audit/infrastructure/InMemoryAuditEventRepository.java` (adapter das etapas 2-3).
- **Estrutura aplicada:**
  - **Subject:** `ValidatePermissionUseCase` mede a duração da chain e publica `PermissionValidatedEvent` via `ApplicationEventPublisher`. Não conhece nenhum observador — nem sabe que o módulo `audit` existe.
  - **Observer:** `AuditLogListener` reage ao evento, converte em `PermissionCheckEvent` (subclasse de `AuditEvent`) e grava pela porta.
- **Por que evento e não chamada direta:** auditoria é efeito colateral, não parte da decisão de permitir ou negar. Uma chamada direta faria `permission` depender de `audit` e quebraria a regra de comunicação entre módulos (`ARCHITECTURE.md`); com evento, acrescentar um segundo consumidor (métricas, alerta de tentativas negadas) é criar outra classe com `@EventListener`, sem tocar em `permission`.
- **O evento não carrega a ApiKey**, de propósito: ele atravessa a fronteira do módulo, e a chave é credencial, não dado de auditoria.
- **`@NamedInterface("events")`:** `permission/domain/event/package-info.java` é anotado para que o pacote seja visível a outros módulos — sem isso, o import feito por `audit` quebraria `ApplicationModulesIntegrationTests.verifiesModularStructure()`.
- **Por que `@EventListener` e não `@ApplicationModuleListener`:** o `@ApplicationModuleListener` do Spring Modulith implica `@TransactionalEventListener(AFTER_COMMIT)`, e a validação de permissão ainda não roda dentro de uma transação — o evento simplesmente não chegaria ao ouvinte. A troca está prevista para a etapa 4, quando a persistência JPA tornar o fluxo transacional; é também o caminho para o processamento assíncrono citado como trabalho futuro.
- **A porta `AuditEventRepository` só registra e lê.** Não tem `update` nem `delete`: um evento de auditoria nunca é alterado nem removido, e é isso que o torna prova do que aconteceu. A restrição está no tipo, não na disciplina de quem chama.
- **Quem atribui o `id` do evento é o adapter, não o domínio** (`InMemoryAuditEventRepository.save()`), seguindo o ADR-001. Na etapa 4 quem gera passa a ser o banco e o domínio não muda.
- **Como estender:** criar outra classe `@Component` com um método `@EventListener` recebendo `PermissionValidatedEvent`. Para auditar um novo tipo de acontecimento, criar uma subclasse de `AuditEvent` implementando `describe()`/`type()` — o `AuditEventResponseMapper` e o `GET /audit-events` não mudam, porque leem só o contrato da classe abstrata.

---

## Planejados (ainda não implementados)

| Pattern | Local previsto | Por que será usado |
|---|---|---|
| Builder | `project/domain/ProjectBuilder` | Montar `Project` com `Roles` e `Routes` passo a passo. Hoje o `@Builder` do Lombok mais `addRole`/`addRoute` cobrem o caso, e cada `add` roda os invariantes do agregado — uma classe de builder própria só se justifica se surgir uma montagem em vários passos que precise validar o conjunto no final |

Ao implementar cada um, mover a entrada correspondente para a seção "Implementados" acima com local real, motivo e como estender — igual ao padrão dos demais.
