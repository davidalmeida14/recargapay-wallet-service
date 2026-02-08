package br.com.recargapay.wallet.domain.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "transactions")
@Table(name = "transactions")
@Getter
@AllArgsConstructor
@NoArgsConstructor
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

  public void processed() {
    this.status = Status.PROCESSED;
    this.updatedAt = OffsetDateTime.now();
  }

  public boolean isCompleted() {
    return status == Status.PROCESSED;
  }

  public boolean isTransfer() {
    return type == Type.TRANSFER;
  }
}
