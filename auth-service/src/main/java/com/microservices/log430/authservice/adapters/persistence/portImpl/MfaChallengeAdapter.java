package com.microservices.log430.authservice.adapters.persistence.portImpl;

import com.microservices.log430.authservice.adapters.persistence.entities.MfaChallengeEntity;
import com.microservices.log430.authservice.adapters.persistence.map.MfaChallengeMapper;
import com.microservices.log430.authservice.adapters.persistence.repository.MfaChallengeRepository;
import com.microservices.log430.authservice.domain.model.entities.MfaChallenge;
import com.microservices.log430.authservice.domain.port.out.MfaChallengePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class MfaChallengeAdapter implements MfaChallengePort {
    private final MfaChallengeRepository mfaChallengeRepository;
    private final MfaChallengeMapper mfaChallengeMapper;

    @Autowired
    public MfaChallengeAdapter(MfaChallengeRepository mfaChallengeRepository, MfaChallengeMapper mfaChallengeMapper) {
        this.mfaChallengeRepository = mfaChallengeRepository;
        this.mfaChallengeMapper = mfaChallengeMapper;
    }

    @Override
    public MfaChallenge save(MfaChallenge challenge) {
        MfaChallengeEntity entity = mfaChallengeMapper.toEntity(challenge);
        MfaChallengeEntity savedEntity = mfaChallengeRepository.save(entity);
        return mfaChallengeMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MfaChallenge> findById(Long id) {
        return mfaChallengeRepository.findById(id)
                .map(mfaChallengeMapper::toDomain);
    }

    @Override
    public Optional<MfaChallenge> findByUserIdAndCode(Long userId, String code) {
        return mfaChallengeRepository.findByUserIdAndCode(userId, code)
                .map(mfaChallengeMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteExpiredChallenges() {
        mfaChallengeRepository.deleteExpiredChallenges(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        mfaChallengeRepository.deleteByUserId(userId);
    }
}
