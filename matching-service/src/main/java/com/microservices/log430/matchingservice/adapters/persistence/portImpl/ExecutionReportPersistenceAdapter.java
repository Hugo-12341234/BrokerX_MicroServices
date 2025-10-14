package com.microservices.log430.matchingservice.adapters.persistence.portImpl;

import com.microservices.log430.matchingservice.adapters.persistence.entities.ExecutionReportEntity;
import com.microservices.log430.matchingservice.adapters.persistence.map.ExecutionReportMapper;
import com.microservices.log430.matchingservice.adapters.persistence.repository.ExecutionReportRepository;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;
import com.microservices.log430.matchingservice.domain.port.out.ExecutionReportPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ExecutionReportPersistenceAdapter implements ExecutionReportPort {
    private final ExecutionReportRepository repository;

    public ExecutionReportPersistenceAdapter(ExecutionReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExecutionReport save(ExecutionReport report) {
        ExecutionReportEntity entity = ExecutionReportMapper.toEntity(report);
        ExecutionReportEntity saved = repository.save(entity);
        return ExecutionReportMapper.toDomain(saved);
    }

    @Override
    public Optional<ExecutionReport> findById(Long id) {
        return repository.findById(id).map(ExecutionReportMapper::toDomain);
    }

    @Override
    public List<ExecutionReport> findAllByOrderId(Long orderId) {
        return repository.findAllByOrderId(orderId).stream().map(ExecutionReportMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionReport> findAll() {
        return repository.findAll().stream().map(ExecutionReportMapper::toDomain).collect(Collectors.toList());
    }
}

