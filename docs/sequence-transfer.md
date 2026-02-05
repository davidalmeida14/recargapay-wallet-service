# Diagrama de Sequência: Transferência

Este diagrama descreve o fluxo de transferência entre duas carteiras digitais.

```mermaid
sequenceDiagram
    autonumber
    actor Usuário
    participant API as TransferController
    participant Sec as SecurityService
    participant WS as WalletService
    participant TS as TransferService
    participant WR as WalletRepository
    participant TR as TransactionRepository
    participant ER as EntryRepository
    participant DB as Banco de Dados (Transacional)

    Usuário->>API: PUT /api/v1/transfers (TransferRequest + Idempotency-Id)
    API->>Sec: getAuthenticatedCustomerId()
    Sec-->>API: customerId
    API->>WS: retrieveDefaultWallet(customerId)
    WS-->>API: originWallet
    
    API->>TS: transfer(originId, destId, amount, idempotencyId)
    
    TS->>TR: findByWalletIdAndIdempotencyIdAndType(...)
    alt Já processado (Idempotência)
        TR-->>TS: Optional[Transaction]
        TS-->>API: void
    else Nova Transferência
        TR-->>TS: Optional.empty()
        
        Note over TS, DB: Início do TransactionTemplate
        TS->>WR: loadByIdForUpdate(originId)
        WR-->>TS: originWallet (LOCKED)
        TS->>WR: loadByIdForUpdate(destId)
        WR-->>TS: destWallet (LOCKED)
        
        TS->>TS: validateCurrencyMatching()
        
        TS->>WR: originWallet.withdraw(amount)
        TS->>WR: destWallet.deposit(amount)
        
        TS->>TS: createPendingTransaction()
        TS->>TR: create(transaction)
        
        TS->>TS: createDebitEntry(origin)
        TS->>TS: createCreditEntry(destination)
        TS->>ER: create(List<Entry>)
        
        TS->>WR: add(originWallet)
        TS->>WR: add(destWallet)
        
        TS->>TR: update(transaction status: PROCESSED)
        Note over TS, DB: Fim do TransactionTemplate
        
        TS-->>API: void
    end
    
    API-->>Usuário: 200 OK
```
