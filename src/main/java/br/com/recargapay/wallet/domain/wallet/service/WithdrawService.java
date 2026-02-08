package br.com.recargapay.wallet.domain.wallet.service;

import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.DEBIT;
import static br.com.recargapay.wallet.domain.transaction.model.Status.PENDING;
import static br.com.recargapay.wallet.domain.transaction.model.Type.WITHDRAWAL;
import static br.com.recargapay.wallet.infrastructure.common.UUIDGenerator.generate;

import br.com.recargapay.wallet.domain.transaction.model.Entry;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.WalletErrorCode;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import br.com.recargapay.wallet.infrastructure.common.Either;
import br.com.recargapay.wallet.infrastructure.common.Error;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WithdrawService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final EntryRepository entryRepository;
  private final TransactionTemplate transactionTemplate;

  public WithdrawService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      EntryRepository entryRepository,
      TransactionTemplate transactionTemplate) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.entryRepository = entryRepository;
    this.transactionTemplate = transactionTemplate;
  }

  public Either<Error, Transaction> withdraw(
      @NonNull UUID walletId, @NonNull BigDecimal amount, @NonNull String idempotencyId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return Either.left(
          Error.of(
              WalletErrorCode.INVALID_AMOUNT.getCode(),
              WalletErrorCode.INVALID_AMOUNT.getMessage()));
    }

    var existing =
        transactionRepository.findByWalletIdAndIdempotencyIdAndType(
            walletId, idempotencyId, WITHDRAWAL);
    if (existing.isPresent()) {
      return Either.right(existing.get());
    }

    return transactionTemplate.execute(
        status -> {
          var walletOpt = walletRepository.loadByIdForUpdate(walletId);
          if (walletOpt.isEmpty()) {
            return Either.left(
                Error.of(
                    WalletErrorCode.WALLET_NOT_FOUND.getCode(),
                    WalletErrorCode.WALLET_NOT_FOUND.getMessage()));
          }

          Wallet wallet = walletOpt.get();

          if (wallet.getBalance().compareTo(amount) < 0) {
            return Either.left(
                Error.of(
                    WalletErrorCode.INSUFFICIENT_BALANCE.getCode(),
                    WalletErrorCode.INSUFFICIENT_BALANCE.getMessage()));
          }

          wallet.withdraw(amount);

          Transaction transaction = createPending(walletId, amount, idempotencyId);
          transactionRepository.create(transaction);
          entryRepository.create(createEntry(transaction));

          walletRepository.save(wallet);

          transaction.processed();
          transactionRepository.update(transaction);

          return Either.right(transaction);
        });
  }

  private @NonNull Entry createEntry(Transaction transaction) {
    return new Entry(
        generate(),
        transaction.getWalletId(),
        transaction.getId(),
        transaction.getAmount(),
        DEBIT,
        OffsetDateTime.now());
  }

  private @NonNull Transaction createPending(
      UUID walletId, BigDecimal amount, String idempotencyId) {
    return new Transaction(
        generate(),
        walletId,
        null,
        idempotencyId,
        amount,
        WITHDRAWAL,
        PENDING,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }
}
