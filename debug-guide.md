# 🔧 Guide de Debug pour Microservices BrokerX (Sans ports externes)

## 🎯 Méthodes de debug disponibles

### **1. Via NGINX Load Balancer (Port 8078)**
```bash
# Tester auth-service
curl http://localhost:8078/auth-service/actuator/health

# Tester order-service
curl http://localhost:8078/order-service/actuator/health

# Tester wallet-service
curl http://localhost:8078/wallet-service/actuator/health

# Tester matching-service
curl http://localhost:8078/matching-service/actuator/health

# Voir les métriques NGINX
curl http://localhost:8078/nginx_status
```

### **2. Via API Gateway (Port 8079)**
```bash
# Routes complètes via l'API Gateway
curl http://localhost:8079/api/v1/users/test      # Auth service
curl http://localhost:8079/api/v1/orders/         # Order service
curl http://localhost:8079/api/v1/wallet/         # Wallet service
curl http://localhost:8079/api/v1/orderbook/      # Matching service
```

### **3. Logs Docker**
```bash
# Voir les logs en temps réel
docker-compose logs -f auth-service
docker-compose logs -f order-service
docker-compose logs -f wallet-service
docker-compose logs -f matching-service

# Logs de toutes les instances d'un service
docker-compose logs auth-service

# Logs d'une instance spécifique
docker logs log430_brokerx_microservices-auth-service-1
docker logs log430_brokerx_microservices-auth-service-2
```

### **4. État des containers**
```bash
# Voir l'état de tous les services
docker-compose ps

# Voir les ressources utilisées
docker stats

# Voir les détails d'un container spécifique
docker inspect log430_brokerx_microservices-auth-service-1
```

### **5. Accès direct aux containers (Pour debug avancé)**
```bash
# Se connecter à un container
docker exec -it log430_brokerx_microservices-auth-service-1 sh

# Une fois dans le container, tester localement
wget http://localhost:8081/actuator/health
```

## 🚀 Commandes de scaling et vérification

### **Scaling basique**
```bash
# 2 instances de chaque
docker-compose up -d --scale auth-service=2 --scale order-service=2 --scale wallet-service=2 --scale matching-service=2

# Vérifier que toutes les instances sont créées
docker-compose ps
```

### **Vérifier le load balancing**
```bash
# Faire plusieurs requêtes pour voir la répartition
for i in {1..10}; do curl http://localhost:8078/auth-service/actuator/health; done
```

### **Monitoring des instances multiples**
```bash
# Prometheus voit automatiquement toutes les instances
curl http://localhost:8090/api/v1/targets

# Grafana dashboards fonctionnent avec toutes les instances
# http://localhost:3001
```

## 🔍 Troubleshooting

### **Si un service ne répond pas**
```bash
# 1. Vérifier l'état du container
docker-compose ps service-name

# 2. Voir les logs d'erreur
docker-compose logs service-name

# 3. Tester via NGINX
curl -v http://localhost:8078/service-name/actuator/health

# 4. Redémarrer le service si nécessaire
docker-compose restart service-name
```

### **Si le load balancer ne fonctionne pas**
```bash
# Vérifier NGINX
docker-compose logs nginx-load-balancer

# Tester la configuration NGINX
docker exec nginx-load-balancer nginx -t

# Recharger la configuration NGINX
docker exec nginx-load-balancer nginx -s reload
```

## 📊 URLs importantes pour le monitoring

- **API Gateway**: http://localhost:8079
- **NGINX Load Balancer**: http://localhost:8078/nginx_status  
- **Prometheus**: http://localhost:8090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Frontend**: http://localhost:3000

## ✅ Avantages de cette configuration

1. **Scaling sans conflits** - Plus d'erreurs de ports
2. **Load balancing automatique** - Docker + NGINX gèrent tout
3. **Monitoring complet** - Prometheus voit toutes les instances
4. **Architecture production-ready** - Un seul point d'entrée

## 🎯 Exemple de workflow de test

```bash
# 1. Démarrer avec scaling
docker-compose up -d --scale order-service=3

# 2. Vérifier que tout fonctionne
docker-compose ps
curl http://localhost:8078/nginx_status

# 3. Tester le load balancing
curl http://localhost:8078/order-service/actuator/health

# 4. Voir les logs si problème
docker-compose logs order-service

# 5. Monitorer via Grafana
# Aller sur http://localhost:3001
```
