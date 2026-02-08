package br.com.recargapay.wallet.domain.transaction.repository;

import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.transaction.model.Type;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

  void create(Transaction transaction);

  Optional<Transaction> findByWalletIdAndIdempotencyIdAndType(
      UUID walletId, String idempotencyId, Type type);

  void update(Transaction transaction);

  Optional<Transaction> loadById(UUID transactionId);
}
