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
class WithdrawControllerE2ETest extends EndToEndTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Nested
  @DisplayName("PUT /api/v1/withdrawals")
  class Withdraw {

    @Test
    @DisplayName("returns 200 and decreases balance")
    void withdrawsSuccessfully() throws Exception {
      var email = "withdraw@example.com";
      var password = "password";
      register(mockMvc, "Withdraw User", email, password);
      var token = login(mockMvc, email, password);

      var createBody = "{\"currency\":\"BRL\"}";
      mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON).content(createBody))
          .andExpect(status().isCreated());

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-dep-for-withdraw")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"200.00\"}"))
          .andExpect(status().isOk());

      mockMvc
          .perform(
              put("/api/v1/withdrawals")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-withdraw-1")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"75.00\"}"))
          .andExpect(status().isOk());

      mockMvc
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(125));
    }

    @Test
    @DisplayName("returns 422 when insufficient balance")
    void returns422WhenInsufficient() throws Exception {
      var email = "insufficient@example.com";
      var password = "password";
      register(mockMvc, "Insufficient User", email, password);
      var token = login(mockMvc, email, password);

      var createBody = "{\"currency\":\"BRL\"}";
      mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON).content(createBody))
          .andExpect(status().isCreated());

      mockMvc
          .perform(
              put("/api/v1/withdrawals")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-insufficient")
                  .contentType(APPLICATION_JSON)
                  .content("{\"amount\":\"100.00\"}"))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error").value("InsufficientBalanceException"));
    }

    @Test
    @DisplayName("returns 401 when not authenticated")
    void returns401WhenNotAuthenticated() throws Exception {
      var body = "{\"amount\":\"10.00\"}";
      mockMvc
          .perform(
              put("/api/v1/withdrawals")
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-withdraw-401")
                  .contentType(APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }
  }
}
