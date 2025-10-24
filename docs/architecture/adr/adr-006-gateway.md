# ADR 006 : Choix de l’API Gateway

**Statut** : Acceptée\
**Date** : 2025-10-24

## Contexte
L’API Gateway est un composant clé pour centraliser le routage, la sécurité, la documentation et la gestion des accès aux microservices. Plusieurs solutions sont envisageables : Kong, KrakenD, Spring Cloud Gateway, etc. Le choix doit permettre une intégration native avec l’écosystème Spring, la gestion des routes versionnées, des headers, du CORS, et la documentation Swagger.

## Décision
Nous avons choisi Spring Cloud Gateway comme API Gateway pour BrokerX. Ce choix s’explique par l’intégration native avec Spring Boot, la facilité de configuration des routes, la gestion des filtres, du CORS, des headers, et la compatibilité avec Docker. Spring Cloud Gateway permet d’unifier la sécurité, le monitoring, la documentation Swagger et le versionnage des APIs, tout en évitant la duplication de logique dans chaque microservice. La gestion des routes, des filtres et du CORS est flexible et adaptée à l’écosystème Spring déjà utilisé dans tous les microservices. La documentation Swagger peut être centralisée et exposée via la Gateway, facilitant l’accès pour les développeurs et les clients. La compatibilité avec Docker et la facilité de déploiement sont des atouts majeurs pour la CI/CD et la maintenance.

## Conséquences
- Centralisation du routage, de la sécurité et de la documentation.
- Intégration native avec l’écosystème Spring et Docker.
- Facilité d’évolution et de maintenance des routes et des règles d’accès.
- Complexité supplémentaire pour la gestion des filtres avancés et du monitoring.
