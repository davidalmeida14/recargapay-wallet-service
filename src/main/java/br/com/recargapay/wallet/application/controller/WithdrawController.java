package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.application.definitions.TransactionResponse;
import br.com.recargapay.wallet.application.definitions.WithdrawRequest;
import br.com.recargapay.wallet.domain.wallet.exception.WalletErrorCode;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.domain.wallet.service.WithdrawService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Withdrawals", description = "Operations related to withdrawing funds from the wallet")
public class WithdrawController {

  private final WithdrawService withdrawService;
  private final WalletService walletService;
  private final SecurityService securityService;

  public WithdrawController(
      WithdrawService withdrawService,
      WalletService walletService,
      SecurityService securityService) {
    this.withdrawService = withdrawService;
    this.walletService = walletService;
    this.securityService = securityService;
  }

  @Operation(
      summary = "Withdraw funds",
      description =
          "Withdraws a specified amount from the authenticated customer's default wallet. "
              + "Requires an idempotency ID to prevent duplicate transactions.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Withdrawal successful",
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
  @PutMapping("/withdrawals")
  public ResponseEntity<?> withdraw(
      @Parameter(
              description = "Unique ID to ensure idempotency of the request",
              required = true,
              example = "uuid-or-unique-string")
          @RequestHeader(name = Headers.X_IDEMPOTENCY_ID)
          String idempotencyId,
      @Valid @RequestBody WithdrawRequest request) {

    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.retrieveDefaultWallet(customerId);

    return withdrawService
        .withdraw(wallet.getId(), request.amount(), idempotencyId)
        .fold(
            error -> {
              HttpStatus status =
                  error.code().equals(WalletErrorCode.WALLET_NOT_FOUND.getCode())
                      ? HttpStatus.NOT_FOUND
                      : error.code().equals(WalletErrorCode.CURRENCY_MISMATCH.getCode())
                          ? HttpStatus.UNPROCESSABLE_ENTITY
                          : HttpStatus.BAD_REQUEST;
              return ResponseEntity.status(status).body(error);
            },
            transaction -> {
              BigDecimal availableBalance = walletService.retrieveBalance(wallet.getId());
              return ResponseEntity.ok(TransactionResponse.from(transaction, availableBalance));
            });
  }
}
