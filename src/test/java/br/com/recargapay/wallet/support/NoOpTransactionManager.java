package br.com.recargapay.wallet.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * PlatformTransactionManager que não persiste transações. Usado em testes unitários para que o
 * TransactionTemplate execute o callback sem reflexão, compatível com JDK 25 (lambdas).
 */
public final class NoOpTransactionManager implements PlatformTransactionManager {

  @Override
  public TransactionStatus getTransaction(
      org.springframework.transaction.TransactionDefinition definition) {
    return new SimpleTransactionStatus();
  }

  @Override
  public void commit(TransactionStatus status) {}

  @Override
  public void rollback(TransactionStatus status) {}
}
