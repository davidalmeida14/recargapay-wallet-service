package br.com.recargapay.wallet.infrastructure.persistence.transaction;

import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.transaction.model.Type;
import br.com.recargapay.wallet.domain.transaction.repository.TransactionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

interface TransactionJpaRepository extends JpaRepository<Transaction, UUID> {

  Optional<Transaction> findByWalletIdAndIdempotencyIdAndType(
      UUID walletId, String idempotencyId, Type type);
}

@Component
public class TransactionDao implements TransactionRepository {

  private final TransactionJpaRepository transactionJpaRepository;

  public TransactionDao(TransactionJpaRepository transactionJpaRepository) {
    this.transactionJpaRepository = transactionJpaRepository;
  }

  @Override
  public void create(Transaction transaction) {
    transactionJpaRepository.save(transaction);
  }

  @Override
  public Optional<Transaction> findByWalletIdAndIdempotencyIdAndType(
      UUID walletId, String idempotencyId, Type type) {
    return transactionJpaRepository.findByWalletIdAndIdempotencyIdAndType(
        walletId, idempotencyId, type);
  }

  @Override
  public void update(Transaction transaction) {
    transactionJpaRepository.save(transaction);
  }
}
