package com.microservices.log430.notificationservice.adapters.persistence.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_log")
public class NotificationLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String channel;

    public NotificationLogEntity() {}

    public NotificationLogEntity(Long userId, String message, Instant timestamp, String channel) {
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

