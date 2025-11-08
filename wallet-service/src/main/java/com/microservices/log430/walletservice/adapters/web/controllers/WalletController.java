package com.microservices.log430.walletservice.adapters.web.controllers;

import com.microservices.log430.walletservice.adapters.web.dto.DepositResponse;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import com.microservices.log430.walletservice.adapters.web.dto.DepositRequest;
import com.microservices.log430.walletservice.adapters.web.dto.WalletResponse;
import com.microservices.log430.walletservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.walletservice.domain.port.in.StockPort;
import com.microservices.log430.walletservice.adapters.web.dto.WalletUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.UUID;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    private final WalletDepositPort walletDepositPort;
    private final StockPort stockPort;

    public WalletController(WalletDepositPort walletDepositPort, StockPort stockPort) {
        this.walletDepositPort = walletDepositPort;
        this.stockPort = stockPort;
    }

    /**
     * Endpoint GET portefeuille - COÛTEUX (100-300ms)
     * Cache: TTL 30 secondes, clé = userId (Long)
     * Raison: Calculs complexes portefeuille + positions, appelé très fréquemment
     */
    @GetMapping("")
    @Cacheable(value = "walletCache",
               key = "#p0 != null ? T(java.lang.Long).valueOf(#p0) : 'no-user'",
               condition = "#p0 != null && !#p0.trim().isEmpty()")
    public ResponseEntity<?> getWallet(@RequestHeader("X-User-Id") String userIdHeader, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");

        logger.info("🔍 GET portefeuille (cache miss potentiel) - userIdHeader: {}, requestId: {}", userIdHeader, requestId);

        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            logger.warn("Header X-User-Id manquant. Path: {}, RequestId: {}", path, requestId);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-User-Id manquant",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException e) {
            logger.error("Header X-User-Id invalide: {}. Path: {}, RequestId: {}", userIdHeader, path, requestId);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-User-Id invalide",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        var walletOpt = walletDepositPort.getWalletByUserId(userId);
        if (walletOpt.isEmpty()) {
            logger.warn("Portefeuille introuvable pour userId={}, RequestId: {}", userId, requestId);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Portefeuille introuvable",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        var wallet = walletOpt.get();
        logger.info("Portefeuille récupéré avec succès pour userId={}", userId);
        WalletResponse response = new WalletResponse(true, "Portefeuille récupéré avec succès", wallet);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    @CacheEvict(value = "walletCache", key = "T(java.lang.Long).valueOf(#userIdHeader)")
    public ResponseEntity<?> deposit(@RequestHeader("X-User-Id") String userIdHeader,
                                     @RequestBody DepositRequest request,
                                     HttpServletRequest httpRequest) {
        try {
            String path = httpRequest.getRequestURI();
            String requestId = httpRequest.getHeader("X-Request-Id");
            logger.info("Requête dépôt reçue. Path: {}, RequestId: {}, X-User-Id: {}", path, requestId, userIdHeader);
            if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
                logger.warn("Header X-User-Id manquant. Path: {}, RequestId: {}", path, requestId);
                ErrorResponse err = new ErrorResponse(
                        Instant.now(),
                        path,
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Header X-User-Id manquant",
                        requestId != null ? requestId : ""
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
            Long userId;
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException e) {
                logger.error("Header X-User-Id invalide: {}. Path: {}, RequestId: {}", userIdHeader, path, requestId);
                ErrorResponse err = new ErrorResponse(
                        Instant.now(),
                        path,
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Header X-User-Id invalide",
                        requestId != null ? requestId : ""
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
            String idempotencyKey = httpRequest.getHeader("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                idempotencyKey = UUID.randomUUID().toString();
                logger.info("Idempotency-Key généré: {}", idempotencyKey);
            } else {
                logger.info("Idempotency-Key reçu: {}", idempotencyKey);
            }
            logger.info("Dépôt demandé: userId={}, montant={}, idempotencyKey={}", userId, request.getAmount(), idempotencyKey);
            WalletDepositPort.DepositRequest domainRequest = new WalletDepositPort.DepositRequest(
                    userId,
                    request.getAmount(),
                    idempotencyKey
            );
            WalletDepositPort.DepositResult result = walletDepositPort.deposit(domainRequest);
            if (result.isSuccess()) {
                logger.info("Dépôt réussi: userId={}, montant={}, nouveau solde={}, transactionId={}", userId, request.getAmount(), result.getNewBalance(), result.getTransactionId());
                return ResponseEntity.ok(DepositResponse.success(
                        result.getMessage(),
                        result.getNewBalance(),
                        result.getTransactionId()
                ));
            } else {
                logger.warn("Dépôt échoué: userId={}, montant={}, raison={}", userId, request.getAmount(), result.getMessage());
                ErrorResponse err = new ErrorResponse(
                        Instant.now(),
                        path,
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        result.getMessage(),
                        requestId != null ? requestId : ""
                );

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
        } catch (Exception e) {
            logger.error("Erreur lors du dépôt: {}", e.getMessage(), e);
            ErrorResponse err = new ErrorResponse(
                    Instant.now(),
                    httpRequest.getRequestURI(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "Erreur lors du dépôt",
                    httpRequest.getHeader("X-Request-Id") != null ? httpRequest.getHeader("X-Request-Id") : ""
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    /**
     * Endpoint GET stock - TRÈS COÛTEUX (50-150ms)
     * Cache: TTL 2 minutes, clé = symbol
     * Raison: Données de référence, répétées massivement (AAPL, GOOGL, etc.)
     */
    @GetMapping("/stock")
    @Cacheable(value = "stockCache", key = "#p0",
               unless = "#result == null or #result.statusCode.value() != 200")
    public ResponseEntity<?> getStockBySymbol(@RequestParam("symbol") String symbol, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        logger.info("📈 GET stock (cache miss potentiel) - symbol: {}, requestId: {}", symbol, requestId);
        var stockRuleOpt = stockPort.getStockRuleBySymbol(symbol);
        if (stockRuleOpt.isEmpty()) {
            logger.warn("StockRule introuvable pour le symbole '{}'. Path: {}, RequestId: {}", symbol, path, requestId);
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "StockRule introuvable pour ce symbole",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
        var stockRule = stockRuleOpt.get();
        logger.info("StockRule récupéré avec succès pour le symbole '{}'", symbol);
        return ResponseEntity.ok(stockRule);
    }

    /**
     * Endpoint POST update wallet - INVALIDE LE CACHE
     * Invalidation: walletCache pour userId concerné
     * Raison: Cohérence des données après modification portefeuille
     */
    @PostMapping("/update")
    @CacheEvict(value = "walletCache", key = "#p0.userId")
    public ResponseEntity<?> updateWallet(@RequestBody WalletUpdateRequest request, HttpServletRequest httpRequest) {
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        logger.info("🗑️ POST update wallet (INVALIDATION CACHE) - userId: {}, symbol: {}, qtyChange: {}, amountChange: {}, requestId: {}",
            request.userId, request.symbol, request.quantityChange, request.amountChange, requestId);
        logger.warn("⚠️ CACHE INVALIDATED for userId: {} - walletCache entry will be removed", request.userId);
        try {
            var result = walletDepositPort.updateWallet(request.userId, request.symbol, request.quantityChange, request.amountChange);
            if (result.isSuccess()) {
                logger.info("MAJ portefeuille réussie pour userId={}", request.userId);
                return ResponseEntity.ok(new WalletResponse(true, "Portefeuille mis à jour", result.getWallet()));
            } else {
                logger.warn("MAJ portefeuille échouée pour userId={}, raison={}", request.userId, result.getMessage());
                ErrorResponse err = new ErrorResponse(
                    java.time.Instant.now(),
                    path,
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    result.getMessage(),
                    requestId
                );
                return ResponseEntity.badRequest().body(err);
            }
        } catch (Exception e) {
            logger.error("Exception technique lors de la MAJ portefeuille pour userId={}, symbol={}, qtyChange={}, amountChange={}: {}", request.userId, request.symbol, request.quantityChange, request.amountChange, e.getMessage(), e);
            ErrorResponse err = new ErrorResponse(
                java.time.Instant.now(),
                path,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erreur lors de la mise à jour du portefeuille : " + e.getMessage(),
                requestId
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
