package com.microservices.log430.notificationservice.domain.service;

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
    public NotificationLog sendNotification(NotificationLog notificationLog) {
        // Ajoute l'horodatage ici si non fourni
        if (notificationLog.getTimestamp() == null) {
            notificationLog.setTimestamp(Instant.now());
        }
        boolean websocketSent = false;
        try {
            logger.info("Tentative d'envoi WebSocket à userId={}, channel={}", notificationLog.getUserId(), notificationLog.getChannel());
            // Envoi WebSocket (topic par userId)
            messagingTemplate.convertAndSend("/topic/notifications/" + notificationLog.getUserId(), notificationLog.getMessage());
            websocketSent = true;
            logger.info("Notification WebSocket envoyée avec succès à userId={}", notificationLog.getUserId());
        } catch (Exception e) {
            logger.error("Échec de l'envoi WebSocket à userId={}: {}", notificationLog.getUserId(), e.getMessage(), e);
            websocketSent = false;
        }
        if (!websocketSent) {
            // Fallback email
            try {
                logger.info("Envoi fallback email à userId={}", notificationLog.getUserId());
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo("user" + notificationLog.getUserId() + "@example.com");
                mail.setSubject("Notification BrokerX");
                mail.setText(notificationLog.getMessage());
                mailSender.send(mail);
                logger.info("Email envoyé avec succès à userId={}", notificationLog.getUserId());
            } catch (Exception e) {
                logger.error("Échec de l'envoi email à userId={}: {}", notificationLog.getUserId(), e.getMessage(), e);
            }
        }
        logger.info("Journalisation de la notification pour userId={}, channel={}", notificationLog.getUserId(), notificationLog.getChannel());
        // Journalisation
        return notificationLogPort.save(notificationLog);
    }
}
