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

@Service
public class RegistrationService implements RegistrationPort {
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
        Optional<User> existing = Optional.empty();
        if (email != null && !email.isBlank()) {
            existing = userPort.findByEmail(email);
        }
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        // Créer l'entité du domaine avec le statut PENDING par défaut
        User user = new User(null, email, hashedPassword, name, adresse, dateDeNaissance, User.Status.PENDING);
        User savedUser = userPort.save(user);

        // Générer le token brut et le hash
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        VerificationToken verificationToken = new VerificationToken(null, tokenHash, savedUser, LocalDateTime.now().plusDays(1));
        tokenPort.save(verificationToken);

        // Envoyer l'email avec le lien d'activation
        String link = baseUrl + "/verify?token=" + rawToken;
        emailSenderPort.sendEmail(savedUser.getEmail(), "Activation de votre compte", "Cliquez ici pour activer votre compte : " + link);

        return savedUser;
    }

    @Override
    public boolean verifyUser(String token) {
        String tokenHash = hashToken(token);
        Optional<VerificationToken> optToken = tokenPort.findByTokenHash(tokenHash);
        if (optToken.isPresent() && optToken.get().getExpiryDate().isAfter(LocalDateTime.now())) {
            User user = optToken.get().getUser();
            user.activate();
            userPort.save(user);
            tokenPort.delete(optToken.get());
            // Audit
            String documentHash = hashToken(user.getEmail() + user.getName());
            Audit audit = new Audit(
                    user.getId(),
                    "USER_ACTIVATED",
                    LocalDateTime.now(),
                    documentHash
            );
            auditPort.saveAudit(audit);
            return true;
        }
        return false;
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
