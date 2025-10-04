package com.microservices.log430.authservice.adapters.persistence.portImpl;

import com.microservices.log430.authservice.adapters.persistence.entities.UserEntity;
import com.microservices.log430.authservice.adapters.persistence.map.PersistenceMappers;
import com.microservices.log430.authservice.adapters.persistence.repository.SpringUserRepository;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.port.out.UserPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserAdapter implements UserPort {
    private final SpringUserRepository springUserRepository;

    public UserAdapter(SpringUserRepository springUserRepository) {
        this.springUserRepository = springUserRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springUserRepository.findByEmail(email)
                .map(PersistenceMappers::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springUserRepository.findById(id)
                .map(PersistenceMappers::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = PersistenceMappers.toEntity(user);
        UserEntity savedEntity = springUserRepository.save(entity);
        return PersistenceMappers.toDomain(savedEntity);
    }
}
