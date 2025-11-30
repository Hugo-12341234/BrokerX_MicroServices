package com.microservices.log430.matchingservice.domain.service;

import com.microservices.log430.matchingservice.adapters.web.dto.LastPriceDTO;
import com.microservices.log430.matchingservice.adapters.web.dto.OrderBookDTO;
import com.microservices.log430.matchingservice.domain.model.entities.OrderBook;
import com.microservices.log430.matchingservice.domain.model.entities.ExecutionReport;
import com.microservices.log430.matchingservice.adapters.web.dto.MatchingResult;
import com.microservices.log430.matchingservice.domain.port.in.MatchingPort;
import com.microservices.log430.matchingservice.domain.port.out.OrderBookPort;
import com.microservices.log430.matchingservice.domain.port.out.ExecutionReportPort;
import com.microservices.log430.matchingservice.adapters.messaging.events.OrderPlacedEvent;
import com.microservices.log430.matchingservice.adapters.messaging.events.OrderMatchedEvent;
import com.microservices.log430.matchingservice.adapters.messaging.events.OrderRejectedEvent;
import com.microservices.log430.matchingservice.adapters.messaging.events.NotificationEvent;
import com.microservices.log430.matchingservice.adapters.messaging.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatchingService implements MatchingPort {
    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    private final OrderBookPort orderBookPort;
    private final ExecutionReportPort executionReportPort;
    private final OutboxService outboxService;

    public MatchingService(OrderBookPort orderBookPort, ExecutionReportPort executionReportPort, OutboxService outboxService) {
        this.orderBookPort = orderBookPort;
        this.executionReportPort = executionReportPort;
        this.outboxService = outboxService;
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
            ExecutionReport savedExec = executionReportPort.save(exec);
            executions.add(savedExec);
            logger.info("Exécution sauvegardée : id={}, orderId={}, fillQty={}, fillPrice={}",
                    savedExec.getId(), savedExec.getOrderId(), savedExec.getFillQuantity(), savedExec.getFillPrice());
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
        logger.info("=== DÉBUT CLEANUP pour symbol: {} ===", symbol);

        List<OrderBook> dayOrders = orderBookPort.findAllBySymbol(symbol);
        LocalDateTime now = LocalDateTime.now();
        List<OrderBook> modifiedOrders = new ArrayList<>();

        logger.info("CLEANUP: {} ordres trouvés pour symbol {}, now={}", dayOrders.size(), symbol, now);

        for (OrderBook order : dayOrders) {
            logger.info("CLEANUP CHECK: id={}, clientOrderId={}, userId={}, symbol={}, duration={}, timestamp={}, status={}",
                    order.getId(), order.getClientOrderId(), order.getUserId(), order.getSymbol(),
                    order.getDuration(), order.getTimestamp(), order.getStatus());

            if ("DAY".equalsIgnoreCase(order.getDuration())) {
                LocalDateTime orderTime = order.getTimestamp();

                if (orderTime == null) {
                    logger.error("CLEANUP ERROR: Ordre DAY avec timestamp NULL - id={}, clientOrderId={}, SUPPRESSION FORCÉE",
                            order.getId(), order.getClientOrderId());
                    order.setStatus("Cancelled");
                    orderBookPort.save(order);
                    modifiedOrders.add(order);
                    orderBookPort.deleteById(order.getId());
                    continue;
                }

                LocalDateTime expirationTime = orderTime.plusHours(24);
                boolean isExpired = expirationTime.isBefore(now);

                logger.info("CLEANUP DAY: clientOrderId={}, orderTime={}, expirationTime={}, now={}, isExpired={}",
                        order.getClientOrderId(), orderTime, expirationTime, now, isExpired);

                if (isExpired) {
                    logger.warn("CLEANUP: SUPPRESSION ordre DAY expiré - clientOrderId={}, orderTime={}, expirationTime={}",
                            order.getClientOrderId(), orderTime, expirationTime);
                    order.setStatus("Cancelled");
                    orderBookPort.save(order);
                    modifiedOrders.add(order);
                    orderBookPort.deleteById(order.getId());
                } else {
                    logger.info("CLEANUP: Ordre DAY VALIDE conservé - clientOrderId={}", order.getClientOrderId());
                }
            } else {
                logger.info("CLEANUP: Ordre non-DAY ignoré - clientOrderId={}, duration={}",
                        order.getClientOrderId(), order.getDuration());
            }
        }

        logger.info("=== FIN CLEANUP pour symbol: {}, {} ordres supprimés ===", symbol, modifiedOrders.size());
        return modifiedOrders;
    }


    @Override
    public OrderBook modifyOrder(String clientOrderId, OrderBook orderBook) {
        Optional<OrderBook> existingOrder = orderBookPort.findByClientOrderId(clientOrderId);
        if (existingOrder.isEmpty()) throw new IllegalArgumentException("Ordre non trouvé");
        OrderBook existing = existingOrder.get();
        if ("Filled".equals(existing.getStatus()) || "Cancelled".equals(existing.getStatus())) {
            throw new IllegalStateException("Impossible de modifier un ordre rempli ou annulé");
        }
        // Appliquer les modifications sur les quantités restantes
        existing.setQuantity(orderBook.getQuantity());
        existing.setPrice(orderBook.getPrice());
        existing.setType(orderBook.getType());
        existing.setDuration(orderBook.getDuration());
        // Si besoin, d'autres champs peuvent être mis à jour ici
        OrderBook updatedOrder = orderBookPort.save(existing);
        logger.info("Ordre modifié : id={}, clientOrderId={}, status={}", updatedOrder.getId(), updatedOrder.getClientOrderId(), updatedOrder.getStatus());
        return updatedOrder;
    }

    @Override
    public OrderBook cancelOrder(Long orderId) {
        Optional<OrderBook> existingOrder = orderBookPort.findById(orderId);
        if (existingOrder.isEmpty()) throw new IllegalArgumentException("Ordre non trouvé");
        OrderBook existing = existingOrder.get();
        if ("Filled".equals(existing.getStatus()) || "Cancelled".equals(existing.getStatus())) {
            throw new IllegalStateException("Impossible d'annuler un ordre rempli ou déjà annulé");
        }
        existing.setStatus("Cancelled");
        OrderBook cancelledOrder = orderBookPort.save(existing);
        logger.info("Ordre annulé : id={}, clientOrderId={}, status={}", cancelledOrder.getId(), cancelledOrder.getClientOrderId(), cancelledOrder.getStatus());
        return cancelledOrder;
    }

    @Override
    public OrderBookDTO getOrderBookBySymbol(String symbol) {
        // Logique pour récupérer le snapshot du carnet d'ordres pour le symbole
        return orderBookPort.getOrderBookSnapshot(symbol);
    }

    @Override
    public LastPriceDTO getLastPriceBySymbol(String symbol) {
        Optional<ExecutionReport> lastReportOpt = executionReportPort.findLastBySymbol(symbol);
        if (lastReportOpt.isEmpty()) return null;
        ExecutionReport lastReport = lastReportOpt.get();
        return new LastPriceDTO(symbol, lastReport.getFillPrice(), lastReport.getExecutionTime().toString());
    }

    @Override
    public void processOrderPlacedEvent(OrderPlacedEvent orderPlacedEvent) {
        logger.info("Début du processOrderPlacedEvent pour clientOrderId={}, userId={}, symbol={}, version={}",
                   orderPlacedEvent.getClientOrderId(), orderPlacedEvent.getUserId(), orderPlacedEvent.getSymbol(), orderPlacedEvent.getVersion());
        try {
            // Conversion de l'événement en OrderBook
            OrderBook orderBook = convertOrderPlacedEventToOrderBook(orderPlacedEvent);
            logger.info("Début du matching pour ordre: clientOrderId={}, userId={}, symbol={}, side={}",
                       orderBook.getClientOrderId(), orderBook.getUserId(), orderBook.getSymbol(), orderBook.getSide());
            // Exécuter le matching
            MatchingResult result = matchOrder(orderBook);
            if (result != null && result.updatedOrder != null) {
                // Publier les événements selon le résultat
                publishMatchingEvents(result, orderPlacedEvent);
            } else {
                logger.error("Résultat de matching null pour ordre: {}", orderBook.getClientOrderId());
                publishOrderRejected(orderPlacedEvent, "Erreur interne lors du matching");
            }
        } catch (Exception e) {
            logger.error("Erreur lors du traitement de l'ordre placé: {}", e.getMessage(), e);
            publishOrderRejected(orderPlacedEvent, "Erreur lors du matching: " + e.getMessage());
        }
    }

    /**
     * Convertit un OrderPlacedEvent en OrderBook
     */
    private OrderBook convertOrderPlacedEventToOrderBook(OrderPlacedEvent event) {
        OrderBook orderBook = new OrderBook();
        orderBook.setOrderId(event.getId()); // Assigner l'orderId depuis l'événement
        orderBook.setClientOrderId(event.getClientOrderId());
        orderBook.setUserId(event.getUserId());
        orderBook.setSymbol(event.getSymbol());
        orderBook.setSide(event.getSide());
        orderBook.setType(event.getType());
        orderBook.setQuantity(event.getQuantity());
        orderBook.setPrice(event.getPrice());
        orderBook.setDuration(event.getDuration());
        orderBook.setTimestamp(event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        orderBook.setStatus(event.getStatus());
        orderBook.setRejectReason(event.getRejectReason());
        orderBook.setVersion(event.getVersion());
        orderBook.setQuantityRemaining(event.getQuantity());

        return orderBook;
    }

    /**
     * Publie les événements selon le résultat du matching
     */
    private void publishMatchingEvents(MatchingResult result, OrderPlacedEvent originalOrder) {
        try {
            // Publier l'événement ORDER_MATCHED ou ORDER_REJECTED selon le cas
            if ("REJETE".equals(result.updatedOrder.getStatus()) ||
                    (result.updatedOrder.getRejectReason() != null && !result.updatedOrder.getRejectReason().isEmpty())) {
                // Ordre rejeté
                publishOrderRejected(originalOrder, result.updatedOrder.getRejectReason());
            } else {
                // Ordre accepté (avec ou sans exécutions)
                publishOrderMatched(result, originalOrder);
            }

            // Publier les notifications spécifiques selon le résultat
            publishDetailedNotifications(result, originalOrder);

        } catch (Exception e) {
            logger.error("Erreur lors de la publication des événements de matching: {}", e.getMessage(), e);
            // Publier une notification d'erreur
            publishErrorNotification(originalOrder, "Erreur lors du traitement: " + e.getMessage());
        }
    }

    /**
     * Publie l'événement ORDER_MATCHED
     */
    private void publishOrderMatched(MatchingResult result, OrderPlacedEvent originalOrder) {
        List<OrderMatchedEvent.ExecutionDetails> executionDetails = null;

        if (result.executions != null) {
            executionDetails = result.executions.stream()
                    .map(exec -> new OrderMatchedEvent.ExecutionDetails(
                            exec.getId(),
                            exec.getBuyerUserId(),
                            exec.getSellerUserId(),
                            exec.getSymbol(),
                            exec.getFillQuantity(),
                            exec.getFillPrice(),
                            exec.getExecutionTime(),
                            exec.getOrderId()
                    ))
                    .collect(Collectors.toList());
        }

        List<String> modifiedCandidateIds = null;
        if (result.modifiedCandidates != null) {
            modifiedCandidateIds = result.modifiedCandidates.stream()
                    .map(OrderBook::getClientOrderId)
                    .collect(Collectors.toList());
        }

        OrderMatchedEvent event = new OrderMatchedEvent(
                originalOrder.getId(),
                originalOrder.getClientOrderId(),
                originalOrder.getUserId(),
                originalOrder.getSymbol(),
                originalOrder.getSide(),
                originalOrder.getType(),
                originalOrder.getQuantity(),
                originalOrder.getPrice(),
                originalOrder.getDuration(),
                originalOrder.getTimestamp(),
                result.updatedOrder.getStatus(),
                result.updatedOrder.getRejectReason(),
                originalOrder.getVersion(),
                executionDetails,
                modifiedCandidateIds
        );

        outboxService.saveEvent("ORDER_MATCHED", originalOrder.getId(), event);
        logger.info("Événement ORDER_MATCHED publié pour ordre: {}", originalOrder.getClientOrderId());
    }

    /**
     * Publie l'événement ORDER_REJECTED
     */
    private void publishOrderRejected(OrderPlacedEvent originalOrder, String rejectReason) {
        OrderRejectedEvent event = new OrderRejectedEvent(
                originalOrder.getId(),
                originalOrder.getClientOrderId(),
                originalOrder.getUserId(),
                originalOrder.getSymbol(),
                originalOrder.getSide(),
                originalOrder.getType(),
                originalOrder.getQuantity(),
                originalOrder.getPrice(),
                originalOrder.getDuration(),
                originalOrder.getTimestamp(),
                "REJETE",
                rejectReason,
                originalOrder.getVersion()
        );

        outboxService.saveEvent("ORDER_REJECTED", originalOrder.getId(), event);
        logger.info("Événement ORDER_REJECTED publié pour ordre: {} - raison: {}",
                originalOrder.getClientOrderId(), rejectReason);
    }

    /**
     * Publie les notifications détaillées selon le résultat du matching
     */
    private void publishDetailedNotifications(MatchingResult result, OrderPlacedEvent originalOrder) {
        try {
            // Déterminer le type d'ordre et son statut final
            boolean isDAY = "DAY".equalsIgnoreCase(originalOrder.getDuration());
            boolean isIOC = "IOC".equalsIgnoreCase(originalOrder.getDuration());
            boolean isFOK = "FOK".equalsIgnoreCase(originalOrder.getDuration());

            boolean hasExecutions = result.executions != null && !result.executions.isEmpty();

            // Publier notification de statut d'ordre
            publishOrderStatusNotification(result, originalOrder, isDAY, isIOC, isFOK, hasExecutions);

            // Publier notifications d'exécution individuelles si il y en a
            if (hasExecutions) {
                publishExecutionNotifications(result.executions);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la publication des notifications détaillées: {}", e.getMessage(), e);
        }
    }

    /**
     * Publie la notification de statut d'ordre
     */
    private void publishOrderStatusNotification(MatchingResult result, OrderPlacedEvent originalOrder,
                                                boolean isDAY, boolean isIOC, boolean isFOK, boolean hasExecutions) {
        String notificationMessage;
        String status;
        String finalStatus = result.updatedOrder.getStatus();
        boolean isMarketOrder = "MARKET".equalsIgnoreCase(originalOrder.getType());

        // Calculer les quantités exécutées et annulées pour IOC
        int executedQuantity = 0;
        Double averageExecutionPrice = null;
        if (hasExecutions && result.executions != null) {
            executedQuantity = result.executions.stream()
                    .mapToInt(ExecutionReport::getFillQuantity)
                    .sum();

            // Calculer le prix moyen d'exécution pour les ordres marché
            if (isMarketOrder && executedQuantity > 0) {
                double totalValue = result.executions.stream()
                        .mapToDouble(exec -> exec.getFillQuantity() * exec.getFillPrice())
                        .sum();
                averageExecutionPrice = totalValue / executedQuantity;
            }
        }
        int cancelledQuantity = originalOrder.getQuantity() - executedQuantity;

        // Déterminer le message et le statut selon le type d'ordre et le résultat
        if ("Cancelled".equals(finalStatus) && isFOK) {
            // FOK annulé - aucun match trouvé
            if (isMarketOrder) {
                notificationMessage = String.format("Ordre FOK ANNULÉ : %s %d %s (ordre marché). Aucun match trouvé. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(), originalOrder.getId());
            } else {
                notificationMessage = String.format("Ordre FOK ANNULÉ : %s %d %s @ %.2f$. Aucun match trouvé. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), originalOrder.getId());
            }
            status = "FOK_CANCELLED";

        } else if ("Cancelled".equals(finalStatus) && isIOC) {
            // IOC complètement annulé - aucune exécution
            if (isMarketOrder) {
                notificationMessage = String.format("Ordre IOC ANNULÉ : %s %d %s (ordre marché). 0 exécuté, %d annulé. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getQuantity(), originalOrder.getId());
            } else {
                notificationMessage = String.format("Ordre IOC ANNULÉ : %s %d %s @ %.2f$. 0 exécuté, %d annulé. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), originalOrder.getQuantity(), originalOrder.getId());
            }
            status = "IOC_CANCELLED";

        } else if ("PartiallyFilled".equals(finalStatus) && isIOC) {
            // IOC partiellement exécuté
            if (isMarketOrder) {
                if (averageExecutionPrice != null) {
                    notificationMessage = String.format("Ordre IOC TRAITÉ : %s %s @ %.2f$ moyen (ordre marché). %d exécuté, %d annulé. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getSymbol(), averageExecutionPrice,
                            executedQuantity, cancelledQuantity, originalOrder.getId());
                } else {
                    notificationMessage = String.format("Ordre IOC TRAITÉ : %s %s (ordre marché). %d exécuté, %d annulé. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getSymbol(),
                            executedQuantity, cancelledQuantity, originalOrder.getId());
                }
            } else {
                notificationMessage = String.format("Ordre IOC TRAITÉ : %s %s @ %.2f$. %d exécuté, %d annulé. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), executedQuantity, cancelledQuantity, originalOrder.getId());
            }
            status = "IOC_PARTIAL_FILLED";

        } else if ("Filled".equals(finalStatus) && isIOC) {
            // IOC complètement exécuté
            if (isMarketOrder) {
                if (averageExecutionPrice != null) {
                    notificationMessage = String.format("Ordre IOC COMPLÈTEMENT EXÉCUTÉ : %s %d %s @ %.2f$ moyen (ordre marché). OrderId : %s",
                            originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                            averageExecutionPrice, originalOrder.getId());
                } else {
                    notificationMessage = String.format("Ordre IOC COMPLÈTEMENT EXÉCUTÉ : %s %d %s (ordre marché). OrderId : %s",
                            originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(), originalOrder.getId());
                }
            } else {
                notificationMessage = String.format("Ordre IOC COMPLÈTEMENT EXÉCUTÉ : %s %d %s @ %.2f$. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), originalOrder.getId());
            }
            status = "IOC_FILLED";

        } else if ("Working".equals(finalStatus) && isDAY) {
            if (!hasExecutions) {
                if (isMarketOrder) {
                    notificationMessage = String.format("Ordre DAY PLACÉ : %s %d %s (ordre marché). En attente dans le carnet. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(), originalOrder.getId());
                } else {
                    notificationMessage = String.format("Ordre DAY PLACÉ : %s %d %s @ %.2f$. En attente dans le carnet. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                            originalOrder.getPrice(), originalOrder.getId());
                }
                status = "DAY_PLACED_NO_MATCH";
            } else {
                if (isMarketOrder) {
                    if (averageExecutionPrice != null) {
                        notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s @ %.2f$ moyen (ordre marché). Reste en attente. OrderId : %s",
                                originalOrder.getSide(), originalOrder.getSymbol(), averageExecutionPrice, originalOrder.getId());
                    } else {
                        notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s (ordre marché). Reste en attente. OrderId : %s",
                                originalOrder.getSide(), originalOrder.getSymbol(), originalOrder.getId());
                    }
                } else {
                    notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s @ %.2f$. Reste en attente. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getSymbol(),
                            originalOrder.getPrice(), originalOrder.getId());
                }
                status = "DAY_PARTIAL_FILLED";
            }

        } else if ("PartiallyFilled".equals(finalStatus) && isDAY) {
            if (isMarketOrder) {
                if (averageExecutionPrice != null) {
                    notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s @ %.2f$ moyen (ordre marché). Reste en attente. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getSymbol(), averageExecutionPrice, originalOrder.getId());
                } else {
                    notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s (ordre marché). Reste en attente. OrderId : %s",
                            originalOrder.getSide(), originalOrder.getSymbol(), originalOrder.getId());
                }
            } else {
                notificationMessage = String.format("Ordre DAY partiellement exécuté : %s %s @ %.2f$. Reste en attente. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), originalOrder.getId());
            }
            status = "DAY_PARTIAL_FILLED";

        } else if ("Filled".equals(finalStatus)) {
            if (isMarketOrder) {
                if (averageExecutionPrice != null) {
                    notificationMessage = String.format("Ordre COMPLÈTEMENT EXÉCUTÉ : %s %d %s @ %.2f$ moyen (ordre marché). OrderId : %s",
                            originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                            averageExecutionPrice, originalOrder.getId());
                } else {
                    notificationMessage = String.format("Ordre COMPLÈTEMENT EXÉCUTÉ : %s %d %s (ordre marché). OrderId : %s",
                            originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(), originalOrder.getId());
                }
            } else {
                notificationMessage = String.format("Ordre COMPLÈTEMENT EXÉCUTÉ : %s %d %s @ %.2f$. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), originalOrder.getId());
            }
            status = "FILLED";

        } else if ("Cancelled".equals(finalStatus)) {
            if (isMarketOrder) {
                notificationMessage = String.format("Ordre ANNULÉ : %s %d %s (ordre marché). OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(), originalOrder.getId());
            } else {
                notificationMessage = String.format("Ordre ANNULÉ : %s %d %s @ %.2f$. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), originalOrder.getId());
            }
            status = "CANCELLED";

        } else {
            if (isMarketOrder) {
                notificationMessage = String.format("Ordre traité : %s %d %s (ordre marché). Statut : %s. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        finalStatus, originalOrder.getId());
            } else {
                notificationMessage = String.format("Ordre traité : %s %d %s @ %.2f$. Statut : %s. OrderId : %s",
                        originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                        originalOrder.getPrice(), finalStatus, originalOrder.getId());
            }
            status = "PROCESSED";
        }

        // Publier la notification de statut
        NotificationEvent orderStatusEvent = new NotificationEvent(
                originalOrder.getUserId(),
                notificationMessage,
                Instant.now(),
                "WEBSOCKET",
                null,
                status
        );

        outboxService.saveEvent("NOTIFICATION_SEND", originalOrder.getId(), orderStatusEvent);
        logger.info("Notification de statut publiée pour orderId={}, userId={}, status={}",
                originalOrder.getId(), originalOrder.getUserId(), status);
    }

    /**
     * Publie les notifications d'exécution individuelles
     */
    private void publishExecutionNotifications(List<ExecutionReport> executions) {
        for (ExecutionReport exec : executions) {
            // Notification pour l'acheteur
            if (exec.getBuyerUserId() != null && exec.getBuyerUserId() != 9999L) {
                String buyerMsg = String.format("Exécution d'ordre ACHAT : %d %s @ %.2f$. OrderId: %s. ExecutionReportId: %s. Date: %s",
                        exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());

                NotificationEvent buyerNotification = new NotificationEvent(
                        exec.getBuyerUserId(), buyerMsg, Instant.now(), "WEBSOCKET", null, "EXECUTED"
                );

                outboxService.saveEvent("NOTIFICATION_SEND", exec.getBuyerUserId(), buyerNotification);
                logger.info("Notification d'exécution publiée pour buyer userId={}, executionId={}",
                        exec.getBuyerUserId(), exec.getId());
            }

            // Notification pour le vendeur
            if (exec.getSellerUserId() != null && exec.getSellerUserId() != 9999L) {
                String sellerMsg = String.format("Exécution d'ordre VENTE : %d %s @ %.2f$. OrderId: %s. ExecutionReportId: %s. Date: %s",
                        exec.getFillQuantity(), exec.getSymbol(), exec.getFillPrice(), exec.getOrderId(), exec.getId(), Instant.now());

                NotificationEvent sellerNotification = new NotificationEvent(
                        exec.getSellerUserId(), sellerMsg, Instant.now(), "WEBSOCKET", null, "EXECUTED"
                );

                outboxService.saveEvent("NOTIFICATION_SEND", exec.getSellerUserId(), sellerNotification);
                logger.info("Notification d'exécution publiée pour seller userId={}, executionId={}",
                        exec.getSellerUserId(), exec.getId());
            }
        }
    }

    /**
     * Publie une notification d'erreur
     */
    private void publishErrorNotification(OrderPlacedEvent originalOrder, String errorMessage) {
        try {
            String message = String.format("Erreur lors du traitement de l'ordre : %s %d %s. OrderId : %s. Erreur : %s",
                    originalOrder.getSide(), originalOrder.getQuantity(), originalOrder.getSymbol(),
                    originalOrder.getId(), errorMessage);

            NotificationEvent errorEvent = new NotificationEvent(
                    originalOrder.getUserId(),
                    message,
                    Instant.now(),
                    "WEBSOCKET",
                    null,
                    "ERROR"
            );

            outboxService.saveEvent("NOTIFICATION_SEND", originalOrder.getId(), errorEvent);
            logger.info("Notification d'erreur publiée pour orderId={}, userId={}",
                    originalOrder.getId(), originalOrder.getUserId());
        } catch (Exception e) {
            logger.error("Impossible de publier la notification d'erreur : {}", e.getMessage(), e);
        }
    }

}
