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

        // Nettoyer les ordres DAY expirés avant le matching
        List<OrderBook> wipedCandidates = cleanupExpiredDayOrders(orderBook.getSymbol());

        OrderBook savedOrder = orderBookPort.save(orderBook);
        List<ExecutionReport> executions = new ArrayList<>();
        List<OrderBook> modifiedCandidates = new ArrayList<>(); // Tracker les candidats modifiés
        modifiedCandidates.addAll(wipedCandidates);
        logger.debug("Ordre inséré dans le carnet : id={}, clientOrderId={}, status={}",
                savedOrder.getId(), savedOrder.getClientOrderId(), savedOrder.getStatus());

        // Récupérer les ordres opposés
        List<OrderBook> oppositeOrders = orderBookPort.findAllBySymbol(savedOrder.getSymbol());
        // Seuls les ordres actifs (Working ou PartiallyFilled) du côté opposé sont matchables
        oppositeOrders.removeIf(o -> o.getSide().equals(savedOrder.getSide()) || !(o.getStatus().equals("Working") || o.getStatus().equals("PartiallyFilled")));

        // Trier selon la priorité prix/temps, en gérant les ordres "MARCHE" (prix null)
        Comparator<OrderBook> comparator = (o1, o2) -> {
            // Vérification null-safe directe sur les prix
            Double price1 = o1.getPrice();
            Double price2 = o2.getPrice();

            // Gérer les cas où un ou les deux prix sont null
            if (price1 == null && price2 == null) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
            if (price1 == null) return -1; // ordres marché prioritaires
            if (price2 == null) return 1;

            // Les deux ont un prix non-null, appliquer la logique ACHAT/VENTE
            if ("ACHAT".equals(savedOrder.getSide())) {
                // Pour un ordre d'achat entrant, on veut les prix de vente les plus bas en premier
                int cmp = price1.compareTo(price2);
                if (cmp != 0) return cmp;
            } else {
                // Pour un ordre de vente entrant, on veut les prix d'achat les plus hauts en premier
                int cmp = price2.compareTo(price1);
                if (cmp != 0) return cmp;
            }
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        };
        oppositeOrders.sort(comparator);

        int initialQty = savedOrder.getQuantityRemaining();
        boolean isFOK = "FOK".equalsIgnoreCase(savedOrder.getDuration());
        boolean isIOC = "IOC".equalsIgnoreCase(savedOrder.getDuration());
        boolean isDAY = "DAY".equalsIgnoreCase(savedOrder.getDuration());
        int totalMatched = 0;

        for (OrderBook candidate : oppositeOrders) {
            if (savedOrder.getUserId().equals(candidate.getUserId())) continue;

            // Vérifier l'expiration des ordres DAY candidats
            if ("DAY".equalsIgnoreCase(candidate.getDuration())) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime candidateTime = candidate.getTimestamp();
                if (candidateTime != null && candidateTime.plusHours(24).isBefore(now)) {
                    logger.info("Candidat DAY expiré : clientOrderId={}, timestamp={}",
                            candidate.getClientOrderId(), candidateTime);
                    candidate.setStatus("Cancelled");
                    orderBookPort.save(candidate);
                    orderBookPort.deleteById(candidate.getId());
                    modifiedCandidates.add(candidate); // Tracker le candidat expiré
                    continue; // Ignorer ce candidat expiré
                }
            }

            // Empêcher le matching entre deux ordres de type marché
            if ("MARCHE".equalsIgnoreCase(savedOrder.getType()) && candidate.getPrice() == null) continue;
            // Protection : si les deux prix sont null, ignorer le matching
            if (savedOrder.getPrice() == null && candidate.getPrice() == null) continue;
            logger.debug("Candidat : id={}, clientOrderId={}, side={}, price={}, qtyRemaining={}, timestamp={}",
                    candidate.getId(), candidate.getClientOrderId(), candidate.getSide(), candidate.getPrice(), candidate.getQuantityRemaining(), candidate.getTimestamp());

            boolean priceMatch;
            if ("MARCHE".equalsIgnoreCase(savedOrder.getType()) || candidate.getPrice() == null || savedOrder.getPrice() == null) {
                // Si l'un des deux est marché, le matching est toujours possible
                priceMatch = true;
            } else {
                // Utiliser Double.compare pour éviter l'unboxing NPE
                if ("ACHAT".equals(savedOrder.getSide())) {
                    priceMatch = Double.compare(savedOrder.getPrice(), candidate.getPrice()) >= 0;
                } else {
                    priceMatch = Double.compare(savedOrder.getPrice(), candidate.getPrice()) <= 0;
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

            // Détermination du prix d'exécution
            Double executionPrice;
            if (candidate.getPrice() == null && savedOrder.getPrice() != null) {
                executionPrice = savedOrder.getPrice(); // Si le candidat est marché, prendre le prix de l'ordre limite
            } else if (candidate.getPrice() != null) {
                executionPrice = candidate.getPrice(); // Sinon, prendre le prix du candidat
            } else {
                continue; // sécurité : aucun prix défini
            }
            logger.info("Matching trouvé : candidateId={}, candidateClientOrderId={}, fillQty={}, fillPrice={}",
                    candidate.getId(), candidate.getClientOrderId(), fillQty, executionPrice);
            ExecutionReport exec = new ExecutionReport();
            exec.setOrderId(savedOrder.getOrderId());
            exec.setFillQuantity(fillQty);
            exec.setFillPrice(executionPrice);
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
            // Mise à jour des quantités
            int savedOrderInitialQty = savedOrder.getQuantity();
            int candidateInitialQty = candidate.getQuantity();
            savedOrder.setQuantityRemaining(savedOrder.getQuantityRemaining() - fillQty);
            candidate.setQuantityRemaining(candidate.getQuantityRemaining() - fillQty);
            // Mise à jour du statut pour savedOrder
            if (savedOrder.getQuantityRemaining() == 0) {
                savedOrder.setStatus("Filled");
            } else if (savedOrder.getQuantityRemaining() < savedOrderInitialQty) {
                savedOrder.setStatus("PartiallyFilled");
            }
            // Mise à jour du statut pour candidate
            if (candidate.getQuantityRemaining() == 0) {
                candidate.setStatus("Filled");
            } else if (candidate.getQuantityRemaining() < candidateInitialQty) {
                candidate.setStatus("PartiallyFilled");
            }
            orderBookPort.save(savedOrder);
            orderBookPort.save(candidate);
            modifiedCandidates.add(candidate); // Tracker le candidat modifié par le matching
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
                savedOrder.setStatus(executions.isEmpty() ? "Cancelled" : "PartiallyFilled");
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
                    savedOrder.setStatus(executions.isEmpty() ? "Cancelled" : "PartiallyFilled");
                    orderBookPort.save(savedOrder);
                }
                // Retirer l'ordre DAY expiré du carnet
                orderBookPort.deleteById(savedOrder.getId());
            } else {
                if (savedOrder.getQuantityRemaining() > 0) {
                    // Si des exécutions ont eu lieu, mettre PartiallyFilled, sinon garder Working
                    if (!executions.isEmpty()) {
                        logger.info("DAY : exécution partielle, reste dans le carnet pour clientOrderId={}", savedOrder.getClientOrderId());
                        savedOrder.setStatus("PartiallyFilled");
                    } else {
                        logger.info("DAY : aucune exécution, reste Working dans le carnet pour clientOrderId={}", savedOrder.getClientOrderId());
                        savedOrder.setStatus("Working");
                    }
                    orderBookPort.save(savedOrder);
                } else {
                    // Si rempli, retirer du carnet
                    orderBookPort.deleteById(savedOrder.getId());
                }
            }
        }

        logger.info("Matching terminé pour clientOrderId={}, statut final={}, nombre d'exécutions={}, candidats modifiés={}",
                savedOrder.getClientOrderId(), savedOrder.getStatus(), executions.size(), modifiedCandidates.size());
        return new MatchingResult(savedOrder, executions, modifiedCandidates);
    }

    private List<OrderBook> cleanupExpiredDayOrders(String symbol) {
        List<OrderBook> dayOrders = orderBookPort.findAllBySymbol(symbol);
        LocalDateTime now = LocalDateTime.now();
        List<OrderBook> modifiedOrders = new ArrayList<>();
        for (OrderBook order : dayOrders) {
            if ("DAY".equalsIgnoreCase(order.getDuration())) {
                LocalDateTime orderTime = order.getTimestamp();
                if (orderTime != null && orderTime.plusHours(24).isBefore(now)) {
                    logger.info("Ordre DAY expiré détecté et supprimé : clientOrderId={}, timestamp={}",
                            order.getClientOrderId(), orderTime);
                    order.setStatus("Cancelled");
                    orderBookPort.save(order);
                    modifiedOrders.add(order);
                    orderBookPort.deleteById(order.getId());
                }
            }
        }
        return modifiedOrders;
    }
}
