package com.microservices.log430.walletservice.domain.port.out;

import com.microservices.log430.walletservice.domain.model.entities.WalletAudit;

public interface WalletAuditPort {
    void saveAudit(WalletAudit audit);
}
