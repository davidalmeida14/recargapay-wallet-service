package br.com.recargapay.wallet.domain.wallet.repository;

import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {

  Optional<Wallet> loadByIdForUpdate(UUID walletId);

  Optional<Wallet> findById(UUID walletId);

  Wallet save(Wallet wallet);

  void creditBalance(UUID walletId, BigDecimal amount);

  List<Wallet> listByCustomerIdAndCurrency(UUID customerId, String currency);

  List<Wallet> findByCustomerId(UUID customerId);
}
