#!/bin/bash
# ==============================================
# deploy.sh — Script de déploiement et rollback
# Projet LOG430 - Architecture microservices
# ==============================================

set -e  # Stoppe le script dès qu'une commande échoue

# --- CONFIGURATION -----------------------------------------
COMPOSE_FILE="docker-compose.yml"
HEALTH_URL="http://localhost:8079/actuator/health"   # URL de ton endpoint health
ROLLBACK_TAG="stable"                       # Nom de ton tag d'image stable
# ------------------------------------------------------------

function deploy() {
  echo "🚀 Déploiement de l'environnement complet..."
  echo "----------------------------------------------"

  echo "🧱 Construction des images Docker..."
  docker-compose -f $COMPOSE_FILE build

  echo "🗑️  Nettoyage des conteneurs existants..."
  docker-compose -f $COMPOSE_FILE down

  echo "🚢 Démarrage des services (app, DB, Prometheus, Grafana, Gateway, seed)..."
  docker-compose -f $COMPOSE_FILE up -d

  echo "⏳ Attente du démarrage des services..."
  sleep 30

  echo "🔍 Vérification du healthcheck..."
  if curl -fs $HEALTH_URL > /dev/null; then
    echo "✅ Application en santé à l'adresse $HEALTH_URL"
  else
    echo "❌ Échec du healthcheck ! Le déploiement peut avoir échoué."
    exit 1
  fi

  echo "📦 Déploiement terminé avec succès !"
  echo "----------------------------------------------"
}

function rollback() {
  echo "♻️  Rollback vers la dernière version stable..."
  echo "----------------------------------------------"

  echo "🛑 Arrêt des conteneurs courants..."
  docker-compose -f $COMPOSE_FILE down

  echo "📦 Téléchargement des images stables taguées '$ROLLBACK_TAG'..."
  docker-compose -f $COMPOSE_FILE pull

  echo "🚢 Relancement avec les images stables..."
  docker-compose -f $COMPOSE_FILE up -d

  echo "⏳ Attente du redémarrage..."
  sleep 30

  echo "🔍 Vérification du healthcheck..."
  if curl -fs $HEALTH_URL > /dev/null; then
    echo "✅ Rollback complété, service fonctionnel."
  else
    echo "⚠️ Rollback effectué, mais le healthcheck n'a pas répondu."
  fi

  echo "----------------------------------------------"
}

# --- LOGIQUE DU SCRIPT --------------------------------------

if [ "$1" == "rollback" ]; then
  rollback
else
  deploy
fi
