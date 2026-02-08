package br.com.recargapay.wallet.domain.wallet.exception;

import lombok.Getter;

@Getter
public enum WalletErrorCode {
  WALLET_NOT_FOUND("W:001", "Wallet not found"),
  INSUFFICIENT_BALANCE("W:002", "Insufficient balance"),
  CURRENCY_MISMATCH("W:003", "Currency mismatch between wallets"),
  INVALID_AMOUNT("W:004", "Amount must be greater than zero"),
  SAME_WALLET_TRANSFER("W:005", "Origin and destination wallets must be different");

  private final String code;
  private final String message;

  WalletErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
