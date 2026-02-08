package br.com.recargapay.wallet.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.recargapay.wallet.application.controller.auth.AuthController;
import br.com.recargapay.wallet.application.definitions.auth.RegisterRequest;
import br.com.recargapay.wallet.domain.customer.model.Customer;
import br.com.recargapay.wallet.domain.customer.repository.CustomerRepository;
import br.com.recargapay.wallet.support.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class AuthControllerTest extends UnitTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtEncoder jwtEncoder;

  @InjectMocks private AuthController authController;

  @Test
  @DisplayName("Should register customer successfully")
  void registerSuccessfully() {
    var request = new RegisterRequest("John Doe", "john@example.com", "password123");

    when(customerRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

    var response = authController.register(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(customerRepository).save(any(Customer.class));
  }

  @Test
  @DisplayName("Should return conflict when email already exists")
  void returnConflictWhenEmailExists() {
    var request = new RegisterRequest("John Doe", "john@example.com", "password123");

    when(customerRepository.findByEmail(request.email()))
        .thenReturn(Optional.of(mock(Customer.class)));

    var response = authController.register(request);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    verify(customerRepository, never()).save(any(Customer.class));
  }
}
