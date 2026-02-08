package br.com.recargapay.wallet.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.WalletErrorCode;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import br.com.recargapay.wallet.domain.wallet.service.DepositService;
import br.com.recargapay.wallet.support.UnitTest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class DepositServiceTest extends UnitTest {

  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private EntryRepository entryRepository;
  @Mock private TransactionTemplate transactionTemplate;

  @InjectMocks private DepositService depositService;

  @BeforeEach
  void setup() {
    // Use doAnswer for execute as it receives a TransactionCallback and returns a
    // value
    lenient()
        .doAnswer(
            invocation -> {
              TransactionCallback callback = invocation.getArgument(0);
              return callback.doInTransaction(null);
            })
        .when(transactionTemplate)
        .execute(any());
  }

  @Test
  @DisplayName("Should deposit successfully")
  void depositSuccessfully() {
    UUID walletId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.00");
    String idempotencyId = "idem-123";
    Wallet wallet = new Wallet(UUID.randomUUID(), "BRL");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

    var result = depositService.deposit(walletId, amount, idempotencyId);

    assertEquals(new BigDecimal("100.00"), wallet.getBalance());
    verify(walletRepository).save(wallet);
    verify(transactionRepository)
        .create(any(br.com.recargapay.wallet.domain.transaction.model.Transaction.class));
    verify(entryRepository)
        .create(any(br.com.recargapay.wallet.domain.transaction.model.Entry.class));
    verify(transactionRepository).update(any());
  }

  @Test
  @DisplayName("Should skip deposit if idempotency key exists")
  void skipIfIdempotencyExists() {
    UUID walletId = UUID.randomUUID();
    String idempotencyId = "idem-123";

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(
            Optional.of(mock(br.com.recargapay.wallet.domain.transaction.model.Transaction.class)));

    depositService.deposit(walletId, BigDecimal.TEN, idempotencyId);

    verify(walletRepository, never()).loadByIdForUpdate(any());
    verify(walletRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should return error if amount is zero or negative")
  void throwExceptionIfAmountInvalid() {
    UUID walletId = UUID.randomUUID();
    var result1 = depositService.deposit(walletId, BigDecimal.ZERO, "idem");
    assertTrue(result1.isLeft());
    assertEquals(WalletErrorCode.INVALID_AMOUNT.getCode(), result1.getLeft().get().code());

    var result2 = depositService.deposit(walletId, new BigDecimal("-1"), "idem");
    assertTrue(result2.isLeft());
    assertEquals(WalletErrorCode.INVALID_AMOUNT.getCode(), result2.getLeft().get().code());
  }

  @Test
  @DisplayName("Should return error if wallet does not exist")
  void throwWalletNotFound() {
    UUID walletId = UUID.randomUUID();
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.empty());

    var result = depositService.deposit(walletId, BigDecimal.TEN, "idem");
    assertTrue(result.isLeft());
    assertEquals(WalletErrorCode.WALLET_NOT_FOUND.getCode(), result.getLeft().get().code());
  }
}
