package br.com.recargapay.wallet.application.definitions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request object for transferring funds between wallets")
public record TransferRequest(
    @Schema(
            description = "Unique identifier of the destination wallet",
            example = "550e8400-e29b-41d4-a716-446655440002",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "destinationWalletId is required")
        UUID destinationWalletId,
    @Schema(
            description = "Amount to be transferred",
            example = "50.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount) {}
