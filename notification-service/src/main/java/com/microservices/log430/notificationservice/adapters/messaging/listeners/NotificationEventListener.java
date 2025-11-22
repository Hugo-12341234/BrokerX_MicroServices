package com.microservices.log430.notificationservice.adapters.messaging.listeners;

import com.microservices.log430.notificationservice.adapters.messaging.events.NotificationEvent;
import com.microservices.log430.notificationservice.adapters.web.dto.NotificationLogDTO;
import com.microservices.log430.notificationservice.domain.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener pour les événements NOTIFICATION_SEND
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    @Autowired
    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Écoute les événements NOTIFICATION_SEND et traite les notifications
     */
    @RabbitListener(queues = "${messaging.queue.notification-send}")
    @Transactional
    public void handleNotificationSend(NotificationEvent notificationEvent) {
        logger.info("Réception d'un événement NOTIFICATION_SEND: userId={}, channel={}, status={}",
                   notificationEvent.getUserId(), notificationEvent.getChannel(), notificationEvent.getStatus());

        try {
            // Convertir l'événement en DTO et utiliser le service existant
            NotificationLogDTO notificationDTO = new NotificationLogDTO();
            notificationDTO.setUserId(notificationEvent.getUserId());
            notificationDTO.setMessage(notificationEvent.getMessage());
            notificationDTO.setTimestamp(notificationEvent.getTimestamp());
            notificationDTO.setChannel(notificationEvent.getChannel());
            notificationDTO.setEmail(notificationEvent.getEmail());

            // Utiliser la même logique que le controller
            notificationService.sendNotification(notificationDTO);

            logger.info("Notification traitée avec succès via RabbitMQ pour userId={}",
                       notificationEvent.getUserId());

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la notification: {}", e.getMessage(), e);
        }
    }
}
