package com.microservices.log430.matchingservice.adapters.web.dto;

import java.util.List;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;

public class MatchingResult {
    public OrderBook updatedOrder;
    public List<ExecutionReport> executions;
    public MatchingResult(OrderBook updatedOrder, List<ExecutionReport> executions) {
        this.updatedOrder = updatedOrder;
        this.executions = executions;
    }
}
