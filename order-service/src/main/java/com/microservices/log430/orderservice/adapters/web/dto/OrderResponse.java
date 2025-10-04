package com.microservices.log430.orderservice.adapters.web.dto;

public class OrderResponse {
    private Long id;
    private String status;
    private String message;

    public OrderResponse() {}
    public OrderResponse(Long id, String status, String message) {
        this.id = id;
        this.status = status;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
