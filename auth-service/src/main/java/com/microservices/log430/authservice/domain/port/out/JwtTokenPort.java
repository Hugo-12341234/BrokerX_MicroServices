package com.microservices.log430.authservice.domain.port.out;

public interface JwtTokenPort {
    String generateToken(Long userId, String email);
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
    String getEmailFromToken(String token);
    String getRoleFromToken(String token);
}
