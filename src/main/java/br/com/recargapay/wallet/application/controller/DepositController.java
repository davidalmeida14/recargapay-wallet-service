package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.application.definitions.DepositRequest;
import br.com.recargapay.wallet.application.definitions.TransactionResponse;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.DepositService;
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
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Deposits", description = "Operations related to adding funds to the wallet")
public class DepositController {

  private final DepositService depositService;
  private final WalletService walletService;
  private final SecurityService securityService;

  public DepositController(
      DepositService depositService, WalletService walletService, SecurityService securityService) {
    this.depositService = depositService;
    this.walletService = walletService;
    this.securityService = securityService;
  }

  @Operation(
      summary = "Deposit funds",
      description =
          "Deposits a specified amount into the authenticated customer's default wallet. "
              + "Requires an idempotency ID to prevent duplicate transactions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Deposit successful",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient funds",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - Duplicate transaction (Idempotency)",
            content = @Content)
      })
  @PutMapping("/deposits")
  public ResponseEntity<TransactionResponse> deposit(
      @Parameter(
              description = "Unique ID to ensure idempotency of the request",
              required = true,
              example = "uuid-or-unique-string")
          @RequestHeader(name = Headers.X_IDEMPOTENCY_ID)
          String idempotencyId,
      @Valid @RequestBody DepositRequest depositRequest) {

    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.retrieveDefaultWallet(customerId);

    Transaction depositTransaction =
        depositService.deposit(wallet.getId(), depositRequest.amount(), idempotencyId);

    BigDecimal availableBalance = walletService.retrieveBalance(wallet.getId());

    return ResponseEntity.ok(TransactionResponse.from(depositTransaction, availableBalance));
  }
}
