package com.microservices.log430.matchingservice.adapters.web.dto;

import java.util.List;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;

public class MatchingResult {
    public OrderBook updatedOrder;
    public List<ExecutionReport> executions;
    public List<OrderBook> modifiedCandidates; // Nouveaux ordres candidats modifiés

    public MatchingResult(OrderBook updatedOrder, List<ExecutionReport> executions) {
        this.updatedOrder = updatedOrder;
        this.executions = executions;
        this.modifiedCandidates = new java.util.ArrayList<>();
    }

    public MatchingResult(OrderBook updatedOrder, List<ExecutionReport> executions, List<OrderBook> modifiedCandidates) {
        this.updatedOrder = updatedOrder;
        this.executions = executions;
        this.modifiedCandidates = modifiedCandidates != null ? modifiedCandidates : new java.util.ArrayList<>();
    }
}
