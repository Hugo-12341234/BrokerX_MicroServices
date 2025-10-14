package com.microservices.log430.matchingservice.adapters.persistence.map;

import com.microservices.log430.matchingservice.adapters.persistence.entities.ExecutionReportEntity;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;

public class ExecutionReportMapper {
    public static ExecutionReport toDomain(ExecutionReportEntity entity) {
        if (entity == null) return null;
        ExecutionReport domain = new ExecutionReport();
        domain.setId(entity.getId());
        domain.setOrderId(entity.getOrderId());
        domain.setFillQuantity(entity.getFillQuantity());
        domain.setFillPrice(entity.getFillPrice());
        domain.setFillType(entity.getFillType());
        domain.setExecutionTime(entity.getExecutionTime());
        return domain;
    }

    public static ExecutionReportEntity toEntity(ExecutionReport domain) {
        if (domain == null) return null;
        ExecutionReportEntity entity = new ExecutionReportEntity();
        entity.setId(domain.getId());
        entity.setOrderId(domain.getOrderId());
        entity.setFillQuantity(domain.getFillQuantity());
        entity.setFillPrice(domain.getFillPrice());
        entity.setFillType(domain.getFillType());
        entity.setExecutionTime(domain.getExecutionTime());
        return entity;
    }
}

