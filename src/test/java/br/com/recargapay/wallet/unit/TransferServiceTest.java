package br.com.recargapay.wallet.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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

import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.CurrencyMismatchException;
import br.com.recargapay.wallet.domain.wallet.exception.InsufficientBalanceException;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import br.com.recargapay.wallet.domain.wallet.service.TransferService;
import br.com.recargapay.wallet.support.UnitTest;

class TransferServiceTest extends UnitTest {

  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private EntryRepository entryRepository;
  @Mock private TransactionTemplate transactionTemplate;

  @InjectMocks private TransferService transferService;

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
  @DisplayName("Should transfer successfully")
  void transferSuccessfully() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("50.00");

    Wallet origin = new Wallet(UUID.randomUUID(), "BRL");
    origin.deposit(new BigDecimal("100.00"));
    Wallet dest = new Wallet(UUID.randomUUID(), "BRL");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(originId)).thenReturn(Optional.of(origin));
    when(walletRepository.loadByIdForUpdate(destId)).thenReturn(Optional.of(dest));

    transferService.transfer(originId, destId, amount, "idem-789");

    assertEquals(new BigDecimal("50.00"), origin.getBalance());
    assertEquals(new BigDecimal("50.00"), dest.getBalance());

    verify(walletRepository).add(origin);
    verify(walletRepository).add(dest);
    verify(transactionRepository).create(any(br.com.recargapay.wallet.domain.transaction.model.Transaction.class));
    verify(entryRepository).create(any(java.util.List.class));
  }

  @Test
  @DisplayName("Should throw CurrencyMismatchException if currencies are different")
  void throwCurrencyMismatch() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();

    Wallet origin = new Wallet(UUID.randomUUID(), "BRL");
    Wallet dest = new Wallet(UUID.randomUUID(), "USD");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(originId)).thenReturn(Optional.of(origin));
    when(walletRepository.loadByIdForUpdate(destId)).thenReturn(Optional.of(dest));

    assertThrows(
        CurrencyMismatchException.class,
        () -> transferService.transfer(originId, destId, BigDecimal.TEN, "idem"));
  }

  @Test
  @DisplayName("Should throw InsufficientBalanceException if origin has no funds")
  void throwInsufficientBalance() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();

    Wallet origin = new Wallet(UUID.randomUUID(), "BRL"); // balance 0
    Wallet dest = new Wallet(UUID.randomUUID(), "BRL");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(originId)).thenReturn(Optional.of(origin));
    when(walletRepository.loadByIdForUpdate(destId)).thenReturn(Optional.of(dest));

    assertThrows(
        InsufficientBalanceException.class,
        () -> transferService.transfer(originId, destId, BigDecimal.TEN, "idem"));
  }

  @Test
  @DisplayName("Should skip transfer if idempotency key exists")
  void skipIfIdempotencyExists() {
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.of(mock(br.com.recargapay.wallet.domain.transaction.model.Transaction.class)));

    transferService.transfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "idem");

    verify(walletRepository, never()).loadByIdForUpdate(any());
  }

  @Test
  @DisplayName("Should throw exception if origin and destination are same")
  void throwSameWallet() {
    UUID walletId = UUID.randomUUID();
    assertThrows(
        IllegalArgumentException.class,
        () -> transferService.transfer(walletId, walletId, BigDecimal.TEN, "idem"));
  }

  @Test
  @DisplayName("Should throw WalletNotFoundException if wallet does not exist")
  void throwWalletNotFound() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(any())).thenReturn(Optional.empty());

    assertThrows(
        WalletNotFoundException.class,
        () -> transferService.transfer(originId, destId, BigDecimal.TEN, "idem"));
  }
}
