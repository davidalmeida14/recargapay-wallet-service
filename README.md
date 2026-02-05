# Wallet Service

Serviço de carteira (wallet) para gestão de saldos: criação de carteiras, depósito, saque, transferência e consulta de saldo (atual e histórico).

- **Stack:** Java 25 (recomendado; o projeto está configurado com `java.version=25`), Spring Boot 4.x
- **Banco:** PostgreSQL (produção/e2e opcional com H2)

**Importante (Java 25):** Para compilar e rodar os testes, use o mesmo JDK 25 na compilação e na execução. Defina `JAVA_HOME` apontando para o JDK 25 antes de rodar Maven, por exemplo:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)  # macOS
./mvnw test
```

---

## Como executar a aplicação

1. Subir PostgreSQL (ex.: `docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=wallet postgres:14`)
2. Rodar migrações (Liquibase via aplicação) ou garantir que o schema exista
3. Iniciar a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
4. API em `http://localhost:8080` (e Swagger UI se configurado)

---

## Como executar os testes

Os testes estão organizados por **tags** JUnit 5 e herdam de classes base:

| Tag                  | Classe base     | Descrição |
|----------------------|-----------------|-----------|
| `unit`               | `UnitTest`      | Testes unitários (Mockito, sem Spring) |
| `e2e`                | `EndToEndTest`  | Testes end-to-end dos controllers (Spring Boot + MockMvc, H2 em memória) |
| `integration-legacy` | `IntegrationTest` | Testes de integração com persistência (DataJpaTest, **PostgreSQL obrigatório**) |

### Executar todos os testes

```bash
./mvnw test
```

### Executar apenas testes unitários (tag `unit`)

```bash
./mvnw test -Dgroups=unit
```

- Não precisa de banco.
- Perfil: `unit`.

### Executar apenas testes E2E (tag `e2e`)

```bash
./mvnw test -Dgroups=e2e
```

- Usa H2 em memória (perfil `e2e`).
- Não precisa de PostgreSQL.

### Executar apenas testes de integração (tag `integration-legacy`)

```bash
./mvnw test -Dgroups=integration
```

- **Requer PostgreSQL** em execução (ex.: `localhost:5432`, base `wallet`, usuário/senha em `application-integration.yml` / `application-test.yml`).
- Perfis: `integration`, `test`.

### Resumo dos comandos

| Objetivo              | Comando |
|-----------------------|---------|
| Todos os testes       | `./mvnw test` |
| Só unitários          | `./mvnw test -Dgroups=unit` |
| Só E2E                | `./mvnw test -Dgroups=e2e` |
| Só integração         | `./mvnw test -Dgroups=integration` |

### Estrutura dos testes

- **Classes base:** `support/UnitTest.java`, `support/EndToEndTest.java`, `support/IntegrationTest.java`
- **Unit:** `unit/WalletServiceTest.java`, `unit/DepositServiceTest.java`, `unit/WithdrawServiceTest.java`, `unit/TransferServiceTest.java`
- **E2E:** `e2e/WalletControllerE2ETest.java`, `e2e/DepositControllerE2ETest.java`, `e2e/WithdrawControllerE2ETest.java`, `e2e/TransferControllerE2ETest.java`
- **Integração:** `integration/WalletPersistenceIntegrationTest.java`

Configuração de perfis de teste em `src/test/resources/application-*.yml` (`unit`, `e2e`, `integration`, `test`).

---

## Docker

Para construir e executar a aplicação via Docker:

1. Build da imagem:
   ```bash
   docker build -t wallet-service .
   ```
2. Execução (exemplo com rede compartilhada com PostgreSQL):
   ```bash
   docker run -p 8080:8080 \
     -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/wallet \
     -e DATABASE_USERNAME=postgres \
     -e DATABASE_PASSWORD=postgres \
     wallet-service
   ```

## CI/CD

O projeto possui um workflow do GitHub Actions configurado em `.github/workflows/ci.yml` que:
- Configura o JDK 25.
- Sobe um container PostgreSQL para testes.
- Executa a compilação e todos os testes (`./mvnw test`).
- Verifica a formatação do código (`./mvnw spotless:check`).
