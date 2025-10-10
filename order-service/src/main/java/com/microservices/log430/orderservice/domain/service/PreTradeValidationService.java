package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.external.wallet.StockRule;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.in.PreTradeValidationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PreTradeValidationService implements PreTradeValidationPort {

    private final WalletClient walletClient;

    @Autowired
    public PreTradeValidationService(WalletClient walletClient) {
        this.walletClient = walletClient;
    }

    @Override
    public ValidationResult validateOrder(ValidationRequest request) {
        // Validation 1: Récupérer le stock via WalletClient (microservice wallet)
        StockRule stock = validateAndGetStock(request.getSymbol());
        if (stock == null) {
            return ValidationResult.failure("Symbole invalide: '" + request.getSymbol() + "'.");
        }

        // Validation 3: Vérifier le tick size (pour les ordres limite uniquement)
        if (request.getType() == Order.OrderType.LIMITE) {
            if (request.getPrice() == null || request.getPrice() <= 0) {
                return ValidationResult.failure("Le prix limite doit être spécifié et positif pour un ordre limite");
            }

            ValidationResult tickSizeValidation = validateTickSize(request.getPrice(), stock);
            if (!tickSizeValidation.isValid()) {
                return tickSizeValidation;
            }

            // Validation 4: Vérifier la bande de prix (pour les ordres limite uniquement)
            ValidationResult priceRangeValidation = validatePriceRange(request.getPrice(), stock);
            if (!priceRangeValidation.isValid()) {
                return priceRangeValidation;
            }
        }

        // Validation 5: Vérifier le pouvoir d'achat (seulement pour les achats)
        if (request.getSide() == Order.Side.ACHAT) {
            ValidationResult buyingPowerValidation = validateBuyingPower(
                    request.getType(),
                    request.getQuantity(),
                    request.getPrice(),
                    request.getWallet(),
                    stock
            );
            if (!buyingPowerValidation.isValid()) {
                return buyingPowerValidation;
            }
        }

        return ValidationResult.success();
    }

    private StockRule validateAndGetStock(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        try {
            return walletClient.getStockBySymbol(symbol.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private ValidationResult validateBuyingPower(Order.OrderType type, int quantity, Double price, Wallet wallet, StockRule stock) {
        if (quantity <= 0) {
            return ValidationResult.failure("La quantité doit être positive");
        }

        BigDecimal requiredAmount;

        if (type == Order.OrderType.MARCHE) {
            requiredAmount = stock.getPrice().multiply(new BigDecimal(quantity));
        } else if (type == Order.OrderType.LIMITE) {
            if (price == null || price <= 0) {
                return ValidationResult.failure("Le prix limite doit être spécifié et positif pour un ordre limite");
            }
            requiredAmount = new BigDecimal(price).multiply(new BigDecimal(quantity));
        } else {
            return ValidationResult.failure("Type d'ordre non supporté: " + type);
        }

        if (wallet.getBalance().compareTo(requiredAmount) < 0) {
            return ValidationResult.failure(String.format(
                    "Fonds insuffisants. Requis: $%.2f, Disponible: $%.2f",
                    requiredAmount.doubleValue(),
                    wallet.getBalance().doubleValue()
            ));
        }

        return ValidationResult.success();
    }

    private ValidationResult validateTickSize(Double price, StockRule stock) {
        if (price == null) {
            return ValidationResult.success(); // Déjà géré dans validateBuyingPower
        }

        BigDecimal priceDecimal = new BigDecimal(price.toString());
        BigDecimal tickSize = stock.getTickSize();

        // Vérifier si le prix est un multiple du tick size
        BigDecimal remainder = priceDecimal.remainder(tickSize);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            return ValidationResult.failure(String.format(
                    "Prix invalide: $%.4f. Le prix doit être un multiple du tick size $%.4f (ex: $%.2f, $%.2f, $%.2f...)",
                    price,
                    tickSize.doubleValue(),
                    tickSize.doubleValue(),
                    tickSize.multiply(new BigDecimal("2")).doubleValue(),
                    tickSize.multiply(new BigDecimal("3")).doubleValue()
            ));
        }

        return ValidationResult.success();
    }

    private ValidationResult validatePriceRange(Double price, StockRule stock) {
        if (price == null) {
            return ValidationResult.success(); // Déjà géré dans validateBuyingPower
        }

        BigDecimal priceDecimal = new BigDecimal(price.toString());
        BigDecimal minPrice = stock.getMinBand();
        BigDecimal maxPrice = stock.getMaxBand();

        if (priceDecimal.compareTo(minPrice) < 0) {
            return ValidationResult.failure(String.format(
                    "Prix trop bas: $%.2f. Le prix minimum autorisé est $%.2f (bande de prix: $%.2f - $%.2f)",
                    price,
                    minPrice.doubleValue(),
                    minPrice.doubleValue(),
                    maxPrice.doubleValue()
            ));
        }

        if (priceDecimal.compareTo(maxPrice) > 0) {
            return ValidationResult.failure(String.format(
                    "Prix trop élevé: $%.2f. Le prix maximum autorisé est $%.2f (bande de prix: $%.2f - $%.2f)",
                    price,
                    maxPrice.doubleValue(),
                    minPrice.doubleValue(),
                    maxPrice.doubleValue()
            ));
        }

        return PreTradeValidationPort.ValidationResult.success();
    }
}
