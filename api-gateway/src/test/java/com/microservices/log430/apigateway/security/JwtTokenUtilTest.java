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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private final String testSecret = "mySecretKeyForTestingPurposesOnly1234567890";

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(testSecret);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        // Arrange
        String userId = "123";
        String token = createTestToken(userId);

        // Act
        boolean isValid = jwtTokenUtil.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtTokenUtil.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void getUserId_shouldReturnCorrectUserId() {
        // Arrange
        String userId = "123";
        String token = createTestToken(userId);

        // Act
        Long extractedUserId = jwtTokenUtil.getUserId(token);

        // Assert
        assertEquals(Long.valueOf(userId), extractedUserId);
    }

    @Test
    void getClaims_shouldReturnCorrectClaims() {
        // Arrange
        String userId = "123";
        String token = createTestToken(userId);

        // Act
        Claims claims = jwtTokenUtil.getClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals(userId, claims.getSubject());
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Arrange
        String expiredToken = createExpiredToken("123");

        // Act
        boolean isValid = jwtTokenUtil.validateToken(expiredToken);

        // Assert
        assertFalse(isValid);
    }

    private String createTestToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createExpiredToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago (expired)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
