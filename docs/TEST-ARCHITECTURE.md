# Arquitetura de Testes — Permission SaaS

## Por que testar? A regra de ouro

Testes não são opcionais — eles são a única forma de saber se o código que escrevemos faz o que achamos que faz. Um código sem teste é um código que "parece funcionar hoje". Com o tempo, à medida que o projeto cresce, uma mudança em um lugar quebra outra coisa que ninguém sabia que dependia dali.

O objetivo desta arquitetura é garantir que cada integrante saiba:

- **O que** testar em cada camada
- **Como** escrever o teste (com exemplos prontos para copiar)
- **Onde** colocar o arquivo de teste
- **Quando** rodar os testes

---

## A Pirâmide de Testes

```
          /\
         /  \
        / E2E\         ← smoke test completo (1 por módulo)
       /------\
      /  Slice \       ← testes de controller HTTP (@WebMvcTest)
     /----------\
    / Integration\     ← testes de repositório com banco real
   /--------------\
  /   Unit Tests   \   ← testes de domínio e use case (maioria!)
 /------------------\
```

**Regra prática:** quanto mais baixo na pirâmide, mais testes você escreve. Testes unitários são rápidos e baratos; testes de integração são lentos e custosos — use com moderação e propósito.

| Tipo               | Velocidade    | Spring Context | Banco          | Quantidade      |
| ------------------ | ------------- | -------------- | -------------- | --------------- |
| Unit               | Milissegundos | Não           | Não           | Muitos          |
| Integration (JPA)  | Segundos      | Parcial        | H2 em memória | Poucos          |
| Slice (Controller) | Segundos      | Parcial        | Não           | Um por endpoint |
| Smoke (SpringBoot) | 10–30s       | Completo       | H2 em memória | Um por módulo  |

---

## Estrutura de Arquivos de Teste

Os testes espelham exatamente a estrutura do código principal. Para cada arquivo em `src/main/java`, existe um arquivo de teste correspondente em `src/test/java`.

```
src/test/java/com/saas/permissions/
│
├── modules/
│   ├── identity/
│   │   ├── domain/
│   │   │   └── ClientTest.java                    ← testa regras do entity
│   │   ├── application/
│   │   │   └── RegisterClientUseCaseTest.java     ← testa o use case (unit, com mock)
│   │   ├── infrastructure/
│   │   │   └── ClientRepositoryAdapterIT.java     ← testa o repositório (integration)
│   │   └── api/
│   │       └── ClientControllerTest.java          ← testa o controller HTTP (slice)
│   │
│   ├── billing/
│   │   ├── domain/
│   │   ├── application/
│   │   │   └── SubscribeToPlanUseCaseTest.java
│   │   └── infrastructure/
│   │       └── FakePaymentGatewayAdapterTest.java
│   │
│   ├── project/
│   │   └── application/
│   │       └── CreateProjectUseCaseTest.java
│   │
│   └── permission/
│       ├── domain/
│       │   ├── ApiKeyValidationHandlerTest.java   ← testa cada elo da chain
│       │   ├── TokenValidationHandlerTest.java
│       │   └── RoleRouteValidationHandlerTest.java
│       └── application/
│           └── ValidatePermissionUseCaseTest.java
│
└── PermissionSaasApplicationTests.java        ← smoke test geral (já existe)
```

**Convenção de nomes:**

- `*Test.java` → teste unitário ou slice (sem banco de dados real)
- `*IT.java` → teste de integração (acessa banco H2)

---

## Configuração do Ambiente de Testes

### Perfil `test` (src/test/resources/application-test.yml)

Este arquivo já existe em `src/test/resources/application-test.yml` com o seguinte conteúdo:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false   # Flyway usa SQL específico do Postgres; JPA recria o schema
```

> **Por quê desabilitar o Flyway nos testes?** O Flyway usa SQL específico do PostgreSQL. No H2 (banco em memória dos testes), esse SQL pode falhar. Com `ddl-auto: create-drop`, o Spring cria e destrói o schema automaticamente a partir das entidades JPA.

---

## Tipo 1 — Teste Unitário de Use Case

É o tipo mais importante e o que o time deve dominar primeiro.

**Objetivo:** verificar se a **regra de negócio** está correta, sem subir banco, servidor web ou Spring.

**Ferramenta:** JUnit 5 + Mockito (já incluídos via `spring-boot-starter-test`).

**Como funciona o Mockito:** em vez de usar o repositório real (que precisaria de banco), criamos um "dublê" (`mock`) que simula o comportamento. Você define o que o mock deve retornar para cada chamada.

### Exemplo: RegisterClientUseCaseTest

```java
// src/test/java/com/saas/permissions/modules/identity/application/RegisterClientUseCaseTest.java
package com.saas.permissions.modules.identity.application;

import com.saas.permissions.modules.identity.application.command.RegisterClientCommand;
import com.saas.permissions.modules.identity.domain.AuthProvider;
import com.saas.permissions.modules.identity.domain.Client;
import com.saas.permissions.modules.identity.domain.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)          // habilita o Mockito sem subir Spring
@DisplayName("RegisterClientUseCase")
class RegisterClientUseCaseTest {

    @Mock
    private ClientRepository clientRepository;   // dublê — não toca o banco

    @Mock
    private PasswordEncoder passwordEncoder;     // dublê — não chama BCrypt de verdade

    @InjectMocks
    private RegisterClientUseCase useCase;       // o que estamos testando

    // ── helpers ──────────────────────────────────────────────────────────────

    private RegisterClientCommand validCommand() {
        return new RegisterClientCommand(
            "Ana Silva", "ana@email.com", "11999990000",
            "senha123", null, null
        );
    }

    // ── testes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve registrar cliente com sucesso quando email não existe")
    void shouldRegisterClientSuccessfully() {
        // ARRANGE — configure o que os mocks devem retornar
        when(clientRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash_da_senha");
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        // ACT — execute o método que queremos testar
        Client result = useCase.execute(validCommand());

        // ASSERT — verifique o resultado
        assertThat(result.getName()).isEqualTo("Ana Silva");
        assertThat(result.getEmail()).isEqualTo("ana@email.com");
        assertThat(result.getPasswordHash()).isEqualTo("hash_da_senha");
        assertThat(result.getProvider()).isEqualTo(AuthProvider.local);
        verify(clientRepository).save(any(Client.class));   // confirma que save() foi chamado
    }

    @Test
    @DisplayName("deve lançar exceção quando email já está em uso")
    void shouldThrowWhenEmailAlreadyExists() {
        // ARRANGE
        when(clientRepository.existsByEmail("ana@email.com")).thenReturn(true);

        // ACT + ASSERT — verifica que a exceção correta é lançada
        assertThatThrownBy(() -> useCase.execute(validCommand()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ana@email.com");

        verify(clientRepository, never()).save(any());  // garante que save() NÃO foi chamado
    }

    @Test
    @DisplayName("não deve codificar senha quando provider é OAuth")
    void shouldNotHashPasswordForOAuthProvider() {
        // ARRANGE
        var oauthCommand = new RegisterClientCommand(
            "Ana Silva", "ana@email.com", null,
            null, AuthProvider.google, "google-id-123"
        );
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // ACT
        Client result = useCase.execute(oauthCommand);

        // ASSERT
        assertThat(result.getPasswordHash()).isNull();
        verify(passwordEncoder, never()).encode(any());  // encoder não foi chamado
    }
}
```

### Anatomia de um teste — padrão AAA

Todo teste deve ter exatamente três seções:

```
ARRANGE  → configura o cenário (mocks, dados de entrada)
ACT      → executa a ação que está sendo testada (normalmente 1 linha)
ASSERT   → verifica se o resultado está correto
```

Separe visualmente as seções com comentários `// ARRANGE`, `// ACT`, `// ASSERT`. Isso torna o teste legível para qualquer pessoa do time.

---

## Tipo 2 — Teste Unitário de Domínio

Testa regras que vivem direto nas entidades ou value objects, sem mock algum.

### Exemplo: ClientTest

```java
// src/test/java/com/saas/permissions/modules/identity/domain/ClientTest.java
package com.saas.permissions.modules.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Client")
class ClientTest {

    @Test
    @DisplayName("deve iniciar com status active e emailVerified false")
    void shouldHaveDefaultValues() {
        Client client = Client.builder()
            .name("João")
            .email("joao@email.com")
            .build();

        assertThat(client.getStatus()).isEqualTo(ClientStatus.active);
        assertThat(client.isEmailVerified()).isFalse();
        assertThat(client.isBlocked()).isFalse();
        assertThat(client.getLoginAttempts()).isZero();
    }
}
```

---

## Tipo 3 — Teste de Controller (Slice)

Testa se o endpoint HTTP faz a coisa certa: lê o body, converte para command, chama o use case e retorna o status HTTP correto.

**Ferramenta:** `@WebMvcTest` — sobe apenas a camada web (controller, mappers, filtros). O use case é mockado.

### Exemplo: ClientControllerTest

```java
// src/test/java/com/saas/permissions/modules/identity/api/ClientControllerTest.java
package com.saas.permissions.modules.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.permissions.modules.identity.api.mapper.ClientResponseMapper;
import com.saas.permissions.modules.identity.api.mapper.RegisterClientMapper;
import com.saas.permissions.modules.identity.application.RegisterClientUseCase;
import com.saas.permissions.modules.identity.domain.AuthProvider;
import com.saas.permissions.modules.identity.domain.Client;
import com.saas.permissions.modules.identity.domain.ClientStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@DisplayName("ClientController")
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterClientUseCase registerClientUseCase;

    @MockitoBean
    private RegisterClientMapper registerClientMapper;

    @MockitoBean
    private ClientResponseMapper clientResponseMapper;

    @Test
    @DisplayName("POST /clients/register deve retornar 201 com o cliente criado")
    void shouldReturn201WhenClientRegistered() throws Exception {
        // ARRANGE
        var requestBody = """
            {
                "name": "Ana Silva",
                "email": "ana@email.com",
                "rawPassword": "senha123"
            }
            """;

        var savedClient = Client.builder()
            .id(UUID.randomUUID())
            .name("Ana Silva")
            .email("ana@email.com")
            .status(ClientStatus.active)
            .provider(AuthProvider.local)
            .build();

        when(registerClientMapper.map(any())).thenReturn(null);     // mapper é testado à parte
        when(registerClientUseCase.execute(any())).thenReturn(savedClient);
        when(clientResponseMapper.map(any())).thenReturn(
            new com.saas.permissions.modules.identity.api.dto.ClientResponse(
                savedClient.getId(), "Ana Silva", "ana@email.com", ClientStatus.active
            )
        );

        // ACT + ASSERT
        mockMvc.perform(post("/clients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Ana Silva"))
            .andExpect(jsonPath("$.email").value("ana@email.com"));
    }

    @Test
    @DisplayName("POST /clients/register deve retornar 400 quando body está vazio")
    void shouldReturn400WhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/clients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
```

---

## Tipo 4 — Teste de Integração de Repositório

Testa se o adapter JPA está mapeando corretamente os dados para o banco.

**Ferramenta:** `@DataJpaTest` — sobe apenas o contexto JPA com H2 em memória.

```java
// src/test/java/com/saas/permissions/modules/identity/infrastructure/ClientRepositoryAdapterIT.java
package com.saas.permissions.modules.identity.infrastructure;

import com.saas.permissions.modules.identity.domain.AuthProvider;
import com.saas.permissions.modules.identity.domain.Client;
import com.saas.permissions.modules.identity.domain.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(ClientRepositoryAdapter.class)   // importa apenas o adapter que queremos testar
@DisplayName("ClientRepositoryAdapter (IT)")
class ClientRepositoryAdapterIT {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("deve salvar e recuperar um cliente pelo email")
    void shouldSaveAndFindByEmail() {
        // ARRANGE
        var client = Client.builder()
            .name("Maria")
            .email("maria@email.com")
            .provider(AuthProvider.local)
            .build();

        // ACT
        clientRepository.save(client);
        var found = clientRepository.findByEmail("maria@email.com");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("existsByEmail deve retornar true quando email já está cadastrado")
    void shouldReturnTrueWhenEmailExists() {
        var client = Client.builder()
            .name("Pedro")
            .email("pedro@email.com")
            .provider(AuthProvider.local)
            .build();
        clientRepository.save(client);

        assertThat(clientRepository.existsByEmail("pedro@email.com")).isTrue();
        assertThat(clientRepository.existsByEmail("outro@email.com")).isFalse();
    }
}
```

---

## Tipo 5 — Teste da Chain of Responsibility (módulo permission)

O padrão Chain of Responsibility tem elos independentes — cada elo pode e deve ser testado sozinho.

```java
// src/test/java/com/saas/permissions/modules/permission/domain/ApiKeyValidationHandlerTest.java
package com.saas.permissions.modules.permission.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyValidationHandler")
class ApiKeyValidationHandlerTest {

    @Mock
    private PermissionValidationHandler nextHandler;   // próximo elo da chain

    @InjectMocks
    private ApiKeyValidationHandler handler;

    @Test
    @DisplayName("deve delegar para o próximo handler quando ApiKey é válida")
    void shouldDelegateWhenApiKeyIsValid() {
        // A implementação específica depende dos campos do seu ValidationRequest
        // Padrão: quando a chave é válida, o próximo handler é chamado
        var request = new ValidationRequest("chave-valida", /* demais campos */);
        handler.handle(request);
        verify(nextHandler).handle(request);
    }

    @Test
    @DisplayName("deve rejeitar quando ApiKey está ausente ou inválida")
    void shouldRejectWhenApiKeyIsInvalid() {
        var request = new ValidationRequest(null, /* demais campos */);
        assertThatThrownBy(() -> handler.handle(request))
            .isInstanceOf(UnauthorizedException.class);
        verify(nextHandler, never()).handle(any());
    }
}
```

---

## TDD — Como escrever código com testes

TDD (Test-Driven Development) significa **escrever o teste antes do código**. O ciclo tem três fases:

```
RED    → escreva o teste. Rode. Ele vai falhar (o código ainda não existe).
GREEN  → escreva o mínimo de código para o teste passar. Sem exageros.
REFACTOR → limpe o código sem quebrar o teste. Rode novamente para confirmar.
```

### Passo a passo prático para um novo use case

**1. Crie o arquivo de teste primeiro**

```java
// RegisterClientUseCaseTest.java — escreva ANTES de criar o use case
@Test
void shouldRegisterClientSuccessfully() {
    // ARRANGE
    when(clientRepository.existsByEmail(anyString())).thenReturn(false);
    when(clientRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    // ACT
    Client result = useCase.execute(validCommand()); // isso NÃO compila ainda — tudo bem!

    // ASSERT
    assertThat(result.getName()).isEqualTo("Ana Silva");
}
```

**2. Rode o teste** — ele vai falhar com erro de compilação ou `NullPointerException`. Isso é esperado (fase RED).

**3. Implemente o mínimo** — crie o use case com apenas o suficiente para o teste passar.

**4. Rode novamente** — agora ele deve passar (fase GREEN).

**5. Refatore** — renomeie variáveis, extraia métodos, melhore a legibilidade sem alterar o comportamento.

**6. Volte ao passo 1** para o próximo cenário.

---

## Comandos para rodar os testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar apenas uma classe de teste
./mvnw test -Dtest=RegisterClientUseCaseTest

# Rodar apenas um método específico
./mvnw test -Dtest=RegisterClientUseCaseTest#shouldRegisterClientSuccessfully

# Rodar apenas testes de integração (sufixo IT)
./mvnw test -Dtest="*IT"

# Rodar os testes com relatório detalhado
./mvnw test -Dsurefire.useFile=false

# Ver o relatório HTML após rodar
open target/surefire-reports/*.html
```

---

## O que NÃO testar

Evite testar coisas que o framework já garante:

| Não teste                            | Por quê                                    |
| ------------------------------------- | ------------------------------------------- |
| Getters e setters do Lombok           | O Lombok é testado pelos próprios autores |
| `@NotBlank` sozinho                 | Spring Validation tem testes próprios      |
| Conversão de enum para String no JPA | JPA garante isso                            |
| SQL gerado pelo JPA                   | Faz parte do framework                      |

Teste apenas o comportamento **da sua regra de negócio**.

---

## Guia rápido de decisão

```
Estou testando uma regra de negócio (if, throw, cálculo)?
  └── Use case ou domain → Teste Unitário com Mockito

Estou testando um endpoint HTTP (status code, body JSON)?
  └── Controller → @WebMvcTest

Estou testando se os dados são salvos/buscados corretamente no banco?
  └── Repositório → @DataJpaTest

Quero garantir que o contexto do Spring sobe sem erros?
  └── @SpringBootTest no PermissionSaasApplicationTests
```

---

## Checklist por Pull Request

Antes de abrir um PR, verifique:

- [ ] Todo use case novo tem ao menos 3 cenários testados: sucesso, erro de validação, estado inválido
- [ ] Todo endpoint novo tem teste de slice com pelo menos 2xx e 4xx
- [ ] Nenhum teste acessa banco real (apenas H2 via perfil `test`)
- [ ] `./mvnw test` passa localmente sem erros
- [ ] Os nomes dos testes descrevem o comportamento em português ou inglês claro, não "testMethod1"

---

## Referências rápidas

| O que usar                              | Para quê                              |
| --------------------------------------- | -------------------------------------- |
| `@ExtendWith(MockitoExtension.class)` | Testes unitários sem Spring           |
| `@Mock`                               | Cria um dublê do tipo anotado         |
| `@InjectMocks`                        | Injeta os mocks na classe testada      |
| `when(...).thenReturn(...)`           | Define o retorno de um mock            |
| `verify(mock).method(...)`            | Verifica se um método foi chamado     |
| `assertThat(x).isEqualTo(y)`          | Asserção principal (AssertJ)         |
| `assertThatThrownBy(...)`             | Verifica que uma exceção é lançada |
| `@WebMvcTest(Controller.class)`       | Slice test de controller               |
| `@DataJpaTest`                        | Slice test de repositório JPA         |
| `@MockitoBean`                        | Mock de bean dentro do contexto Spring |
| `@ActiveProfiles("test")`             | Ativa application-test.yml             |
