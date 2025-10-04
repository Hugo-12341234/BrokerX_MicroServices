package com.microservices.log430.authservice.adapters.persistence.portImpl;

import com.microservices.log430.authservice.adapters.persistence.entities.UserAuditEntity;
import com.microservices.log430.authservice.adapters.persistence.map.AuditMapper;
import com.microservices.log430.authservice.adapters.persistence.repository.UserAuditRepository;
import com.microservices.log430.authservice.domain.model.entities.Audit;
import com.microservices.log430.authservice.domain.port.out.AuditPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditAdapter implements AuditPort {
    private final UserAuditRepository auditRepository;
    private final AuditMapper auditMapper;

    @Autowired
    public AuditAdapter(UserAuditRepository auditRepository, AuditMapper auditMapper) {
        this.auditRepository = auditRepository;
        this.auditMapper = auditMapper;
    }

    @Override
    public void saveAudit(Audit audit) {
        UserAuditEntity entity = auditMapper.toEntity(audit);
        auditRepository.save(entity);
    }
}

