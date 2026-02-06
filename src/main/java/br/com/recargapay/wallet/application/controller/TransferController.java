package br.com.recargapay.wallet.application.controller;

import br.com.recargapay.wallet.application.Headers;
import br.com.recargapay.wallet.application.definitions.TransferRequest;
import br.com.recargapay.wallet.domain.wallet.model.Wallet;
import br.com.recargapay.wallet.domain.wallet.service.TransferService;
import br.com.recargapay.wallet.domain.wallet.service.WalletService;
import br.com.recargapay.wallet.infrastructure.security.SecurityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")

public class TransferController {

    private final TransferService transferService;
    private final WalletService walletService;
    private final SecurityService securityService;

    public TransferController(TransferService transferService, WalletService walletService, SecurityService securityService) {
        this.transferService = transferService;
        this.walletService = walletService;
        this.securityService = securityService;
    }

    @PutMapping("/transfers")
    public ResponseEntity<Void> transfer(
            @RequestHeader(name = Headers.X_IDEMPOTENCY_ID) String idempotencyId,
            @Valid @RequestBody TransferRequest request) {

        UUID customerId = securityService.getAuthenticatedCustomerId();
        Wallet originWallet = walletService.retrieveDefaultWallet(customerId);

        transferService.transfer(
                originWallet.getId(),
                request.destinationWalletId(),
                request.amount(),
                idempotencyId);

        return ResponseEntity.ok().build();
    }
}
