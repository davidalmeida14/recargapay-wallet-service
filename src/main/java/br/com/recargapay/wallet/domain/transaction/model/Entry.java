package br.com.recargapay.wallet.domain.transaction.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "entries")
@Entity
public class Entry {
  @Id private UUID id;

  @Column(name = "wallet_id")
  private UUID walletId;

  @Column(name = "transaction_id")
  private UUID transactionId;

  @Column(name = "amount")
  private BigDecimal amount;

  @Column(name = "financial_type")
  @Enumerated(EnumType.STRING)
  private FinancialType financialType;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  public Entry() {}

  public Entry(
      UUID id,
      UUID walletId,
      UUID transactionId,
      BigDecimal amount,
      FinancialType financialType,
      OffsetDateTime createdAt) {
    this.id = id;
    this.walletId = walletId;
    this.transactionId = transactionId;
    this.amount = amount;
    this.financialType = financialType;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getWalletId() {
    return walletId;
  }

  public UUID getTransactionId() {
    return transactionId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public FinancialType getFinancialType() {
    return financialType;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
