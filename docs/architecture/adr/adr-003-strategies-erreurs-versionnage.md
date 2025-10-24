# ADR 003 : Stratégie d’erreurs, versionnage & conformité

**Statut** : Acceptée\
**Date** : 2025-10-24

## Contexte
En architecture microservices, la gestion des erreurs, du versionnage d’API et de la conformité (audit, sécurité, traçabilité) est essentielle pour garantir la robustesse, l’évolutivité et la conformité réglementaire. Les erreurs doivent être normalisées (JSON, codes HTTP), le versionnage doit permettre l’évolution sans rupture, et la traçabilité doit répondre aux exigences KYC/AML et audit métier.

## Décision
Les erreurs sont normalisées en JSON avec des codes HTTP explicites et des messages détaillés. Le versionnage des routes est systématique (/api/v1/...), permettant l’évolution des contrats sans impacter les clients existants. Un audit append-only est mis en place pour toutes les opérations sensibles, et la conformité est assurée par la journalisation et la validation des entrées (authentification, sécurité JWT, validation des payloads).

## Conséquences
- Robustesse accrue : erreurs claires, versionnage maîtrisé, audit complet.
- Facilité d’évolution des APIs et des clients.
- Conformité réglementaire et traçabilité assurées.
- Complexité technique : gestion des versions, des audits et de la sécurité sur chaque service.
