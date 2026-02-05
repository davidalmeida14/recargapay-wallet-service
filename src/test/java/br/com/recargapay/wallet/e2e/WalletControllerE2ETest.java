package br.com.recargapay.wallet.e2e;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WalletControllerE2ETest extends EndToEndTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Nested
  @DisplayName("PUT /api/v1/wallets")
  class CreateWallet {

    @Test
    @DisplayName("returns 201 and wallet when request is valid")
    void createsWallet() throws Exception {
      var email = "test@example.com";
      var password = "password";
      register(mockMvc, "Test User", email, password);
      var token = login(mockMvc, email, password);

      var body = "{\"currency\":\"BRL\"}";

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.balance").value(0))
          .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("returns 400 when body is invalid")
    void returns400WhenInvalid() throws Exception {
      var email = "invalidWallet@example.com";
      var password = "password";
      register(mockMvc, "Invalid User", email, password);
      var token = login(mockMvc, email, password);

      mockMvc
          .perform(
              put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/wallets/balance")
  class GetBalance {

    @Test
    @DisplayName("returns 200 and current balance when wallet exists")
    void returnsBalance() throws Exception {
      var email = "balance@example.com";
      var password = "password";
      register(mockMvc, "Balance User", email, password);
      var token = login(mockMvc, email, password);

      var createBody = "{\"currency\":\"BRL\"}";
      mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON).content(createBody))
          .andExpect(status().isCreated());

      mockMvc
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @DisplayName("returns 401 when not authenticated")
    void returns401WhenNotAuthenticated() throws Exception {
      mockMvc
          .perform(get("/api/v1/wallets/balance"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns 200 and historical balance when at param is provided")
    void returnsHistoricalBalance() throws Exception {
      var email = "hist@example.com";
      var password = "password";
      register(mockMvc, "Hist User", email, password);
      var token = login(mockMvc, email, password);

      var createBody = "{\"currency\":\"BRL\"}";
      mockMvc
          .perform(put("/api/v1/wallets")
                  .header("Authorization", "Bearer " + token)
                  .contentType(APPLICATION_JSON).content(createBody))
          .andExpect(status().isCreated());

      var at = "2024-01-01T12:00:00Z";

      mockMvc
          .perform(get("/api/v1/wallets/balance")
                  .header("Authorization", "Bearer " + token)
                  .param("at", at))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.balance").value(0));
    }
  }
}
