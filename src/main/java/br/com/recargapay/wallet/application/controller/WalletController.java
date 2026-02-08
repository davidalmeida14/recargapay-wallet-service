package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.definitions.CreateWalletRequest;
import br.com.recargapay.wallet.application.definitions.CreateWalletResponse;
import br.com.recargapay.wallet.application.definitions.WalletBalanceDefinition;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.infrastructure.security.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Operations related to wallet management")
public class WalletController {

  private final WalletService walletService;
  private final SecurityService securityService;

  public WalletController(WalletService walletService, SecurityService securityService) {
    this.walletService = walletService;
    this.securityService = securityService;
  }

  @Operation(
      summary = "Create a new wallet",
      description =
          "Creates a new wallet for the authenticated customer with the specified currency.",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Wallet created successfully",
            content = @Content(schema = @Schema(implementation = CreateWalletResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
      })
  @PutMapping
  public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest request) {
    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.createWallet(customerId, request.currency());
    return ResponseEntity.created(resolveLocation(wallet))
        .body(CreateWalletResponse.toResponse(wallet));
  }

  @Operation(
      summary = "Get wallet balance",
      description =
          "Retrieves the current balance or the historical balance at a specific point in time for the authenticated customer's default wallet.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Balance retrieved successfully",
            content = @Content(schema = @Schema(implementation = WalletBalanceDefinition.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
      })
  @GetMapping("/balance")
  public ResponseEntity<WalletBalanceDefinition> getBalance(
      @Parameter(
              description = "Optional timestamp to retrieve historical balance (ISO 8601 format)",
              example = "2023-10-27T10:00:00Z")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime at) {
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
