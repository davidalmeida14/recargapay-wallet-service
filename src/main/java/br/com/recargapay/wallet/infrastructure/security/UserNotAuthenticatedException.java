package br.com.recargapay.wallet.infrastructure.security;

public class UserNotAuthenticatedException extends RuntimeException {
  public UserNotAuthenticatedException() {
    super("User not authenticated");
  }
}
