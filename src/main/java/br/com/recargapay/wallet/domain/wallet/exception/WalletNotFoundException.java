package br.com.recargapay.wallet.domain.wallet.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

  private final UUID walletId;

  public WalletNotFoundException(String message) {
    super(message);
    this.walletId = null;
  }

  public WalletNotFoundException(String message, Throwable cause) {
    super(message, cause);
    this.walletId = null;
  }

  public WalletNotFoundException(UUID walletId) {
    super("Wallet not found for id " + walletId);
    this.walletId = walletId;
  }

  public UUID getWalletId() {
    return walletId;
  }
}
