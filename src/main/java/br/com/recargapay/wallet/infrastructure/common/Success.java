package br.com.recargapay.wallet.infrastructure.common;

public record Success<T>(T data, String message) {

  public Success(T data) {
    this(data, null);
  }

  public static <T> Success<T> of(T data) {
    return new Success<>(data);
  }

  public static <T> Success<T> of(T data, String message) {
    return new Success<>(data, message);
  }

  public static Success<Void> empty() {
    return new Success<>(null);
  }

  public static Success<Void> empty(String message) {
    return new Success<>(null, message);
  }
}
