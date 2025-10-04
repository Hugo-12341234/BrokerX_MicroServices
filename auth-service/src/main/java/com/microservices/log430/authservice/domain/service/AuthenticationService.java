package com.microservices.log430.authservice.domain.service;

import com.microservices.log430.authservice.domain.model.entities.Audit;
import com.microservices.log430.authservice.domain.model.entities.MfaChallenge;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.port.in.AuthenticationPort;
import com.microservices.log430.authservice.domain.port.out.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService implements AuthenticationPort {
    private final UserPort userPort;
    private final MfaChallengePort mfaChallengePort;
    private final EmailSenderPort emailSenderPort;
    private final AuditPort auditPort;
    private final JwtTokenPort jwtTokenPort;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public AuthenticationService(UserPort userPort,
                                 MfaChallengePort mfaChallengePort,
                                 EmailSenderPort emailSenderPort,
                                 AuditPort auditPort,
                                 JwtTokenPort jwtTokenPort,
                                 PasswordEncoder passwordEncoder) {
        this.userPort = userPort;
        this.mfaChallengePort = mfaChallengePort;
        this.emailSenderPort = emailSenderPort;
        this.auditPort = auditPort;
        this.jwtTokenPort = jwtTokenPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String authenticate(String email, String password, String ipAddress, String userAgent) {
        // Valider identifiant/mot de passe
        Optional<User> userOpt = userPort.findByEmail(email);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            // Audit échec d'authentification
            auditFailedLogin(email, ipAddress, "INVALID_CREDENTIALS");
            throw new IllegalArgumentException("Identifiants invalides");
        }

        User user = userOpt.get();

        if (user.getStatus() == User.Status.SUSPENDED) {
            auditFailedLogin(email, ipAddress, "ACCOUNT_SUSPENDED");
            throw new IllegalArgumentException("Compte suspendu. Veuillez contacter le support.");
        }

        // Vérifier que le compte est actif
        if (user.getStatus() != User.Status.ACTIVE) {
            auditFailedLogin(email, ipAddress, "ACCOUNT_NOT_ACTIVE");
            throw new IllegalArgumentException("Compte non activé");
        }

        // Générer et envoyer le code MFA
        String mfaCode = generateMfaCode();
        MfaChallenge challenge = new MfaChallenge(
                user.getId(),
                mfaCode,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(5), // Expire dans 5 minutes
                ipAddress,
                0, // failedAttempts
                null // lockedUntil
        );

        MfaChallenge savedChallenge = mfaChallengePort.save(challenge);

        // Envoyer le code par email
        emailSenderPort.sendEmail(
                user.getEmail(),
                "Code de vérification - LOG430 BrokerX",
                "Votre code de vérification est : " + mfaCode + "\n\nCe code expire dans 5 minutes."
        );

        // Retourner l'ID du challenge pour la suite du processus
        return savedChallenge.getId().toString();
    }

    @Override
    public String verifyMfa(String challengeId, String code, String ipAddress, String userAgent) {
        try {
            Long id = Long.parseLong(challengeId);
            Optional<MfaChallenge> challengeOpt = mfaChallengePort.findById(id);

            if (challengeOpt.isEmpty()) {
                auditFailedMfa(challengeId, ipAddress, "CHALLENGE_NOT_FOUND");
                throw new IllegalArgumentException("Challenge non trouvé");
            }

            MfaChallenge challenge = challengeOpt.get();

            // Vérifier expiration
            if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
                auditFailedMfa(challengeId, ipAddress, "CHALLENGE_EXPIRED");
                throw new IllegalArgumentException("Le challenge MFA a expiré.");
            }
            // Vérifier réutilisation
            if (challenge.isUsed()) {
                auditFailedMfa(challengeId, ipAddress, "CHALLENGE_ALREADY_USED");
                throw new IllegalArgumentException("Ce challenge MFA a déjà été utilisé.");
            }

            // Vérifier le verrouillage
            if (challenge.getLockedUntil() != null && LocalDateTime.now().isBefore(challenge.getLockedUntil())) {
                long secondsLeft = java.time.Duration.between(LocalDateTime.now(), challenge.getLockedUntil()).getSeconds();
                throw new IllegalArgumentException("Votre compte est verrouillé. Veuillez réessayer dans " + secondsLeft + " secondes.");
            }

            // Vérifier que le code correspond
            if (!challenge.getCode().equals(code)) {
                int failedAttempts = challenge.getFailedAttempts() + 1;
                challenge.setFailedAttempts(failedAttempts);
                if (failedAttempts == 3) {
                    LocalDateTime lockedUntil = LocalDateTime.now().plusSeconds(30);
                    challenge.setLockedUntil(lockedUntil);
                    mfaChallengePort.save(challenge);
                    throw new IllegalArgumentException("Trop d'essais. Votre compte est verrouillé pour 30 secondes.");
                } else if (challenge.getFailedAttempts() > 3) {
                    // Suspendre le compte utilisateur
                    Optional<User> userOpt = userPort.findById(challenge.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setStatus(User.Status.SUSPENDED);
                        userPort.save(user);
                    }
                    throw new IllegalArgumentException("Votre compte a été suspendu. Redirection vers la page de connexion.");
                } else {
                    mfaChallengePort.save(challenge);
                    int remaining = 3 - failedAttempts;
                    throw new IllegalArgumentException("Code invalide. Il vous reste " + remaining + " tentative(s).");
                }
            }


            challenge.markAsUsed();
            mfaChallengePort.save(challenge);

            // Récupérer l'utilisateur pour créer le JWT
            Optional<User> userOpt = userPort.findById(challenge.getUserId());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Utilisateur non trouvé");
            }

            User user = userOpt.get();

            // Générer le JWT
            String jwtToken = jwtTokenPort.generateToken(user.getId(), user.getEmail());

            // Audit succès d'authentification avec TOUTES les informations demandées
            auditSuccessfulLoginWithToken(challenge.getUserId(), ipAddress, userAgent, jwtToken);

            return jwtToken;
        } catch (NumberFormatException e) {
            auditFailedMfa(challengeId, ipAddress, "INVALID_CHALLENGE_ID");
            throw new IllegalArgumentException("ID de challenge invalide");
        }
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenPort.validateToken(token);
    }

    @Override
    public void logout(String token) {
        // Avec JWT stateless, il n'y a rien à faire côté serveur
        // Le token expirera automatiquement
        // Auditer la déconnexion si nécessaire
        if (validateToken(token)) {
            Long userId = jwtTokenPort.getUserIdFromToken(token);
            auditLogout(userId);
        }
    }

    private String generateMfaCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private void auditFailedLogin(String email, String ipAddress, String reason) {
        Audit audit = new Audit(
                null, // userId null car échec
                "LOGIN_FAILED_" + reason,
                LocalDateTime.now(),
                generateHash(email + ipAddress + reason),
                ipAddress,
                null, // userAgent non disponible pour les échecs de login
                null  // sessionToken null car échec
        );
        auditPort.saveAudit(audit);
    }

    private void auditFailedMfa(String challengeId, String ipAddress, String reason) {
        // Pour les échecs MFA, on DOIT avoir un userId car l'utilisateur est déjà authentifié
        Long userId = null;
        try {
            Long id = Long.parseLong(challengeId);
            Optional<MfaChallenge> challengeOpt = mfaChallengePort.findById(id);
            if (challengeOpt.isPresent()) {
                userId = challengeOpt.get().getUserId();
            }
        } catch (Exception e) {
            // En cas d'erreur, userId reste null mais cela ne devrait pas arriver
        }

        Audit audit = new Audit(
                userId, // Maintenant on récupère l'userId du challenge MFA
                "MFA_FAILED_" + reason,
                LocalDateTime.now(),
                generateHash(challengeId + ipAddress + reason),
                ipAddress,
                null, // userAgent pourrait être ajouté si nécessaire
                null  // sessionToken null car échec
        );
        auditPort.saveAudit(audit);
    }

    private void auditSuccessfulLoginWithToken(Long userId, String ipAddress, String userAgent, String jwtToken) {
        Audit audit = new Audit(
                userId,
                "SESSION_CREATED",
                LocalDateTime.now(),
                generateHash(userId + ipAddress + userAgent + jwtToken),
                ipAddress,
                userAgent,
                jwtToken // Token JWT complet inclus dans l'audit
        );
        auditPort.saveAudit(audit);
    }

    private void auditSuccessfulLogin(Long userId, String ipAddress, String userAgent) {
        Audit audit = new Audit(
                userId,
                "LOGIN_SUCCESS",
                LocalDateTime.now(),
                generateHash(userId + ipAddress + userAgent),
                ipAddress,
                userAgent,
                null // sessionToken sera ajouté après génération du JWT
        );
        auditPort.saveAudit(audit);
    }

    private void auditLogout(Long userId) {
        Audit audit = new Audit(
                userId,
                "LOGOUT",
                LocalDateTime.now(),
                generateHash(userId + LocalDateTime.now().toString()),
                null, // IP non disponible pour logout
                null, // userAgent non disponible pour logout
                null  // token non stocké pour logout
        );
        auditPort.saveAudit(audit);
    }

    private String generateHash(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return java.util.Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du hash", e);
        }
    }
}
