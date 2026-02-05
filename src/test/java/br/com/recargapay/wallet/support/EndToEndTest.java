package br.com.recargapay.wallet.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for end-to-end (controller) tests. Full Spring context, random port, MockMvc.
 *
 * <p>Tag: {@code e2e}. Profile: {@code e2e}.
 */
@Tag("e2e")
@ActiveProfiles("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public abstract class EndToEndTest {

    protected String login(org.springframework.test.web.servlet.MockMvc mockMvc, String email, String password) throws Exception {
        var loginBody = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        var loginResult = mockMvc.perform(post("/api/v1/authentication")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        var response = loginResult.getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("accessToken").asText();
    }

    protected void register(org.springframework.test.web.servlet.MockMvc mockMvc, String fullName, String email, String password) throws Exception {
        var registerBody = "{\"fullName\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(fullName, email, password);
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());
    }
}
