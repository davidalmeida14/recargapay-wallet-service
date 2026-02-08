package br.com.recargapay.wallet.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.WalletErrorCode;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import br.com.recargapay.wallet.domain.wallet.service.TransferService;
import br.com.recargapay.wallet.support.UnitTest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class TransferServiceTest extends UnitTest {

  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private EntryRepository entryRepository;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks private TransferService transferService;

  @BeforeEach
  void setup() {
    lenient()
        .doAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(null);
            })
        .when(transactionTemplate)
        .execute(any());
  }

  @Test
  @DisplayName("Should transfer successfully")
  void transferSuccessfully() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("50.00");

    Wallet origin = new Wallet(originId, "BRL");
    origin.deposit(new BigDecimal("100.00"));
    Wallet dest = new Wallet(destId, "BRL");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(originId)).thenReturn(Optional.of(origin));
    when(walletRepository.loadByIdForUpdate(destId)).thenReturn(Optional.of(dest));

    var either = transferService.transfer(originId, destId, amount, "idem-789");
    Transaction result = either.getRight().get();

    assertEquals(originId, result.getWalletId());
    assertEquals(destId, result.getWalletDestinationId());
    assertEquals(amount, result.getAmount());

    assertEquals(new BigDecimal("50.00"), origin.getBalance());

    verify(walletRepository).save(origin);
    verify(transactionRepository).create(any(Transaction.class));
  }

  @Test
  @DisplayName("Should return error if currencies are different")
  void throwCurrencyMismatch() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();

    Wallet origin = new Wallet(UUID.randomUUID(), "BRL");
    Wallet dest = new Wallet(UUID.randomUUID(), "USD");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(originId)).thenReturn(Optional.of(origin));
    when(walletRepository.loadByIdForUpdate(destId)).thenReturn(Optional.of(dest));

    var result = transferService.transfer(originId, destId, BigDecimal.TEN, "idem");

    assertTrue(result.isLeft());
    assertEquals(WalletErrorCode.CURRENCY_MISMATCH.getCode(), result.getLeft().get().code());
  }

  @Test
  @DisplayName("Should return error if origin has no funds")
  void throwInsufficientBalance() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();

    Wallet origin = new Wallet(UUID.randomUUID(), "BRL"); // balance 0
    Wallet dest = new Wallet(UUID.randomUUID(), "BRL");

    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(originId)).thenReturn(Optional.of(origin));
    when(walletRepository.loadByIdForUpdate(destId)).thenReturn(Optional.of(dest));

    var result = transferService.transfer(originId, destId, BigDecimal.TEN, "idem");

    assertTrue(result.isLeft());
    assertEquals(WalletErrorCode.INSUFFICIENT_BALANCE.getCode(), result.getLeft().get().code());
  }

  @Test
  @DisplayName("Should skip transfer if idempotency key exists")
  void skipIfIdempotencyExists() {
    Transaction existing = mock(Transaction.class);
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.of(existing));

    var result =
        transferService.transfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "idem");

    assertEquals(existing, result.getRight().get());
    verify(walletRepository, never()).loadByIdForUpdate(any());
  }

  @Test
  @DisplayName("Should return error if origin and destination are same")
  void throwSameWallet() {
    UUID walletId = UUID.randomUUID();
    var result = transferService.transfer(walletId, walletId, BigDecimal.TEN, "idem");
    assertTrue(result.isLeft());
    assertEquals(WalletErrorCode.SAME_WALLET_TRANSFER.getCode(), result.getLeft().get().code());
  }

  @Test
  @DisplayName("Should return error if wallet does not exist")
  void throwWalletNotFound() {
    UUID originId = UUID.randomUUID();
    UUID destId = UUID.randomUUID();
    when(transactionRepository.findByWalletIdAndIdempotencyIdAndType(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(walletRepository.loadByIdForUpdate(any())).thenReturn(Optional.empty());

    var result = transferService.transfer(originId, destId, BigDecimal.TEN, "idem");
    assertTrue(result.isLeft());
    assertEquals(WalletErrorCode.WALLET_NOT_FOUND.getCode(), result.getLeft().get().code());
  }
}
