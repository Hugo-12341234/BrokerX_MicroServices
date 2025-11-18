package com.microservices.log430.orderservice.domain.service;

import com.microservices.log430.orderservice.adapters.external.marketdata.MarketDataClient;
import com.microservices.log430.orderservice.adapters.external.marketdata.StockRule;
import com.microservices.log430.orderservice.adapters.external.wallet.Wallet;
import com.microservices.log430.orderservice.adapters.external.wallet.WalletClient;
import com.microservices.log430.orderservice.domain.model.entities.Order;
import com.microservices.log430.orderservice.domain.port.in.PreTradeValidationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Service
public class PreTradeValidationService implements PreTradeValidationPort {
    private static final Logger logger = LoggerFactory.getLogger(PreTradeValidationService.class);

    private final MarketDataClient marketDataClient;

    @Autowired
    public PreTradeValidationService(MarketDataClient marketDataClient) {
        this.marketDataClient = marketDataClient;
    }

    @Override
    public ValidationResult validateOrder(ValidationRequest request) {
        logger.info("Début de la validation pré-trade pour symbol={}, side={}, type={}, quantité={}, prix={}",
            request.getSymbol(), request.getSide(), request.getType(), request.getQuantity(), request.getPrice());
        // Validation 1: Récupérer le stock via WalletClient (microservice wallet)
        StockRule stock = validateAndGetStock(request.getSymbol());
        if (stock == null) {
            logger.warn("Symbole invalide ou introuvable: '{}'", request.getSymbol());
            return ValidationResult.failure("Symbole invalide: '" + request.getSymbol() + "'.");
        }
        // Validation 3: Vérifier le tick size (pour les ordres limite uniquement)
        if (request.getType() == Order.OrderType.LIMITE) {
            if (request.getPrice() == null || request.getPrice() <= 0) {
                logger.warn("Prix limite non spécifié ou non positif pour symbol={}, userId={}", request.getSymbol(), request.getWallet().getUserId());
                return ValidationResult.failure("Le prix limite doit être spécifié et positif pour un ordre limite");
            }
            ValidationResult tickSizeValidation = validateTickSize(request.getPrice(), stock);
            if (!tickSizeValidation.isValid()) {
                logger.warn("Tick size invalide pour symbol={}, prix={}, tickSize={}", request.getSymbol(), request.getPrice(), stock.getTickSize());
                return tickSizeValidation;
            }
            // Validation 4: Vérifier la bande de prix (pour les ordres limite uniquement)
            ValidationResult priceRangeValidation = validatePriceRange(request.getPrice(), stock);
            if (!priceRangeValidation.isValid()) {
                logger.warn("Prix hors bande autorisée pour symbol={}, prix={}, min={}, max={}", request.getSymbol(), request.getPrice(), stock.getMinBand(), stock.getMaxBand());
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
                logger.warn("Pouvoir d'achat insuffisant pour userId={}, symbol={}, requis={}, disponible={}",
                    request.getWallet().getUserId(), request.getSymbol(),
                    buyingPowerValidation.getRejectReason(), request.getWallet().getBalance());
                return buyingPowerValidation;
            }
        }
        logger.info("Validation pré-trade réussie pour symbol={}, userId={}", request.getSymbol(), request.getWallet().getUserId());
        return ValidationResult.success();
    }

    private StockRule validateAndGetStock(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            logger.warn("Symbole non spécifié pour la validation du stock");
            return null;
        }
        try {
            logger.info("Récupération du stock pour symbol='{}' via market-data-service", symbol.trim());
            return marketDataClient.getStockBySymbol(symbol.trim());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du stock via market-data-service pour symbol='{}': {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    private ValidationResult validateBuyingPower(Order.OrderType type, int quantity, Double price, Wallet wallet, StockRule stock) {
        if (quantity <= 0) {
            logger.warn("Quantité non positive pour userId={}, symbol={}", wallet.getUserId(), stock.getSymbol());
            return ValidationResult.failure("La quantité doit être positive");
        }
        BigDecimal requiredAmount;
        if (type == Order.OrderType.MARCHE) {
            requiredAmount = stock.getPrice().multiply(new BigDecimal(quantity));
        } else if (type == Order.OrderType.LIMITE) {
            if (price == null || price <= 0) {
                logger.warn("Prix limite non spécifié ou non positif pour userId={}, symbol={}", wallet.getUserId(), stock.getSymbol());
                return ValidationResult.failure("Le prix limite doit être spécifié et positif pour un ordre limite");
            }
            requiredAmount = new BigDecimal(price).multiply(new BigDecimal(quantity));
        } else {
            logger.warn("Type d'ordre non supporté: {} pour userId={}, symbol={}", type, wallet.getUserId(), stock.getSymbol());
            return ValidationResult.failure("Type d'ordre non supporté: " + type);
        }
        if (wallet.getBalance().compareTo(requiredAmount) < 0) {
            logger.warn("Fonds insuffisants pour userId={}, symbol={}, requis={}, disponible={}", wallet.getUserId(), stock.getSymbol(), requiredAmount, wallet.getBalance());
            return ValidationResult.failure(String.format(
                    "Fonds insuffisants. Requis: $%.2f, Disponible: $%.2f",
                    requiredAmount.doubleValue(),
                    wallet.getBalance().doubleValue()
            ));
        }
        logger.info("Pouvoir d'achat suffisant pour userId={}, symbol={}, requis={}, disponible={}", wallet.getUserId(), stock.getSymbol(), requiredAmount, wallet.getBalance());
        return ValidationResult.success();
    }

    private ValidationResult validateTickSize(Double price, StockRule stock) {
        if (price == null) {
            logger.warn("Prix non spécifié pour la validation du tick size");
            return ValidationResult.success(); // Déjà géré dans validateBuyingPower
        }
        BigDecimal priceDecimal = new BigDecimal(price.toString());
        BigDecimal tickSize = stock.getTickSize();
        BigDecimal remainder = priceDecimal.remainder(tickSize);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            logger.warn("Prix {} n'est pas un multiple du tick size {} pour symbol={}", price, tickSize, stock.getSymbol());
            return ValidationResult.failure(String.format(
                    "Prix invalide: $%.4f. Le prix doit être un multiple du tick size $%.4f (ex: $%.2f, $%.2f, $%.2f...)",
                    price,
                    tickSize.doubleValue(),
                    tickSize.doubleValue(),
                    tickSize.multiply(new BigDecimal("2")).doubleValue(),
                    tickSize.multiply(new BigDecimal("3")).doubleValue()
            ));
        }
        logger.info("Tick size valide pour symbol={}, prix={}, tickSize={}", stock.getSymbol(), price, tickSize);
        return ValidationResult.success();
    }

    private ValidationResult validatePriceRange(Double price, StockRule stock) {
        if (price == null) {
            logger.warn("Prix non spécifié pour la validation de la bande de prix");
            return ValidationResult.success(); // Déjà géré dans validateBuyingPower
        }
        BigDecimal priceDecimal = new BigDecimal(price.toString());
        BigDecimal minPrice = stock.getMinBand();
        BigDecimal maxPrice = stock.getMaxBand();
        if (priceDecimal.compareTo(minPrice) < 0) {
            logger.warn("Prix trop bas pour symbol={}, prix={}, min={}", stock.getSymbol(), price, minPrice);
            return ValidationResult.failure(String.format(
                    "Prix trop bas: $%.2f. Le prix minimum autorisé est $%.2f (bande de prix: $%.2f - $%.2f)",
                    price,
                    minPrice.doubleValue(),
                    minPrice.doubleValue(),
                    maxPrice.doubleValue()
            ));
        }
        if (priceDecimal.compareTo(maxPrice) > 0) {
            logger.warn("Prix trop élevé pour symbol={}, prix={}, max={}", stock.getSymbol(), price, maxPrice);
            return ValidationResult.failure(String.format(
                    "Prix trop élevé: $%.2f. Le prix maximum autorisé est $%.2f (bande de prix: $%.2f - $%.2f)",
                    price,
                    maxPrice.doubleValue(),
                    minPrice.doubleValue(),
                    maxPrice.doubleValue()
            ));
        }
        logger.info("Prix dans la bande autorisée pour symbol={}, prix={}, min={}, max={}", stock.getSymbol(), price, minPrice, maxPrice);
        return PreTradeValidationPort.ValidationResult.success();
    }
}
