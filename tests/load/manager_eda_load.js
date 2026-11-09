// Target: k6 v2.3.0
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    // 1. Smoke test: minimal baseline verification
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '5s',
      gracefulStop: '2s',
    },
    // 2. Load test: concurrent Club Manager dashboard traffic
    manager_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 10 },  // Ramp-up
        { duration: '15s', target: 20 }, // Steady peak load
        { duration: '5s', target: 0 },   // Ramp-down
      ],
      gracefulRampDown: '2s',
      startTime: '6s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<600'], // p95 response time < 300ms, p99 < 600ms
    http_req_failed: ['rate<0.01'],                 // Error rate < 1%
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';
const MANAGER_TOKEN = __ENV.MANAGER_AUTH_TOKEN || 'mock-manager-jwt-token';

export default function () {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${MANAGER_TOKEN}`,
    },
    tags: { name: 'ManagerEdaOverview' },
  };

  // Test primary executive EDA endpoint with 90-day time window
  const res = http.get(`${BASE_URL}/api/manager-analytics/eda/overview?days=90`, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'latency is within SLA (< 500ms)': (r) => r.timings.duration < 500,
    'contains vetDelays': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.vetDelays !== undefined;
      } catch (e) {
        return false;
      }
    },
    'contains trainerEvaluation': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.trainerEvaluation !== undefined;
      } catch (e) {
        return false;
      }
    },
    'contains stallOccupancy': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.stallOccupancy !== undefined;
      } catch (e) {
        return false;
      }
    },
    'contains admissionFunnel': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.admissionFunnel !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  sleep(1);
}
