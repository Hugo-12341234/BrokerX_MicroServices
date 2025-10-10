package com.microservices.log430.orderservice.domain.port.in;

import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import com.microservices.log430.orderservice.domain.model.entities.Order;

public interface PreTradeValidationPort {
    ValidationResult validateOrder(ValidationRequest request);

    class ValidationRequest {
        private final String symbol;
        private final Order.Side side;
        private final Order.OrderType type;
        private final int quantity;
        private final Double price;
        private final Wallet wallet;

        public ValidationRequest(String symbol, Order.Side side, Order.OrderType type,
                                 int quantity, Double price, Wallet wallet) {
            this.symbol = symbol;
            this.side = side;
            this.type = type;
            this.quantity = quantity;
            this.price = price;
            this.wallet = wallet;
        }

        public String getSymbol() { return symbol; }
        public Order.Side getSide() { return side; }
        public Order.OrderType getType() { return type; }
        public int getQuantity() { return quantity; }
        public Double getPrice() { return price; }
        public Wallet getWallet() { return wallet; }
    }

    class ValidationResult {
        private final boolean valid;
        private final String rejectReason;

        public ValidationResult(boolean valid, String rejectReason) {
            this.valid = valid;
            this.rejectReason = rejectReason;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() { return valid; }
        public String getRejectReason() { return rejectReason; }
    }
}

