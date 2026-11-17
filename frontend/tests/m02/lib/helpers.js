// Shared helpers for M02 relative UI tests. UI-only: never call the API or the DB from a spec.
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '../../../..');
const EVIDENCE_ROOT = path.join(REPO, 'docs/bmad/testing/evidence/relative');

// Business rules mirrored from the UI's training-rules payload. Case steps refer to
// these tokens instead of magic numbers; if the app rules change, change this object.
const RULES = {
  exerciseDurationMin: 15,
  exerciseDurationMax: 240,
  exerciseDistanceMin: 100,
  exerciseDistanceMax: 10000,
  courseSessionsMin: 1,
  courseSessionsMax: 60,
  courseExercisesMax: 30,
  groupCapacity: 6,
  groupNameMax: 100,
  feedbackMax: 2000,
  hrMin: 30,
  hrMax: 240,
  monthsMin: 0,
  monthsMax: 60,
  fileMaxMB: 10,
  pageSize: 10,
  courseRunHorizonDays: 365,
  quarterHourMinutes: 15,
  trainingWindowStart: '06:00',
  trainingWindowEnd: '10:00',
  readinessScoreMin: 0,
  readinessScoreMax: 10,
  performanceWindowDays: 30,
  trendWeeks: 12,
};

const RUN_ID = process.env.RUN_ID || 'local';

const iso = (d) => {
  const x = new Date(d);
  const y = x.getFullYear();
  const m = String(x.getMonth() + 1).padStart(2, '0');
  const day = String(x.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};
const addDays = (n, from = new Date()) => {
  const d = new Date(from);
  d.setDate(d.getDate() + n);
  return d;
};
const today = () => new Date();
const todayIso = () => iso(today());
const yesterdayIso = () => iso(addDays(-1));
const tomorrowIso = () => iso(addDays(1));
const plusDaysIso = (n) => iso(addDays(n));

function evidenceDir(agent) {
  const dir = path.join(EVIDENCE_ROOT, `agent-${agent.toLowerCase()}`);
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

async function shot(page, agent, caseId, label) {
  const dir = evidenceDir(agent);
  const file = `${caseId}_${label}.png`;
  await page.screenshot({ path: path.join(dir, file), fullPage: true });
  return file;
}

/**
 * Screenshot + attach + annotate in one call so the JSON summariser can prove
 * that every Pass has a real image file.
 */
async function capture(page, testInfo, agent, caseId, label) {
  const file = await shot(page, agent, caseId, label);
  const full = path.join(evidenceDir(agent), file);
  await testInfo.attach(file, { path: full, contentType: 'image/png' });
  testInfo.annotations.push({ type: 'evidence', description: file });
  return file;
}

/**
 * Log in through the real login form. Every spec starts from a clean storage state.
 */
async function login(page, email, password = 'admin123') {
  await page.goto('/login', { waitUntil: 'domcontentloaded' });
  await page.locator('#login-email').waitFor({ state: 'visible' });
  await page.fill('#login-email', email);
  await page.fill('#login-password', password);
  await page.click('#login-submit');
  await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 30_000 });
  await page.waitForLoadState('domcontentloaded');
}

async function logout(page) {
  // The UI exposes the account menu in the header. Clear cookies as a fallback so
  // the next case always starts unauthenticated; no backend/API call is made.
  const ctx = page.context();
  await ctx.clearCookies();
  try {
    await page.evaluate(() => {
      window.localStorage.clear();
      window.sessionStorage.clear();
    });
  } catch {
    // page may be closed
  }
}

async function goto(page, urlPath) {
  await page.goto(urlPath, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(800);
}

/** Independent browser session (own cookies) for role/security checks. */
async function newSession(browser, email, password = 'admin123') {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000' });
  const page = await context.newPage();
  if (email) await login(page, email, password);
  return { context, page };
}

async function bodyText(page) {
  return page.locator('body').innerText();
}

async function expectVisible(locator, label) {
  await locator.first().waitFor({ state: 'visible', timeout: 15_000 });
  return locator.first();
}

async function noHorizontalScroll(page) {
  return page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1);
}

async function expectToast(page, pattern) {
  const toast = page.locator('[data-sonner-toast], [role="status"], .toast').filter({ hasText: pattern }).first();
  await toast.waitFor({ state: 'visible', timeout: 15_000 });
  return toast;
}

function uniqueName(caseId, suffix = '') {
  const s = suffix ? `_${suffix}` : '';
  return `M02_${caseId}_${RUN_ID}${s}`.slice(0, RULES.groupNameMax);
}

/** Open the native <select> by label and choose an option by visible text. */
async function selectByLabel(page, label, optionText) {
  const select = page.getByLabel(label, { exact: false }).first();
  await select.selectOption({ label: optionText });
}

/** Read all table rows' innerText in the first visible table. */
async function tableRows(page) {
  return page.locator('table tbody tr').evaluateAll((rows) => rows.map((r) => r.innerText));
}

module.exports = {
  RULES,
  RUN_ID,
  REPO,
  EVIDENCE_ROOT,
  iso,
  addDays,
  today,
  todayIso,
  yesterdayIso,
  tomorrowIso,
  plusDaysIso,
  evidenceDir,
  shot,
  capture,
  login,
  logout,
  newSession,
  goto,
  bodyText,
  expectVisible,
  noHorizontalScroll,
  expectToast,
  uniqueName,
  selectByLabel,
  tableRows,
};
