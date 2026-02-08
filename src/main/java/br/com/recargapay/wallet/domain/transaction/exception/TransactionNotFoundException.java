package br.com.recargapay.wallet.domain.transaction.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

  public TransactionNotFoundException(UUID transactionId) {
    super("Transaction not found for id " + transactionId);
  }
}
