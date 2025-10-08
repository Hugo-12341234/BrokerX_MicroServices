package com.microservices.log430.walletservice.adapters.persistence.portImpl;

import com.microservices.log430.walletservice.domain.model.entities.WalletAudit;
import com.microservices.log430.walletservice.domain.port.out.WalletAuditPort;
import com.microservices.log430.walletservice.adapters.persistence.entities.WalletAuditEntity;
import com.microservices.log430.walletservice.adapters.persistence.map.WalletAuditMapper;
import com.microservices.log430.walletservice.adapters.persistence.repository.WalletAuditRepository;
import org.springframework.stereotype.Component;

@Component
public class WalletAuditAdapter implements WalletAuditPort {
    private final WalletAuditRepository walletAuditRepository;

    public WalletAuditAdapter(WalletAuditRepository walletAuditRepository) {
        this.walletAuditRepository = walletAuditRepository;
    }

    @Override
    public void saveAudit(WalletAudit audit) {
        WalletAuditEntity entity = WalletAuditMapper.toEntity(audit);
        walletAuditRepository.save(entity);
    }
}
