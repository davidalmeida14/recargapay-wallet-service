package br.com.recargapay.wallet.application.definitions;

import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateWalletResponse(
    UUID id, UUID customerId, BigDecimal balance, OffsetDateTime createdAt) {
  public static CreateWalletResponse toResponse(Wallet wallet) {
    return new CreateWalletResponse(
        wallet.getId(), wallet.getCustomerId(), wallet.getBalance(), wallet.getCreatedAt());
  }
}
