# Padrões de Projeto — Permission SaaS

Este arquivo documenta cada padrão de projeto (GoF) usado no sistema: onde vive, por que foi escolhido, e como estender.

**Status atual:** só os módulos `identity` e `shared` têm código implementado. Os padrões dos módulos `billing`, `project`, `permission` e `audit` estão **planejados** conforme a tabela do `CLAUDE.md`, mas ainda não escritos — estão marcados como tal abaixo.

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

---

## Planejados (ainda não implementados)

| Pattern | Local previsto | Por que será usado |
|---|---|---|
| Factory Method | `billing/infrastructure/ApiKeyFactory` | Centralizar a estratégia de criação da API Key, escondendo o algoritmo de geração do restante do módulo `billing` |
| Builder | `project/domain/ProjectBuilder` | Montar `Project` com `Roles` e `Routes` passo a passo, evitando um construtor com muitos parâmetros opcionais |
| Adapter | `billing/infrastructure/FakePaymentGatewayAdapter` | Adaptar o gateway de pagamento simulado à porta `PaymentGateway`, permitindo trocar por um gateway real no futuro sem tocar no `SubscribeToPlanUseCase` |
| Chain of Responsibility | `permission/domain/` — `ApiKeyValidationHandler`, `TokenValidationHandler`, `RoleRouteValidationHandler` | Cada handler valida uma preocupação (API key, token, role/rota) e repassa adiante; permite adicionar uma nova validação sem tocar nas existentes (OCP) |
| Observer | `audit/application/AuditLogListener` | Desacoplar o registro de auditoria da lógica de validação de permissão — `permission` dispara o evento, `audit` reage |

Ao implementar cada um, mover a entrada correspondente para a seção "Implementados" acima com local real, motivo e como estender — igual ao padrão dos dois primeiros.
