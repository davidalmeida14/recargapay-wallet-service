package br.com.recargapay.wallet.support;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

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

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void setupQueues() throws Exception {
    String endpoint = "http://localhost:4566";
    String region = "us-east-1";
    String queueName = "transfer-credit-pending";

    try (SqsClient sqsClient =
        SqsClient.builder()
            .endpointOverride(java.net.URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build()) {

      ListQueuesResponse listQueuesResponse = sqsClient.listQueues();
      boolean queueExists =
          listQueuesResponse.queueUrls().stream().anyMatch(url -> url.contains(queueName));

      if (!queueExists) {
        sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
      } else {
        String queueUrl =
            listQueuesResponse.queueUrls().stream()
                .filter(url -> url.contains(queueName))
                .findFirst()
                .get();
        sqsClient.purgeQueue(b -> b.queueUrl(queueUrl));
      }
    } catch (Exception e) {
      System.err.println("Failed to setup SQS queue: " + e.getMessage());
    }
  }

  @AfterEach
  void cleanupDatabase() {
    try {
      jdbcTemplate.execute("TRUNCATE TABLE entries CASCADE");
      jdbcTemplate.execute("TRUNCATE TABLE transactions CASCADE");
      jdbcTemplate.execute("TRUNCATE TABLE wallets CASCADE");
      jdbcTemplate.execute("TRUNCATE TABLE customers CASCADE");
    } catch (Exception e) {
      System.err.println("Database cleanup failed: " + e.getMessage());
    }
  }

  protected String createWalletAndGetId(
      org.springframework.test.web.servlet.MockMvc mockMvc, String token, String currency)
      throws Exception {
    var result =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/wallets")
                    .header("Authorization", "Bearer " + token)
                    .contentType(APPLICATION_JSON)
                    .content("{\"currency\":\"" + currency + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(result.getResponse().getContentAsString())
        .get("id")
        .asText();
  }

  protected void deposit(
      org.springframework.test.web.servlet.MockMvc mockMvc,
      String token,
      String amount,
      String idempotencyId)
      throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/deposits")
                .header("Authorization", "Bearer " + token)
                .header(
                    br.com.recargapay.wallet.application.Headers.X_IDEMPOTENCY_ID, idempotencyId)
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"" + amount + "\"}"))
        .andExpect(status().isOk());
  }

  protected void withdraw(
      org.springframework.test.web.servlet.MockMvc mockMvc,
      String token,
      String amount,
      String idempotencyId)
      throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/withdrawals")
                .header("Authorization", "Bearer " + token)
                .header(
                    br.com.recargapay.wallet.application.Headers.X_IDEMPOTENCY_ID, idempotencyId)
                .contentType(APPLICATION_JSON)
                .content("{\"amount\":\"" + amount + "\"}"))
        .andExpect(status().isOk());
  }

  protected String login(
      org.springframework.test.web.servlet.MockMvc mockMvc, String email, String password)
      throws Exception {
    var loginBody = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    var loginResult =
        mockMvc
            .perform(
                post("/api/v1/authentication").contentType(APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

    var response = loginResult.getResponse().getContentAsString();
    return new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(response)
        .get("accessToken")
        .asText();
  }

  protected void register(
      org.springframework.test.web.servlet.MockMvc mockMvc,
      String fullName,
      String email,
      String password)
      throws Exception {
    var registerBody =
        "{\"fullName\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
            .formatted(fullName, email, password);
    mockMvc
        .perform(post("/api/v1/customers").contentType(APPLICATION_JSON).content(registerBody))
        .andExpect(status().isCreated());
  }
}
