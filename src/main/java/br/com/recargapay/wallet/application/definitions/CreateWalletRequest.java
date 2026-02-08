package br.com.recargapay.wallet.application.definitions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for creating a new wallet")
public record CreateWalletRequest(
    @Schema(
            description = "ISO 4217 currency code",
            example = "BRL",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3)
        String currency) {}
