package com.microservices.log430.walletservice.domain.port.in;

import com.microservices.log430.walletservice.domain.model.entities.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletDepositPort {
    DepositResult deposit(DepositRequest request);
    Optional<Wallet> getWalletByUserId(Long userId);
    UpdateResult updateWallet(Long userId, String symbol, int quantityChange, double amountChange);

    class DepositRequest {
        private final Long userId;
        private final BigDecimal amount;
        private final String idempotencyKey;

        public DepositRequest(Long userId, BigDecimal amount, String idempotencyKey) {
            this.userId = userId;
            this.amount = amount;
            this.idempotencyKey = idempotencyKey;
        }

        public Long getUserId() { return userId; }
        public BigDecimal getAmount() { return amount; }
        public String getIdempotencyKey() { return idempotencyKey; }
    }

    class DepositResult {
        private final boolean success;
        private final String message;
        private final BigDecimal newBalance;
        private final Long transactionId;

        public DepositResult(boolean success, String message, BigDecimal newBalance, Long transactionId) {
            this.success = success;
            this.message = message;
            this.newBalance = newBalance;
            this.transactionId = transactionId;
        }

        public static DepositResult success(String message, BigDecimal newBalance, Long transactionId) {
            return new DepositResult(true, message, newBalance, transactionId);
        }

        public static DepositResult failure(String message) {
            return new DepositResult(false, message, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BigDecimal getNewBalance() { return newBalance; }
        public Long getTransactionId() { return transactionId; }
    }

    class UpdateResult {
        private final boolean success;
        private final String message;
        private final Wallet wallet;
        public UpdateResult(boolean success, String message, Wallet wallet) {
            this.success = success;
            this.message = message;
            this.wallet = wallet;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Wallet getWallet() { return wallet; }
    }
}
