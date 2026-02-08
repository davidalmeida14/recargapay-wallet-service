package br.com.recargapay.wallet.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.InsufficientBalanceException;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import br.com.recargapay.wallet.domain.wallet.service.WithdrawService;
import br.com.recargapay.wallet.support.UnitTest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.support.TransactionTemplate;

class WithdrawServiceTest extends UnitTest {

  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private EntryRepository entryRepository;
  @Mock private TransactionTemplate transactionTemplate;

  @InjectMocks private WithdrawService withdrawService;

  @BeforeEach
  void setup() {
    // Use doAnswer for execute as it receives a TransactionCallback and returns a value
    lenient()
        .doAnswer(
            invocation -> {
              org.springframework.transaction.support.TransactionCallback callback =
                  invocation.getArgument(0);
              return callback.doInTransaction(null);
            })
        .when(transactionTemplate)
        .execute(any());
  }

  @Test
  @DisplayName("Should withdraw successfully")
  void withdrawSuccessfully() {
    UUID walletId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("50.00");
    String idempotencyId = "idem-456";
    Wallet wallet = new Wallet(UUID.randomUUID(), "BRL");
    wallet.deposit(new BigDecimal("100.00"));

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

    var result = withdrawService.withdraw(walletId, amount, idempotencyId);

    assertEquals(new BigDecimal("50.00"), wallet.getBalance());
    verify(walletRepository).save(wallet);
    verify(transactionRepository)
        .create(any(br.com.recargapay.wallet.domain.transaction.model.Transaction.class));
    verify(entryRepository)
        .create(any(br.com.recargapay.wallet.domain.transaction.model.Entry.class));
    verify(transactionRepository).update(any());
  }

  @Test
  @DisplayName("Should throw InsufficientBalanceException if balance is not enough")
  void throwInsufficientBalance() {
    UUID walletId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("150.00");
    Wallet wallet = new Wallet(UUID.randomUUID(), "BRL");
    wallet.deposit(new BigDecimal("100.00"));

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

    assertThrows(
        InsufficientBalanceException.class,
        () -> withdrawService.withdraw(walletId, amount, "idem"));

    verify(walletRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should skip withdraw if idempotency key exists")
  void skipIfIdempotencyExists() {
    UUID walletId = UUID.randomUUID();
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(
            Optional.of(mock(br.com.recargapay.wallet.domain.transaction.model.Transaction.class)));

    withdrawService.withdraw(walletId, BigDecimal.TEN, "idem");

    verify(walletRepository, never()).loadByIdForUpdate(any());
  }

  @Test
  @DisplayName("Should throw exception if amount is zero or negative")
  void throwExceptionIfAmountInvalid() {
    UUID walletId = UUID.randomUUID();
    assertThrows(
        IllegalArgumentException.class,
        () -> withdrawService.withdraw(walletId, BigDecimal.ZERO, "idem"));
  }

  @Test
  @DisplayName("Should throw WalletNotFoundException if wallet does not exist")
  void throwWalletNotFound() {
    UUID walletId = UUID.randomUUID();
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(walletId)).thenReturn(Optional.empty());

    assertThrows(
        WalletNotFoundException.class,
        () -> withdrawService.withdraw(walletId, BigDecimal.TEN, "idem"));
  }
}
