package com.microservices.log430.authservice.adapters.persistence.repository;

import com.microservices.log430.authservice.adapters.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}

