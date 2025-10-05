package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.adapters.web.dto.LoginRequest;
import com.microservices.log430.authservice.adapters.web.dto.LoginResponse;
import com.microservices.log430.authservice.adapters.web.dto.MfaVerificationRequest;
import com.microservices.log430.authservice.adapters.web.dto.MfaVerificationResponse;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.port.in.AuthenticationPort;
import com.microservices.log430.authservice.domain.port.out.JwtTokenPort;
import com.microservices.log430.authservice.domain.port.out.UserPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationPort authenticationPort;
    private final JwtTokenPort jwtTokenPort;
    private final UserPort userPort;

    @Autowired
    public AuthController(AuthenticationPort authenticationPort, JwtTokenPort jwtTokenPort, UserPort userPort) {
        this.authenticationPort = authenticationPort;
        this.jwtTokenPort = jwtTokenPort;
        this.userPort = userPort;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            String challengeId = authenticationPort.authenticate(request.getEmail(), request.getPassword(), ipAddress, userAgent);

            LoginResponse response = new LoginResponse(
                    challengeId,
                    "Code de vérification envoyé par email. Veuillez vérifier votre boîte de réception.",
                    true,
                    true
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            LoginResponse response = new LoginResponse(null, e.getMessage(), false, false);
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/mfa")
    public String showMfaPage(@RequestParam String challengeId, Model model) {
        model.addAttribute("challengeId", challengeId);
        return "auth/mfa";
    }

    @PostMapping("/verify-mfa")
    @ResponseBody
    public ResponseEntity<MfaVerificationResponse> verifyMfa(@RequestBody MfaVerificationRequest request,
                                                             HttpServletRequest httpRequest,
                                                             HttpServletResponse httpResponse) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Générer le JWT complet (header.payload.signature)
            String jwtTokenString = authenticationPort.verifyMfa(request.getChallengeId(), request.getCode(), ipAddress, userAgent);

            // Stocker le JWT dans un cookie sécurisé
            jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt", jwtTokenString);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // true en production avec HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 heures
            httpResponse.addCookie(jwtCookie);

            MfaVerificationResponse response = new MfaVerificationResponse(
                    true,
                    "Authentification réussie ! Redirection vers le dashboard...",
                    jwtTokenString
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            MfaVerificationResponse response = new MfaVerificationResponse(
                    false,
                    e.getMessage(),
                    null
            );
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpServletRequest request, Model model) {
        String jwtTokenString = getJwtFromRequest(request);

        if (jwtTokenString == null || !authenticationPort.validateToken(jwtTokenString)) {
            return "redirect:/auth/login";
        }

        // Extraire les informations utilisateur du JWT
        try {
            Long userId = jwtTokenPort.getUserIdFromToken(jwtTokenString);
            String userEmail = jwtTokenPort.getEmailFromToken(jwtTokenString);

            // Récupérer l'utilisateur complet pour avoir son nom et son solde
            Optional<User> userOpt = userPort.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("userName", user.getName());
                model.addAttribute("userEmail", user.getEmail());
            } else {
                model.addAttribute("userName", "Utilisateur");
                model.addAttribute("userEmail", userEmail);
                model.addAttribute("userBalance", java.math.BigDecimal.ZERO);
            }
        } catch (Exception e) {
            model.addAttribute("userName", "Utilisateur");
            model.addAttribute("userEmail", "");
            model.addAttribute("userBalance", java.math.BigDecimal.ZERO);
        }

        return "auth/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String jwtTokenString = getJwtFromRequest(request);

        if (jwtTokenString != null) {
            // Auditer la déconnexion
            authenticationPort.logout(jwtTokenString);
        }

        // Supprimer le cookie JWT
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("jwt", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Expirer immédiatement
        response.addCookie(jwtCookie);

        return "redirect:/auth/login";
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