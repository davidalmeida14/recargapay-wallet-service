package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.application.definitions.TransactionResponse;
import br.com.recargapay.wallet.application.definitions.WithdrawRequest;
import br.com.recargapay.wallet.domain.transaction.model.Transaction;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.domain.wallet.service.WithdrawService;
import br.com.recargapay.wallet.infrastructure.security.SecurityService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
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

  @PutMapping("/withdrawals")
  public ResponseEntity<TransactionResponse> withdraw(
      @RequestHeader(name = Headers.X_IDEMPOTENCY_ID) String idempotencyId,
      @Valid @RequestBody WithdrawRequest request) {

    UUID customerId = securityService.getAuthenticatedCustomerId();
    Wallet wallet = walletService.retrieveDefaultWallet(customerId);

    Transaction transaction =
        withdrawService.withdraw(wallet.getId(), request.amount(), idempotencyId);

    BigDecimal availableBalance = walletService.retrieveBalance(wallet.getId());

    return ResponseEntity.ok(TransactionResponse.from(transaction, availableBalance));
  }
}
