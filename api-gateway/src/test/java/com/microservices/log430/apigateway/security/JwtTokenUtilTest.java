package com.microservices.log430.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private SecretKey testSecretKey;
    private String testSecret;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Generate a secure Base64 secret string that's long enough for HS512 (at least 64 bytes)
        SecretKey tempKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        testSecret = Base64.getEncoder().encodeToString(tempKey.getEncoded());

        // This will create the same SecretKey as JwtTokenUtil does
        testSecretKey = Keys.hmacShaKeyFor(testSecret.getBytes());

        jwtTokenUtil = new JwtTokenUtil(testSecret);

        // Create a valid token for testing using the same key generation method
        validToken = Jwts.builder()
            .setSubject("1")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(testSecretKey)
            .compact();
    }

    @Test
    void getUserIdFromToken_ValidToken_ShouldReturnUserId() {
        // When
        String userId = String.valueOf(jwtTokenUtil.getUserId(validToken));

        // Then
        assertEquals("1", userId);
    }

    @Test
    void validateToken_ValidToken_ShouldReturnTrue() {
        // When
        boolean isValid = jwtTokenUtil.validateToken(validToken);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_ExpiredToken_ShouldReturnFalse() {
        // Given
        String expiredToken = Jwts.builder()
            .setSubject("1")
            .setIssuedAt(new Date(System.currentTimeMillis() - 86400000 * 2)) // 2 days ago
            .setExpiration(new Date(System.currentTimeMillis() - 86400000)) // 1 day ago
            .signWith(testSecretKey)
            .compact();

        // When
        boolean isValid = jwtTokenUtil.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_InvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenUtil.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }
}
