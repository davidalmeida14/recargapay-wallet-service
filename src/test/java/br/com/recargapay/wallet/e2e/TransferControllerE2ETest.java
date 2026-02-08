package br.com.recargapay.wallet.e2e;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.support.EndToEndTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Transactional
class TransferControllerE2ETest extends EndToEndTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  private ExecutorService executorService = Executors.newFixedThreadPool(2);

  @Nested
  @DisplayName("PUT /api/v1/transfers")
  class Transfer {

    @Test
    @DisplayName("returns 200 and moves balance between wallets")
    void transfersSuccessfully() throws Exception {
      var emailA = "customerA@example.com";
      var emailB = "customerB@example.com";
      var password = "password";

      register(mockMvc, "Customer A", emailA, password);
      register(mockMvc, "Customer B", emailB, password);

      var tokenA = login(mockMvc, emailA, password);
      var tokenB = login(mockMvc, emailB, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB =
          mockMvc
              .perform(
                  put("/api/v1/wallets")
                      .header("Authorization", "Bearer " + tokenB)
                      .contentType(APPLICATION_JSON)
                      .content("{\"currency\":\"BRL\"}"))
              .andExpect(status().isCreated())
              .andReturn();

      var destId =
          objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-origin")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"150.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":\"%s\",\"amount\":\"60.00\"}".formatted(destId);
      mockMvc
          .perform(
              put("/api/v1/transfers")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-transfer-1")
                  .contentType(APPLICATION_JSON)
                  .content(transferBody))
          .andExpect(status().isOk());

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + tokenA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(90));

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + tokenB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(60));
    }

    @Test
    @DisplayName("returns 422 when currencies differ")
    void returns422WhenCurrencyMismatch() throws Exception {
      var emailA = "mismatchA@example.com";
      var emailB = "mismatchB@example.com";
      var password = "password";

      register(mockMvc, "Mismatch A", emailA, password);
      register(mockMvc, "Mismatch B", emailB, password);

      var tokenA = login(mockMvc, emailA, password);
      var tokenB = login(mockMvc, emailB, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB =
          mockMvc
              .perform(
                  put("/api/v1/wallets")
                      .header("Authorization", "Bearer " + tokenB)
                      .contentType(APPLICATION_JSON)
                      .content("{\"currency\":\"USD\"}"))
              .andExpect(status().isCreated())
              .andReturn();

      var destId =
          objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-brl")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":\"%s\",\"amount\":\"50.00\"}".formatted(destId);
      mockMvc
          .perform(
              put("/api/v1/transfers")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-currency-mismatch")
                  .contentType(APPLICATION_JSON)
                  .content(transferBody))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error").value("CurrencyMismatchException"));
    }

    @Test
    @DisplayName("returns 400 when destinationWalletId is null")
    void returns400WhenDestinationWalletIdIsNull() throws Exception {
      var email = "nulldest@example.com";
      var password = "password";

      register(mockMvc, "Customer Null", email, password);
      var token = login(mockMvc, email, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-null-dest")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":null,\"amount\":\"50.00\"}";
      mockMvc
          .perform(
              put("/api/v1/transfers")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-null-dest")
                  .contentType(APPLICATION_JSON)
                  .content(transferBody))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("ValidationError"));
    }

    @Test
    @DisplayName("returns 400 when amount is null")
    void returns400WhenAmountIsNull() throws Exception {
      var emailA = "nullamount@example.com";
      var emailB = "nullamountdest@example.com";
      var password = "password";

      register(mockMvc, "Customer A", emailA, password);
      register(mockMvc, "Customer B", emailB, password);

      var tokenA = login(mockMvc, emailA, password);
      var tokenB = login(mockMvc, emailB, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB =
          mockMvc
              .perform(
                  put("/api/v1/wallets")
                      .header("Authorization", "Bearer " + tokenB)
                      .contentType(APPLICATION_JSON)
                      .content("{\"currency\":\"BRL\"}"))
              .andExpect(status().isCreated())
              .andReturn();

      var destId =
          objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-null-amount")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":\"%s\",\"amount\":null}".formatted(destId);
      mockMvc
          .perform(
              put("/api/v1/transfers")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-null-amount")
                  .contentType(APPLICATION_JSON)
                  .content(transferBody))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("ValidationError"));
    }

    @Test
    @DisplayName("returns 400 when amount is negative")
    void returns400WhenAmountIsNegative() throws Exception {
      var emailA = "negamount@example.com";
      var emailB = "negamountdest@example.com";
      var password = "password";

      register(mockMvc, "Customer A", emailA, password);
      register(mockMvc, "Customer B", emailB, password);

      var tokenA = login(mockMvc, emailA, password);
      var tokenB = login(mockMvc, emailB, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB =
          mockMvc
              .perform(
                  put("/api/v1/wallets")
                      .header("Authorization", "Bearer " + tokenB)
                      .contentType(APPLICATION_JSON)
                      .content("{\"currency\":\"BRL\"}"))
              .andExpect(status().isCreated())
              .andReturn();

      var destId =
          objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-neg-amount")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":\"%s\",\"amount\":\"-10.00\"}".formatted(destId);
      mockMvc
          .perform(
              put("/api/v1/transfers")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-neg-amount")
                  .contentType(APPLICATION_JSON)
                  .content(transferBody))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("ValidationError"));
    }

    @Test
    @DisplayName(
        "prevents duplicate transfers with different idempotency keys when balance is insufficient")
    void preventsDoubleSpendingWithInsufficientBalance() throws Exception {
      var emailA = "racecond@example.com";
      var emailB = "raceconddest@example.com";
      var password = "password";

      register(mockMvc, "Race Customer A", emailA, password);
      register(mockMvc, "Race Customer B", emailB, password);

      var tokenA = login(mockMvc, emailA, password);
      var tokenB = login(mockMvc, emailB, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB =
          mockMvc
              .perform(
                  put("/api/v1/wallets")
                      .header("Authorization", "Bearer " + tokenB)
                      .contentType(APPLICATION_JSON)
                      .content("{\"currency\":\"BRL\"}"))
              .andExpect(status().isCreated())
              .andReturn();

      var destId =
          objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-race")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":\"%s\",\"amount\":\"100.00\"}".formatted(destId);

      try {
        var future1 =
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return mockMvc
                        .perform(
                            put("/api/v1/transfers")
                                .header("Authorization", "Bearer " + tokenA)
                                .header(Headers.X_IDEMPOTENCY_ID, "e2e-race-1")
                                .contentType(APPLICATION_JSON)
                                .content(transferBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                },
                executorService);

        var future2 =
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return mockMvc
                        .perform(
                            put("/api/v1/transfers")
                                .header("Authorization", "Bearer " + tokenA)
                                .header(Headers.X_IDEMPOTENCY_ID, "e2e-race-2")
                                .contentType(APPLICATION_JSON)
                                .content(transferBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                },
                executorService);

        // Wait for both futures to complete
        var status1 = future1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        var status2 = future2.get(10, java.util.concurrent.TimeUnit.SECONDS);

        // One should succeed (200), one should fail (422 - InsufficientBalanceException)
        var successCount = (status1 == 200 ? 1 : 0) + (status2 == 200 ? 1 : 0);
        var failureCount = (status1 == 422 ? 1 : 0) + (status2 == 422 ? 1 : 0);

        assert successCount == 1
            : "Expected exactly one successful transfer, got "
                + successCount
                + " (status1="
                + status1
                + ", status2="
                + status2
                + ")";
        assert failureCount == 1
            : "Expected exactly one failed transfer, got "
                + failureCount
                + " (status1="
                + status1
                + ", status2="
                + status2
                + ")";
      } finally {
        executorService.shutdown();
      }

      // Verify final balance: only one transfer succeeded
      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + tokenA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(0));

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + tokenB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    @DisplayName("returns 200 on duplicate idempotency without double transfer")
    void idempotentTransfer() throws Exception {
      var emailA = "idemTransA@example.com";
      var emailB = "idemTransB@example.com";
      var password = "password";

      register(mockMvc, "Idem Customer A", emailA, password);
      register(mockMvc, "Idem Customer B", emailB, password);

      var tokenA = login(mockMvc, emailA, password);
      var tokenB = login(mockMvc, emailB, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON)
                  .content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB =
          mockMvc
              .perform(
                  put("/api/v1/wallets")
                      .header("Authorization", "Bearer " + tokenB)
                      .contentType(APPLICATION_JSON)
                      .content("{\"currency\":\"BRL\"}"))
              .andExpect(status().isCreated())
              .andReturn();

      var destId =
          objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-idem")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"80.00\"}"))
          .andExpect(status().isOk());

      var transferBody = "{\"destinationWalletId\":\"%s\",\"amount\":\"30.00\"}".formatted(destId);

      var idem = "e2e-idem-transfer";
      for (int i = 0; i < 2; i++) {
        mockMvc
            .perform(
                put("/api/v1/transfers")
                    .header("Authorization", "Bearer " + tokenA)
                    .header(Headers.X_IDEMPOTENCY_ID, idem)
                    .contentType(APPLICATION_JSON)
                    .content(transferBody))
            .andExpect(status().isOk());
      }

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + tokenA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(50));

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + tokenB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(30));
    }
  }
}
