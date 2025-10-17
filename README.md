# BrokerX - Microservices Trading Platform

Plateforme de trading en microservices avec monitoring complet (Prometheus + Grafana).

## 🏗️ Architecture

### Microservices
- **API Gateway** (8079) - Point d'entrée unique
- **Auth Service** (8081) - Authentification et gestion des utilisateurs
- **Order Service** (8082) - Gestion des ordres de trading
- **Wallet Service** (8083) - Portefeuilles et soldes (avec cache)
- **Matching Service** (8084) - Carnet d'ordres et matching

### Infrastructure
- **PostgreSQL** - Base de données par microservice
- **Prometheus** (9090) - Collecte de métriques
- **Grafana** (3001) - Dashboards de monitoring
- **Frontend React** (3000) - Interface utilisateur

## 🚀 Démarrage rapide

### Option 1: Backend Docker + Frontend en développement (RECOMMANDÉ)

```bash
# 1. Démarrer les microservices et monitoring
docker-compose up --build -d

# 2. Démarrer le frontend en développement
cd frontend
npm install
npm start
```

### Option 2: Tout en Docker

Décommenter la section `frontend` dans `docker-compose.yml`, puis :

```bash
docker-compose up --build -d
```

## 📊 Accès aux services

- **Frontend React**: http://localhost:3000
- **API Gateway**: http://localhost:8079
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin123)

### Dashboards Grafana disponibles
- **Microservices Overview** - 4 Golden Signals globaux
- **API Gateway** - Métriques de routage
- **Auth Service** - Métriques de sécurité
- **Order Service** - Métriques de trading
- **Wallet Service** - Métriques de cache
- **Matching Service** - Métriques de performance

## 🔧 Configuration

### Développement
Les fichiers `application.properties` standard sont utilisés.

### Production
Copier `.env.example` vers `.env` et modifier les valeurs :

## 🛠️ Commandes utiles

```bash
# Démarrer tout
docker-compose up --build -d

# Voir les logs
docker-compose logs -f wallet-service

# Redémarrer un service
docker-compose restart auth-service

# Arrêter tout
docker-compose down

# Arrêter et supprimer les volumes (⚠️ EFFACE LES DONNÉES)
docker-compose down -v
```

## 📈 Monitoring

### Métriques automatiques incluses :
- **Latence** : P50, P95, P99 des requêtes HTTP
- **Trafic** : Requests per second (RPS)
- **Erreurs** : Taux d'erreur 4xx/5xx
- **Saturation** : CPU, mémoire, threads, DB connections
- **Cache** : Hits/misses, ratios (Wallet Service)

### Health Checks
Tous les services ont des health checks automatiques. Vérifier dans :
- Docker: `docker-compose ps`
- Prometheus: http://localhost:9090/targets
- Actuator: http://localhost:808X/actuator/health

## 🔒 Sécurité

- Utilisateurs non-root dans les containers
- Variables sensibles externalisées (`.env`)
- Headers de sécurité (Nginx)
- Health checks et restart policies

## 📚 Structure du projet

```
├── docker-compose.yml           # Orchestration principale
├── .env.example                # Variables d'environnement
├── monitoring/                 # Configuration Prometheus/Grafana
├── api-gateway/
│   ├── Dockerfile
│   └── src/.../application-docker.properties
├── auth-service/
├── order-service/
├── wallet-service/
├── matching-service/
└── frontend/
    ├── Dockerfile              # Production avec Nginx
    └── nginx.conf
```

## 🚀 Déploiement

### Serveur de production
```bash
git clone <ton-repo>
# Modifier .env avec les vraies valeurs
docker-compose up -d
```

### Variables importantes à changer en prod :
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `REACT_APP_API_URL` (ton domaine)
- `GRAFANA_ADMIN_PASSWORD`
