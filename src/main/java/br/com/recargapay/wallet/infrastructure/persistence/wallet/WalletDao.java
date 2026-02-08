package br.com.recargapay.wallet.infrastructure.persistence.wallet;

import br.com.recargapay.wallet.domain.wallet.model.Currency;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
interface WalletJpaRepository extends JpaRepository<Wallet, UUID> {

  @Query(value = "SELECT * FROM wallets WHERE id = :id FOR UPDATE;", nativeQuery = true)
  Optional<Wallet> findByIdForUpdate(UUID id);

  List<Wallet> findAllByCustomerIdAndCurrency(UUID customerId, Currency currency);

  List<Wallet> findAllByCustomerId(UUID customerId);

  @Modifying()
  @Query(
      value = "UPDATE wallets SET balance = balance + :amount WHERE id = :walletId",
      nativeQuery = true)
  void creditBalance(UUID walletId, BigDecimal amount);
}

@Component
public class WalletDao implements WalletRepository {

  private final WalletJpaRepository walletJpaRepository;

  public WalletDao(WalletJpaRepository walletJpaRepository) {
    this.walletJpaRepository = walletJpaRepository;
  }

  @Override
  public Optional<Wallet> loadByIdForUpdate(UUID walletId) {
    return walletJpaRepository.findByIdForUpdate(walletId);
  }

  @Override
  public Optional<Wallet> findById(UUID walletId) {
    return walletJpaRepository.findById(walletId);
  }

  @Override
  public Wallet save(Wallet wallet) {
    return walletJpaRepository.saveAndFlush(wallet);
  }

  @Override
  @Transactional
  public void creditBalance(UUID walletId, BigDecimal amount) {
    walletJpaRepository.creditBalance(walletId, amount);
  }

  @Override
  public List<Wallet> listByCustomerIdAndCurrency(UUID customerId, String currency) {
    return walletJpaRepository.findAllByCustomerIdAndCurrency(
        customerId, Currency.valueOf(currency));
  }

  @Override
  public List<Wallet> findByCustomerId(UUID customerId) {
    return walletJpaRepository.findAllByCustomerId(customerId);
  }
}
