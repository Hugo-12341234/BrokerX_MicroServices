package com.microservices.log430.notificationservice.domain.model.entities;

import java.time.Instant;

public class NotificationLog {
    private Long id;
    private Long userId;
    private String message;
    private Instant timestamp;
    private String channel;

    public NotificationLog() {}

    public NotificationLog(Long userId, String message, Instant timestamp, String channel) {
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
        this.channel = channel;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
