package br.com.recargapay.wallet.domain.wallet.service;

import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.CREDIT;
import static br.com.recargapay.wallet.domain.transaction.model.FinancialType.DEBIT;
import static br.com.recargapay.wallet.domain.transaction.model.Status.PENDING;
import static br.com.recargapay.wallet.domain.transaction.model.Type.TRANSFER;
import static br.com.recargapay.wallet.infrastructure.common.UUIDGenerator.generate;

import br.com.recargapay.wallet.domain.transaction.event.TransferCreditPendingEvent;
import br.com.recargapay.wallet.domain.transaction.exception.TransactionNotFoundException;
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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class TransferService {
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final EntryRepository entryRepository;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher applicationEventPublisher;

  public TransferService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      EntryRepository entryRepository,
      TransactionTemplate transactionTemplate,
      ApplicationEventPublisher applicationEventPublisher) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.entryRepository = entryRepository;
    this.transactionTemplate = transactionTemplate;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public Either<Error, Transaction> transfer(
      @NonNull UUID originWalletId,
      @NonNull UUID destinationWalletId,
      @NonNull BigDecimal amount,
      @NonNull String idempotencyId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return Either.left(
          Error.of(
              WalletErrorCode.INVALID_AMOUNT.getCode(),
              WalletErrorCode.INVALID_AMOUNT.getMessage()));
    }

    if (originWalletId.equals(destinationWalletId)) {
      return Either.left(
          Error.of(
              WalletErrorCode.SAME_WALLET_TRANSFER.getCode(),
              WalletErrorCode.SAME_WALLET_TRANSFER.getMessage()));
    }

    var existing =
        transactionRepository.findByWalletIdAndIdempotencyIdAndType(
            originWalletId, idempotencyId, TRANSFER);
    if (existing.isPresent()) {
      return Either.right(existing.get());
    }

    return transactionTemplate.execute(
        status -> {
          var originWalletOpt = walletRepository.loadByIdForUpdate(originWalletId);
          if (originWalletOpt.isEmpty()) {
            return Either.left(
                Error.of(WalletErrorCode.WALLET_NOT_FOUND.getCode(), "Origin wallet not found"));
          }
          Wallet originWallet = originWalletOpt.get();

          var destinationWalletOpt = walletRepository.loadByIdForUpdate(destinationWalletId);
          if (destinationWalletOpt.isEmpty()) {
            return Either.left(
                Error.of(
                    WalletErrorCode.WALLET_NOT_FOUND.getCode(), "Destination wallet not found"));
          }
          Wallet destinationWallet = destinationWalletOpt.get();

          if (!originWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            return Either.left(
                Error.of(
                    WalletErrorCode.CURRENCY_MISMATCH.getCode(),
                    WalletErrorCode.CURRENCY_MISMATCH.getMessage()));
          }

          if (originWallet.getBalance().compareTo(amount) < 0) {
            return Either.left(
                Error.of(
                    WalletErrorCode.INSUFFICIENT_BALANCE.getCode(),
                    WalletErrorCode.INSUFFICIENT_BALANCE.getMessage()));
          }

          Transaction transaction =
              createPending(originWalletId, destinationWalletId, amount, idempotencyId);

          transactionRepository.create(transaction);

          entryRepository.create(createDebitEntry(transaction));

          originWallet.withdraw(amount);
          walletRepository.save(originWallet);

          applicationEventPublisher.publishEvent(
              new TransferCreditPendingEvent(transaction.getId()));

          return Either.right(transaction);
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

  public void processDestinationCredit(UUID transactionId) {

    Transaction transaction =
        transactionRepository
            .loadById(transactionId)
            .orElseThrow(
                () -> {
                  log.error("Transaction {} not found for processing credit", transactionId);
                  return new TransactionNotFoundException(transactionId);
                });

    if (transaction.isCompleted()) {
      log.warn("Transaction {} already completed.", transactionId);
      return;
    }

    if (!transaction.isTransfer()) {
      log.warn("Transaction {} is not a transfer.", transactionId);
      return;
    }

    transactionTemplate.executeWithoutResult(
        status -> {
          Wallet wallet =
              walletRepository
                  .loadByIdForUpdate(transaction.getWalletDestinationId())
                  .orElseThrow();
          entryRepository.create(createCreditEntry(transaction));
          wallet.deposit(transaction.getAmount());
          walletRepository.save(wallet);
          transaction.processed();
          transactionRepository.update(transaction);
        });
  }
}
