package br.com.recargapay.wallet.application.definitions;

import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID walletId,
    BigDecimal amount,
    BigDecimal availableBalance,
    OffsetDateTime createdAt) {

  public static TransactionResponse from(Transaction transaction, BigDecimal availableBalance) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getWalletId(),
        transaction.getAmount(),
        availableBalance,
        transaction.getCreatedAt());
  }
}
