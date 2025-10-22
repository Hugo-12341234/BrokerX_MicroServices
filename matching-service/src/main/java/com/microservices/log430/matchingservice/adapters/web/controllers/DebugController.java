package com.microservices.log430.matchingservice.adapters.web.controllers;

import com.microservices.log430.matchingservice.adapters.persistence.entities.OrderBookEntity;
import com.microservices.log430.matchingservice.adapters.persistence.repository.OrderBookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/debug/bookorder")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private OrderBookRepository orderBookRepository;

    @PostMapping("/seed-orders")
    public ResponseEntity<Map<String, Object>> createSeedOrders() {
        logger.info("Starting creation of seed orders");

        try {
            List<OrderBookEntity> seedOrders = new ArrayList<>();
            long baseOrderId = System.currentTimeMillis();

            logger.debug("Creating AAPL seed order");
            // Créer les mêmes ordres que dans la migration V5
            OrderBookEntity appleOrder = new OrderBookEntity();
            appleOrder.setClientOrderId("seed-AAPL-" + System.currentTimeMillis());
            appleOrder.setUserId(9999L);
            appleOrder.setSymbol("AAPL");
            appleOrder.setSide("VENTE");
            appleOrder.setType("LIMITE");
            appleOrder.setQuantity(10);
            appleOrder.setPrice(100.0);
            appleOrder.setDuration("DAY");
            appleOrder.setTimestamp(LocalDateTime.now());
            appleOrder.setStatus("Working");
            appleOrder.setQuantityRemaining(10);
            appleOrder.setOrderId(baseOrderId + 1);

            logger.debug("Creating MSFT seed order");
            OrderBookEntity msftOrder = new OrderBookEntity();
            msftOrder.setClientOrderId("seed-MSFT-" + System.currentTimeMillis());
            msftOrder.setUserId(9999L);
            msftOrder.setSymbol("MSFT");
            msftOrder.setSide("VENTE");
            msftOrder.setType("LIMITE");
            msftOrder.setQuantity(10);
            msftOrder.setPrice(200.0);
            msftOrder.setDuration("DAY");
            msftOrder.setTimestamp(LocalDateTime.now());
            msftOrder.setStatus("Working");
            msftOrder.setQuantityRemaining(10);
            msftOrder.setOrderId(baseOrderId + 2);

            logger.debug("Creating TSLA seed order");
            OrderBookEntity tslaOrder = new OrderBookEntity();
            tslaOrder.setClientOrderId("seed-TSLA-" + System.currentTimeMillis());
            tslaOrder.setUserId(9999L);
            tslaOrder.setSymbol("TSLA");
            tslaOrder.setSide("VENTE");
            tslaOrder.setType("LIMITE");
            tslaOrder.setQuantity(15);
            tslaOrder.setPrice(150.0);
            tslaOrder.setDuration("DAY");
            tslaOrder.setTimestamp(LocalDateTime.now());
            tslaOrder.setStatus("Working");
            tslaOrder.setQuantityRemaining(15);
            tslaOrder.setOrderId(baseOrderId + 3);

            logger.debug("Creating GOOG seed order");
            OrderBookEntity googOrder = new OrderBookEntity();
            googOrder.setClientOrderId("seed-GOOG-" + System.currentTimeMillis());
            googOrder.setUserId(9999L);
            googOrder.setSymbol("GOOG");
            googOrder.setSide("VENTE");
            googOrder.setType("LIMITE");
            googOrder.setQuantity(17);
            googOrder.setPrice(1200.0);
            googOrder.setDuration("DAY");
            googOrder.setTimestamp(LocalDateTime.now());
            googOrder.setStatus("Working");
            googOrder.setQuantityRemaining(17);
            googOrder.setOrderId(baseOrderId + 4);

            seedOrders.add(appleOrder);
            seedOrders.add(msftOrder);
            seedOrders.add(tslaOrder);
            seedOrders.add(googOrder);

            logger.info("Saving {} seed orders to database", seedOrders.size());
            // Sauvegarder tous les ordres
            List<OrderBookEntity> savedOrders = orderBookRepository.saveAll(seedOrders);
            logger.info("Successfully saved {} seed orders", savedOrders.size());

            // Log details of saved orders
            for (OrderBookEntity order : savedOrders) {
                logger.debug("Saved order - ID: {}, Symbol: {}, Quantity: {}, Price: {}",
                    order.getId(), order.getSymbol(), order.getQuantity(), order.getPrice());
            }

            // Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ordres seed créés avec succès");
            response.put("ordersCreated", savedOrders.size());
            response.put("orders", savedOrders.stream().map(order -> {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("id", order.getId());
                orderInfo.put("symbol", order.getSymbol());
                orderInfo.put("quantity", order.getQuantity());
                orderInfo.put("price", order.getPrice());
                orderInfo.put("side", order.getSide());
                return orderInfo;
            }).toList());

            logger.info("Seed orders creation completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating seed orders: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors de la création des ordres seed: " + e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
