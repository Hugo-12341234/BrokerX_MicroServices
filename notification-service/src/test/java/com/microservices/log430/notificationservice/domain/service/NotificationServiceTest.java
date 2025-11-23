package com.microservices.log430.notificationservice.domain.service;

import com.microservices.log430.notificationservice.adapters.web.dto.NotificationLogDTO;
import com.microservices.log430.notificationservice.domain.port.out.NotificationLogPort;
import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogPort notificationLogPort;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationLogDTO mockNotificationDTO;
    private NotificationLog mockNotificationLog;

    @BeforeEach
    void setUp() {
        mockNotificationDTO = new NotificationLogDTO();
        mockNotificationDTO.setUserId(1L);
        mockNotificationDTO.setMessage("Test notification");
        mockNotificationDTO.setChannel("WEBSOCKET");
        mockNotificationDTO.setTimestamp(Instant.now());

        mockNotificationLog = new NotificationLog();
        mockNotificationLog.setId(1L);
        mockNotificationLog.setUserId(1L);
        mockNotificationLog.setMessage("Test notification");
        mockNotificationLog.setChannel("WEBSOCKET");
        mockNotificationLog.setTimestamp(Instant.now());
    }

    @Test
    void sendNotification_shouldProcessWebSocketNotification() {
        // Arrange
        when(notificationLogPort.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), anyString());

        // Act
        NotificationLog result = notificationService.sendNotification(mockNotificationDTO);

        // Assert
        assertNotNull(result);
        assertEquals(mockNotificationLog.getId(), result.getId());
        assertEquals(mockNotificationLog.getUserId(), result.getUserId());
        assertEquals(mockNotificationLog.getMessage(), result.getMessage());

        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/1"), eq("Test notification"));
        verify(notificationLogPort).save(any(NotificationLog.class));
    }

    @Test
    void sendNotification_shouldSetTimestampIfNotProvided() {
        // Arrange
        mockNotificationDTO.setTimestamp(null);
        when(notificationLogPort.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), anyString());

        // Act
        NotificationLog result = notificationService.sendNotification(mockNotificationDTO);

        // Assert
        assertNotNull(result);
        assertNotNull(mockNotificationDTO.getTimestamp()); // Should be set by the service
        verify(notificationLogPort).save(any(NotificationLog.class));
    }

    @Test
    void sendNotification_shouldHandleEmailChannel() {
        // Arrange
        mockNotificationDTO.setChannel("EMAIL");
        mockNotificationDTO.setEmail("test@example.com");
        when(notificationLogPort.save(any(NotificationLog.class))).thenReturn(mockNotificationLog);
        doThrow(new RuntimeException("WebSocket failed")).when(messagingTemplate).convertAndSend(anyString(), anyString());

        // Act
        NotificationLog result = notificationService.sendNotification(mockNotificationDTO);

        // Assert
        assertNotNull(result);
        verify(notificationLogPort).save(any(NotificationLog.class));
        // Email functionality would be called in the fallback
    }
}
