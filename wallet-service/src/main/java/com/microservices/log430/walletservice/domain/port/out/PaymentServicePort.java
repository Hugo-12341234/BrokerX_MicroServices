package com.microservices.log430.walletservice.domain.port.out;

public interface PaymentServicePort {
    boolean processPayment(String idempotencyKey, Long userId);
}