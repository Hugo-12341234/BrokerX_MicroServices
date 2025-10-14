package com.microservices.log430.matchingservice.adapters.persistence.repository;

import com.microservices.log430.matchingservice.adapters.persistence.entities.ExecutionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionReportRepository extends JpaRepository<ExecutionReportEntity, Long> {
    java.util.List<ExecutionReportEntity> findAllByOrderId(Long orderId);
}

