package br.com.recargapay.wallet.application.controller.auth;

import br.com.recargapay.wallet.application.definitions.auth.LoginRequest;
import br.com.recargapay.wallet.application.definitions.auth.LoginResponse;
import br.com.recargapay.wallet.application.definitions.auth.RegisterRequest;
import br.com.recargapay.wallet.domain.customer.model.Customer;
import br.com.recargapay.wallet.domain.customer.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Authentication",
    description = "Operations related to customer authentication and registration")
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

  @Operation(
      summary = "Customer login",
      description = "Authenticates a customer and returns a JWT access token.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
      })
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

  @Operation(
      summary = "Register a new customer",
      description = "Creates a new customer account with the provided details.",
      responses = {
        @ApiResponse(responseCode = "201", description = "Customer registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
      })
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
