package com.microservices.log430.walletservice.adapters.persistence.portImpl;

import com.microservices.log430.walletservice.domain.port.out.PaymentServicePort;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentServiceAdapter implements PaymentServicePort {

    @Override
    public boolean processPayment(String idempotencyKey, Long userId) {
        // Service simulé : autorise toutes les transactions
        // L'idempotence est gérée au niveau métier, pas ici
        return true;
    }
}
