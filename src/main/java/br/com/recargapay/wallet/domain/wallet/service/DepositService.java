package br.com.recargapay.wallet.domain.wallet.service;

import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.CREDIT;
import static br.com.recargapay.wallet.domain.transaction.model.Status.PENDING;
import static br.com.recargapay.wallet.domain.transaction.model.Type.DEPOSIT;
import static br.com.recargapay.wallet.infrastructure.common.UUIDGenerator.generate;

import br.com.recargapay.wallet.domain.transaction.model.Entry;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DepositService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final EntryRepository entryRepository;
  private final TransactionTemplate transactionTemplate;

  public DepositService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      EntryRepository entryRepository,
      TransactionTemplate transactionTemplate) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.entryRepository = entryRepository;
    this.transactionTemplate = transactionTemplate;
  }

  public Transaction deposit(
      @NonNull UUID walletId, @NonNull BigDecimal amount, @NonNull String idempotencyId) {

    validateAmount(amount);

    var existing =
        transactionRepository.findByWalletIdAndIdempotencyIdAndType(
            walletId, idempotencyId, DEPOSIT);
    if (existing.isPresent()) {
      return existing.get();
    }

    Transaction deposit =
        transactionTemplate.execute(
            status -> {
              Wallet wallet =
                  walletRepository
                      .loadByIdForUpdate(walletId)
                      .orElseThrow(() -> new WalletNotFoundException(walletId));
              Transaction transaction = createPending(walletId, amount, idempotencyId);
              transactionRepository.create(transaction);
              entryRepository.create(createEntry(transaction));
              wallet.deposit(amount);
              walletRepository.save(wallet);
              transaction.processed();
              transactionRepository.update(transaction);
              return transaction;
            });

    return deposit;
  }

  private void validateAmount(@NonNull BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Deposit amount must be greater than zero.");
    }
  }

  private @NonNull Entry createEntry(Transaction transaction) {
    return new Entry(
        generate(),
        transaction.getWalletId(),
        transaction.getId(),
        transaction.getAmount(),
        CREDIT,
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
        DEPOSIT,
        PENDING,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }
}
