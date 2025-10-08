package com.microservices.log430.authservice.domain.service;

import com.microservices.log430.authservice.domain.model.entities.Audit;
import com.microservices.log430.authservice.domain.model.entities.MfaChallenge;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.port.in.AuthenticationPort;
import com.microservices.log430.authservice.domain.port.out.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService implements AuthenticationPort {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
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
        logger.info("AuthenticationService instancié");
    }

    @Override
    public String authenticate(String email, String password, String ipAddress, String userAgent) {
        logger.info("Tentative d'authentification pour l'email : {} depuis IP {}", email, ipAddress);
        // Valider identifiant/mot de passe
        Optional<User> userOpt = userPort.findByEmail(email);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            logger.warn("Échec d'authentification pour l'email : {} depuis IP {} - Mot de passe ou email invalide", email, ipAddress);
            // Audit échec d'authentification
            auditFailedLogin(email, ipAddress, "INVALID_CREDENTIALS");
            throw new IllegalArgumentException("Identifiants invalides");
        }

        User user = userOpt.get();

        if (user.getStatus() == User.Status.SUSPENDED) {
            logger.warn("Compte suspendu pour l'email : {} depuis IP {}", email, ipAddress);
            auditFailedLogin(email, ipAddress, "ACCOUNT_SUSPENDED");
            throw new IllegalArgumentException("Compte suspendu. Veuillez contacter le support.");
        }

        // Vérifier que le compte est actif
        if (user.getStatus() != User.Status.ACTIVE) {
            logger.warn("Compte non activé pour l'email : {} depuis IP {}", email, ipAddress);
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

        logger.info("Code MFA généré pour l'utilisateur {} : {} (challengeId={})", user.getEmail(), mfaCode, savedChallenge.getId());
        // Envoyer le code par email
        emailSenderPort.sendEmail(
                user.getEmail(),
                "Code de vérification - LOG430 BrokerX",
                "Votre code de vérification est : " + mfaCode + "\n\nCe code expire dans 5 minutes."
        );
        logger.info("Email MFA envoyé à {}", user.getEmail());
        // Retourner l'ID du challenge pour la suite du processus ainsi
        return savedChallenge.getId().toString();
    }

    @Override
    public String verifyMfa(String challengeId, String code, String ipAddress, String userAgent) {
        logger.info("Vérification MFA pour challengeId={} depuis IP {}", challengeId, ipAddress);
        try {
            Long id = Long.parseLong(challengeId);
            Optional<MfaChallenge> challengeOpt = mfaChallengePort.findById(id);

            if (challengeOpt.isEmpty()) {
                logger.warn("Challenge MFA non trouvé : {} depuis IP {}", challengeId, ipAddress);
                auditFailedMfa(challengeId, ipAddress, "CHALLENGE_NOT_FOUND");
                throw new IllegalArgumentException("Challenge non trouvé");
            }

            MfaChallenge challenge = challengeOpt.get();

            // Vérifier expiration
            if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
                logger.warn("Challenge MFA expiré : {} depuis IP {}", challengeId, ipAddress);
                auditFailedMfa(challengeId, ipAddress, "CHALLENGE_EXPIRED");
                throw new IllegalArgumentException("Le challenge MFA a expiré.");
            }
            // Vérifier réutilisation
            if (challenge.isUsed()) {
                logger.warn("Challenge MFA déjà utilisé : {} depuis IP {}", challengeId, ipAddress);
                auditFailedMfa(challengeId, ipAddress, "CHALLENGE_ALREADY_USED");
                throw new IllegalArgumentException("Ce challenge MFA a déjà été utilisé.");
            }

            // Vérifier le verrouillage
            if (challenge.getLockedUntil() != null && LocalDateTime.now().isBefore(challenge.getLockedUntil())) {
                long secondsLeft = java.time.Duration.between(LocalDateTime.now(), challenge.getLockedUntil()).getSeconds();
                logger.warn("Compte verrouillé temporairement pour challengeId={} ({} secondes restantes) depuis IP {}", challengeId, secondsLeft, ipAddress);
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
                    logger.warn("Trop d'essais MFA pour challengeId={}. Compte verrouillé 30 secondes depuis IP {}", challengeId, ipAddress);
                    throw new IllegalArgumentException("Trop d'essais. Votre compte est verrouillé pour 30 secondes.");
                } else if (challenge.getFailedAttempts() > 3) {
                    // Suspendre le compte utilisateur
                    Optional<User> userOpt = userPort.findById(challenge.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setStatus(User.Status.SUSPENDED);
                        userPort.save(user);
                        logger.warn("Compte suspendu pour userId={} suite à trop d'échecs MFA depuis IP {}", user.getId(), ipAddress);
                    }
                    throw new IllegalArgumentException("Votre compte a été suspendu. Redirection vers la page de connexion.");
                } else {
                    mfaChallengePort.save(challenge);
                    int remaining = 3 - failedAttempts;
                    logger.warn("Code MFA invalide pour challengeId={}. Il reste {} tentative(s) depuis IP {}", challengeId, remaining, ipAddress);
                    throw new IllegalArgumentException("Code invalide. Il vous reste " + remaining + " tentative(s).");
                }
            }


            challenge.markAsUsed();
            mfaChallengePort.save(challenge);

            // Récupérer l'utilisateur pour créer le JWT
            Optional<User> userOpt = userPort.findById(challenge.getUserId());
            if (userOpt.isEmpty()) {
                logger.error("Utilisateur non trouvé pour challengeId={} lors de la génération du JWT", challengeId);
                throw new IllegalArgumentException("Utilisateur non trouvé");
            }

            User user = userOpt.get();
            String jwtToken = jwtTokenPort.generateToken(user.getId(), user.getEmail());
            logger.info("Authentification MFA réussie pour userId={} (challengeId={}) - JWT généré", user.getId(), challengeId);
            // Audit succès d'authentification avec TOUTES les informations demandées
            auditSuccessfulLoginWithToken(challenge.getUserId(), ipAddress, userAgent, jwtToken);

            return jwtToken;
        } catch (NumberFormatException e) {
            logger.error("ID de challenge MFA invalide : {} depuis IP {}", challengeId, ipAddress);
            auditFailedMfa(challengeId, ipAddress, "INVALID_CHALLENGE_ID");
            throw new IllegalArgumentException("ID de challenge invalide");
        }
    }

    @Override
    public boolean validateToken(String token) {
        logger.info("Validation du token JWT");
        boolean valid = jwtTokenPort.validateToken(token);
        logger.info("Résultat de la validation du token : {}", valid);
        return valid;
    }

    @Override
    public void logout(String token) {
        logger.info("Demande de déconnexion pour le token JWT");
        // Avec JWT stateless, il n'y a rien à faire côté serveur
        // Le token expirera automatiquement
        // Auditer la déconnexion si nécessaire
        if (validateToken(token)) {
            Long userId = jwtTokenPort.getUserIdFromToken(token);
            logger.info("Déconnexion réussie pour userId={}", userId);
            auditLogout(userId);
        } else {
            logger.warn("Déconnexion ignorée : token JWT invalide");
        }
    }

    private String generateMfaCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private void auditFailedLogin(String email, String ipAddress, String reason) {
        logger.info("Audit échec de login : email={}, ip={}, raison={}", email, ipAddress, reason);
        try {
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
        } catch (Exception e) {
            logger.error("Erreur lors de l'audit d'échec de login : {}", e.getMessage());
        }
    }

    private void auditFailedMfa(String challengeId, String ipAddress, String reason) {
        logger.info("Audit échec MFA : challengeId={}, ip={}, raison={}", challengeId, ipAddress, reason);
        Long userId = null;
        try {
            Long id = Long.parseLong(challengeId);
            Optional<MfaChallenge> challengeOpt = mfaChallengePort.findById(id);
            if (challengeOpt.isPresent()) {
                userId = challengeOpt.get().getUserId();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du userId pour l'audit MFA : {}", e.getMessage());
        }
        try {
            Audit audit = new Audit(
                userId,
                "MFA_FAILED_" + reason,
                LocalDateTime.now(),
                generateHash(challengeId + ipAddress + reason),
                ipAddress,
                null,
                null
            );
            auditPort.saveAudit(audit);
        } catch (Exception e) {
            logger.error("Erreur lors de l'audit d'échec MFA : {}", e.getMessage());
        }
    }

    private void auditSuccessfulLoginWithToken(Long userId, String ipAddress, String userAgent, String jwtToken) {
        logger.info("Audit succès login avec token : userId={}, ip={}, userAgent={}", userId, ipAddress, userAgent);
        try {
            Audit audit = new Audit(
                userId,
                "SESSION_CREATED",
                LocalDateTime.now(),
                generateHash(userId + ipAddress + userAgent + jwtToken),
                ipAddress,
                userAgent,
                jwtToken
            );
            auditPort.saveAudit(audit);
        } catch (Exception e) {
            logger.error("Erreur lors de l'audit de succès login avec token : {}", e.getMessage());
        }
    }

    private void auditSuccessfulLogin(Long userId, String ipAddress, String userAgent) {
        logger.info("Audit succès login : userId={}, ip={}, userAgent={}", userId, ipAddress, userAgent);
        try {
            Audit audit = new Audit(
                userId,
                "LOGIN_SUCCESS",
                LocalDateTime.now(),
                generateHash(userId + ipAddress + userAgent),
                ipAddress,
                userAgent,
                null
            );
            auditPort.saveAudit(audit);
        } catch (Exception e) {
            logger.error("Erreur lors de l'audit de succès login : {}", e.getMessage());
        }
    }

    private void auditLogout(Long userId) {
        logger.info("Audit logout : userId={}", userId);
        try {
            Audit audit = new Audit(
                userId,
                "LOGOUT",
                LocalDateTime.now(),
                generateHash(userId + LocalDateTime.now().toString()),
                null,
                null,
                null
            );
            auditPort.saveAudit(audit);
        } catch (Exception e) {
            logger.error("Erreur lors de l'audit de logout : {}", e.getMessage());
        }
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
