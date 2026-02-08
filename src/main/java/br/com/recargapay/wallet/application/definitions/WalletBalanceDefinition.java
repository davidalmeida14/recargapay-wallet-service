package br.com.recargapay.wallet.application.definitions;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Response object representing the wallet balance")
public record WalletBalanceDefinition(
    @Schema(description = "Current or historical balance of the wallet", example = "1050.50")
        BigDecimal balance) {}
