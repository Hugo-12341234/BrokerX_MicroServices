package com.microservices.log430.orderservice.adapters.persistence.repository;

import com.microservices.log430.orderservice.adapters.persistence.entities.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByClientOrderId(String clientOrderId);
    List<OrderEntity> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderEntity w WHERE w.userId = ?1")
    void deleteByUserId(Long userId);
}
