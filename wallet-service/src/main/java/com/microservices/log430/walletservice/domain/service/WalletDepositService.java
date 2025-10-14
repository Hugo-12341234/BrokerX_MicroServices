package com.microservices.log430.walletservice.domain.service;

import com.microservices.log430.walletservice.domain.model.entities.Transaction;
import com.microservices.log430.walletservice.domain.model.entities.WalletAudit;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import com.microservices.log430.walletservice.domain.port.out.PaymentServicePort;
import com.microservices.log430.walletservice.domain.port.out.TransactionPort;
import com.microservices.log430.walletservice.domain.port.out.WalletAuditPort;
import com.microservices.log430.walletservice.domain.port.out.WalletPort;
import com.microservices.log430.walletservice.domain.model.entities.Wallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WalletDepositService implements WalletDepositPort {
    private static final Logger logger = LoggerFactory.getLogger(WalletDepositService.class);

    private final TransactionPort transactionPort;
    private final PaymentServicePort paymentServicePort;
    private final WalletAuditPort walletAuditPort;
    private final WalletPort walletPort;

    // Limites de validation
    private static final BigDecimal MIN_DEPOSIT = new BigDecimal("10.00");
    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("50000.00");

    public WalletDepositService(TransactionPort transactionPort,
                                PaymentServicePort paymentServicePort,
                                WalletAuditPort walletAuditPort,
                                WalletPort walletPort) {
        this.transactionPort = transactionPort;
        this.paymentServicePort = paymentServicePort;
        this.walletAuditPort = walletAuditPort;
        this.walletPort = walletPort;
    }

    private void logAuditEvent(String type, Long userId, String details) {
        logger.info("AuditEvent: type={}, userId={}, details={}", type, userId, details);
        WalletAudit audit = new WalletAudit(
            userId,
            type + ": " + details,
            java.time.LocalDateTime.now()
        );
        walletAuditPort.saveAudit(audit);
    }

    @Override
    @Transactional
    public DepositResult deposit(DepositRequest request) {
        logger.info("Début dépôt: userId={}, montant={}, idempotencyKey={}", request.getUserId(), request.getAmount(), request.getIdempotencyKey());
        try {
            Wallet wallet = walletPort.findByUserId(request.getUserId()).orElse(null);
            if (wallet == null) {
                logger.info("Aucun portefeuille trouvé, création d'un nouveau portefeuille pour userId={}", request.getUserId());
                // Création d'un nouveau portefeuille pour l'utilisateur
                wallet = new Wallet();
                wallet.setUserId(request.getUserId());
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setCreatedAt(LocalDateTime.now());
                wallet.setUpdatedAt(LocalDateTime.now());
                wallet = walletPort.save(wallet);
                logAuditEvent("WALLET_CREATED", wallet.getUserId(), "Portefeuille créé automatiquement");
            }
            logger.info("Vérification idempotence pour idempotencyKey={}");
            // E2. Idempotence : vérifier si cette transaction existe déjà
            Optional<Transaction> existingTransaction = transactionPort.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                Transaction existing = existingTransaction.get();
                logger.info("Transaction existante trouvée: id={}, status={}", existing.getId(), existing.getStatus());
                if (existing.getStatus() == Transaction.TransactionStatus.SETTLED) {
                    return DepositResult.success("Dépôt déjà effectué", wallet.getBalance(), existing.getId());
                }
                return DepositResult.failure("Transaction en cours de traitement");
            }
            logger.info("Création d'une transaction Pending pour userId={}, montant={}", request.getUserId(), request.getAmount());
            // 3. Créer une transaction Pending (même si le montant est invalide)
            Transaction transaction = new Transaction(
                    request.getUserId(),
                    request.getIdempotencyKey(),
                    request.getAmount(),
                    Transaction.TransactionType.DEPOSIT,
                    "Dépôt au portefeuille"
            );
            transaction = transactionPort.save(transaction);
            logger.info("Transaction créée: id={}, status={}", transaction.getId(), transaction.getStatus());
            // 2. Validation des limites (min/max, anti-fraude) - APRÈS création de la transaction
            if (request.getAmount().compareTo(MIN_DEPOSIT) < 0) {
                logger.warn("Montant trop faible pour dépôt: {} < MIN_DEPOSIT {}", request.getAmount(), MIN_DEPOSIT);
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_FAILED", wallet.getUserId(), "Montant trop faible: " + request.getAmount());
                return DepositResult.failure("Montant minimum: " + MIN_DEPOSIT + "$.");
            }
            if (request.getAmount().compareTo(MAX_DEPOSIT) > 0) {
                logger.warn("Montant trop élevé pour dépôt: {} > MAX_DEPOSIT {}", request.getAmount(), MAX_DEPOSIT);
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_FAILED", wallet.getUserId(), "Montant trop élevé: " + request.getAmount());
                return DepositResult.failure("Montant maximum: " + MAX_DEPOSIT + "$.");
            }
            logger.info("Appel du service de paiement simulé pour userId={}, idempotencyKey={}", wallet.getUserId(), request.getIdempotencyKey());
            // 4. Service Paiement Simulé
            boolean paymentSuccess = paymentServicePort.processPayment(
                    request.getIdempotencyKey(),
                    wallet.getUserId()
            );
            logger.info("Résultat du paiement: {}", paymentSuccess);
            if (!paymentSuccess) {
                logger.error("Paiement refusé pour userId={}, idempotencyKey={}", wallet.getUserId(), request.getIdempotencyKey());
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_PAYMENT_FAILED", wallet.getUserId(), "Paiement refusé par le service");
                return DepositResult.failure("Paiement refusé");
            }
            logger.info("Crédit du portefeuille: userId={}, montant={}, solde avant={}, solde après={}", wallet.getUserId(), request.getAmount(), wallet.getBalance(), wallet.getBalance().add(request.getAmount()));
            // 5. Créditer le portefeuille, journaliser et notifier
            wallet.setBalance(wallet.getBalance().add(request.getAmount()));
            walletPort.save(wallet);
            logger.info("Transaction marquée comme SETTLED: id={}", transaction.getId());
            transaction.markAsSettled();
            transaction = transactionPort.save(transaction);
            logger.info("Dépôt réussi: userId={}, nouveau solde={}, transactionId={}", wallet.getUserId(), wallet.getBalance(), transaction.getId());
            // Audit de succès
            logAuditEvent("DEPOSIT_SUCCESS", wallet.getUserId(),
                    "Dépôt de " + request.getAmount() + "$ - Nouveau solde: " + wallet.getBalance());

            return WalletDepositPort.DepositResult.success(
                    "Dépôt de " + request.getAmount() + "$ effectué avec succès",
                    wallet.getBalance(),
                    transaction.getId()
            );
        } catch (Exception e) {
            logger.error("Exception technique lors du dépôt: userId={}, montant={}, idempotencyKey={}, erreur={}", request.getUserId(), request.getAmount(), request.getIdempotencyKey(), e.getMessage(), e);
            logAuditEvent("DEPOSIT_ERROR", request.getUserId(), "Erreur technique: " + e.getMessage());
            return DepositResult.failure("Erreur technique lors du dépôt");
        }
    }

    public Optional<Wallet> getWalletByUserId(Long userId) {
        logger.info("Recherche portefeuille pour userId={}", userId);
        Optional<Wallet> walletOpt = walletPort.findByUserId(userId);
        if (walletOpt.isPresent()) {
            logger.info("Portefeuille trouvé pour userId={}", userId);
            return walletOpt;
        }
        logger.info("Aucun portefeuille trouvé, création d'un nouveau portefeuille pour userId={}", userId);
        // Création d'un nouveau portefeuille si inexistant
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        Wallet savedWallet = walletPort.save(wallet);
        logAuditEvent("WALLET_CREATED", userId, "Portefeuille créé automatiquement (getWalletByUserId)");
        logger.info("Portefeuille créé pour userId={}", userId);
        return Optional.of(savedWallet);
    }

    @Override
    @Transactional
    public UpdateResult updateWallet(Long userId, String symbol, int quantityChange, double amountChange) {
        logger.info("Début updateWallet: userId={}, symbol={}, qtyChange={}, amountChange={}", userId, symbol, quantityChange, amountChange);
        try {
            Optional<Wallet> walletOpt = walletPort.findByUserId(userId);
            if (walletOpt.isEmpty()) {
                logger.error("Portefeuille introuvable pour userId={}, symbol={}, qtyChange={}, amountChange={}", userId, symbol, quantityChange, amountChange);
                logAuditEvent("UPDATE_FAIL", userId, "Portefeuille introuvable");
                return new UpdateResult(false, "Portefeuille introuvable", null);
            }
            Wallet wallet = walletOpt.get();
            logger.info("Mise à jour de la quantité du stock: userId={}, symbol={}, qtyChange={}", userId, symbol, quantityChange);
            wallet.updateStockQuantity(symbol, quantityChange);
            logger.info("Mise à jour du solde: userId={}, montant={}, solde avant={}, solde après={}", userId, amountChange, wallet.getBalance(), wallet.getBalance().add(java.math.BigDecimal.valueOf(amountChange)));
            wallet.setBalance(wallet.getBalance().add(java.math.BigDecimal.valueOf(amountChange)));
            walletPort.save(wallet);
            logAuditEvent("UPDATE_SUCCESS", userId, String.format("%s: qty %+d, montant %+f", symbol, quantityChange, amountChange));
            logger.info("Portefeuille mis à jour pour userId={}, symbol={}, qtyChange={}, amountChange={}", userId, symbol, quantityChange, amountChange);
            return new UpdateResult(true, "Portefeuille mis à jour", wallet);
        } catch (Exception e) {
            logger.error("Exception technique lors de l'updateWallet pour userId={}, symbol={}, qtyChange={}, amountChange={}: {}", userId, symbol, quantityChange, amountChange, e.getMessage(), e);
            return new UpdateResult(false, "Erreur technique lors de la mise à jour du portefeuille : " + e.getMessage(), null);
        }
    }
}
