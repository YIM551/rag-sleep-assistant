import http from 'k6/http';
import { check, sleep } from 'k6';

// 2026 harness. Default is dry-run. A local URL alone does not prevent paid model calls.
const enabled = __ENV.ALLOW_LOCAL_LOAD === 'yes' && __ENV.LOCAL_BACKEND_USES_STUBS === 'yes';
const base = (__ENV.BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
if (!/^http:\/\/(127\.0\.0\.1|localhost)(:\d{1,5})?$/.test(base)) {
  throw new Error('BASE_URL must be a loopback HTTP origin, without a path.');
}
const users = Number(__ENV.VUS || '1');
const iterations = Number(__ENV.ITERATIONS || '3');
if (!Number.isInteger(users) || users < 1 || users > 3 ||
    !Number.isInteger(iterations) || iterations < 1 || iterations > 10) {
  throw new Error('Limits: VUS 1..3 and ITERATIONS 1..10.');
}
export const options = {
  scenarios: {
    local_stub: {
      executor: 'shared-iterations',
      vus: enabled ? users : 1,
      iterations: enabled ? iterations : 1,
      maxDuration: '60s',
      gracefulStop: '0s',
    },
  },
  summaryTrendStats: ['avg', 'p(95)', 'max'],
  thresholds: enabled ? { http_req_failed: ['rate==0'] } : {},
};

export default function () {
  if (!enabled) {
    console.log('DRY RUN: no HTTP request. Requires ALLOW_LOCAL_LOAD=yes and LOCAL_BACKEND_USES_STUBS=yes.');
    return;
  }
  const headers = { 'Content-Type': 'application/json' };
  if (__ENV.RAG_TEST_TOKEN) headers.Authorization = `Bearer ${__ENV.RAG_TEST_TOKEN}`;
  const res = http.post(`${base}/api/v1/rag/query`,
    JSON.stringify({ query: 'Synthetic sleep fixture question', topK: 3 }),
    { headers, timeout: '15s', redirects: 0, tags: { scenario: 'local-stub-contract' } });
  check(res, {
    'HTTP 200': (r) => r.status === 200,
    'answer contract': (r) => {
      try { const body = r.json(); return typeof body.message_detailed === 'string' && Array.isArray(body.citations); }
      catch (_) { return false; }
    },
  });
  sleep(0.25);
}
