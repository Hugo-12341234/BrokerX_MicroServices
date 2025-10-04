package com.microservices.log430.authservice.adapters.persistence.repository;

import com.microservices.log430.authservice.adapters.persistence.entities.MfaChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MfaChallengeRepository extends JpaRepository<MfaChallengeEntity, Long> {
    Optional<MfaChallengeEntity> findByUserIdAndCode(Long userId, String code);
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM MfaChallengeEntity m WHERE m.expiresAt < :now")
    void deleteExpiredChallenges(LocalDateTime now);
}

