package com.microservices.log430.authservice.adapters.persistence.map;

import com.microservices.log430.authservice.adapters.persistence.entities.UserEntity;
import com.microservices.log430.authservice.adapters.persistence.entities.VerificationTokenEntity;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.model.entities.VerificationToken;

public class PersistenceMappers {
    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        User user = new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getName(),
                entity.getAdresse(),
                entity.getDateDeNaissance(),
                User.Status.valueOf(entity.getStatus().name()),
                entity.getBalance()
        );
        return user;
    }

    public static UserEntity toEntity(User user) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setName(user.getName());
        entity.setAdresse(user.getAdresse());
        entity.setDateDeNaissance(user.getDateDeNaissance());
        entity.setStatus(UserEntity.Status.valueOf(user.getStatus().name()));
        entity.setBalance(user.getBalance());
        return entity;
    }

    public static VerificationToken toDomain(VerificationTokenEntity entity) {
        if (entity == null) return null;
        VerificationToken token = new VerificationToken(
                entity.getId(),
                entity.getTokenHash(),
                toDomain(entity.getUser()),
                entity.getExpiryDate()
        );
        return token;
    }

    public static VerificationTokenEntity toEntity(VerificationToken token) {
        if (token == null) return null;
        VerificationTokenEntity entity = new VerificationTokenEntity();
        entity.setId(token.getId());
        entity.setTokenHash(token.getTokenHash());
        entity.setUser(toEntity(token.getUser()));
        entity.setExpiryDate(token.getExpiryDate());
        return entity;
    }
}

