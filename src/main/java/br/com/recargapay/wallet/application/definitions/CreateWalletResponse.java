package br.com.recargapay.wallet.application.definitions;

import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Response object for a newly created wallet")
public record CreateWalletResponse(
    @Schema(
            description = "Unique identifier of the wallet",
            example = "550e8400-e29b-41d4-a716-446655440001")
        UUID id,
    @Schema(
            description = "Unique identifier of the customer who owns the wallet",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID customerId,
    @Schema(description = "Initial balance of the wallet", example = "0.00") BigDecimal balance,
    @Schema(description = "Timestamp when the wallet was created") OffsetDateTime createdAt) {
  public static CreateWalletResponse toResponse(Wallet wallet) {
    return new CreateWalletResponse(
        wallet.getId(), wallet.getCustomerId(), wallet.getBalance(), wallet.getCreatedAt());
  }
}
