package br.com.recargapay.wallet.e2e;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.support.EndToEndTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class DepositControllerE2ETest extends EndToEndTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Nested
  @DisplayName("PUT /api/v1/deposits")
  class Deposit {

    @Test
    @DisplayName("returns 200 and increases balance")
    void depositsSuccessfully() throws Exception {
      var email = "deposit@example.com";
      var password = "password";
      register(mockMvc, "Deposit User", email, password);
      var token = login(mockMvc, email, password);

      var createBody = "{\"currency\":\"BRL\"}";
      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON)
                  .content(createBody))
          .andExpect(status().isCreated());

      var depositBody = "{\"amount\":\"100.50\"}";
      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-deposit-1")
                  .contentType(APPLICATION_JSON)
                  .content(depositBody))
          .andExpect(status().isOk());

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(100.50));
    }

    @Test
    @DisplayName("returns 200 on duplicate idempotency key without double-crediting")
    void idempotentDeposit() throws Exception {
      var email = "idem@example.com";
      var password = "password";
      register(mockMvc, "Idem User", email, password);
      var token = login(mockMvc, email, password);

      var createBody = "{\"currency\":\"BRL\"}";
      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON)
                  .content(createBody))
          .andExpect(status().isCreated());

      var depositBody = "{\"amount\":\"50.00\"}";
      var idem = "e2e-idem-deposit";

      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, idem)
                  .contentType(APPLICATION_JSON)
                  .content(depositBody))
          .andExpect(status().isOk());
      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + token)
                  .header(Headers.X_IDEMPOTENCY_ID, idem)
                  .contentType(APPLICATION_JSON)
                  .content(depositBody))
          .andExpect(status().isOk());

      mockMvc
          .perform(get("/api/v1/wallets/balance").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(50));
    }

    @Test
    @DisplayName("returns 401 when not authenticated")
    void returns401WhenNotAuthenticated() throws Exception {
      var body = "{\"amount\":\"10.00\"}";
      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header(Headers.X_IDEMPOTENCY_ID, "e2e-401")
                  .contentType(APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 400 when amount is invalid or header missing")
    void returns400WhenInvalid() throws Exception {
      var email = "invalid@example.com";
      var password = "password";
      register(mockMvc, "Invalid User", email, password);
      var token = login(mockMvc, email, password);

      var body = "{\"amount\":\"0\"}";
      mockMvc
          .perform(
              put("/api/v1/deposits")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
  }
}
