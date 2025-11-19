package com.microservices.log430.matchingservice.domain.port.out;

import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;
import java.util.List;
import java.util.Optional;

public interface ExecutionReportPort {
    ExecutionReport save(ExecutionReport report);
    Optional<ExecutionReport> findById(Long id);
    List<ExecutionReport> findAllByOrderId(Long orderId);
    List<ExecutionReport> findAll();
    Optional<ExecutionReport> findLastBySymbol(String symbol);
}
