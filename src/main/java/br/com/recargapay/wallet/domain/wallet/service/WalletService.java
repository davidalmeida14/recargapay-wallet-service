package br.com.recargapay.wallet.domain.wallet.service;

import br.com.recargapay.wallet.domain.transaction.model.FinancialType;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import br.com.recargapay.wallet.domain.wallet.exception.WalletNotFoundException;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WalletService {
  private final WalletRepository walletRepository;
  private final EntryRepository entryRepository;
  private final TransactionTemplate transactionTemplate;

  public WalletService(WalletRepository walletRepository, EntryRepository entryRepository, TransactionTemplate transactionTemplate) {
    this.walletRepository = walletRepository;
    this.entryRepository = entryRepository;
    this.transactionTemplate = transactionTemplate;
  }

  public Wallet createWallet(UUID customerId, String currency) {
    var wallets = walletRepository.listByCustomerIdAndCurrency(customerId, currency);
    if (!wallets.isEmpty()) {
      return wallets.getFirst();
    }

    var wallet = new Wallet(customerId, currency);

    transactionTemplate.executeWithoutResult(
        status -> {
          walletRepository.add(wallet);
        });

    return wallet;
  }

  public BigDecimal retrieveBalance(UUID walletId) {
    return walletRepository
        .loadByIdForUpdate(walletId)
        .map(Wallet::getBalance)
        .orElseThrow(() -> new WalletNotFoundException(walletId));
  }

  public BigDecimal retrieveHistoricalBalance(UUID walletId, OffsetDateTime at) {
    if (!walletRepository.findById(walletId).isPresent()) {
      throw new WalletNotFoundException(walletId);
    }
    var entries = entryRepository.findByWalletIdAndCreatedAtBeforeOrderByCreatedAtAsc(walletId, at);
    return entries.stream()
        .map(
            e ->
                e.getFinancialType() == FinancialType.CREDIT
                    ? e.getAmount()
                    : e.getAmount().negate())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Wallet retrieveDefaultWallet(UUID customerId) {
    var wallets = walletRepository.findByCustomerId(customerId);
    if (wallets.isEmpty()) {
      return createWallet(customerId, "BRL");
    }
    // Return first wallet for now, as we only support one for this challenge
    return wallets.getFirst();
  }
}
