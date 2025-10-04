package com.microservices.log430.authservice.domain.port.out;

import com.microservices.log430.authservice.domain.model.entities.User;

import java.util.Optional;

public interface UserPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User save(User user);
}
