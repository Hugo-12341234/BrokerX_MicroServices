package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.adapters.web.dto.LoginRequest;
import com.microservices.log430.authservice.adapters.web.dto.LoginResponse;
import com.microservices.log430.authservice.adapters.web.dto.MfaVerificationRequest;
import com.microservices.log430.authservice.adapters.web.dto.MfaVerificationResponse;
import com.microservices.log430.authservice.domain.port.in.AuthenticationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationPort authenticationPort;

    @Autowired
    public AuthController(AuthenticationPort authenticationPort) {
        this.authenticationPort = authenticationPort;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
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
            LoginResponse response = new LoginResponse(null, e.getMessage(), false, false);
            if ("Utilisateur non trouvé".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else if ("Mot de passe incorrect".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<MfaVerificationResponse> verifyMfa(@RequestBody MfaVerificationRequest request,
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
            logger.info("Authentification MFA réussie, JWT généré et stocké en cookie pour le challengeId : {}", request.getChallengeId());
            MfaVerificationResponse response = new MfaVerificationResponse(
                    true,
                    "Authentification réussie ! Redirection vers le dashboard...",
                    jwtTokenString,
                    "success"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Échec de la vérification MFA pour le challengeId {} : {}", request.getChallengeId(), e.getMessage());
            String status = "error";
            String msg = e.getMessage();
            if (msg.contains("verrouillé") || msg.contains("bloqué")) {
                status = "locked";
            } else if (msg.contains("suspendu")) {
                status = "suspended";
            }
            MfaVerificationResponse response = new MfaVerificationResponse(
                    false,
                    msg,
                    null,
                    status
            );
            if ("Code MFA incorrect".equals(msg)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            } else if (status.equals("locked")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else if (status.equals("suspended")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
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