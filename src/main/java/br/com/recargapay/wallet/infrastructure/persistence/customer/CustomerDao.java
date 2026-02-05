package br.com.recargapay.wallet.infrastructure.persistence.customer;

import br.com.recargapay.wallet.domain.customer.model.Customer;
import br.com.recargapay.wallet.domain.customer.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface CustomerJpaRepository extends JpaRepository<Customer, UUID> {
  Optional<Customer> findByEmail(String email);
}

@Repository
public class CustomerDao implements CustomerRepository {

  private final CustomerJpaRepository repository;

  public CustomerDao(CustomerJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Customer save(Customer customer) {
    return repository.save(customer);
  }

  @Override
  public Optional<Customer> findById(UUID id) {
    return repository.findById(id);
  }

  @Override
  public Optional<Customer> findByEmail(String email) {
    return repository.findByEmail(email);
  }
}
