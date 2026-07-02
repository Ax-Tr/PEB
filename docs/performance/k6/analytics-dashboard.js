// k6 load test: analytics dashboards (served from event-fed read-models, never OLTP).
// Run: BASE_URL=... TOKEN=... k6 run analytics-dashboard.js
// Verifies the < 3 s P95 dashboard SLO under peak load.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 1000 },
        { duration: '10m', target: 5000 }, // peak dashboards
        { duration: '3m', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{kind:analytics}': ['p(95)<3000'], // AC: dashboards < 3s
    http_req_failed: ['rate<0.005'],
  },
};

const params = {
  headers: { Authorization: `Bearer ${TOKEN}` },
  tags: { kind: 'analytics' },
};

export default function () {
  const y = 2026;
  const m = 5;
  http.get(`${BASE_URL}/api/v1/analytics/pnl?year=${y}&month=${m}`, params);
  http.get(`${BASE_URL}/api/v1/analytics/cashflow`, params);
  const r = http.get(`${BASE_URL}/api/v1/analytics/receivables-aging`, params);
  check(r, { 'status 200': (res) => res.status === 200 });
  // Freshness should stay FRESH under load (read-model lag within threshold).
  http.get(`${BASE_URL}/api/v1/analytics/freshness`, params);
  sleep(1);
}
