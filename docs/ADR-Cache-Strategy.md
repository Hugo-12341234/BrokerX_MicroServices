# ADR-001: Stratégie de Cache pour les Microservices

## Statut
Accepté - 2025-01-16

## Contexte
Le système BrokerX nécessite des optimisations de performance pour les appels fréquents aux données de référence (stocks, portefeuilles) dans un contexte de trading en temps réel.

### Endpoints identifiés comme coûteux :
1. `GET /api/v1/wallet/stock?symbol=X` - 50-150ms par appel
2. `GET /api/v1/wallet` - 100-300ms avec calculs de positions

### Contraintes :
- Architecture microservices avec load balancing prévu
- Latence critique (trading temps réel)
- Données semi-statiques avec TTL acceptable

## Décision
**Implémentation de cache mémoire local** avec Spring Boot `@Cacheable` pour la phase initiale.

## Justifications

### Avantages retenus :
- **Performance optimale** : Accès direct RAM (0.1-0.5ms)
- **Simplicité** : Pas d'infrastructure externe
- **Résilience** : Pas de point de défaillance supplémentaire
- **Développement** : Configuration minimale, focus sur la logique métier

### Compromis acceptés :
- **Fragmentation du cache** : Chaque instance maintient son propre cache
- **Efficacité réduite** : En multi-instance, cache hit ~60-70% vs 90%+
- **Cold starts** : Nouvelles instances partent sans cache

## Alternatives considérées

### Redis (Cache distribué) - Rejeté pour phase 1
**Avantages non retenus :**
- Cache partagé entre instances
- Persistance après redémarrage

**Inconvénients décisifs :**
- Complexité infrastructure (+redis cluster, monitoring, backup)
- Latence réseau (+1-3ms par appel)  
- Point de défaillance unique
- Over-engineering pour le contexte actuel

## Stratégie de migration

### Critères de révision vers Redis :
1. **Scale** : >5 instances simultanées par service
2. **Volume** : >1000 requêtes/seconde soutenues
3. **Cohérence** : Besoin de cache coherence stricte entre instances
4. **Métriques** : Cache hit rate <50% en production

### Implémentation progressive :
```java
// Phase 1 : Cache mémoire (Actuel)
@Cacheable(value = "stockCache", key = "#symbol")
public StockRule getStockBySymbol(String symbol) {
    return walletClient.getStockBySymbol(symbol);
}

// Phase 2 : Migration conditionnelle
@ConditionalOnProperty("cache.type=redis")
@Cacheable(value = "stockCache", key = "#symbol", cacheManager = "redisCacheManager")
```

## Métriques de succès
- Réduction latence P95 : >70% sur endpoints cachés
- Réduction charge DB : >60% sur requêtes répétitives  
- Cache hit rate : >80% en single instance, >60% en multi-instance

## Conséquences
- **Positive** : Gains de performance immédiats avec complexité minimale
- **Négative** : Efficacité sous-optimale en architecture distribuée
- **Neutre** : Migration path claire vers solution distribuée si besoin
