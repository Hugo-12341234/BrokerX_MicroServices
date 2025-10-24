package com.microservices.log430.authservice.domain.service;

import com.microservices.log430.authservice.domain.model.entities.MfaChallenge;
import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserPort userPort;

    @Mock
    private MfaChallengePort mfaChallengePort;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private AuditPort auditPort;

    @Mock
    private JwtTokenPort jwtTokenPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User(
            1L,
            "test@example.com",
            "hashedPassword",
            "Test User",
            "123 Test St",
            LocalDate.of(1990, 1, 1),
            User.Status.ACTIVE
        );
    }

    @Test
    void authenticate_HappyPath_ShouldReturnChallengeId() {
        // Given
        when(userPort.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        MfaChallenge savedChallenge = new MfaChallenge();
        savedChallenge.setId(123L);
        when(mfaChallengePort.save(any(MfaChallenge.class))).thenReturn(savedChallenge);

        // When
        String challengeId = authenticationService.authenticate("test@example.com", "password123", "127.0.0.1", "TestAgent");

        // Then
        assertEquals("123", challengeId);
        verify(emailSenderPort).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void authenticate_UserNotFound_ShouldThrowException() {
        // Given
        when(userPort.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            authenticationService.authenticate("nonexistent@example.com", "password123", "127.0.0.1", "TestAgent")
        );

        verify(emailSenderPort, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void authenticate_WrongPassword_ShouldThrowException() {
        // Given
        when(userPort.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            authenticationService.authenticate("test@example.com", "wrongpassword", "127.0.0.1", "TestAgent")
        );

        verify(emailSenderPort, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void verifyMfa_HappyPath_ShouldReturnJwtToken() {
        // Given
        MfaChallenge challenge = new MfaChallenge();
        challenge.setId(123L);
        challenge.setUserId(1L);
        challenge.setCode("123456");
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setUsed(false);

        when(mfaChallengePort.findById(123L)).thenReturn(Optional.of(challenge));
        when(userPort.findById(1L)).thenReturn(Optional.of(mockUser)); // Ajouter le mock pour trouver l'utilisateur
        when(jwtTokenPort.generateToken(1L, "test@example.com")).thenReturn("jwt-token-123");

        // When
        String token = authenticationService.verifyMfa("123", "123456", "127.0.0.1", "TestAgent");

        // Then
        assertEquals("jwt-token-123", token);
        verify(mfaChallengePort).save(challenge);
        assertTrue(challenge.isUsed());
    }

    @Test
    void verifyMfa_InvalidCode_ShouldThrowException() {
        // Given
        MfaChallenge challenge = new MfaChallenge();
        challenge.setId(123L);
        challenge.setCode("123456");
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setUsed(false);

        when(mfaChallengePort.findById(123L)).thenReturn(Optional.of(challenge));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            authenticationService.verifyMfa("123", "wrong-code", "127.0.0.1", "TestAgent")
        );

        verify(jwtTokenPort, never()).generateToken(anyLong(), anyString());
    }
}
