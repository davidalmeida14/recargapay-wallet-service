package br.com.recargapay.wallet.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.recargapay.wallet.domain.transaction.model.Entry;
import br.com.recargapay.wallet.domain.transaction.model.FinancialType;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.support.UnitTest;

class WalletServiceTest extends UnitTest {

  @Mock private WalletRepository walletRepository;
  @Mock private EntryRepository entryRepository;
  @Mock private TransactionTemplate transactionTemplate;

  @InjectMocks private WalletService walletService;

  @BeforeEach
  void setup() {
    lenient().doAnswer(
            invocation -> {
              Consumer<TransactionStatus> callback = invocation.getArgument(0);
              callback.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
  }

  @Test
  @DisplayName("Should create wallet if not exists")
  void createWallet() {
    UUID customerId = UUID.randomUUID();
    String currency = "BRL";

    when(walletRepository.listByCustomerIdAndCurrency(customerId, currency)).thenReturn(List.of());

    Wallet wallet = walletService.createWallet(customerId, currency);

    assertEquals(customerId, wallet.getCustomerId());
    assertEquals(currency, wallet.getCurrency().name());
    verify(walletRepository).add(any(Wallet.class));
  }

  @Test
  @DisplayName("Should return existing wallet if already exists")
  void returnExistingWallet() {
    UUID customerId = UUID.randomUUID();
    String currency = "BRL";
    Wallet existing = new Wallet(customerId, currency);

    when(walletRepository.listByCustomerIdAndCurrency(customerId, currency)).thenReturn(List.of(existing));

    Wallet wallet = walletService.createWallet(customerId, currency);

    assertEquals(existing, wallet);
    verify(walletRepository, never()).add(any());
  }

  @Test
  @DisplayName("Should retrieve balance")
  void retrieveBalance() {
    UUID walletId = UUID.randomUUID();
    Wallet wallet = new Wallet(UUID.randomUUID(), "BRL");
    wallet.deposit(new BigDecimal("123.45"));

    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

    BigDecimal balance = walletService.retrieveBalance(walletId);

    assertEquals(new BigDecimal("123.45"), balance);
  }

  @Test
  @DisplayName("Should retrieve historical balance")
  void retrieveHistoricalBalance() {
    UUID walletId = UUID.randomUUID();
    OffsetDateTime at = OffsetDateTime.now();
    
    when(walletRepository.findById(walletId)).thenReturn(Optional.of(mock(Wallet.class)));
    
    List<Entry> entries = List.of(
        new Entry(UUID.randomUUID(), walletId, UUID.randomUUID(), new BigDecimal("100.00"), FinancialType.CREDIT, at.minusDays(1)),
        new Entry(UUID.randomUUID(), walletId, UUID.randomUUID(), new BigDecimal("30.00"), FinancialType.DEBIT, at.minusHours(1))
    );
    
    when(entryRepository.findByWalletIdAndCreatedAtBeforeOrderByCreatedAtAsc(walletId, at)).thenReturn(entries);

    BigDecimal balance = walletService.retrieveHistoricalBalance(walletId, at);

    assertEquals(new BigDecimal("70.00"), balance);
  }

  @Test
  @DisplayName("Should throw WalletNotFoundException when retrieving balance of non-existent wallet")
  void throwNotFoundOnBalance() {
    UUID walletId = UUID.randomUUID();
    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.empty());

    assertThrows(WalletNotFoundException.class, () -> walletService.retrieveBalance(walletId));
  }
}
