// Experimento de concorrência no consumo de tokens de resgate do Benefix.
//
// Dispara um burst de VUs consumindo o MESMO token simultaneamente e classifica
// cada resposta por classe de status:
//   200 -> confirmado | 400 -> rejeitado (CAS/lock) | 409 -> conflito de unicidade
//   429 -> rate limit | demais -> inesperado
//
// Uso:
//   VUS=10 RUN_ID=lock-10-1 k6 run redemption-concurrency.js
//   (BASE_URL e SEED_FILE opcionais; padrão lê seed-result.json)
//
// Critério de sucesso por rodada: redemption_confirmed == VUS,
// redemption_rejected_consumed == 0 (sobra de VUs), 0 duplicações no verify.sql.

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { b64encode } from 'k6/encoding';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = Number(__ENV.VUS || 10);
const SEED_FILE = __ENV.SEED_FILE || 'seed-result.json';

const confirmed = new Counter('redemption_confirmed');
const rejectedConsumed = new Counter('redemption_rejected_consumed');
const uniqueConflict = new Counter('redemption_unique_conflict');
const rateLimited = new Counter('redemption_rate_limited');
const unexpected = new Counter('redemption_unexpected');
const confirmedDuration = new Trend('redemption_confirmed_duration', true);
const rejectedDuration = new Trend('redemption_rejected_duration', true);

export const options = {
  scenarios: {
    burst: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: VUS,
      maxDuration: '30s',
      gracefulStop: '0s',
    },
  },
  discardResponseBodies: false,
  // Sem thresholds: os resultados são analisados pelos contadores e pelo summary exportado.
};

// Leitura do seed no estágio init: k6 permite open() apenas no escopo global.
const seed = JSON.parse(open(SEED_FILE));

function jwtFromResponse(res) {
  const setCookie = res.headers['Set-Cookie'];
  const match = setCookie && setCookie.match(/jwt=([^;]+)/);
  if (!match) throw new Error('Cookie jwt ausente na resposta de login');
  return match[1];
}

export function setup() {
  const login = (email, password, what) => {
    const res = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(res, { [`${what}: login 200`]: (r) => r.status === 200 });
    if (res.status !== 200) throw new Error(`${what}: login falhou com status ${res.status}`);
    return jwtFromResponse(res);
  };

  const managerJwt = login(seed.provider.managerEmail, seed.provider.password, 'manager');
  const employeeJwt = login(seed.employee.email, seed.employee.password, 'employee');

  // Token fresco por rodada: emissão revoga tokens ativos anteriores do mesmo par
  // employee+benefit e o TTL é de 3 minutos.
  const issue = http.post(
    `${BASE_URL}/redemptions/benefits/${seed.benefitId}/token`,
    null,
    { headers: { Cookie: `jwt=${employeeJwt}` } },
  );
  check(issue, { 'issue token 201': (r) => r.status === 201 });
  if (issue.status !== 201) throw new Error(`Emissão do token falhou com status ${issue.status}: ${issue.body}`);
  const token = issue.json('token');
  console.log(`[setup] token bruto (use verify.sql com o hash): ${token}`);

  return { token, managerJwt, vus: VUS };
}

export default function (data) {
  const res = http.post(
    `${BASE_URL}/redemptions/provider/consume`,
    JSON.stringify({ token: data.token }),
    { headers: { 'Content-Type': 'application/json', Cookie: `jwt=${data.managerJwt}` } },
  );

  // O logger do k6 envolve console.log em logfmt com escapes múltiplos, o que
  // inviabiliza extrair JSON cru; base64 contorna isso (ver README, seção 6).
  console.log(
    'BN-RESP ' +
      b64encode(
        JSON.stringify({
          run: __ENV.RUN_ID || null,
          vu: exec.vu.idInTest,
          iter: exec.scenario.iterationInTest,
          status: res.status,
          durationMs: Math.round(res.timings.duration),
          body: res.body,
        }),
      ),
  );

  if (res.status === 200) {
    confirmed.add(1);
    confirmedDuration.add(res.timings.duration);
  } else if (res.status === 400) {
    rejectedConsumed.add(1);
    rejectedDuration.add(res.timings.duration);
  } else if (res.status === 409) uniqueConflict.add(1);
  else if (res.status === 429) rateLimited.add(1);
  else unexpected.add(1, { status: String(res.status) });
}

export function handleSummary(data) {
  const runId = __ENV.RUN_ID || `run-${VUS}vus-${Date.now()}`;
  const resultFile = __ENV.RESULT_FILE || `results/${runId}.json`;
  return {
    stdout: textSummary(data, runId),
    [resultFile]: JSON.stringify(data, null, 2),
  };
}

function textSummary(data, runId) {
  const metrics = data.metrics;
  const count = (m) => (metrics[m] ? metrics[m].values.count : 0);
  // Trend values no k6 v2 não expõem count; o n vem do contador da classe correspondente.
  const latency = (trend, counter) => {
    const values = metrics[trend] && metrics[trend].values;
    const n = count(counter);
    if (!values || n === 0) return 'n/a';
    return `${values.med.toFixed(1)} / ${values['p(95)'].toFixed(1)} (n=${n})`;
  };
  const overall = metrics.http_req_duration && metrics.http_req_duration.values;
  return [
    `=== ${runId} (VUs=${VUS}) ===`,
    `confirmados (200):         ${count('redemption_confirmed')}`,
    `rejeitados CAS/lock (400): ${count('redemption_rejected_consumed')}`,
    `conflito unicidade (409):  ${count('redemption_unique_conflict')}`,
    `rate limit (429):          ${count('redemption_rate_limited')}`,
    `inesperados:               ${count('redemption_unexpected')}`,
    `latência confirmados p50/p95 (ms): ${latency('redemption_confirmed_duration', 'redemption_confirmed')}`,
    `latência rejeitados  p50/p95 (ms): ${latency('redemption_rejected_duration', 'redemption_rejected_consumed')}`,
    `latência geral       p50/p95 (ms): ${
      overall ? `${overall.med.toFixed(1)} / ${overall['p(95)'].toFixed(1)}` : 'n/a'
    }`,
    '',
  ].join('\n');
}
