package br.com.recargapay.wallet.domain.transaction.repository;

import br.com.recargapay.wallet.domain.transaction.model.Entry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EntryRepository {

  Entry create(Entry entry);

  List<Entry> create(List<Entry> entries);

  List<Entry> findByWalletIdAndCreatedAtBeforeOrderByCreatedAtAsc(UUID walletId, OffsetDateTime at);
}
