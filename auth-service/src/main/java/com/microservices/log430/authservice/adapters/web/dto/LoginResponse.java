package com.microservices.log430.authservice.adapters.web.dto;

public class LoginResponse {
    private String challengeId;
    private String message;
    private boolean success;
    private boolean requiresMfa;

    public LoginResponse() {}

    public LoginResponse(String challengeId, String message, boolean success, boolean requiresMfa) {
        this.challengeId = challengeId;
        this.message = message;
        this.success = success;
        this.requiresMfa = requiresMfa;
    }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isRequiresMfa() { return requiresMfa; }
    public void setRequiresMfa(boolean requiresMfa) { this.requiresMfa = requiresMfa; }
}