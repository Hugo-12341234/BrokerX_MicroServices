package com.microservices.log430.matchingservice.adapters.messaging.events;

import java.time.Instant;

/**
 * Événement publié pour envoyer une notification à un utilisateur
 */
public class NotificationEvent {

    private Long userId;
    private String message;
    private Instant timestamp;
    private String channel;
    private String email;

    public NotificationEvent() {}

    public NotificationEvent(Long userId, String message, Instant timestamp, String channel, String email) {
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
        this.channel = channel;
        this.email = email;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
