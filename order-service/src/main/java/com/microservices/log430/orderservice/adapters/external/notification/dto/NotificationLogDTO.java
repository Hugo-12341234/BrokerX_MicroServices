package com.microservices.log430.orderservice.adapters.external.notification.dto;

import java.time.Instant;

public class NotificationLogDTO {
    private Long userId;
    private String message;
    private Instant timestamp;
    private String channel;
    private String email;

    public NotificationLogDTO() {}

    public NotificationLogDTO(Long userId, String message, Instant timestamp, String channel, String email) {
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
        this.channel = channel;
        this.email = email;
    }

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

