package br.com.recargapay.wallet.domain.wallet.service;

import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.DEBIT;
import static br.com.recargapay.wallet.domain.transaction.model.Status.PENDING;
import static br.com.recargapay.wallet.domain.transaction.model.Type.WITHDRAWAL;
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
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WithdrawService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final EntryRepository entryRepository;
  private final TransactionTemplate transactionTemplate;

  public WithdrawService(WalletRepository walletRepository, TransactionRepository transactionRepository, EntryRepository entryRepository, TransactionTemplate transactionTemplate) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.entryRepository = entryRepository;
    this.transactionTemplate = transactionTemplate;
  }

  public Transaction withdraw(
      @NonNull UUID walletId, @NonNull BigDecimal amount, @NonNull String idempotencyId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Withdraw amount must be greater than zero.");
    }

    var existing =
        transactionRepository.findByWalletIdAndIdempotencyIdAndType(
            walletId, idempotencyId, WITHDRAWAL);
    if (existing.isPresent()) {
      return existing.get();
    }

    Transaction withdrawTransaction =
        transactionTemplate.execute(
            status -> {
              Wallet wallet =
                  walletRepository
                      .loadByIdForUpdate(walletId)
                      .orElseThrow(() -> new WalletNotFoundException(walletId));

              wallet.withdraw(amount);

              Transaction transaction = createPending(walletId, amount, idempotencyId);
              transactionRepository.create(transaction);
              entryRepository.create(createEntry(transaction));

              walletRepository.add(wallet);

              transaction.processed();
              transactionRepository.update(transaction);

              return transaction;
            });

    return withdrawTransaction;
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
