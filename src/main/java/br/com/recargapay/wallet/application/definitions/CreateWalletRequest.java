package br.com.recargapay.wallet.application.definitions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateWalletRequest(
    @NotBlank(message = "currency is required") @Size(min = 3, max = 3) String currency) {}
