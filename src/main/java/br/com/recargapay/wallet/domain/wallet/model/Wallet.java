package br.com.recargapay.wallet.domain.wallet.model;

import br.com.recargapay.wallet.domain.wallet.exception.InsufficientBalanceException;
import br.com.recargapay.wallet.infrastructure.common.UUIDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

@Table(name = "wallets")
@Entity
public class Wallet {
  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "balance", nullable = false)
  private BigDecimal balance;

  @Column(name = "currency", nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Wallet() {}

  public Wallet(
      UUID id,
      UUID customerId,
      BigDecimal balance,
      Currency currency,
      boolean active,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    this.id = id;
    this.customerId = customerId;
    this.balance = balance;
    this.currency = currency;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public Currency getCurrency() {
    return currency;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public boolean isActive() {
    return active;
  }

  public void deposit(@NonNull BigDecimal amount) {
    balance = balance.add(amount);
    this.updatedAt = OffsetDateTime.now();
  }

  public void withdraw(@NonNull BigDecimal amount) {
    if (balance.compareTo(amount) < 0) {
      throw new InsufficientBalanceException(getId(), amount, balance);
    }
    balance = balance.subtract(amount);
  }

  public Wallet(UUID customerId, String currency) {
    this.id = UUIDGenerator.generate();
    this.customerId = customerId;
    this.balance = BigDecimal.ZERO;
    this.currency = Currency.valueOf(currency);
    this.active = true;
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }
}
