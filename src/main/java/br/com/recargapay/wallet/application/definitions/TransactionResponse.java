package br.com.recargapay.wallet.application.definitions;

import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Response object representing a transaction and the updated balance")
public record TransactionResponse(
    @Schema(
            description = "Unique identifier of the transaction",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
    @Schema(
            description = "Unique identifier of the wallet",
            example = "550e8400-e29b-41d4-a716-446655440001")
        UUID walletId,
    @Schema(description = "Amount of the transaction", example = "100.00") BigDecimal amount,
    @Schema(description = "Current available balance after the transaction", example = "1050.50")
        BigDecimal availableBalance,
    @Schema(description = "Timestamp when the transaction was created") OffsetDateTime createdAt) {

  public static TransactionResponse from(Transaction transaction, BigDecimal availableBalance) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getWalletId(),
        transaction.getAmount(),
        availableBalance,
        transaction.getCreatedAt());
  }
}
