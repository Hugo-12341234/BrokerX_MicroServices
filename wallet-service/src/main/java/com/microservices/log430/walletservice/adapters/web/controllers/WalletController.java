package com.microservices.log430.walletservice.adapters.web.controllers;

import com.microservices.log430.walletservice.adapters.web.dto.DepositResponse;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import com.microservices.log430.walletservice.adapters.web.dto.DepositRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletDepositPort walletDepositPort;

    public WalletController(WalletDepositPort walletDepositPort) {
        this.walletDepositPort = walletDepositPort;
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
