package com.microservices.log430.marketdataservice.adapters.web.dto;

import java.time.Instant;

public class ErrorResponse {
    private Instant timestamp;
    private String path;
    private int status;
    private String error;
    private String message;
    private String requestId;

    public ErrorResponse(Instant timestamp, String path, int status, String error, String message, String requestId) {
        this.timestamp = timestamp;
        this.path = path;
        this.status = status;
        this.error = error;
        this.message = message;
        this.requestId = requestId;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getPath() { return path; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getRequestId() { return requestId; }
}

