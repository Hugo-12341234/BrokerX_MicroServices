package com.microservices.log430.walletservice.adapters.web.controllers;

import com.microservices.log430.walletservice.adapters.web.dto.DepositResponse;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletDepositPort walletDepositPort;
    private final UserPort userPort;
    private final AuthenticationPort authenticationPort;
    private final JwtTokenPort jwtTokenPort;

    public WalletController(WalletDepositPort walletDepositPort,
                            UserPort userPort,
                            AuthenticationPort authenticationPort,
                            JwtTokenPort jwtTokenPort) {
        this.walletDepositPort = walletDepositPort;
        this.userPort = userPort;
        this.authenticationPort = authenticationPort;
        this.jwtTokenPort = jwtTokenPort;
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(@RequestBody DepositRequest request,
                                                   HttpServletRequest httpRequest) {
        // Récupérer et valider le JWT comme dans AuthController
        String jwtTokenString = getJwtFromRequest(httpRequest);

        if (jwtTokenString == null || !authenticationPort.validateToken(jwtTokenString)) {
            return ResponseEntity.status(401)
                    .body(DepositResponse.failure("Non authentifié"));
        }

        try {
            // Extraire l'utilisateur du JWT
            Long userId = jwtTokenPort.getUserIdFromToken(jwtTokenString);
            Optional<User> userOpt = userPort.findById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                        .body(DepositResponse.failure("Utilisateur non trouvé"));
            }

            User user = userOpt.get();

            // Récupérer la clé d'idempotence depuis l'en-tête ou générer une nouvelle
            String idempotencyKey = httpRequest.getHeader("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                idempotencyKey = UUID.randomUUID().toString();
            }

            WalletDepositPort.DepositRequest domainRequest = new WalletDepositPort.DepositRequest(
                    user.getId(),
                    request.getAmount(),
                    idempotencyKey
            );

            WalletDepositPort.DepositResult result = walletDepositPort.deposit(domainRequest);

            if (result.isSuccess()) {
                return ResponseEntity.ok(DepositResponse.success(
                        result.getMessage(),
                        result.getNewBalance(),
                        result.getTransactionId()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(DepositResponse.failure(result.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(DepositResponse.failure("Erreur lors de l'authentification"));
        }
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
}
