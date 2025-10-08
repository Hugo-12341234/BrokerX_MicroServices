package com.microservices.log430.walletservice.adapters.web.dto;

import com.microservices.log430.walletservice.domain.model.entities.Wallet;

public class WalletResponse {
    private boolean success;
    private String message;
    private Wallet wallet;

    public WalletResponse() {}

    public WalletResponse(boolean success, String message, Wallet wallet) {
        this.success = success;
        this.message = message;
        this.wallet = wallet;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public static WalletResponse success(Wallet wallet) {
        return new WalletResponse(true, null, wallet);
    }

    public static WalletResponse failure(String message) {
        return new WalletResponse(false, message, null);
    }
}

