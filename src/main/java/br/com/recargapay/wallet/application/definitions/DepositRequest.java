package br.com.recargapay.wallet.application.definitions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
    @NotNull(message = "amount is required") @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount) {}
