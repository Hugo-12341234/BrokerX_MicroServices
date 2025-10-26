import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Métriques personnalisées
const walletGetRate = new Rate('wallet_get_success');
const walletGetCounter = new Counter('wallet_get_total');
const walletGetTrend = new Trend('wallet_get_response_time');

// Configuration du test de charge
export const options = {
  scenarios: {
    wallet_get_ramp: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 200 },
        { duration: '30s', target: 400 },
        { duration: '30s', target: 600 },
        //{ duration: '30s', target: 800 },
        //{ duration: '30s', target: 1000 },
        //{ duration: '30s', target: 800 },
        { duration: '30s', target: 400 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<2000'],
    'wallet_get_success': ['rate>0.95'],
    'http_req_failed': ['rate<0.05'],
  },
};

const API_GATEWAY_URL = 'http://localhost:8079';
const WALLET_ENDPOINT = `${API_GATEWAY_URL}/api/v1/wallet`;
const USER_POOL_SIZE = 100;

function generateUserId() {
  return Math.floor(Math.random() * USER_POOL_SIZE) + 1;
}

export default function () {
  const userId = 1; // Utilise toujours le même userId pour maximiser l'utilisation du cache
  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': userId.toString(),
    'X-Internal-Call': 'true',
    'X-Request-Id': `wallet-req-${__VU}-${__ITER}`,
  };

  const res = http.get(WALLET_ENDPOINT, { headers });

  const success = check(res, {
    'wallet GET success': (r) => r.status === 200,
    'response time < 2s': (r) => r.timings.duration < 2000,
    'no server errors': (r) => r.status < 500,
  });

  walletGetRate.add(success);
  walletGetCounter.add(1);
  walletGetTrend.add(res.timings.duration);

  if (res.status >= 400) {
    console.log(`❌ Erreur ${res.status} pour userId ${userId}: ${res.body}`);
  }

  //sleep(Math.random() * 0.1); // Délai optionnel pour simuler un usage réel
}

export function setup() {
  console.log('🚀 Démarrage du test de charge wallet');
  console.log(`🎯 Endpoint ciblé: ${WALLET_ENDPOINT}`);
  return { startTime: new Date() };
}

export function teardown(data) {
  const duration = (new Date() - data.startTime) / 1000;
  console.log(`✅ Test terminé après ${duration}s`);
  console.log('📈 Vérifiez les métriques dans Grafana pour les résultats détaillés');
}
