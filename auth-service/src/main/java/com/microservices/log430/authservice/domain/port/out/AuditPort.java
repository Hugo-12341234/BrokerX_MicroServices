package com.microservices.log430.authservice.domain.port.out;

import com.microservices.log430.authservice.domain.model.entities.Audit;

public interface AuditPort {
    void saveAudit(Audit audit);
}
