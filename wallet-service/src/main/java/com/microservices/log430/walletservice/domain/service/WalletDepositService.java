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

@Service
public class WalletDepositService implements WalletDepositPort {

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
        try {
            Wallet wallet = walletPort.findByUserId(request.getUserId()).orElse(null);
            if (wallet == null) {
                // Création d'un nouveau portefeuille pour l'utilisateur
                wallet = new Wallet();
                wallet.setUserId(request.getUserId());
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setCreatedAt(LocalDateTime.now());
                wallet.setUpdatedAt(LocalDateTime.now());
                wallet = walletPort.save(wallet);
                logAuditEvent("WALLET_CREATED", wallet.getUserId(), "Portefeuille créé automatiquement");
            }

            // E2. Idempotence : vérifier si cette transaction existe déjà
            Optional<Transaction> existingTransaction = transactionPort.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                Transaction existing = existingTransaction.get();
                if (existing.getStatus() == Transaction.TransactionStatus.SETTLED) {
                    return DepositResult.success("Dépôt déjà effectué", wallet.getBalance(), existing.getId());
                }
                return DepositResult.failure("Transaction en cours de traitement");
            }

            // 3. Créer une transaction Pending (même si le montant est invalide)
            Transaction transaction = new Transaction(
                    request.getUserId(),
                    request.getIdempotencyKey(),
                    request.getAmount(),
                    Transaction.TransactionType.DEPOSIT,
                    "Dépôt au portefeuille"
            );
            transaction = transactionPort.save(transaction);

            // 2. Validation des limites (min/max, anti-fraude) - APRÈS création de la transaction
            if (request.getAmount().compareTo(MIN_DEPOSIT) < 0) {
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_FAILED", wallet.getUserId(), "Montant trop faible: " + request.getAmount());
                return DepositResult.failure("Montant minimum: " + MIN_DEPOSIT + "$.");
            }

            if (request.getAmount().compareTo(MAX_DEPOSIT) > 0) {
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_FAILED", wallet.getUserId(), "Montant trop élevé: " + request.getAmount());
                return DepositResult.failure("Montant maximum: " + MAX_DEPOSIT + "$.");
            }

            // 4. Service Paiement Simulé
            boolean paymentSuccess = paymentServicePort.processPayment(
                    request.getIdempotencyKey(),
                    wallet.getUserId()
            );

            if (!paymentSuccess) {
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_PAYMENT_FAILED", wallet.getUserId(), "Paiement refusé par le service");
                return DepositResult.failure("Paiement refusé");
            }

            // 5. Créditer le portefeuille, journaliser et notifier
            wallet.setBalance(wallet.getBalance().add(request.getAmount()));
            walletPort.save(wallet);

            transaction.markAsSettled();
            transaction = transactionPort.save(transaction);

            // Audit de succès
            logAuditEvent("DEPOSIT_SUCCESS", wallet.getUserId(),
                    "Dépôt de " + request.getAmount() + "$ - Nouveau solde: " + wallet.getBalance());

            return WalletDepositPort.DepositResult.success(
                    "Dépôt de " + request.getAmount() + "$ effectué avec succès",
                    wallet.getBalance(),
                    transaction.getId()
            );

        } catch (Exception e) {
            logAuditEvent("DEPOSIT_ERROR", request.getUserId(), "Erreur technique: " + e.getMessage());
            return DepositResult.failure("Erreur technique lors du dépôt");
        }
    }

    public Optional<Wallet> getWalletByUserId(Long userId) {
        Optional<Wallet> walletOpt = walletPort.findByUserId(userId);
        if (walletOpt.isPresent()) {
            return walletOpt;
        }
        // Création d'un nouveau portefeuille si inexistant
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        Wallet savedWallet = walletPort.save(wallet);
        logAuditEvent("WALLET_CREATED", userId, "Portefeuille créé automatiquement (getWalletByUserId)");
        return Optional.of(savedWallet);
    }
}
