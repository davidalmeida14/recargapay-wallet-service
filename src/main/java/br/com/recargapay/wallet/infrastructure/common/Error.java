package br.com.recargapay.wallet.infrastructure.common;

public record Error(String code, String message, String details) {

  public Error(String code, String message) {
    this(code, message, null);
  }

  public static Error of(String code, String message) {
    return new Error(code, message);
  }

  public static Error of(String code, String message, String details) {
    return new Error(code, message, details);
  }
}
