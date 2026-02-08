package br.com.recargapay.wallet.domain.transaction.event;

import java.util.UUID;

public record TransferCreditPendingEvent(UUID transactionId) {}
