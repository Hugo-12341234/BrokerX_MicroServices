package com.microservices.log430.walletservice.domain.service;

import com.microservices.log430.walletservice.domain.model.entities.Transaction;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WalletDepositService implements WalletDepositPort {

    private final TransactionPort transactionPort;
    private final UserPort userPort;
    private final PaymentServicePort paymentServicePort;
    private final AuditPort auditPort;

    // Limites de validation
    private static final BigDecimal MIN_DEPOSIT = new BigDecimal("10.00");
    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("50000.00");

    public WalletDepositService(TransactionPort transactionPort,
                                UserPort userPort,
                                PaymentServicePort paymentServicePort,
                                AuditPort auditPort) {
        this.transactionPort = transactionPort;
        this.userPort = userPort;
        this.paymentServicePort = paymentServicePort;
        this.auditPort = auditPort;
    }

    private void logAuditEvent(String action, Long userId, String details) {
        Audit audit = new Audit(
                userId,
                action,
                LocalDateTime.now(),
                details,  // utilise details comme documentHash
                null,     // ipAddress - null pour les événements internes
                null,     // userAgent - null pour les événements internes
                null      // sessionToken - null pour les événements internes
        );
        auditPort.saveAudit(audit);
    }

    @Override
    @Transactional
    public DepositResult deposit(DepositRequest request) {
        try {
            // E2. Idempotence : vérifier si cette transaction existe déjà
            Optional<Transaction> existingTransaction = transactionPort.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                Transaction existing = existingTransaction.get();
                if (existing.getStatus() == Transaction.TransactionStatus.SETTLED) {
                    // Retourner le résultat précédent
                    User user = userPort.findById(request.getUserId()).orElse(null);
                    if (user != null) {
                        return DepositResult.success("Dépôt déjà effectué", user.getBalance(), existing.getId());
                    }
                }
                return DepositResult.failure("Transaction en cours de traitement");
            }

            // Validation de l'utilisateur
            Optional<User> userOpt = userPort.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                logAuditEvent("DEPOSIT_FAILED", request.getUserId(), "Utilisateur non trouvé");
                return DepositResult.failure("Utilisateur non trouvé");
            }

            User user = userOpt.get();
            if (user.getStatus() != User.Status.ACTIVE) {
                logAuditEvent("DEPOSIT_FAILED", user.getId(), "Compte non actif");
                return DepositResult.failure("Votre compte doit être actif pour effectuer un dépôt");
            }

            // 3. Créer une transaction Pending (même si le montant est invalide)
            Transaction transaction = new Transaction(
                    user.getId(),
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
                logAuditEvent("DEPOSIT_FAILED", user.getId(), "Montant trop faible: " + request.getAmount());
                return DepositResult.failure("Montant minimum: " + MIN_DEPOSIT + "$");
            }

            if (request.getAmount().compareTo(MAX_DEPOSIT) > 0) {
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_FAILED", user.getId(), "Montant trop élevé: " + request.getAmount());
                return DepositResult.failure("Montant maximum: " + MAX_DEPOSIT + "$");
            }

            // 4. Service Paiement Simulé
            boolean paymentSuccess = paymentServicePort.processPayment(
                    request.getIdempotencyKey(),
                    user.getId()
            );

            if (!paymentSuccess) {
                // E1. Paiement rejeté : état Failed
                transaction.markAsFailed();
                transactionPort.save(transaction);
                logAuditEvent("DEPOSIT_PAYMENT_FAILED", user.getId(), "Paiement refusé par le service");
                return DepositResult.failure("Paiement refusé");
            }

            // 5. Créditer le portefeuille, journaliser et notifier
            user.creditBalance(request.getAmount());
            userPort.save(user);

            transaction.markAsSettled();
            transaction = transactionPort.save(transaction);

            // Audit de succès
            logAuditEvent("DEPOSIT_SUCCESS", user.getId(),
                    "Dépôt de " + request.getAmount() + "$ - Nouveau solde: " + user.getBalance());

            return WalletDepositPort.DepositResult.success(
                    "Dépôt de " + request.getAmount() + "$ effectué avec succès",
                    user.getBalance(),
                    transaction.getId()
            );

        } catch (Exception e) {
            logAuditEvent("DEPOSIT_ERROR", request.getUserId(), "Erreur technique: " + e.getMessage());
            return DepositResult.failure("Erreur technique lors du dépôt");
        }
    }
}
