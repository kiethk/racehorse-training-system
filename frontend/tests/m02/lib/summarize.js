#!/usr/bin/env node
// Convert Playwright's JSON report into the M02 agent result schema.
// Usage: node tests/m02/lib/summarize.js --agent A --run-id 202611131230 [--raw path] [--out path]
const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const get = (flag, fallback) => {
  const i = args.indexOf(flag);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
};
const agent = get('--agent', 'A').toUpperCase();
const runId = get('--run-id', process.env.RUN_ID || 'local');
const repo = path.resolve(__dirname, '../../../..');
const raw = get('--raw', path.join(repo, `docs/bmad/testing/evidence/relative/playwright-raw-${agent.toLowerCase()}.json`));
const out = get('--out', path.join(repo, `docs/bmad/testing/evidence/relative/agent-${agent.toLowerCase()}/agent-${agent.toLowerCase()}-results.json`));

const report = JSON.parse(fs.readFileSync(raw, 'utf8'));

function walkSuite(suite, acc) {
  for (const spec of suite.specs || []) acc.push(spec);
  for (const child of suite.suites || []) walkSuite(child, acc);
}
const specs = [];
for (const s of report.suites || []) walkSuite(s, specs);

function mapStatus(spec) {
  // A spec can contain multiple tests (retries/projects); use the final result.
  const tests = spec.tests || [];
  const statuses = tests.map((t) => {
    const results = t.results || [];
    const last = results[results.length - 1];
    return last ? last.status : t.status;
  });
  if (statuses.includes('failed') || statuses.includes('timedOut') || statuses.includes('interrupted')) return 'Fail';
  if (statuses.includes('skipped')) return 'Blocked';
  return 'Pass';
}

const cases = [];
for (const spec of specs) {
  const m = /(TRN_TC\d+)/.exec(spec.title || '');
  if (!m) continue;
  const caseId = m[1];
  const annotations = [];
  let error = '';
  for (const t of spec.tests || []) {
    for (const a of t.annotations || []) annotations.push(a);
    for (const r of t.results || []) {
      for (const a of r.attachments || []) annotations.push({ type: 'attachment', description: a.name, path: a.path });
      if (r.error && r.error.message) error = r.error.message.split('\n')[0];
    }
  }
  const evidence = annotations
    .filter((a) => a.type === 'evidence')
    .map((a) => a.description)
    .filter(Boolean);
  const status = mapStatus(spec);
  let notes = get('--notes', '');
  if (error) notes = error.slice(0, 300);
  if (!notes) notes = `relative values; run ${runId}`;
  cases.push({ case: caseId, status, evidence, notes });
}

cases.sort((a, b) => a.case.localeCompare(b.case));
const summary = {
  total: cases.length,
  pass: cases.filter((c) => c.status === 'Pass').length,
  fail: cases.filter((c) => c.status === 'Fail').length,
  blocked: cases.filter((c) => c.status === 'Blocked').length,
};
const payload = { agent, runId, cases, summary };
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(payload, null, 2));
console.log(`Wrote ${out}: ${JSON.stringify(summary)}`);
