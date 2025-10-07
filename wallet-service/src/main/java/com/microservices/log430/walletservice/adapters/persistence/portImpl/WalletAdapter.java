package com.microservices.log430.walletservice.adapters.persistence.portImpl;

import com.microservices.log430.walletservice.domain.port.out.WalletPort;
import com.microservices.log430.walletservice.domain.model.entities.Wallet;
import com.microservices.log430.walletservice.adapters.persistence.entities.WalletEntity;
import com.microservices.log430.walletservice.adapters.persistence.repository.WalletRepository;
import com.microservices.log430.walletservice.adapters.persistence.map.WalletMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletAdapter implements WalletPort {
    private final WalletRepository walletRepository;

    public WalletAdapter(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return walletRepository.findById(id)
                .map(WalletMapper::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = WalletMapper.toEntity(wallet);
        WalletEntity saved = walletRepository.save(entity);
        return WalletMapper.toDomain(saved);
    }

    @Override
    public Optional<Wallet> findByUserId(Long userId) {
        WalletEntity entity = walletRepository.findByUserId(userId);
        return Optional.ofNullable(WalletMapper.toDomain(entity));
    }
}
