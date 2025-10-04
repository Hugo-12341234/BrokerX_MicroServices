package com.microservices.log430.authservice.domain.model.entities;

import java.time.LocalDateTime;

public class Audit {
    private Long id;
    private Long userId;
    private String action;
    private LocalDateTime timestamp;
    private String documentHash;
    private String ipAddress;
    private String userAgent;
    private String sessionToken;

    public Audit() {}

    public Audit(Long userId, String action, LocalDateTime timestamp, String documentHash) {
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
        this.documentHash = documentHash;
    }

    public Audit(Long userId, String action, LocalDateTime timestamp, String documentHash,
                 String ipAddress, String userAgent, String sessionToken) {
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
        this.documentHash = documentHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionToken = sessionToken;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getDocumentHash() { return documentHash; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}

