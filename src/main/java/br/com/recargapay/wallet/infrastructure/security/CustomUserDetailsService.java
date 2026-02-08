package br.com.recargapay.wallet.infrastructure.security;

import br.com.recargapay.wallet.domain.customer.repository.CustomerRepository;
import java.util.Collections;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final CustomerRepository customerRepository;

  public CustomUserDetailsService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return customerRepository
        .findByEmail(email)
        .map(
            customer ->
                new User(customer.getEmail(), customer.getPassword(), Collections.emptyList()))
        .orElseThrow(
            () -> new UsernameNotFoundException("Customer not found with email: " + email));
  }
}
