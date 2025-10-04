package com.microservices.log430.authservice.adapters.persistence.repository;

import com.microservices.log430.authservice.adapters.persistence.entities.UserAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuditRepository extends JpaRepository<UserAuditEntity, Long> {
}
