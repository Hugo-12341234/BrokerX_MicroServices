package com.microservices.log430.authservice.adapters.web.dto;

public class MfaVerificationResponse {
    private boolean success;
    private String message;
    private String token;
    private String status; // "error", "locked", "suspended", "success"

    public MfaVerificationResponse() {}

    public MfaVerificationResponse(boolean success, String message, String token, String status) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.status = status;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
