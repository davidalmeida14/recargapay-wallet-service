# Wallet Service

Serviço de carteira digital robusto para gestão de saldos e transações financeiras. Este projeto foi desenvolvido como parte de um desafio técnico para a RecargaPay, focando em escalabilidade, consistência eventual e tratamento rigoroso de erros de negócio.

## 🚀 Tecnologias Utilizadas

- **Java 25**: Utilizando as últimas funcionalidades da linguagem (Virtual Threads, Records, Pattern Matching).
- **Spring Boot 4.x (Milestone)**: Framework base para a aplicação.
- **PostgreSQL**: Banco de dados relational para persistência de dados.
- **LocalStack (SQS)**: Simulação de serviços AWS para mensageria assíncrona.
- **Liquibase**: Gerenciamento de migrações de banco de dados.
- **Spring Security + OAuth2 (JWT)**: Proteção de endpoints e autenticação de usuários.
- **Docker & Docker Compose**: Containerização da aplicação e infraestrutura.
- **SpringDoc OpenAPI (Swagger)**: Documentação interativa da API.

## 🏗️ Arquitetura e Padrões

O projeto segue princípios de **Clean Architecture** e **Domain-Driven Design (DDD)**, organizado em camadas:

- **Application**: Controllers, DTOs e Workers (consumidores de fila).
- **Domain**: Entidades de negócio, serviços de domínio, repositórios (interfaces) e lógica central.
- **Infrastructure**: Implementações de persistência, configurações de segurança, mensageria e adaptadores externos.

### Decisões Técnicas Relevantes:

1.  **Tratamento de Erros com `Either`**: Em vez de depender exclusivamente de exceções para erros de negócio, utilizamos o padrão funcional `Either<Error, Success>`. Isso torna o fluxo de erro explícito na assinatura dos métodos e facilita o tratamento nos controllers.
2.  **Idempotência**: Todos os endpoints de transação (`Deposit`, `Withdraw`, `Transfer`) exigem um header `X-Idempotency-Id`. Isso garante que operações repetidas (ex: devido a falhas de rede) não resultem em duplicidade de débitos ou créditos.
3.  **Processamento Assíncrono**: Transferências são divididas em duas etapas:
    - **Débito Imediato**: Ocorre de forma síncrona na conta de origem durante a requisição.
    - **Crédito Assíncrono**: Um evento é disparado via SQS para que um worker processe o crédito na conta de destino de forma resiliente.
4.  **Virtual Threads**: Configurado para alta performance em operações bloqueantes de I/O.

## 🛠️ Como Executar

A maneira mais simples de subir o projeto é via **Docker**, garantindo que todas as dependências (Banco, SQS) estejam configuradas corretamente.

### Pré-requisitos
- Docker e Docker Compose instalados.
- Make (opcional, mas recomendado para usar os atalhos).

### Comandos Rápidos

| Comando | Descrição |
| :--- | :--- |
| `make up` | Constrói a imagem e sobe a aplicação + infraestrutura completa. |
| `make down` | Para e remove todos os containers e volumes. |
| `make dependencies/services/run` | Sobe apenas a infra (Postgres, LocalStack, Redis). |
| `make db/migrate` | Executa as migrações do banco de dados via Liquibase. |

A aplicação estará disponível em: `http://localhost:8080`

## 📖 Documentação da API (Swagger)

Após subir a aplicação, você pode acessar a documentação detalhada dos endpoints, esquemas de requisição e resposta em:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

## 🔄 Fluxos de Negócio

### 1. Autenticação e Registro
- **POST `/api/v1/customers`**: Registra um novo cliente.
- **POST `/api/v1/authentication`**: Gera um token JWT para acesso.

### 2. Gestão de Carteira
- **PUT `/api/v1/wallets`**: Cria uma nova carteira para o cliente autenticado (ex: BRL, USD).
- **GET `/api/v1/wallets/balance`**: Consulta o saldo atual ou histórico (via query param `at`).

### 3. Transações
- **PUT `/api/v1/deposits`**: Adiciona fundos à carteira.
- **PUT `/api/v1/withdrawals`**: Retira fundos da carteira (valida saldo).
- **PUT `/api/v1/transfers`**: Transfere valores entre carteiras (mesma moeda).

## 🧪 Testes

O projeto possui uma suíte abrangente de testes:

- **Unitários**: Testam a lógica de domínio isoladamente.
- **E2E (End-to-End)**: Testam os fluxos completos da API utilizando MockMvc e LocalStack real para SQS.

Para rodar os testes localmente (requer Java 25 instalado):
```bash
make test
```
Ou individualmente via Maven:
```bash
./mvnw test -Dgroups=unit
./mvnw test -Dgroups=e2e
```
