package com.microservices.log430.walletservice.adapters.persistence.map;

import com.microservices.log430.walletservice.adapters.persistence.entities.WalletAuditEntity;
import com.microservices.log430.walletservice.domain.model.entities.WalletAudit;

public class WalletAuditMapper {
    public static WalletAudit toDomain(WalletAuditEntity entity) {
        if (entity == null) return null;
        WalletAudit audit = new WalletAudit(
            entity.getUserId(),
            entity.getDetails(),
            entity.getCreatedAt()
        );
        audit.setId(entity.getId());
        return audit;
    }

    public static WalletAuditEntity toEntity(WalletAudit domain) {
        if (domain == null) return null;
        return new WalletAuditEntity(
                domain.getUserId(),
                domain.getDetails(),
                domain.getCreatedAt()
        );
    }
}
