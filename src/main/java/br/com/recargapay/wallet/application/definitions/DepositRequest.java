package br.com.recargapay.wallet.application.definitions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Request object for making a deposit")
public record DepositRequest(
    @Schema(
            description = "Amount to be deposited",
            example = "100.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount) {}
