package br.com.recargapay.wallet.domain.customer.repository;

import br.com.recargapay.wallet.domain.customer.model.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
  Customer save(Customer customer);
  Optional<Customer> findById(UUID id);
  Optional<Customer> findByEmail(String email);
}
