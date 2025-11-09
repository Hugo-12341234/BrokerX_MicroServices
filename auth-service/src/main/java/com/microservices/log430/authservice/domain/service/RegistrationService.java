package com.microservices.log430.authservice.domain.service;

import com.microservices.log430.authservice.domain.model.entities.Audit;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.model.entities.VerificationToken;
import com.microservices.log430.authservice.domain.port.in.RegistrationPort;
import com.microservices.log430.authservice.domain.port.out.AuditPort;
import com.microservices.log430.authservice.domain.port.out.EmailSenderPort;
import com.microservices.log430.authservice.domain.port.out.UserPort;
import com.microservices.log430.authservice.domain.port.out.VerificationTokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RegistrationService implements RegistrationPort {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenPort tokenPort;
    private final EmailSenderPort emailSenderPort;
    private final AuditPort auditPort;
    private final String baseUrl;

    public RegistrationService(UserPort userPort,
                               PasswordEncoder passwordEncoder,
                               VerificationTokenPort tokenPort,
                               EmailSenderPort emailSenderPort,
                               AuditPort auditPort,
                               @Value("${app.base-url}") String baseUrl) {
        this.userPort = userPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenPort = tokenPort;
        this.emailSenderPort = emailSenderPort;
        this.auditPort = auditPort;
        this.baseUrl = baseUrl;
    }

    @Override
    public User register(String email, String rawPassword, String name, String adresse, LocalDate dateDeNaissance) {
        logger.info("Début de l'inscription pour l'email : {}", email);
        Optional<User> existing = Optional.empty();
        if (email != null && !email.isBlank()) {
            existing = userPort.findByEmail(email);
        }
        if (existing.isPresent()) {
            logger.warn("Tentative d'inscription avec un email déjà utilisé : {}", email);
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        // Créer l'entité du domaine avec le statut PENDING par défaut
        User user = new User(null, email, hashedPassword, name, adresse, dateDeNaissance, User.Status.PENDING);
        User savedUser = userPort.save(user);
        logger.info("Utilisateur enregistré avec statut PENDING : {}", savedUser.getId());

        // Générer le token brut et le hash
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        VerificationToken verificationToken = new VerificationToken(null, tokenHash, savedUser, LocalDateTime.now().plusDays(1));
        tokenPort.save(verificationToken);
        logger.info("Token de vérification généré et sauvegardé pour l'utilisateur : {}", savedUser.getId());

        // Envoyer l'email avec le lien d'activation
        String link = baseUrl + "/verify?token=" + rawToken;
        emailSenderPort.sendEmail(savedUser.getEmail(), "Activation de votre compte", "Cliquez ici pour activer votre compte : " + link);
        logger.info("Email d'activation envoyé à : {}", savedUser.getEmail());

        return savedUser;
    }

    @Override
    public boolean verifyUser(String token) {
        logger.info("Début de la vérification du token : {}", token);
        String tokenHash = hashToken(token);
        Optional<VerificationToken> optToken = tokenPort.findByTokenHash(tokenHash);
        if (optToken.isPresent() && optToken.get().getExpiryDate().isAfter(LocalDateTime.now())) {
            User user = optToken.get().getUser();
            user.activate();
            userPort.save(user);
            tokenPort.delete(optToken.get());
            logger.info("Utilisateur activé : {}. Token supprimé.", user.getId());
            String documentHash = hashToken(user.getEmail() + user.getName());
            Audit audit = new Audit(
                    user.getId(),
                    "USER_ACTIVATED",
                    LocalDateTime.now(),
                    documentHash
            );
            auditPort.saveAudit(audit);
            logger.info("Audit enregistré pour l'utilisateur : {}", user.getId());
            return true;
        }
        logger.warn("Échec de la vérification du token : {} (invalide ou expiré)", token);
        return false;
    }

    @Override
    public User getUserInfo(Long userId) {
        Optional<User> userOpt = userPort.findById(userId);
        return userOpt.orElse(null);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
