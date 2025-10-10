package com.microservices.log430.walletservice.adapters.web.controllers;

import com.microservices.log430.walletservice.adapters.web.dto.DepositResponse;
import com.microservices.log430.walletservice.domain.port.in.WalletDepositPort;
import com.microservices.log430.walletservice.adapters.web.dto.DepositRequest;
import com.microservices.log430.walletservice.adapters.web.dto.WalletResponse;
import com.microservices.log430.walletservice.adapters.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletDepositPort walletDepositPort;

    public WalletController(WalletDepositPort walletDepositPort) {
        this.walletDepositPort = walletDepositPort;
    }

    @GetMapping("")
    public ResponseEntity<?> getWallet(HttpServletRequest httpRequest) {
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String path = httpRequest.getRequestURI();
        String requestId = httpRequest.getHeader("X-Request-Id");
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-User-Id manquant",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException e) {
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Header X-User-Id invalide",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        var walletOpt = walletDepositPort.getWalletByUserId(userId);
        if (walletOpt.isEmpty()) {
            ErrorResponse err = new ErrorResponse(
                Instant.now(),
                path,
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Portefeuille introuvable",
                requestId != null ? requestId : ""
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
        var wallet = walletOpt.get();
        WalletResponse response = new WalletResponse(true, "Portefeuille récupéré avec succès", wallet);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request,
                                                   HttpServletRequest httpRequest) {
        try {
            String userIdHeader = httpRequest.getHeader("X-User-Id");
            String path = httpRequest.getRequestURI();
            String requestId = httpRequest.getHeader("X-Request-Id");
            if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
                ErrorResponse err = new ErrorResponse(
                        Instant.now(),
                        path,
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Header X-User-Id manquant",
                        requestId != null ? requestId : ""
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
            Long userId;
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException e) {
                ErrorResponse err = new ErrorResponse(
                        Instant.now(),
                        path,
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Header X-User-Id invalide",
                        requestId != null ? requestId : ""
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
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
                ErrorResponse err = new ErrorResponse(
                        Instant.now(),
                        path,
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        result.getMessage(),
                        requestId != null ? requestId : ""
                );

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
        } catch (Exception e) {
            ErrorResponse err = new ErrorResponse(
                    Instant.now(),
                    httpRequest.getRequestURI(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "Erreur lors du dépôt",
                    httpRequest.getHeader("X-Request-Id") != null ? httpRequest.getHeader("X-Request-Id") : ""
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
