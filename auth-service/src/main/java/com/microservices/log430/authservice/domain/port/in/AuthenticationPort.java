package com.microservices.log430.authservice.domain.port.in;

public interface AuthenticationPort {
    String authenticate(String email, String password, String ipAddress, String userAgent);
    String verifyMfa(String challengeId, String code, String ipAddress, String userAgent);
    boolean validateToken(String token);
    void logout(String token);
}