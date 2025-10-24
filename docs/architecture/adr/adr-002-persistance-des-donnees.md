# ADR 002 : Stratégie de persistance & transactions

**Statut** : Acceptée\
**Date** : 2025-10-24

## Contexte
Dans BrokerX, chaque microservice gère sa propre base de données PostgreSQL pour garantir l’isolation, la scalabilité et la conformité. Les migrations sont gérées par Flyway, l’accès aux données par JPA/Hibernate, et l’intégrité par des transactions ACID. L’audit, l’idempotence et la traçabilité sont des exigences fortes du métier (ordres, comptes, positions).

## Décision
Chaque microservice dispose d’une base PostgreSQL dédiée, avec schéma et migrations indépendants. Ce choix permet d’éviter les effets de bord et les blocages entre domaines : une erreur ou une surcharge sur un service n’impacte pas les autres. PostgreSQL a été choisi pour sa robustesse, sa gestion avancée des transactions et sa compatibilité avec les outils de migration (Flyway) et d’ORM (JPA/Hibernate). Les migrations indépendantes facilitent l’évolution du schéma sans coordination complexe entre équipes. Les transactions sont gérées au niveau du service, avec rollback sur erreur. Les clés d’idempotence sont utilisées pour les opérations critiques (dépôts, ordres). Un journal d’audit append-only est mis en place dans chaque base de données pour la traçabilité et la conformité. Cette approche évite la complexité des transactions distribuées, tout en assurant la cohérence locale et la traçabilité.

## Conséquences
- Isolation des données : chaque service est responsable de son modèle et de son intégrité.
- Scalabilité et résilience accrues : pas de dépendance croisée sur la persistance.
- Migrations reproductibles et auditables (Flyway).
- Gestion robuste des erreurs et des transactions.
- Complexité accrue pour la cohérence globale (pas de transactions distribuées).
