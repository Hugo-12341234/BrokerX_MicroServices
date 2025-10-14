package com.microservices.log430.matchingservice.domain.service;

import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;
import com.microservices.log430.matchingservice.domain.port.in.MatchingPort;
import com.microservices.log430.matchingservice.domain.port.out.OrderBookPort;
import com.microservices.log430.matchingservice.domain.port.out.ExecutionReportPort;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.time.LocalDateTime;

@Service
public class MatchingService implements MatchingPort {
    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    private final OrderBookPort orderBookPort;
    private final ExecutionReportPort executionReportPort;

    public MatchingService(OrderBookPort orderBookPort, ExecutionReportPort executionReportPort) {
        this.orderBookPort = orderBookPort;
        this.executionReportPort = executionReportPort;
    }

    @Override
    public MatchingResult matchOrder(OrderBook orderBook) {
        logger.info("Début du matching pour clientOrderId={}, symbol={}, side={}, type={}, quantity={}, price={}, duration={}",
                orderBook.getClientOrderId(), orderBook.getSymbol(), orderBook.getSide(), orderBook.getType(), orderBook.getQuantity(), orderBook.getPrice(), orderBook.getDuration());
        OrderBook savedOrder = orderBookPort.save(orderBook);
        List<ExecutionReport> executions = new ArrayList<>();
        logger.debug("Ordre inséré dans le carnet : id={}, clientOrderId={}, status={}",
                savedOrder.getId(), savedOrder.getClientOrderId(), savedOrder.getStatus());

        // Récupérer les ordres opposés
        List<OrderBook> oppositeOrders = orderBookPort.findAllBySymbol(savedOrder.getSymbol());
        // Seuls les ordres actifs (Working ou PartiallyFilled) du côté opposé sont matchables
        oppositeOrders.removeIf(o -> o.getSide().equals(savedOrder.getSide()) || !(o.getStatus().equals("Working") || o.getStatus().equals("PartiallyFilled")));

        // Trier selon la priorité prix/temps
        Comparator<OrderBook> comparator;
        if ("ACHAT".equals(savedOrder.getSide())) {
            comparator = Comparator.comparing(OrderBook::getPrice)
                                   .thenComparing(OrderBook::getTimestamp);
        } else {
            comparator = Comparator.comparing(OrderBook::getPrice, Comparator.reverseOrder())
                                   .thenComparing(OrderBook::getTimestamp);
        }
        oppositeOrders.sort(comparator);

        int initialQty = savedOrder.getQuantityRemaining();
        boolean isFOK = "FOK".equalsIgnoreCase(savedOrder.getDuration());
        boolean isIOC = "IOC".equalsIgnoreCase(savedOrder.getDuration());
        boolean isDAY = "DAY".equalsIgnoreCase(savedOrder.getDuration());
        int totalMatched = 0;

        for (OrderBook candidate : oppositeOrders) {
            // Empêcher le matching avec soi-même
            if (savedOrder.getUserId().equals(candidate.getUserId())) continue;
            logger.debug("Candidat : id={}, clientOrderId={}, side={}, price={}, qtyRemaining={}, timestamp={}",
                    candidate.getId(), candidate.getClientOrderId(), candidate.getSide(), candidate.getPrice(), candidate.getQuantityRemaining(), candidate.getTimestamp());

            boolean priceMatch;
            if ("MARCHE".equalsIgnoreCase(savedOrder.getType())) {
                priceMatch = true;
            } else {
                if ("ACHAT".equals(savedOrder.getSide())) {
                    priceMatch = savedOrder.getPrice() >= candidate.getPrice();
                } else {
                    priceMatch = savedOrder.getPrice() <= candidate.getPrice();
                }
            }
            if (!priceMatch) continue;

            int fillQty = Math.min(savedOrder.getQuantityRemaining(), candidate.getQuantityRemaining());
            if (fillQty <= 0) continue;

            // FOK : vérifier si on peut tout remplir
            if (isFOK && totalMatched + fillQty < initialQty) {
                totalMatched += fillQty;
                continue;
            }

            logger.info("Matching trouvé : candidateId={}, candidateClientOrderId={}, fillQty={}, fillPrice={}",
                    candidate.getId(), candidate.getClientOrderId(), fillQty, candidate.getPrice());
            ExecutionReport exec = new ExecutionReport();
            exec.setOrderId(savedOrder.getId());
            exec.setFillQuantity(fillQty);
            exec.setFillPrice(candidate.getPrice());
            exec.setFillType(fillQty == savedOrder.getQuantityRemaining() ? "Full" : "Partial");
            exec.setExecutionTime(LocalDateTime.now());
            exec.setSymbol(savedOrder.getSymbol());
            // Ajout des informations d'utilisateur
            if ("ACHAT".equals(savedOrder.getSide())) {
                exec.setBuyerUserId(savedOrder.getUserId());
                exec.setSellerUserId(candidate.getUserId());
            } else {
                exec.setBuyerUserId(candidate.getUserId());
                exec.setSellerUserId(savedOrder.getUserId());
            }
            // log pour savoir ce qui est dans l'exécution
            logger.info("Création de l'exécution : orderId={}, fillQty={}, fillPrice={}, fillType={}, buyerUserId={}, sellerUserId={}",
                    exec.getOrderId(), exec.getFillQuantity(), exec.getFillPrice(), exec.getFillType(), exec.getBuyerUserId(), exec.getSellerUserId());
            executionReportPort.save(exec);
            executions.add(exec);
            savedOrder.setQuantityRemaining(savedOrder.getQuantityRemaining() - fillQty);
            candidate.setQuantityRemaining(candidate.getQuantityRemaining() - fillQty);
            if (savedOrder.getQuantityRemaining() == 0) savedOrder.setStatus("Filled");
            if (candidate.getQuantityRemaining() == 0) candidate.setStatus("Filled");
            orderBookPort.save(savedOrder);
            orderBookPort.save(candidate);
            logger.debug("Quantités mises à jour : savedOrderRemaining={}, candidateRemaining={}",
                    savedOrder.getQuantityRemaining(), candidate.getQuantityRemaining());
            totalMatched += fillQty;
            if (savedOrder.getQuantityRemaining() == 0) break;
        }

        // FOK : si pas tout rempli, annuler
        if (isFOK) {
            if (totalMatched < initialQty) {
                logger.info("FOK : quantité totale non disponible, annulation de l'ordre clientOrderId={}", savedOrder.getClientOrderId());
                savedOrder.setStatus("Cancelled");
                savedOrder.setQuantityRemaining(initialQty);
                executions.clear();
                orderBookPort.save(savedOrder);
                // Retirer l'ordre du carnet
                orderBookPort.deleteById(savedOrder.getId());
            } else {
                // Si rempli, retirer du carnet
                orderBookPort.deleteById(savedOrder.getId());
            }
        }
        // IOC : exécute ce qui est possible, annule le reste et retire du carnet
        else if (isIOC) {
            if (savedOrder.getQuantityRemaining() > 0) {
                logger.info("IOC : exécution partielle, annulation du reste pour clientOrderId={}", savedOrder.getClientOrderId());
                savedOrder.setStatus(executions.isEmpty() ? "Cancelled" : "PartialFilled");
                orderBookPort.save(savedOrder);
            }
            // Retirer l'ordre IOC du carnet
            orderBookPort.deleteById(savedOrder.getId());
        }
        // DAY : exécution partielle possible, reste dans le carnet jusqu'à expiration
        else if (isDAY) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime orderTime = savedOrder.getTimestamp();
            if (orderTime != null && orderTime.plusHours(24).isBefore(now)) {
                // Annulation du reste après 24h
                if (savedOrder.getQuantityRemaining() > 0) {
                    logger.info("DAY : ordre expiré après 24h, annulation du reste pour clientOrderId={}", savedOrder.getClientOrderId());
                    savedOrder.setStatus(executions.isEmpty() ? "Cancelled" : "PartialFilled");
                    orderBookPort.save(savedOrder);
                }
                // Retirer l'ordre DAY expiré du carnet
                orderBookPort.deleteById(savedOrder.getId());
            } else {
                if (savedOrder.getQuantityRemaining() > 0) {
                    logger.info("DAY : exécution partielle, reste dans le carnet pour clientOrderId={}", savedOrder.getClientOrderId());
                    savedOrder.setStatus("PartiallyFilled");
                    orderBookPort.save(savedOrder);
                } else {
                    // Si rempli, retirer du carnet
                    orderBookPort.deleteById(savedOrder.getId());
                }
            }
        }

        logger.info("Matching terminé pour clientOrderId={}, statut final={}, nombre d'exécutions={}",
                savedOrder.getClientOrderId(), savedOrder.getStatus(), executions.size());
        return new MatchingResult(savedOrder, executions);
    }
}
