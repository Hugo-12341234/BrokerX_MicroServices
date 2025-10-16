package com.microservices.log430.orderservice.adapters.external.matching.dto;

import java.util.List;

public class MatchingResult {
    public OrderBookDTO updatedOrder;
    public List<ExecutionReportDTO> executions;
    public List<OrderBookDTO> modifiedCandidates; // Nouveaux ordres candidats modifiés

    // Ajout d'un constructeur public sans argument
    public MatchingResult() {}

    public MatchingResult(OrderBookDTO updatedOrder, List<ExecutionReportDTO> executions) {
        this.updatedOrder = updatedOrder;
        this.executions = executions;
        this.modifiedCandidates = new java.util.ArrayList<>();
    }

    public MatchingResult(OrderBookDTO updatedOrder, List<ExecutionReportDTO> executions, List<OrderBookDTO> modifiedCandidates) {
        this.updatedOrder = updatedOrder;
        this.executions = executions;
        this.modifiedCandidates = modifiedCandidates != null ? modifiedCandidates : new java.util.ArrayList<>();
    }

    // Getters et setters
    public OrderBookDTO getUpdatedOrder() { return updatedOrder; }
    public void setUpdatedOrder(OrderBookDTO updatedOrder) { this.updatedOrder = updatedOrder; }
    public List<ExecutionReportDTO> getExecutions() { return executions; }
    public void setExecutions(List<ExecutionReportDTO> executions) { this.executions = executions; }
    public List<OrderBookDTO> getModifiedCandidates() { return modifiedCandidates; }
    public void setModifiedCandidates(List<OrderBookDTO> modifiedCandidates) { this.modifiedCandidates = modifiedCandidates; }
}
