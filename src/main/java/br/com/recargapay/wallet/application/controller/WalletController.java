package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.definitions.CreateWalletRequest;
import br.com.recargapay.wallet.application.definitions.CreateWalletResponse;
import br.com.recargapay.wallet.application.definitions.WalletBalanceDefinition;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.infrastructure.security.SecurityService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

  private final WalletService walletService;
  private final SecurityService securityService;

  public WalletController(WalletService walletService, SecurityService securityService) {
    this.walletService = walletService;
    this.securityService = securityService;
  }

  @PutMapping
  public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest request) {
    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.createWallet(customerId, request.currency());
    return ResponseEntity.created(resolveLocation(wallet))
        .body(CreateWalletResponse.toResponse(wallet));
  }

  @GetMapping("/balance")
  public ResponseEntity<WalletBalanceDefinition> getBalance(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime at) {
    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.retrieveDefaultWallet(customerId);
    BigDecimal balance =
        at == null
            ? walletService.retrieveBalance(wallet.getId())
            : walletService.retrieveHistoricalBalance(wallet.getId(), at);
    return ResponseEntity.ok(new WalletBalanceDefinition(balance));
  }

  private static @NonNull URI resolveLocation(Wallet wallet) {
    return UriComponentsBuilder.fromPath("/api/v1/wallets/{id}")
        .buildAndExpand(wallet.getId())
        .toUri();
  }
}
