package com.microservices.log430.authservice.adapters.persistence.map;

import com.microservices.log430.authservice.adapters.persistence.entities.UserAuditEntity;
import com.microservices.log430.authservice.domain.model.entities.Audit;
import org.springframework.stereotype.Component;

@Component
public class AuditMapper {
    public UserAuditEntity toEntity(Audit audit) {
        return new UserAuditEntity(
                audit.getUserId(),
                audit.getAction(),
                audit.getTimestamp(),
                audit.getDocumentHash(),
                audit.getIpAddress(),
                audit.getUserAgent(),
                audit.getSessionToken()
        );
    }

    public Audit toDomain(UserAuditEntity entity) {
        Audit audit = new Audit(
                entity.getUserId(),
                entity.getAction(),
                entity.getTimestamp(),
                entity.getDocumentHash(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getSessionToken()
        );
        audit.setId(entity.getId());
        return audit;
    }
}

