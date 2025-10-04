package com.microservices.log430.walletservice.adapters.web.dto;

import java.math.BigDecimal;

public class DepositResponse {
    private boolean success;
    private String message;
    private BigDecimal newBalance;
    private Long transactionId;

    public DepositResponse() {}

    public DepositResponse(boolean success, String message, BigDecimal newBalance, Long transactionId) {
        this.success = success;
        this.message = message;
        this.newBalance = newBalance;
        this.transactionId = transactionId;
    }

    public static DepositResponse success(String message, BigDecimal newBalance, Long transactionId) {
        return new DepositResponse(true, message, newBalance, transactionId);
    }

    public static DepositResponse failure(String message) {
        return new DepositResponse(false, message, null, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
}

