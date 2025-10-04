package com.microservices.log430.authservice.adapters.persistence.repository;

import com.microservices.log430.authservice.adapters.persistence.entities.VerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {
    Optional<VerificationTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationTokenEntity v WHERE v.user.id = ?1")
    void deleteByUserId(Long userId);
}