package com.microservices.log430.walletservice.adapters.web.dto;

public class WalletUpdateRequest {
    public Long userId;
    public String symbol;
    public int quantityChange;
    public double amountChange;

    public WalletUpdateRequest() {}
    public WalletUpdateRequest(Long userId, String symbol, int quantityChange, double amountChange) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantityChange = quantityChange;
        this.amountChange = amountChange;
    }
}

