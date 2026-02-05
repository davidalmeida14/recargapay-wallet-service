package br.com.recargapay.wallet.e2e;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.support.EndToEndTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON).content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB = mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenB)
                  .contentType(APPLICATION_JSON).content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated())
          .andReturn();

      var destId = objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-origin")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"150.00\"}"))
          .andExpect(status().isOk());

      var transferBody =
          "{\"destinationWalletId\":\"%s\",\"amount\":\"60.00\"}"
              .formatted(destId);
      mockMvc
          .perform(
              put("/api/v1/transfers")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-transfer-1")
                  .contentType(APPLICATION_JSON)
                  .content(transferBody))
          .andExpect(status().isOk());

      mockMvc
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + tokenA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(90));

      mockMvc
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + tokenB))
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
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON).content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB = mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenB)
                  .contentType(APPLICATION_JSON).content("{\"currency\":\"USD\"}"))
          .andExpect(status().isCreated())
          .andReturn();

      var destId = objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-brl")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isOk());

      var transferBody =
          "{\"destinationWalletId\":\"%s\",\"amount\":\"50.00\"}"
              .formatted(destId);
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
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenA)
                  .contentType(APPLICATION_JSON).content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated());

      var createB = mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + tokenB)
                  .contentType(APPLICATION_JSON).content("{\"currency\":\"BRL\"}"))
          .andExpect(status().isCreated())
          .andReturn();

      var destId = objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asText();

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + tokenA)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-idem")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"80.00\"}"))
          .andExpect(status().isOk());

      var transferBody =
          "{\"destinationWalletId\":\"%s\",\"amount\":\"30.00\"}"
              .formatted(destId);

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
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + tokenA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(50));

      mockMvc
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + tokenB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(30));
    }
  }
}
