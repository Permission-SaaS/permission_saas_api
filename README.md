# permission_saas_api

Aplicação principal do **Permission SaaS** (`permission-service`, porta 8080): um monolito modular em
Java 21 / Spring Boot 4.1.0 / PostgreSQL que valida permissões por API para projetos multi-tenant —
cadastro de cliente, plano e API Key, projetos com cargos e rotas, e a validação
`POST /validate-permission`.

Faz parte da organização [Permission-SaaS](https://github.com/Permission-SaaS). O repositório
[`permission_saas`](https://github.com/Permission-SaaS/permission_saas) é o guarda-chuva: reúne todos
os repositórios como submódulos, sobe o sistema inteiro pelo Docker Compose e guarda a visão de
arquitetura, o log de ADRs e as entregas da Pós-Graduação.

## Rodar só esta aplicação

Precisa de JDK 21 e de um PostgreSQL 16 em `localhost:5432` (banco `permissions_saas`, usuário
`saas`, senha `saas123`). O profile `dev`, o padrão, já aponta para lá e não usa o Config Server:
nenhuma variável é necessária.

```bash
docker compose up -d postgres rabbitmq   # na raiz do guarda-chuva
./mvnw spring-boot:run                   # porta 8080
curl http://localhost:8080/ping          # pong
```

Com o banco em outra porta: `DB_URL=jdbc:postgresql://localhost:5434/permissions_saas ./mvnw spring-boot:run`.
A trilha de auditoria depende do RabbitMQ e do
[`audit-service`](https://github.com/Permission-SaaS/permission_saas_audit) no ar.

Swagger UI: http://localhost:8080/swagger-ui/index.html (usuário `admin`, senha `admin123` no `dev`).

## Build e testes

```bash
./mvnw clean package -DskipTests   # build
./mvnw test                        # testes de unidade, inclusive a verificação do Spring Modulith
./mvnw verify                      # inclui os testes de integração (*IT)
```

Os testes rodam no profile `test`, com H2 em memória: não precisam de banco nem do Config Server.

## Documentação

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): camadas, módulos, regras entre módulos, tratamento de erros, pacotes
- [`docs/DOMAIN.md`](docs/DOMAIN.md): entidades, value objects e invariantes
- [`docs/API.md`](docs/API.md): cada endpoint, com exemplo de `curl`
- [`docs/PATTERNS.md`](docs/PATTERNS.md): os padrões de projeto aplicados
- [`docs/TEST-ARCHITECTURE.md`](docs/TEST-ARCHITECTURE.md): camadas e convenções de teste
