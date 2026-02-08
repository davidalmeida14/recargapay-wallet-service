package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.application.definitions.TransactionResponse;
import br.com.recargapay.wallet.application.definitions.TransferRequest;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.TransferService;
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
@Tag(name = "Transfers", description = "Operations related to transferring funds between wallets")
public class TransferController {

  private final TransferService transferService;
  private final WalletService walletService;
  private final SecurityService securityService;

  public TransferController(
      TransferService transferService,
      WalletService walletService,
      SecurityService securityService) {
    this.transferService = transferService;
    this.walletService = walletService;
    this.securityService = securityService;
  }

  @Operation(
      summary = "Transfer funds",
      description =
          "Transfers a specified amount from the authenticated customer's wallet to another wallet. "
              + "Requires an idempotency ID to prevent duplicate transactions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Transfer successful",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient funds",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Destination wallet not found",
            content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - Duplicate transaction (Idempotency)",
            content = @Content)
      })
  @PutMapping("/transfers")
  public ResponseEntity<TransactionResponse> transfer(
      @Parameter(
              description = "Unique ID to ensure idempotency of the request",
              required = true,
              example = "uuid-or-unique-string")
          @RequestHeader(name = Headers.X_IDEMPOTENCY_ID)
          String idempotencyId,
      @Valid @RequestBody TransferRequest request) {

    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet originWallet = walletService.retrieveDefaultWallet(customerId);

    Transaction transaction =
        transferService.transfer(
            originWallet.getId(), request.destinationWalletId(), request.amount(), idempotencyId);

    BigDecimal availableBalance = walletService.retrieveBalance(originWallet.getId());

    return ResponseEntity.ok(TransactionResponse.from(transaction, availableBalance));
  }
}
