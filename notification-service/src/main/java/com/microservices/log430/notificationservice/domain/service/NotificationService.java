package com.microservices.log430.notificationservice.domain.service;

import com.microservices.log430.notificationservice.adapters.web.dto.NotificationLogDTO;
import com.microservices.log430.notificationservice.domain.port.in.NotificationPort;
import com.microservices.log430.notificationservice.domain.port.out.NotificationLogPort;
import com.microservices.log430.notificationservice.domain.model.entities.NotificationLog;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Service
public class NotificationService implements NotificationPort {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationLogPort notificationLogPort;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @Autowired
    public NotificationService(NotificationLogPort notificationLogPort, SimpMessagingTemplate messagingTemplate, JavaMailSender mailSender) {
        this.notificationLogPort = notificationLogPort;
        this.messagingTemplate = messagingTemplate;
        this.mailSender = mailSender;
    }

    @Override
    public NotificationLog sendNotification(NotificationLogDTO notificationLogDTO) {
        // Ajoute l'horodatage ici si non fourni
        if (notificationLogDTO.getTimestamp() == null) {
            notificationLogDTO.setTimestamp(Instant.now());
        }
        boolean websocketSent = false;
        try {
            logger.info("Tentative d'envoi WebSocket à userId={}, channel={}", notificationLogDTO.getUserId(), notificationLogDTO.getChannel());
            logger.info("Message: {}", notificationLogDTO.getMessage());
            // Envoi WebSocket (topic par userId)
            messagingTemplate.convertAndSend("/topic/notifications/" + notificationLogDTO.getUserId(), notificationLogDTO.getMessage());
            websocketSent = true;
            logger.info("Notification WebSocket envoyée avec succès à userId={}", notificationLogDTO.getUserId());
        } catch (Exception e) {
            logger.error("Échec de l'envoi WebSocket à userId={}: {}", notificationLogDTO.getUserId(), e.getMessage(), e);
            websocketSent = false;
        }
        if (!websocketSent) {
            // Fallback email
            try {
                logger.info("Envoi fallback email à userId={}", notificationLogDTO.getUserId());
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(notificationLogDTO.getEmail());
                mail.setSubject("Notification BrokerX");
                mail.setText(notificationLogDTO.getMessage());
                mailSender.send(mail);
                logger.info("Email envoyé avec succès à userId={}", notificationLogDTO.getUserId());
            } catch (Exception e) {
                logger.error("Échec de l'envoi email à userId={}: {}", notificationLogDTO.getUserId(), e.getMessage(), e);
            }
        }
        logger.info("Journalisation de la notification pour userId={}, channel={}", notificationLogDTO.getUserId(), notificationLogDTO.getChannel());
        // Journalisation
        NotificationLog notificationLog = new NotificationLog(
                notificationLogDTO.getUserId(),
                notificationLogDTO.getMessage(),
                notificationLogDTO.getTimestamp(),
                notificationLogDTO.getChannel()
        );
        return notificationLogPort.save(notificationLog);
    }
}
