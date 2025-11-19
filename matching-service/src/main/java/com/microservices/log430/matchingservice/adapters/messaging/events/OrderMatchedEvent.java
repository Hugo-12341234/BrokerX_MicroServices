package com.microservices.log430.matchingservice.adapters.messaging.events;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Événement publié quand un ordre est matché avec succès
 */
public class OrderMatchedEvent {

    private Long orderId;
    private String clientOrderId;
    private Long userId;
    private String symbol;
    private String side;
    private String type;
    private int quantity;
    private Double price;
    private String duration;
    private Instant timestamp;
    private String status;
    private String rejectReason;
    private Long version;
    private List<ExecutionDetails> executions;
    private List<String> modifiedCandidateIds;

    public OrderMatchedEvent() {}

    public OrderMatchedEvent(Long orderId, String clientOrderId, Long userId, String symbol,
                            String side, String type, int quantity, Double price, String duration,
                            Instant timestamp, String status, String rejectReason, Long version,
                            List<ExecutionDetails> executions, List<String> modifiedCandidateIds) {
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.duration = duration;
        this.timestamp = timestamp;
        this.status = status;
        this.rejectReason = rejectReason;
        this.version = version;
        this.executions = executions;
        this.modifiedCandidateIds = modifiedCandidateIds;
    }

    // Classe interne pour les détails d'exécution
    public static class ExecutionDetails {
        private Long id;
        private Long buyerUserId;
        private Long sellerUserId;
        private String symbol;
        private Integer fillQuantity;
        private Double fillPrice;
        private LocalDateTime executionTime;
        private Long orderId;

        public ExecutionDetails() {}

        public ExecutionDetails(Long id, Long buyerUserId, Long sellerUserId,
                               String symbol, Integer fillQuantity, Double fillPrice,
                               LocalDateTime executionTime, Long orderId) {
            this.id = id;
            this.buyerUserId = buyerUserId;
            this.sellerUserId = sellerUserId;
            this.symbol = symbol;
            this.fillQuantity = fillQuantity;
            this.fillPrice = fillPrice;
            this.executionTime = executionTime;
            this.orderId = orderId;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getBuyerUserId() { return buyerUserId; }
        public void setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; }
        public Long getSellerUserId() { return sellerUserId; }
        public void setSellerUserId(Long sellerUserId) { this.sellerUserId = sellerUserId; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public Integer getFillQuantity() { return fillQuantity; }
        public void setFillQuantity(Integer fillQuantity) { this.fillQuantity = fillQuantity; }
        public Double getFillPrice() { return fillPrice; }
        public void setFillPrice(Double fillPrice) { this.fillPrice = fillPrice; }
        public LocalDateTime getExecutionTime() { return executionTime; }
        public void setExecutionTime(LocalDateTime executionTime) { this.executionTime = executionTime; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<ExecutionDetails> getExecutions() { return executions; }
    public void setExecutions(List<ExecutionDetails> executions) { this.executions = executions; }
    public List<String> getModifiedCandidateIds() { return modifiedCandidateIds; }
    public void setModifiedCandidateIds(List<String> modifiedCandidateIds) { this.modifiedCandidateIds = modifiedCandidateIds; }
}
