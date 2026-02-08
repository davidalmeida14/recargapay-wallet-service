package br.com.recargapay.wallet.infrastructure.persistence.entry;

import br.com.recargapay.wallet.domain.transaction.model.Entry;
import br.com.recargapay.wallet.domain.transaction.repository.EntryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
interface EntryJpaRepository extends JpaRepository<Entry, UUID> {

  List<Entry> findByWalletIdAndCreatedAtBeforeOrderByCreatedAtAsc(UUID walletId, OffsetDateTime at);
}

@Component
public class EntryDao implements EntryRepository {

  private final EntryJpaRepository entryJpaRepository;

  public EntryDao(EntryJpaRepository entryJpaRepository) {
    this.entryJpaRepository = entryJpaRepository;
  }

  @Override
  public Entry create(Entry entry) {
    return entryJpaRepository.save(entry);
  }

  @Override
  public List<Entry> create(List<Entry> entries) {
    return entryJpaRepository.saveAll(entries);
  }

  @Override
  public List<Entry> findByWalletIdAndCreatedAtBeforeOrderByCreatedAtAsc(
      UUID walletId, OffsetDateTime at) {
    return entryJpaRepository.findByWalletIdAndCreatedAtBeforeOrderByCreatedAtAsc(walletId, at);
  }
}
