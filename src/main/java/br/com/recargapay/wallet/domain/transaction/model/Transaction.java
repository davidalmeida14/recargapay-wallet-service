package br.com.recargapay.wallet.domain.transaction.model;

import br.com.recargapay.wallet.infrastructure.common.UUIDGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "transactions")
@Table(name = "transactions")
public class Transaction {

  @Id private UUID id;

  @Column(name = "wallet_id")
  private UUID walletId;

  @Column(name = "wallet_destination_id")
  private UUID walletDestinationId; // Used in case of TRANSFER

  @Column(name = "idempotency_id")
  private String idempotencyId;

  @Column(name = "amount")
  private BigDecimal amount;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private Type type; // DEPOSIT, WITHDRAWAL, TRANSFER

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private Status status;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public Transaction() {}

  public Transaction(
      UUID id,
      UUID walletId,
      UUID walletDestinationId,
      String idempotencyId,
      BigDecimal amount,
      Type type,
      Status status,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    this.id = id;
    this.walletId = walletId;
    this.walletDestinationId = walletDestinationId;
    this.idempotencyId = idempotencyId;
    this.amount = amount;
    this.type = type;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getWalletId() {
    return walletId;
  }

  public UUID getWalletDestinationId() {
    return walletDestinationId;
  }

  public String getIdempotencyId() {
    return idempotencyId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Type getType() {
    return type;
  }

  public Status getStatus() {
    return status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void processed() {
    this.status = Status.PROCESSED;
    this.updatedAt = OffsetDateTime.now();
  }
}
