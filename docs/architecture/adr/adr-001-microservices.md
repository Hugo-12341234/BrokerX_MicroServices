# ADR 001 : Style architectural & découpage microservices

**Statut** : Acceptée\
**Date** : 2025-10-24

## Contexte
Le projet BrokerX, initialement monolithique, doit évoluer vers une architecture microservices pour répondre aux exigences de scalabilité, modularité, résilience et déploiement indépendant. Le découpage logique s’appuie sur les domaines métier : Authentification, Ordres, Matching, Wallet. L’API Gateway devient le point d’entrée unique, assurant le routage, la sécurité et la cohérence des appels.

## Décision
Nous adoptons une architecture microservices REST, chaque service étant conteneurisé et indépendant, avec une API Gateway (Spring Cloud Gateway) pour le routage et la gestion des accès. Ce choix permet de centraliser la sécurité, le monitoring, la documentation (Swagger) et le versionnage des APIs, tout en évitant la duplication de logique dans chaque microservice. Le découpage par domaine métier assure une meilleure évolutivité : chaque équipe peut travailler sur un service sans impacter les autres, et chaque service peut être déployé ou mis à l’échelle indépendamment. Les communications inter-services se font via HTTP REST, avec des routes versionnées et des codes d’erreur normalisés.

## Conséquences
- Scalabilité horizontale : chaque service peut être répliqué indépendamment.
- Déploiement et maintenance facilités : isolation des pannes, évolutivité.
- Complexité accrue : gestion des dépendances, monitoring, orchestration.
- API Gateway centralise la sécurité, le routage et la documentation (Swagger).
- Migration facilitée : chaque domaine peut évoluer ou être remplacé sans impacter les autres.
