package com.microservices.log430.authservice.domain.service;

import com.microservices.log430.authservice.domain.model.entities.User;
import com.microservices.log430.authservice.domain.model.entities.VerificationToken;
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
class RegistrationServiceTest {

    @Mock
    private UserPort userPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenPort tokenPort;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        // Constructor injection requires base URL, we'll set it via reflection if needed
    }

    @Test
    void register_HappyPath_ShouldReturnUser() {
        // Given
        when(userPort.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = new User(
            1L,
            "newuser@example.com",
            "hashedPassword",
            "John Doe",
            "123 Main St",
            LocalDate.of(1990, 1, 1),
            User.Status.PENDING
        );
        when(userPort.save(any(User.class))).thenReturn(savedUser);

        VerificationToken savedToken = new VerificationToken();
        savedToken.setTokenHash("token123");
        when(tokenPort.save(any(VerificationToken.class))).thenReturn(savedToken);

        // When
        User result = registrationService.register(
            "newuser@example.com",
            "password123",
            "John Doe",
            "123 Main St",
            LocalDate.of(1990, 1, 1)
        );

        // Then
        assertNotNull(result);
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals(User.Status.PENDING, result.getStatus());
        verify(emailSenderPort).sendEmail(eq("newuser@example.com"), anyString(), anyString());
    }

    @Test
    void register_EmailAlreadyExists_ShouldThrowException() {
        // Given
        User existingUser = new User(
            1L,
            "existing@example.com",
            "hashedPassword",
            "Existing User",
            "456 Old St",
            LocalDate.of(1985, 5, 15),
            User.Status.ACTIVE
        );
        when(userPort.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            registrationService.register(
                "existing@example.com",
                "password123",
                "John Doe",
                "123 Main St",
                LocalDate.of(1990, 1, 1)
            )
        );

        verify(userPort, never()).save(any());
        verify(emailSenderPort, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void verifyEmail_HappyPath_ShouldActivateUser() {
        // Given
        User pendingUser = new User(
            1L,
            "pending@example.com",
            "hashedPassword",
            "Pending User",
            "789 Pending St",
            LocalDate.of(1992, 3, 10),
            User.Status.PENDING
        );

        VerificationToken validToken = new VerificationToken();
        validToken.setUser(pendingUser);
        validToken.setTokenHash("hashed-token");
        validToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // Token non expiré

        // Le service va hasher le token "valid-token", donc on utilise anyString()
        when(tokenPort.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));

        // When
        boolean result = registrationService.verifyUser("valid-token");

        // Then
        assertTrue(result);
        assertEquals(User.Status.ACTIVE, pendingUser.getStatus());
        verify(userPort).save(pendingUser);
        verify(auditPort).saveAudit(any());
    }

    @Test
    void verifyEmail_InvalidToken_ShouldReturnFalse() {
        // Given
        // Le service va hasher le token "invalid-token", donc on utilise anyString()
        when(tokenPort.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // When
        boolean result = registrationService.verifyUser("invalid-token");

        // Then
        assertFalse(result);
        verify(userPort, never()).save(any());
    }
}
