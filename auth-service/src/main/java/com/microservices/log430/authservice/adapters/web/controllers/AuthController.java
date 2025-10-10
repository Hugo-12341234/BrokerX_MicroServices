package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.adapters.web.dto.LoginRequest;
import com.microservices.log430.authservice.adapters.web.dto.LoginResponse;
import com.microservices.log430.authservice.adapters.web.dto.MfaVerificationRequest;
import com.microservices.log430.authservice.adapters.web.dto.MfaVerificationResponse;
import com.microservices.log430.authservice.adapters.web.dto.ErrorResponse;
import com.microservices.log430.authservice.domain.port.in.AuthenticationPort;
import com.microservices.log430.authservice.domain.port.out.MfaChallengePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationPort authenticationPort;
    private final MfaChallengePort mfaChallengePort;

    @Autowired
    public AuthController(AuthenticationPort authenticationPort, MfaChallengePort mfaChallengePort) {
        this.authenticationPort = authenticationPort;
        this.mfaChallengePort = mfaChallengePort;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        logger.info("Tentative de connexion pour l'email : {}", request.getEmail());
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String challengeId = authenticationPort.authenticate(request.getEmail(), request.getPassword(), ipAddress, userAgent);
            logger.info("Code de vérification MFA envoyé à l'utilisateur : {}", request.getEmail());
            LoginResponse response = new LoginResponse(
                    challengeId,
                    "Code de vérification envoyé par email. Veuillez vérifier votre boîte de réception.",
                    true,
                    true
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Échec de la connexion pour l'email {} : {}", request.getEmail(), e.getMessage());
            int status = HttpStatus.BAD_REQUEST.value();
            String error = "Bad Request";
            if ("Utilisateur non trouvé".equals(e.getMessage())) {
                status = HttpStatus.NOT_FOUND.value();
                error = "Not Found";
            } else if ("Mot de passe incorrect".equals(e.getMessage())) {
                status = HttpStatus.UNAUTHORIZED.value();
                error = "Unauthorized";
            }
            String path = httpRequest.getRequestURI();
            String requestId = httpRequest.getHeader("X-Request-Id");
            ErrorResponse errResp = new ErrorResponse(
                java.time.Instant.now(),
                path,
                status,
                error,
                e.getMessage(),
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(status).body(errResp);
        }
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerificationRequest request,
                                                             HttpServletRequest httpRequest,
                                                             HttpServletResponse httpResponse) {
        logger.info("Vérification MFA pour le challengeId : {}", request.getChallengeId());
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String jwtTokenString = authenticationPort.verifyMfa(request.getChallengeId(), request.getCode(), ipAddress, userAgent);
            jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt", jwtTokenString);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // true en production avec HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60);
            httpResponse.addCookie(jwtCookie);
            // Récupérer le userId à partir du challenge
            Long userId = null;
            try {
                Long challengeIdLong = Long.parseLong(request.getChallengeId());
                var challengeOpt = mfaChallengePort.findById(challengeIdLong);
                if (challengeOpt.isPresent()) {
                    userId = challengeOpt.get().getUserId();
                }
            } catch (Exception e) {
                logger.warn("Impossible d'extraire le userId du challenge : {}", e.getMessage());
            }
            logger.info("Authentification MFA réussie, JWT généré et stocké en cookie pour le challengeId : {} (userId={})", request.getChallengeId(), userId);
            MfaVerificationResponse response = new MfaVerificationResponse(
                    true,
                    "Authentification réussie ! Redirection vers le dashboard...",
                    jwtTokenString,
                    "success",
                    userId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Échec de la vérification MFA pour le challengeId {} : {}", request.getChallengeId(), e.getMessage());
            int status = HttpStatus.BAD_REQUEST.value();
            String error = "Bad Request";
            String msg = e.getMessage();
            if (msg.contains("verrouillé") || msg.contains("bloqué")) {
                status = HttpStatus.FORBIDDEN.value();
                error = "Forbidden";
            } else if (msg.contains("suspendu")) {
                status = HttpStatus.FORBIDDEN.value();
                error = "Forbidden";
            } else if ("Code MFA incorrect".equals(msg)) {
                status = HttpStatus.UNAUTHORIZED.value();
                error = "Unauthorized";
            }
            String path = httpRequest.getRequestURI();
            String requestId = httpRequest.getHeader("X-Request-Id");
            ErrorResponse errResp = new ErrorResponse(
                java.time.Instant.now(),
                path,
                status,
                error,
                msg,
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(status).body(errResp);
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String jwtTokenString = getJwtFromRequest(request);
        if (jwtTokenString != null) {
            logger.info("Déconnexion de l'utilisateur avec JWT : {}", jwtTokenString);
            authenticationPort.logout(jwtTokenString);
        } else {
            logger.warn("Tentative de déconnexion sans JWT valide");
        }
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);
        return ResponseEntity.ok().body("Déconnexion réussie");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Chercher d'abord dans les cookies
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback: chercher dans l'en-tête Authorization
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
