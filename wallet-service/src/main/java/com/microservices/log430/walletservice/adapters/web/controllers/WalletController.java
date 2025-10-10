package com.microservices.log430.walletservice.adapters.web.controllers;

import com.microservices.log430.walletservice.adapters.web.dto.DepositResponse;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import com.microservices.log430.walletservice.adapters.web.dto.DepositRequest;
import com.microservices.log430.walletservice.adapters.web.dto.WalletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletDepositPort walletDepositPort;

    public WalletController(WalletDepositPort walletDepositPort) {
        this.walletDepositPort = walletDepositPort;
    }

    @GetMapping("")
    public ResponseEntity<WalletResponse> getWallet(HttpServletRequest httpRequest) {
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            return ResponseEntity.status(400)
                    .body(WalletResponse.failure("Header X-User-Id manquant"));
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(400)
                    .body(WalletResponse.failure("Header X-User-Id invalide"));
        }
        var walletOpt = walletDepositPort.getWalletByUserId(userId);
        if (walletOpt.isEmpty()) {
            return ResponseEntity.status(404).body(WalletResponse.failure("Portefeuille introuvable"));
        }
        var wallet = walletOpt.get();
        WalletResponse response = new WalletResponse(true, "Portefeuille récupéré avec succès", wallet);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(@RequestBody DepositRequest request,
                                                   HttpServletRequest httpRequest) {
        try {
            String userIdHeader = httpRequest.getHeader("X-User-Id");
            if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
                return ResponseEntity.status(400)
                        .body(DepositResponse.failure("Header X-User-Id manquant"));
            }
            Long userId;
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(400)
                        .body(DepositResponse.failure("Header X-User-Id invalide"));
            }
            String idempotencyKey = httpRequest.getHeader("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                idempotencyKey = UUID.randomUUID().toString();
            }
            WalletDepositPort.DepositRequest domainRequest = new WalletDepositPort.DepositRequest(
                    userId,
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
                    .body(DepositResponse.failure("Erreur lors du dépôt"));
        }
    }
}
