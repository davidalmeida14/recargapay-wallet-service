package br.com.recargapay.wallet.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.domain.transaction.model.Status;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.support.EndToEndTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ProcessCreditTransferWorkerE2ETest extends EndToEndTest {

  @Autowired private TransactionRepository transactionRepository;
  @Autowired private EntryRepository entryRepository;

  @Test
  @DisplayName("Should process credit transfer correctly from SQS message")
  void shouldProcessCreditTransferCorrectly() throws Exception {
    // 1. Setup Data
    var emailA = "origin@worker.com";
    var emailB = "dest@worker.com";
    var password = "password";

    register("Origin User", emailA, password);
    register("Dest User", emailB, password);

    var tokenA = login(emailA, password);
    var tokenB = login(emailB, password);

    createWalletAndGetId(tokenA, "BRL");
    var destWalletId = createWalletAndGetId(tokenB, "BRL");

    deposit(tokenA, "100.00", "dep-worker-1");

    // 2. Initiate Transfer
    var transferBody =
        "{\"destinationWalletId\":\"%s\",\"amount\":\"40.00\"}".formatted(destWalletId);
    var result =
        mockMvc
            .perform(
                put("/api/v1/transfers")
                    .header("Authorization", "Bearer " + tokenA)
                    .header(Headers.X_IDEMPOTENCY_ID, "transfer-worker-1")
                    .contentType(APPLICATION_JSON)
                    .content(transferBody))
            .andExpect(status().isOk())
            .andReturn();

    var transactionId =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

    // 3. Await and Verify Results
    awaitTransactionProcessed(UUID.fromString(transactionId));

    // Check destination balance
    awaitBalance(tokenB, 40.00);

    // Verify transaction status in DB
    var results =
        jdbcTemplate.queryForList(
            "SELECT status FROM transactions WHERE id = ?",
            String.class,
            UUID.fromString(transactionId));
    // Verify entry was created for destination
    var creditCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM entries WHERE transaction_id = ? AND financial_type = 'CREDIT';",
            Integer.class,
            UUID.fromString(transactionId));

    var debitCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM entries WHERE transaction_id = ? AND financial_type = 'DEBIT';",
            Integer.class,
            UUID.fromString(transactionId));

    assertEquals(1, creditCount);
    assertEquals(1, debitCount);
    assertEquals(Status.PROCESSED.name(), results.get(0));
  }

  @Test
  @DisplayName("Should process credit transfer correctly when message is manually sent to SQS")
  void shouldProcessManualSqsMessage() throws Exception {
    // 1. Setup Data
    var emailA = "origin-manual@worker.com";
    var emailB = "dest-manual@worker.com";
    var password = "password";

    register("Origin User Manual", emailA, password);
    register("Dest User Manual", emailB, password);

    var tokenA = login(emailA, password);
    var tokenB = login(emailB, password);

    var originWalletId = createWalletAndGetId(tokenA, "BRL");
    var destWalletId = createWalletAndGetId(tokenB, "BRL");

    deposit(tokenA, "100.00", "dep-manual-1");

    // 2. Create a PENDING transaction manually in DB
    UUID transactionId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO transactions (id, wallet_id, wallet_destination_id, amount, type, status, idempotency_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'TRANSFER', 'PENDING', ?, now(), now())",
        transactionId,
        UUID.fromString(originWalletId),
        UUID.fromString(destWalletId),
        new java.math.BigDecimal("30.00"),
        "idem-manual-transfer");

    // 3. Send message to SQS manually using software.amazon.awssdk.services.sqs.SqsClient
    try (software.amazon.awssdk.services.sqs.SqsClient sqsClient =
        software.amazon.awssdk.services.sqs.SqsClient.builder()
            .endpointOverride(java.net.URI.create("http://localhost:4566"))
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(
                software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                        "test", "test")))
            .build()) {

      String queueUrl =
          sqsClient.getQueueUrl(b -> b.queueName("transfer-credit-pending")).queueUrl();
      String messageBody = "{\"transactionId\":\"%s\"}".formatted(transactionId);

      sqsClient.sendMessage(b -> b.queueUrl(queueUrl).messageBody(messageBody));
    }

    // 4. Await and Verify Results
    awaitTransactionProcessed(transactionId);
    awaitBalance(tokenB, 30.00);

    // Verify transaction status in DB
    var results =
        jdbcTemplate.queryForList(
            "SELECT status FROM transactions WHERE id = ?", String.class, transactionId);
    assertEquals(Status.PROCESSED.name(), results.get(0));
  }
}
