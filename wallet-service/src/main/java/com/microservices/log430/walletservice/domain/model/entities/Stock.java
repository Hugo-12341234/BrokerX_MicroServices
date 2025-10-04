package com.microservices.log430.walletservice.domain.model.entities;

import java.math.BigDecimal;

public class Stock {
    private final String symbol;
    private final String name;
    private final double price;
    private final boolean active;
    private final BigDecimal tickSize;
    private final BigDecimal priceRangePercentage; // Pourcentage pour la bande de prix

    public Stock(String symbol, String name, double price, boolean active, BigDecimal tickSize, BigDecimal priceRangePercentage) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.active = active;
        this.tickSize = tickSize;
        this.priceRangePercentage = priceRangePercentage;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal getTickSize() {
        return tickSize;
    }

    public BigDecimal getPriceRangePercentage() {
        return priceRangePercentage;
    }

    // Méthodes utilitaires pour la bande de prix
    public BigDecimal getMinAllowedPrice() {
        BigDecimal currentPrice = new BigDecimal(price);
        BigDecimal reduction = currentPrice.multiply(priceRangePercentage);
        return currentPrice.subtract(reduction);
    }

    public BigDecimal getMaxAllowedPrice() {
        BigDecimal currentPrice = new BigDecimal(price);
        BigDecimal increase = currentPrice.multiply(priceRangePercentage);
        return currentPrice.add(increase);
    }
}

