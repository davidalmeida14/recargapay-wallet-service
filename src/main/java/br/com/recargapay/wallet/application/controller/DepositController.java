package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.application.definitions.DepositRequest;
import br.com.recargapay.wallet.application.definitions.TransactionResponse;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.DepositService;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.infrastructure.security.SecurityService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
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

  @PutMapping("/deposits")
  public ResponseEntity<TransactionResponse> deposit(
      @RequestHeader(name = Headers.X_IDEMPOTENCY_ID) String idempotencyId,
      @Valid @RequestBody DepositRequest depositRequest) {

    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.retrieveDefaultWallet(customerId);

    Transaction depositTransaction =
        depositService.deposit(wallet.getId(), depositRequest.amount(), idempotencyId);

    BigDecimal availableBalance = walletService.retrieveBalance(wallet.getId());

    return ResponseEntity.ok(TransactionResponse.from(depositTransaction, availableBalance));
  }
}
