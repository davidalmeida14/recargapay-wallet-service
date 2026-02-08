package br.com.recargapay.wallet.e2e;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.recargapay.wallet.support.EndToEndTest;
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
class CustomerControllerE2ETest extends EndToEndTest {

  @Autowired private MockMvc mockMvc;

  @Nested
  @DisplayName("POST /api/v1/customers")
  class RegisterCustomer {

    @Test
    @DisplayName("returns 201 when registration is successful")
    void registersSuccessfully() throws Exception {
      var body =
          "{\"fullName\":\"New Customer\",\"email\":\"new@example.com\",\"password\":\"password123\"}";

      mockMvc
          .perform(post("/api/v1/customers").contentType(APPLICATION_JSON).content(body))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("returns 409 when email already exists")
    void returnsConflict() throws Exception {
      var email = "existing@example.com";
      register(mockMvc, "Existing User", email, "password");

      var body =
          "{\"fullName\":\"Duplicate User\",\"email\":\""
              + email
              + "\",\"password\":\"password123\"}";

      mockMvc
          .perform(post("/api/v1/customers").contentType(APPLICATION_JSON).content(body))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("returns 400 when body is invalid")
    void returns400WhenInvalid() throws Exception {
      var body = "{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"\"}";

      mockMvc
          .perform(post("/api/v1/customers").contentType(APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
  }
}
