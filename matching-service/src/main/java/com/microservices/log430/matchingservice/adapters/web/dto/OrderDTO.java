package com.microservices.log430.matchingservice.adapters.web.dto;

import java.time.Instant;

public class OrderDTO {
    public Long id;
    public String clientOrderId;
    public Long userId;
    public String symbol;
    public String side;
    public String type;
    public int quantity;
    public Double price;
    public String duration;
    public Instant timestamp;
    public String status;
    public String rejectReason;
    public Long version;
}