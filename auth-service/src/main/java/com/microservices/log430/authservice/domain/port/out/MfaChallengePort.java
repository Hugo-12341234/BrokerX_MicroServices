package com.microservices.log430.authservice.domain.port.out;

import com.microservices.log430.authservice.domain.model.entities.MfaChallenge;

import java.util.Optional;

public interface MfaChallengePort {
    MfaChallenge save(MfaChallenge challenge);
    Optional<MfaChallenge> findById(Long id);
    Optional<MfaChallenge> findByUserIdAndCode(Long userId, String code);
    void deleteExpiredChallenges();
    void deleteByUserId(Long userId);
}

