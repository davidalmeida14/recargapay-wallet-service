package br.com.recargapay.wallet.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.recargapay.wallet.support.EndToEndTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Transactional
class WalletHistoricalBalanceE2ETest extends EndToEndTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  private String TOKEN = "";

  @BeforeEach
  void setup() {
    var email = "historical@example.com";
    var password = "password";
    try {
      register(mockMvc, "Historical User", email, password);
      TOKEN = login(mockMvc, email, password);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("Should retrieve correct historical balance after deposit and multiple withdrawals")
  void shouldRetrieveHistoricalBalanceCorrectly() throws Exception {

    // 1. Create Wallet
    UUID walletId = UUID.fromString(createWalletAndGetId(mockMvc, TOKEN, "BRL"));

    // 2. Deposit R$1000
    deposit(mockMvc, TOKEN, "1000.00", "dep-1000");

    // 3. Perform several withdrawals on "alternate days"
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime day1 = now.minusDays(10);
    OffsetDateTime day2 = now.minusDays(8);
    OffsetDateTime day3 = now.minusDays(6); // This will be our target date
    OffsetDateTime day4 = now.minusDays(4);
    OffsetDateTime day6 = now.minusDays(2);

    // Update Deposit to Day 1
    updateTransactionDate("dep-1000", day1);
    updateEntriesDate(walletId, day1, null);

    // Withdrawal 1 (Day 2)
    withdraw(mockMvc, TOKEN, "100.00", "with-1");
    updateTransactionDate("with-1", day2);
    updateEntriesDate(walletId, day2, day1);

    // Withdrawal 2 (Day 4)
    withdraw(mockMvc, TOKEN, "200.00", "with-2");
    updateTransactionDate("with-2", day4);
    updateEntriesDate(walletId, day4, day2);

    // Withdrawal 3 (Day 6)
    withdraw(mockMvc, TOKEN, "50.00", "with-3");
    updateTransactionDate("with-3", day6);
    updateEntriesDate(walletId, day6, day4);

    // 4. Validate Balance at Day 3 (Should be 1000 - 100 = 900)
    validateHistoricalBalance(TOKEN, day3, 900.00);

    // 5. Validate Balance at Day 5 (Should be 1000 - 100 - 200 = 700)
    OffsetDateTime day5 = now.minusDays(3);
    validateHistoricalBalance(TOKEN, day5, 700.00);

    // 6. Validate Current Balance (Should be 1000 - 100 - 200 - 50 = 650)
    validateHistoricalBalance(TOKEN, null, 650.00);
  }

  private void updateTransactionDate(String idempotencyId, OffsetDateTime date) {
    jdbcTemplate.update(
        "UPDATE transactions SET created_at = ?, updated_at = ? WHERE idempotency_id = ?",
        date,
        date,
        idempotencyId);
  }

  private void updateEntriesDate(UUID walletId, OffsetDateTime date, OffsetDateTime after) {
    if (after == null) {
      jdbcTemplate.update("UPDATE entries SET created_at = ? WHERE wallet_id = ?", date, walletId);
    } else {
      jdbcTemplate.update(
          "UPDATE entries SET created_at = ? WHERE wallet_id = ? AND created_at > ?",
          date,
          walletId,
          after);
    }
  }

  private void validateHistoricalBalance(String token, OffsetDateTime at, double expectedBalance)
      throws Exception {
    var request = get("/api/v1/wallets/balance").header("Authorization", "Bearer " + token);

    if (at != null) {
      request.param("at", at.toString());
    }

    mockMvc
        .perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(expectedBalance));
  }
}
