package br.com.recargapay.wallet.domain.transaction.vo;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record DepositInput(
    @NonNull UUID walletId,
    @NonNull BigDecimal amount,
    @NonNull String idempotencyId,
    @Nullable String idempotencyScope) {}
