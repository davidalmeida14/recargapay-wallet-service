package br.com.recargapay.wallet.application.controller.auth;

import br.com.recargapay.wallet.application.definitions.auth.LoginRequest;
import br.com.recargapay.wallet.application.definitions.auth.LoginResponse;
import br.com.recargapay.wallet.application.definitions.auth.RegisterRequest;
import br.com.recargapay.wallet.domain.customer.model.Customer;
import br.com.recargapay.wallet.domain.customer.repository.CustomerRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

  public static final String ISSUER = "wallet-platform";
  public static final String CUSTOMER_CLAIM = "email";
  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;

  public AuthController(
      CustomerRepository customerRepository,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder) {
    this.customerRepository = customerRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
  }

  @PostMapping("/authentication")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    Customer customer =
        customerRepository
            .findByEmail(loginRequest.email())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

    if (!passwordEncoder.matches(loginRequest.password(), customer.getPassword())) {
      throw new RuntimeException("Invalid credentials");
    }

    Instant now = Instant.now();
    long expiry = 36000L;

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(customer.getId().toString())
            .claim(CUSTOMER_CLAIM, customer.getEmail())
            .build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    return ResponseEntity.ok(new LoginResponse(token));
  }

  @PostMapping("/customers")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
    if (customerRepository.findByEmail(registerRequest.email()).isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    Customer customer =
        new Customer(
            registerRequest.fullName(),
            registerRequest.email(),
            passwordEncoder.encode(registerRequest.password()));

    customerRepository.save(customer);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
