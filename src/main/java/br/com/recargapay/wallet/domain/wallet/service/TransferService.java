package br.com.recargapay.wallet.domain.wallet.service;

import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.CREDIT;
import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.DEBIT;
import static br.com.recargapay.wallet.domain.transaction.model.Status.PENDING;
import static br.com.recargapay.wallet.domain.transaction.model.Type.TRANSFER;
import static br.com.recargapay.wallet.infrastructure.common.UUIDGenerator.generate;

import br.com.recargapay.wallet.domain.transaction.model.Entry;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.CurrencyMismatchException;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransferService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final EntryRepository entryRepository;
  private final TransactionTemplate transactionTemplate;

  public TransferService(WalletRepository walletRepository, TransactionRepository transactionRepository, EntryRepository entryRepository, TransactionTemplate transactionTemplate) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.entryRepository = entryRepository;
    this.transactionTemplate = transactionTemplate;
  }

  public void transfer(
      @NonNull UUID originWalletId,
      @NonNull UUID destinationWalletId,
      @NonNull BigDecimal amount,
      @NonNull String idempotencyId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be greater than zero.");
    }

    if (originWalletId.equals(destinationWalletId)) {
      throw new IllegalArgumentException("Origin and destination wallets must be different.");
    }

    var existing =
        transactionRepository.findByWalletIdAndIdempotencyIdAndType(
            originWalletId, idempotencyId, TRANSFER);
    if (existing.isPresent()) {
      return;
    }

    transactionTemplate.executeWithoutResult(
        status -> {
          // Lock wallets in deterministic order to avoid deadlocks

          Wallet originWallet =
              walletRepository
                  .loadByIdForUpdate(originWalletId)
                  .orElseThrow(() -> new WalletNotFoundException(originWalletId));

          Wallet destinationWallet =
              walletRepository
                  .loadByIdForUpdate(destinationWalletId)
                  .orElseThrow(() -> new WalletNotFoundException(destinationWalletId));

          if (!originWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            throw new CurrencyMismatchException(originWalletId, destinationWalletId);
          }

          originWallet.withdraw(amount);
          destinationWallet.deposit(amount);

          Transaction transaction =
              createPending(originWalletId, destinationWalletId, amount, idempotencyId);

          transactionRepository.create(transaction);

          entryRepository.create(
              List.of(createDebitEntry(transaction), createCreditEntry(transaction)));

          walletRepository.add(originWallet);
          walletRepository.add(destinationWallet);

          transaction.processed();
          transactionRepository.update(transaction);
        });
  }

  private @NonNull Entry createDebitEntry(Transaction transaction) {
    return new Entry(
        generate(),
        transaction.getWalletId(),
        transaction.getId(),
        transaction.getAmount(),
        DEBIT,
        OffsetDateTime.now());
  }

  private @NonNull Entry createCreditEntry(Transaction transaction) {
    return new Entry(
        generate(),
        transaction.getWalletDestinationId(),
        transaction.getId(),
        transaction.getAmount(),
        CREDIT,
        OffsetDateTime.now());
  }

  private @NonNull Transaction createPending(
      UUID originWalletId, UUID destinationWalletId, BigDecimal amount, String idempotencyId) {
    return new Transaction(
        generate(),
        originWalletId,
        destinationWalletId,
        idempotencyId,
        amount,
        TRANSFER,
        PENDING,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }
}
