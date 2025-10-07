package com.microservices.log430.walletservice.domain.port.out;

import com.microservices.log430.walletservice.domain.model.entities.Wallet;
import java.util.Optional;
import java.util.UUID;

public interface WalletPort {
    Optional<Wallet> findById(UUID id);
    Wallet save(Wallet wallet);
    Optional<Wallet> findByUserId(Long userId);
}
