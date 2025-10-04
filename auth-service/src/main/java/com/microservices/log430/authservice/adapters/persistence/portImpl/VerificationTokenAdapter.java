package com.microservices.log430.authservice.adapters.persistence.portImpl;

import com.microservices.log430.authservice.adapters.persistence.entities.VerificationTokenEntity;
import com.microservices.log430.authservice.adapters.persistence.map.PersistenceMappers;
import com.microservices.log430.authservice.adapters.persistence.repository.VerificationTokenRepository;
import com.microservices.log430.authservice.domain.model.entities.VerificationToken;
import com.microservices.log430.authservice.domain.port.out.VerificationTokenPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VerificationTokenAdapter implements VerificationTokenPort {

    private final VerificationTokenRepository verificationTokenRepository;

    public VerificationTokenAdapter(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Override
    public VerificationToken save(VerificationToken token) {
        VerificationTokenEntity entity = PersistenceMappers.toEntity(token);
        VerificationTokenEntity savedEntity = verificationTokenRepository.save(entity);
        return PersistenceMappers.toDomain(savedEntity);
    }

    @Override
    public Optional<VerificationToken> findByTokenHash(String tokenHash) {
        return verificationTokenRepository.findByTokenHash(tokenHash)
                .map(PersistenceMappers::toDomain);
    }

    @Override
    public void delete(VerificationToken token) {
        VerificationTokenEntity entity = PersistenceMappers.toEntity(token);
        verificationTokenRepository.delete(entity);
    }
}
