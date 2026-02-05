package br.com.recargapay.wallet.domain.wallet.exception;

import java.util.UUID;

public class CurrencyMismatchException extends RuntimeException {

  private final UUID originWalletId;
  private final UUID destinationWalletId;

  public CurrencyMismatchException(UUID originWalletId, UUID destinationWalletId) {
    super(
        "Transfer requires same currency in both wallets: origin %s, destination %s"
            .formatted(originWalletId, destinationWalletId));
    this.originWalletId = originWalletId;
    this.destinationWalletId = destinationWalletId;
  }

  public UUID getOriginWalletId() {
    return originWalletId;
  }

  public UUID getDestinationWalletId() {
    return destinationWalletId;
  }
}
