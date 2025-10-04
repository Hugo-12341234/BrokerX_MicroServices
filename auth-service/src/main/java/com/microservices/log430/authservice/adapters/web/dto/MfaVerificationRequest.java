package com.microservices.log430.authservice.adapters.web.dto;

public class MfaVerificationRequest {
    private String challengeId;
    private String code;

    public MfaVerificationRequest() {}

    public MfaVerificationRequest(String challengeId, String code) {
        this.challengeId = challengeId;
        this.code = code;
    }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
