package com.microservices.log430.orderservice.adapters.external.wallet;

import java.io.Serializable;

public class WalletResponse implements Serializable {
    private boolean success;
    private String message;
    private Wallet wallet;

    public WalletResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
}