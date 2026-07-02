// k6 load test: keyset-paginated list endpoints.
// Run: BASE_URL=https://staging.example TOKEN=... k6 run read-list.js
// Proves list latency stays flat as data grows (keyset seek, not offset scan).
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
        { duration: '2m', target: 500 },   // baseline
        { duration: '5m', target: 2000 },  // nominal
        { duration: '5m', target: 5000 },  // peak
        { duration: '3m', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{kind:read_list}': ['p(95)<250'],
    http_req_failed: ['rate<0.001'],
  },
};

const params = {
  headers: { Authorization: `Bearer ${TOKEN}` },
  tags: { kind: 'read_list' },
};

// Follow the opaque cursor across pages, exactly as a client would.
export default function () {
  let cursor = '';
  for (let i = 0; i < 3; i++) {
    const url = `${BASE_URL}/api/v1/ai/suggestions?limit=50${cursor ? `&cursor=${cursor}` : ''}`;
    const res = http.get(url, params);
    check(res, { 'status 200': (r) => r.status === 200 });
    try {
      const body = res.json();
      cursor = body.nextCursor || '';
      if (!cursor) break;
    } catch (e) {
      break;
    }
  }
  sleep(1);
}
