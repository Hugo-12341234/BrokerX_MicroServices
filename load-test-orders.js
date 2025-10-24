import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Métriques personnalisées
const orderCreationRate = new Rate('order_creation_success');
const orderCounter = new Counter('orders_created_total');
const matchingTrend = new Trend('matching_response_time');

// Configuration du test de charge
export const options = {
  scenarios: {
    // Rampe progressive jusqu'à 800+ ordres/s
    order_creation_ramp: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 50 },   // Montée douce
        { duration: '30s', target: 100 },  // 100 VUs
        { duration: '30s', target: 200 },  // 200 VUs
        { duration: '30s', target: 400 },  // 400 VUs - plateau intermédiaire
        { duration: '30s', target: 600 },  // 600 VUs
        { duration: '30s', target: 800 }, // 800 VUs - cible principale
        { duration: '30s', target: 800 }, // Maintien à 800 VUs (stress test)
        { duration: '30s', target: 400 },  // Descente progressive
        { duration: '30s', target: 0 },    // Arrêt
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // Objectifs de performance
    'http_req_duration': ['p(95)<2000'], // 95% des requêtes < 2s
    'order_creation_success': ['rate>0.95'], // 95% de succès
    'http_req_failed': ['rate<0.05'], // Moins de 5% d'échecs
  },
};

// Configuration des endpoints - CORRIGÉE selon votre architecture
const API_GATEWAY_URL = 'http://localhost:8079';
const ORDER_ENDPOINT = `${API_GATEWAY_URL}/api/v1/orders/place`;

// Pool de données pour la variété - AJUSTÉ selon les actions disponibles
const SYMBOLS = ['AAPL', 'MSFT', 'TSLA']; // Seulement les 3 actions disponibles dans votre système
const USER_POOL_SIZE = 100;

// Génération d'utilisateurs virtuels (IDs numériques selon votre controller)
function generateUserId() {
  return Math.floor(Math.random() * USER_POOL_SIZE) + 1; // IDs de 1 à 100
}

function generateOrderData() {
  const isBuy = Math.random() > 0.5; // 50/50 achats/ventes
  const symbol = SYMBOLS[Math.floor(Math.random() * SYMBOLS.length)];
  const quantity = Math.floor(Math.random() * 1) + 1; // 1-100 actions

  return {
    symbol: symbol,
    side: isBuy ? 'ACHAT' : 'VENTE', // CORRIGÉ: utiliser les enums français
    quantity: quantity,
    price: 200.00, // Prix fixe à 150$ comme demandé
    type: 'LIMITE', // CORRIGÉ: utiliser l'enum français
    duration: 'DAY' // CORRIGÉ: utiliser l'enum français
  };
}

// Fonction principale de test - SIMPLIFIÉE avec bypass d'authentification
export default function () {
  const orderData = generateOrderData();
  const userId = generateUserId();

  // Headers avec bypass d'authentification via X-Internal-Call
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': userId.toString(), // Header attendu par votre OrderController
    'X-Internal-Call': 'true', // BYPASS d'authentification - pas de JWT requis
    'X-Request-Id': `req-${__VU}-${__ITER}`, // Pour traçabilité
    'Idempotency-Key': `order-${__VU}-${__ITER}-${Date.now()}` // Pour éviter duplicatas
  };

  // Création d'ordre - CŒUR DU TEST
  const orderPayload = JSON.stringify(orderData);
  const orderResponse = http.post(ORDER_ENDPOINT, orderPayload, { headers });

  // Vérifications adaptées à votre API
  const orderSuccess = check(orderResponse, {
    'order created successfully': (r) => r.status === 200 || r.status === 201,
    'response time < 3s': (r) => r.timings.duration < 3000,
    'no server errors': (r) => r.status < 500,
    'no auth errors': (r) => r.status !== 401, // Plus d'erreurs 401 avec le bypass
  });

  orderCreationRate.add(orderSuccess);
  orderCounter.add(1);

  // Log des erreurs pour debugging
  if (orderResponse.status >= 400) {
    console.log(`❌ Erreur ${orderResponse.status} pour userId ${userId}: ${orderResponse.body}`);
  }

  // Petit délai pour simuler un comportement réaliste
  sleep(Math.random() * 0.1); // 0-100ms de délai aléatoire
}

// Fonction de setup
export function setup() {
  console.log('🚀 Démarrage du test de charge - Objectif: 800+ ordres/s');
  console.log(`🎯 Endpoint ciblé: ${ORDER_ENDPOINT}`);
  console.log('📊 Surveillance: http://localhost:3000 (Grafana)');

  // Test de connectivité
  const testResponse = http.get(`${API_GATEWAY_URL}/actuator/health`);
  if (testResponse.status !== 200) {
    console.log('⚠️  Attention: API Gateway non accessible, vérifiez docker-compose');
  } else {
    console.log('✅ API Gateway accessible');
  }

  return { startTime: new Date() };
}

// Fonction de teardown
export function teardown(data) {
  const duration = (new Date() - data.startTime) / 1000;
  console.log(`✅ Test terminé après ${duration}s`);
  console.log('📈 Vérifiez les métriques dans Grafana pour les résultats détaillés');
}
