package com.microservices.log430.authservice.domain.port.out;

import com.microservices.log430.authservice.domain.model.entities.VerificationToken;
import java.util.Optional;

public interface VerificationTokenPort {
    VerificationToken save(VerificationToken token);
    Optional<VerificationToken> findByTokenHash(String tokenHash);
    void delete(VerificationToken token);
}