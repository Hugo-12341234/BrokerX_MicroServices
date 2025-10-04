package com.microservices.log430.authservice.adapters.persistence.map;

import com.microservices.log430.authservice.adapters.persistence.entities.MfaChallengeEntity;
import com.microservices.log430.authservice.domain.model.entities.MfaChallenge;
import org.springframework.stereotype.Component;

@Component
public class MfaChallengeMapper {

    public MfaChallengeEntity toEntity(MfaChallenge challenge) {
        MfaChallengeEntity entity = new MfaChallengeEntity(
                challenge.getUserId(),
                challenge.getCode(),
                challenge.getCreatedAt(),
                challenge.getExpiresAt(),
                challenge.isUsed(),
                challenge.getIpAddress(),
                challenge.getFailedAttempts(),
                challenge.getLockedUntil()
        );
        entity.setId(challenge.getId());
        return entity;
    }

    public MfaChallenge toDomain(MfaChallengeEntity entity) {
        MfaChallenge challenge = new MfaChallenge(
                entity.getUserId(),
                entity.getCode(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getIpAddress(),
                entity.getFailedAttempts(),
                entity.getLockedUntil()
        );
        challenge.setId(entity.getId());
        challenge.setUsed(entity.isUsed());
        return challenge;
    }
}
