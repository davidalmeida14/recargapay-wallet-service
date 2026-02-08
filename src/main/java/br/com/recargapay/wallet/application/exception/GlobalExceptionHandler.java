package br.com.recargapay.wallet.application.exception;

import br.com.recargapay.wallet.domain.wallet.exception.CurrencyMismatchException;
import br.com.recargapay.wallet.domain.wallet.exception.InsufficientBalanceException;
import br.com.recargapay.wallet.domain.wallet.exception.WalletErrorCode;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(WalletNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
    var body = new java.util.HashMap<String, Object>();
    body.put("code", WalletErrorCode.WALLET_NOT_FOUND.getCode());
    body.put("message", ex.getMessage());
    if (ex.getWalletId() != null) {
      body.put("walletId", ex.getWalletId().toString());
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(InsufficientBalanceException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientBalance(
      InsufficientBalanceException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                "code",
                WalletErrorCode.INSUFFICIENT_BALANCE.getCode(),
                "message",
                WalletErrorCode.INSUFFICIENT_BALANCE.getMessage(),
                "walletId",
                ex.getWalletId().toString(),
                "requested",
                ex.getRequested().toString(),
                "available",
                ex.getAvailable().toString()));
  }

  @ExceptionHandler(CurrencyMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleCurrencyMismatch(CurrencyMismatchException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            Map.of(
                "code",
                WalletErrorCode.CURRENCY_MISMATCH.getCode(),
                "message",
                WalletErrorCode.CURRENCY_MISMATCH.getMessage(),
                "originWalletId",
                ex.getOriginWalletId().toString(),
                "destinationWalletId",
                ex.getDestinationWalletId().toString()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    var errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "ValidationError", "message", "Invalid request", "details", errors));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                "error",
                "MissingRequestHeader",
                "message",
                "Required header '%s' is missing".formatted(ex.getHeaderName())));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "IllegalArgument", "message", ex.getMessage()));
  }
}
