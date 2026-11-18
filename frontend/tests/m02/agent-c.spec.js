// M02 relative UI system test — Agent C
// Scope: TRN_TC49–TRN_TC67 (Admissions, Dashboard, Incidents, Security).
// UI-only: this spec never calls the API or the database. Values are read from the
// live screens, from RULES, or from the fixture IDs in /tmp/m02-fixtures.env.
//
// Execution order is important for the shared database: TC56/TC57 run before any
// incident is created, then admissions, horses, incidents and security. Playwright
// runs the tests in file order with one worker; no serial mode is configured, so a
// known fail does not skip the later cases.
const { test, expect } = require('@playwright/test');
const {
  RULES,
  RUN_ID,
  login,
  goto,
  capture,
  newSession,
  uniqueName,
  expectToast,
} = require('./lib/helpers');

const TRAINER1 = 'trainer1@rtms.com';
const TRAINER2 = 'trainer2@rtms.com';
const OWNER = 'owner@rtms.com';
const GROOM = 'groom1@rtms.com';
const PASSWORD = 'admin123';
const PENDING_TAB = /Pending evaluation/;
const REVIEWED_TAB = /Evaluated by you/;

// ────────────────────────────── admissions helpers ──────────────────────────────

function admissionsTable(page) {
  return page.getByRole('table', { name: 'Admission records' });
}

function admissionRows(page) {
  return admissionsTable(page).locator('tbody tr');
}

function cleanHeader(text) {
  return text.replace(/[↕↑↓]/g, '').split('\n')[0].trim();
}

function lastLine(text) {
  return text.split('\n').map((line) => line.trim()).filter(Boolean).pop();
}

async function loginAs(page, email, landing) {
  await login(page, email);
  await page.waitForURL((url) => url.pathname === landing, { timeout: 30_000 });
  await page.waitForLoadState('domcontentloaded');
}

/**
 * Full page loads re-run the auth restore. Another browser (for example another
 * agent on the shared database) may log in as the same user and revoke our refresh
 * token in the meantime, so a navigation can land on /login. Sign in again and retry
 * the same route once instead of failing the case for an environment race.
 */
async function gotoAuthed(page, path, email = TRAINER1, landing = '/trainer') {
  await goto(page, path);
  if (new URL(page.url()).pathname === '/login') {
    await loginAs(page, email, landing);
    await goto(page, path);
  }
}

async function openAdmissions(page) {
  await gotoAuthed(page, '/trainer/admissions');
  await expect(page.getByRole('heading', { name: 'Admissions', exact: true })).toBeVisible();
  await expect(admissionsTable(page)).toBeVisible();
  await expect(page.getByRole('radio', { name: PENDING_TAB })).toBeVisible();
}

async function tabCount(page, name) {
  const chip = page.getByRole('radio', { name });
  const text = await chip.innerText();
  const match = text.match(/(\d+)\s*$/);
  return match ? Number(match[1]) : Number.NaN;
}

async function rowAdmissionId(row) {
  const href = await row.getByRole('link').first().getAttribute('href');
  return Number(href.split('/').filter(Boolean).pop().split('?')[0]);
}

async function freshPendingAdmission(page, { excludeIds = [], excludeNames = [] } = {}) {
  await openAdmissions(page);
  if ((await paginationTotal(page)) === 0) return null;
  const rows = admissionRows(page);
  for (let index = 0; index < (await rows.count()); index += 1) {
    const row = rows.nth(index);
    const id = await rowAdmissionId(row);
    const name = (await row.locator('td').first().locator('.block.truncate.font-semibold').first().innerText()).trim();
    if (!excludeIds.map(String).includes(String(id)) && !excludeNames.includes(name)) {
      return { href: await row.getByRole('link').first().getAttribute('href'), name };
    }
  }
  return null;
}

async function expectNoAssessment(page) {
  await expect(page.getByText('Head Trainer evaluation (submitted)', { exact: true })).toHaveCount(0);
  await expect(page.getByText('Racing readiness assessment', { exact: true })).toBeVisible();
}

async function chooseScore(page, label, score) {
  const group = page.getByRole('radiogroup', { name: new RegExp(label, 'i') });
  await expect(group, `${label} score scale should be visible`).toBeVisible();
  await group.getByRole('radio', { name: String(score), exact: true }).click();
}

async function fillReadinessForm(page, { status = 'Ready for Racing', scores = [7, 8, 9], months, remarks } = {}) {
  await page.getByRole('radio', { name: status }).check();
  await chooseScore(page, 'Conformation', scores[0]);
  await chooseScore(page, 'Temperament', scores[1]);
  await chooseScore(page, 'Gait quality', scores[2]);
  if (months !== undefined) {
    await page.getByLabel('Months until race-ready').fill(String(months));
  }
  if (remarks) {
    await page.getByLabel('Remarks').fill(remarks);
  }
}

async function submitEvaluation(page) {
  await page.getByRole('button', { name: 'Submit evaluation' }).click();
}

async function expectManagerReviewCurrent(page) {
  const managerStep = page.getByText('Manager Review', { exact: true }).first();
  await expect(managerStep).toBeVisible();
  const classes = await managerStep.getAttribute('class');
  expect(classes, 'Manager Review should be the current pipeline step').toContain('text-[var(--color-primary)]');
  expect(classes, 'Manager Review should be the emphasised current step').toContain('font-bold');
}

async function expectReviewedQueueContains(page, admissionId) {
  await openAdmissions(page);
  await page.getByRole('radio', { name: REVIEWED_TAB }).click();
  await expect(page.getByRole('radio', { name: REVIEWED_TAB })).toHaveAttribute('aria-checked', 'true');
  const link = admissionsTable(page).locator(`a[href^="/trainer/admissions/${admissionId}"]`);
  // The evaluated queue is paginated and sorted by submission date, so the row
  // can be on a later page when the seed already has many reviewed admissions.
  for (let pageIndex = 0; pageIndex < 20; pageIndex += 1) {
    if (await link.count()) {
      await expect(link.first()).toBeVisible();
      return;
    }
    const next = pagination(page).getByRole('button', { name: 'Next', exact: true });
    if (await next.isDisabled()) break;
    await next.click();
    await page.waitForTimeout(600);
  }
  await expect(link.first(), `admission #${admissionId} should be in the evaluated queue`).toBeVisible();
}

// ────────────────────────────── dashboard helpers ───────────────────────────────

async function openDashboard(page, path = '/trainer') {
  await gotoAuthed(page, path);
  await expect(page.getByRole('heading', { name: 'Fitness and training overview' })).toBeVisible();
  await expect(page.getByText(/TOTAL HORSES/i).first()).toBeVisible();
  await expect(page.getByRole('table', { name: 'Assigned horses' })).toBeVisible();
  await expect(page.locator('svg[role="img"][aria-label="Performance rating per week"]')).toBeVisible({ timeout: 25_000 });
}

function kpiText(body, label) {
  const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = body.match(new RegExp(`${escaped}\\s*\\n\\s*([^\\n]+)`, 'i'));
  return match ? match[1].trim() : null;
}

function readKpis(body) {
  return {
    total: Number(kpiText(body, 'TOTAL HORSES')),
    inTraining: Number(kpiText(body, 'IN TRAINING')),
    alerts: Number(kpiText(body, 'HAVE RISK ALERTS')),
    average: kpiText(body, 'AVG PERFORMANCE (30 DAYS)'),
  };
}

function dashboardTable(page) {
  return page.getByRole('table', { name: 'Assigned horses' });
}

function pagination(page) {
  return page.getByRole('navigation', { name: 'Pagination' });
}

async function paginationTotal(page) {
  const text = await pagination(page).innerText();
  const match = text.match(/of\s+(\d+)\s+results/i);
  return match ? Number(match[1]) : Number.NaN;
}

async function chipCount(page, filterName, label) {
  const chip = page.getByRole('radiogroup', { name: filterName }).getByRole('radio', { name: label });
  const text = await chip.innerText();
  const match = text.match(/(\d+)\s*$/);
  return match ? Number(match[1]) : Number.NaN;
}

async function collectDashboardRows(page) {
  const rows = [];
  for (;;) {
    rows.push(...(await dashboardTable(page).locator('tbody tr').allInnerTexts()));
    const next = pagination(page).getByRole('button', { name: 'Next', exact: true });
    if (await next.isDisabled()) break;
    await next.click();
    await page.waitForTimeout(250);
  }
  return rows;
}

function performanceChart(page) {
  return page.locator('svg[role="img"][aria-label="Performance rating per week"]');
}

async function chartDateLabels(page) {
  return performanceChart(page)
    .locator('text')
    .evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').trim()).filter((text) => /^\d{2}\/\d{2}$/.test(text)));
}

function distributionCounts(body) {
  const grab = (pattern) => {
    const match = body.match(pattern);
    return match ? Number(match[1]) : Number.NaN;
  };
  return [
    grab(/Excellent \(≥8\.0\):\s*(\d+)/),
    grab(/On target \(6\.0–7\.9\):\s*(\d+)/),
    grab(/Needs improvement \(<6\.0\):\s*(\d+)/),
    grab(/No data:\s*(\d+)/),
  ];
}

// ─────────────────────────────── horses helpers ────────────────────────────────

function horsesTable(page) {
  return page.getByRole('table', { name: 'Horses' });
}

async function openHorses(page, email = TRAINER1, landing = '/trainer') {
  await gotoAuthed(page, '/trainer/horses', email, landing);
  await expect(page.getByRole('heading', { name: 'Horses', exact: true })).toBeVisible();
  await expect(horsesTable(page)).toBeVisible();
}

async function firstHorseId(page) {
  const href = await horsesTable(page).locator('tbody tr').first().getByRole('link').first().getAttribute('href');
  return Number(href.split('/').filter(Boolean).pop());
}

async function openHorse(page, horseId) {
  await gotoAuthed(page, `/trainer/horses/${horseId}`);
  await expect(page.getByRole('tab', { name: 'Overview' })).toBeVisible({ timeout: 20_000 });
}

async function clickHorseTab(page, label) {
  await page.getByRole('tab', { name: new RegExp(label) }).click();
  await page.waitForTimeout(1000);
}

let cachedTrainerHorseId = null;

async function discoverTrainerHorseId(browser) {
  if (cachedTrainerHorseId !== null) return cachedTrainerHorseId;
  const { context, page } = await newSession(browser, TRAINER1, PASSWORD);
  try {
    await page.waitForURL((url) => url.pathname === '/trainer', { timeout: 30_000 });
    await openHorses(page);
    cachedTrainerHorseId = await firstHorseId(page);
    return cachedTrainerHorseId;
  } finally {
    await context.close();
  }
}

// ────────────────────────────── incidents helpers ──────────────────────────────

function incidentsTable(page) {
  return page.getByRole('table', { name: 'Incident reports' });
}

async function openIncidents(page) {
  await gotoAuthed(page, '/trainer/incidents');
  await expect(page.getByRole('heading', { name: 'Incident Reports', exact: true })).toBeVisible();
  await expect(incidentsTable(page)).toBeVisible();
}

async function closeIncidentModal(page) {
  const modal = page.getByRole('dialog');
  await modal.locator('footer').getByRole('button', { name: 'Close' }).click();
  await expect(modal).toBeHidden();
}

// ────────────────────────────────── tests ──────────────────────────────────────
// Dashboard first: creating an incident later changes the alert count.

test('TRN_TC56 trainer dashboard KPIs, performance chart, correlation and horse table', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openDashboard(page);

  const body = await page.locator('body').innerText();
  const kpis = readKpis(body);
  expect(kpis.total).toBeGreaterThan(0);
  expect(kpis.average).toMatch(/^[\d.]+\/10$/);

  const tableTotal = await paginationTotal(page);
  expect(kpis.total, 'Total horses KPI should match the table total').toBe(tableTotal);

  // Four KPI cards are present with the expected labels.
  expect(Number.isFinite(kpis.total)).toBeTruthy();
  expect(Number.isFinite(kpis.inTraining)).toBeTruthy();
  expect(Number.isFinite(kpis.alerts)).toBeTruthy();
  expect(kpis.average).toMatch(/^[\d.]+\/10$/);
  for (const label of ['TOTAL HORSES', 'IN TRAINING', 'HAVE RISK ALERTS', 'AVG PERFORMANCE \\(30 DAYS\\)']) {
    await expect(page.getByText(new RegExp(label, 'i')).first()).toBeVisible();
  }

  // Performance chart: 0–10 axis, one bar per week and a 12-week range.
  const chart = performanceChart(page);
  await expect(chart).toBeVisible();
  const chartTexts = await chart.locator('text').evaluateAll((nodes) => nodes.map((node) => (node.textContent || '').trim()));
  const axisTicks = Array.from(
    { length: 6 },
    (_, index) => String(RULES.readinessScoreMin + index * ((RULES.readinessScoreMax - RULES.readinessScoreMin) / 5)),
  );
  for (const tick of axisTicks) {
    expect(chartTexts, `Performance chart should show the ${tick} rating tick`).toContain(tick);
  }
  const dates = await chartDateLabels(page);
  expect(dates.length, 'Performance chart should cover the 12-week trend window').toBe(RULES.trendWeeks);

  // Correlation card and the rating distribution.
  await expect(page.getByText('Cardio Fitness & Speed Correlation', { exact: true })).toBeVisible({ timeout: 25_000 });
  await expect(page.getByText('Fleet Bivariate Distribution', { exact: true })).toBeVisible();
  await expect(page.getByText(/Sample Size:/).first()).toBeVisible();
  const distribution = distributionCounts(body);
  expect(distribution.every((value) => Number.isFinite(value))).toBeTruthy();
  expect(distribution.reduce((sum, value) => sum + value, 0), 'distribution should cover every horse').toBe(kpis.total);

  // Horse table and filter chips.
  await expect(dashboardTable(page)).toBeVisible();
  expect(await chipCount(page, 'Horse filter', /All/)).toBe(kpis.total);
  expect(await chipCount(page, 'Horse filter', /In training/)).toBe(kpis.inTraining);
  expect(await chipCount(page, 'Horse filter', /With alerts/)).toBe(kpis.alerts);

  await capture(page, testInfo, 'c', 'TRN_TC56', 'dashboard-kpis-chart');

  // The legacy /trainer/dashboard alias renders the same dashboard.
  await openDashboard(page, '/trainer/dashboard');
  await expect(page.getByText(/TOTAL HORSES/i).first()).toBeVisible();
  await expect(dashboardTable(page)).toBeVisible();
});

test('TRN_TC57 dashboard counts, filters and weekly trend match assigned horses', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openDashboard(page);

  const body = await page.locator('body').innerText();
  const kpis = readKpis(body);

  // Average KPI must equal the mean of the 30-day averages shown in the table.
  const allRows = await collectDashboardRows(page);
  const ratings = allRows
    .map((row) => row.match(/30-day avg\s+([\d.]+)\/10/))
    .filter(Boolean)
    .map((match) => Number(match[1]));
  expect(ratings.length).toBeGreaterThan(0);
  const expectedAverage = (ratings.reduce((sum, value) => sum + value, 0) / ratings.length).toFixed(1);
  expect(
    Math.abs(Number.parseFloat(kpis.average) - Number(expectedAverage)),
    'Avg performance KPI should be the mean of the table 30-day averages',
  ).toBeLessThanOrEqual(0.1);

  // Back to page 1 so the chip filters are visible without a stale page index.
  await openDashboard(page);

  await expect(page.getByRole('radiogroup', { name: 'Horse filter' })).toBeVisible();
  expect(await chipCount(page, 'Horse filter', /All/)).toBe(kpis.total);
  expect(await chipCount(page, 'Horse filter', /In training/)).toBe(kpis.inTraining);
  expect(await chipCount(page, 'Horse filter', /With alerts/)).toBe(kpis.alerts);

  // Hovering a Performance bar shows the weekly tooltip.
  const chart = performanceChart(page);
  await chart.locator('rect[fill="transparent"]').first().hover();
  const tooltipTitle = page.getByText(/Week of /).first();
  await expect(tooltipTitle).toBeVisible();
  await expect(tooltipTitle.locator('xpath=..')).toContainText(/Avg rating/);

  // Chips narrow the table but leave the KPI cards untouched.
  const suit = page.getByRole('radiogroup', { name: 'Horse filter' });
  await suit.getByRole('radio', { name: /In training/ }).click();
  await page.waitForTimeout(400);
  expect(await paginationTotal(page)).toBe(kpis.inTraining);
  expect(readKpis(await page.locator('body').innerText())).toEqual(kpis);

  await suit.getByRole('radio', { name: /With alerts/ }).click();
  await page.waitForTimeout(400);
  expect(await paginationTotal(page)).toBe(kpis.alerts);
  expect(readKpis(await page.locator('body').innerText())).toEqual(kpis);
  await capture(page, testInfo, 'c', 'TRN_TC57', 'alert-filter-kpis');

  await suit.getByRole('radio', { name: /All/ }).click();
  await page.waitForTimeout(400);
  expect(await paginationTotal(page)).toBe(kpis.total);
  expect(readKpis(await page.locator('body').innerText())).toEqual(kpis);

  // Performance distribution and the 12-week chart range.
  const distribution = distributionCounts(await page.locator('body').innerText());
  expect(distribution.reduce((sum, value) => sum + value, 0)).toBe(kpis.total);
  expect((await chartDateLabels(page)).length).toBe(RULES.trendWeeks);
});

// ─────────────────────────────── admissions ───────────────────────────────────

test('TRN_TC49 trainer admissions queue tabs, filters, columns and row actions', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openAdmissions(page);

  const pendingCount = await tabCount(page, PENDING_TAB);
  const reviewedCount = await tabCount(page, REVIEWED_TAB);
  expect(pendingCount).toBeGreaterThan(0);

  const pendingHeaders = (await admissionsTable(page).locator('thead th').allInnerTexts()).map(cleanHeader);
  expect(pendingHeaders).toEqual(['Horse', 'Status', 'Quarantine stall', 'Submitted', 'Action']);

  const rows = admissionRows(page);
  expect(await rows.count()).toBe(Math.min(pendingCount, RULES.pageSize));
  const firstRowText = await rows.first().innerText();
  expect(firstRowText).toContain('In Progress');
  expect(firstRowText).toMatch(/Stall [A-Z0-9-]+|Unassigned/);
  await expect(rows.first().getByRole('link', { name: 'Evaluate' })).toBeVisible();

  // Filters only apply on Apply.
  const horseName = (await admissionRows(page).first().locator('td').first().locator('.block.truncate.font-semibold').first().innerText()).trim();
  const search = page.getByLabel('Search horse name');
  await search.fill(horseName);
  await page.waitForTimeout(300);
  expect(await admissionRows(page).count(), 'typing should not filter before Apply').toBe(Math.min(pendingCount, RULES.pageSize));
  await page.getByRole('button', { name: 'Apply', exact: true }).click();
  await page.waitForTimeout(600);
  expect(await admissionRows(page).count()).toBe(1);
  expect(await admissionRows(page).first().innerText()).toContain(horseName);
  expect(await pagination(page).innerText()).toMatch(/of\s+1\s+results/);

  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await page.waitForTimeout(600);
  expect(await admissionRows(page).count()).toBe(Math.min(pendingCount, RULES.pageSize));

  // Evaluated tab adds the Evaluated column and a View action.
  await page.getByRole('radio', { name: REVIEWED_TAB }).click();
  await expect(page.getByRole('radio', { name: REVIEWED_TAB })).toHaveAttribute('aria-checked', 'true');
  const reviewedHeaders = (await admissionsTable(page).locator('thead th').allInnerTexts()).map(cleanHeader);
  expect(reviewedHeaders).toEqual(['Horse', 'Status', 'Quarantine stall', 'Evaluated', 'Submitted', 'Action']);
  expect(reviewedCount).toBeGreaterThan(0);
  expect(await admissionRows(page).count()).toBe(Math.min(reviewedCount, RULES.pageSize));
  await expect(admissionRows(page).first().getByRole('link', { name: 'View' })).toBeVisible();

  await page.getByRole('radio', { name: PENDING_TAB }).click();
  await page.waitForTimeout(400);
  await capture(page, testInfo, 'c', 'TRN_TC49', 'admissions-queue');
});

test('TRN_TC50 trainer candidate dossier: header, pipeline, tabs and readiness form', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openAdmissions(page);

  const row = admissionRows(page).first();
  const admissionId = await rowAdmissionId(row);
  await row.getByRole('link', { name: 'Evaluate' }).click();
  await expect(page.getByText('Review pipeline', { exact: true })).toBeVisible();
  await expect(page).toHaveURL(new RegExp(`/trainer/admissions/${admissionId}`));

  await expect(page.getByText('Back to admissions', { exact: true })).toBeVisible();
  await expect(page.getByText('In Progress', { exact: true }).first()).toBeVisible();
  await expect(page.getByText(`#${admissionId}`, { exact: true })).toBeVisible();
  await expect(page.getByText(/admission id/i).first()).toBeVisible();

  // Six-step pipeline.
  for (const step of ['Submitted', 'Groom Review', 'Arrival', 'Vet Review', 'Trainer Review', 'Manager Review']) {
    await expect(page.getByText(step, { exact: true }).first()).toBeVisible();
  }

  // Four populated tabs.
  for (const tab of ['Overview', 'Documents', 'Medical Findings', 'Review History']) {
    await expect(page.getByRole('tab', { name: new RegExp(tab) })).toBeVisible();
  }
  await expect(page.getByText('Pedigree & Housing', { exact: true })).toBeVisible();
  await clickHorseTab(page, 'Documents');
  await expect(page.getByText('Documents', { exact: true }).last()).toBeVisible();
  await expect(page.getByText(/\d+ files/)).toBeVisible();
  await clickHorseTab(page, 'Medical Findings');
  await expect(page.getByText('Veterinary Examination & Vitals', { exact: true })).toBeVisible();
  await clickHorseTab(page, 'Review History');
  await expect(page.getByText('Review History', { exact: true }).last()).toBeVisible();

  // Readiness form.
  await expect(page.getByText('Racing readiness assessment', { exact: true })).toBeVisible();
  for (const option of ['Ready for Racing', 'Needs More Training', 'Unsuitable']) {
    await expect(page.getByRole('radio', { name: option })).toBeVisible();
  }
  for (const group of ['Conformation', 'Temperament', 'Gait quality']) {
    await expect(page.getByRole('radiogroup', { name: new RegExp(group, 'i') })).toBeVisible();
  }
  await expect(page.getByLabel('Months until race-ready')).toBeVisible();
  await expect(page.getByLabel('Remarks')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Submit evaluation' })).toBeVisible();

  await capture(page, testInfo, 'c', 'TRN_TC50', 'candidate-dossier');
});

test('TRN_TC51 READY evaluation saves read-only and advances to Manager Review', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  const validationId = process.env.M02_VALIDATION_ID;
  expect(validationId, 'M02_VALIDATION_ID must be exported from /tmp/m02-fixtures.env').toBeTruthy();

  // Discover any fresh TRAINER_REVIEW admission assigned to trainer1, keeping the
  // validation fixture (TC53/TC54) untouched.
  const target = await freshPendingAdmission(page, {
    excludeIds: [validationId],
    excludeNames: ['M02_ADM_VALIDATION'],
  });
  expect(target, 'a fresh TRAINER_REVIEW admission assigned to trainer1 should exist').toBeTruthy();
  const admissionId = Number(target.href.split('/').filter(Boolean).pop().split('?')[0]);
  await gotoAuthed(page, target.href);
  await expect(page.getByText('Review pipeline', { exact: true })).toBeVisible();
  await expectNoAssessment(page);

  const remarks = uniqueName('TRN_TC51', 'ready remarks');
  const readyScores = [
    RULES.readinessScoreMax - 3,
    RULES.readinessScoreMax - 2,
    RULES.readinessScoreMax - 1,
  ];
  const readOnly = page.getByText('Head Trainer evaluation (submitted)', { exact: true });
  await fillReadinessForm(page, {
    status: 'Ready for Racing',
    scores: readyScores,
    months: Math.min(6, RULES.monthsMax),
    remarks,
  });
  await submitEvaluation(page);

  await expect(readOnly).toBeVisible({ timeout: 25_000 });
  const readOnlyText = await readOnly.locator('xpath=..').innerText();
  expect(readOnlyText).toContain('READY');
  expect(readOnlyText).toContain(`${readyScores[0]}/10`);
  expect(readOnlyText).toContain(`${readyScores[1]}/10`);
  expect(readOnlyText).toContain(`${readyScores[2]}/10`);
  expect(readOnlyText).toContain(`${Math.min(6, RULES.monthsMax)} months`);
  expect(readOnlyText).toContain(remarks);

  // No Approve/Reject control: the Club Manager owns the final decision.
  await expect(page.getByRole('button', { name: /Approve|Reject/i })).toHaveCount(0);
  await expectManagerReviewCurrent(page);
  await expect(page.getByText('Rejected', { exact: true })).toHaveCount(0);

  await capture(page, testInfo, 'c', 'TRN_TC51', 'ready-readonly-manager-review');
  await expectReviewedQueueContains(page, admissionId);
});

test('TRN_TC52 UNSUITABLE evaluation keeps final decision with Club Manager', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  const validationId = process.env.M02_VALIDATION_ID;
  expect(validationId, 'M02_VALIDATION_ID must be exported from /tmp/m02-fixtures.env').toBeTruthy();

  // Discover the next fresh TRAINER_REVIEW admission assigned to trainer1.
  const target = await freshPendingAdmission(page, {
    excludeIds: [validationId],
    excludeNames: ['M02_ADM_VALIDATION'],
  });
  expect(target, 'a fresh TRAINER_REVIEW admission assigned to trainer1 should exist').toBeTruthy();
  const admissionId = Number(target.href.split('/').filter(Boolean).pop().split('?')[0]);
  await gotoAuthed(page, target.href);
  await expect(page.getByText('Review pipeline', { exact: true })).toBeVisible();
  await expectNoAssessment(page);

  const remarks = uniqueName('TRN_TC52', 'unsuitable feedback');
  const lowScores = [
    RULES.readinessScoreMin + 2,
    RULES.readinessScoreMin + 3,
    RULES.readinessScoreMin + 1,
  ];
  const readOnly = page.getByText('Head Trainer evaluation (submitted)', { exact: true });
  await fillReadinessForm(page, {
    status: 'Unsuitable',
    scores: lowScores,
    months: undefined, // intentionally blank
    remarks,
  });
  await submitEvaluation(page);

  await expect(readOnly).toBeVisible({ timeout: 25_000 });
  const readOnlyText = await readOnly.locator('xpath=..').innerText();
  expect(readOnlyText).toContain('UNSUITABLE');
  expect(readOnlyText).toContain(`${lowScores[0]}/10`);
  expect(readOnlyText).toContain(`${lowScores[1]}/10`);
  expect(readOnlyText).toContain(`${lowScores[2]}/10`);
  expect(readOnlyText).toContain(remarks);

  // In Progress, Manager Review current, not Rejected.
  await expect(page.getByText('In Progress', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Rejected', { exact: true })).toHaveCount(0);
  await expectManagerReviewCurrent(page);

  // History stores the trainer feedback.
  await clickHorseTab(page, 'Review History');
  await expect(page.getByRole('heading', { name: 'Head Trainer', exact: true })).toBeVisible();
  await expect(page.getByText(remarks, { exact: true }).first()).toBeVisible();

  await capture(page, testInfo, 'c', 'TRN_TC52', 'unsuitable-readonly-history');
  await expectReviewedQueueContains(page, admissionId);
});

test('TRN_TC53 missing readiness scores show one form alert instead of three field errors', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  const validationId = process.env.M02_VALIDATION_ID;
  expect(validationId, 'M02_VALIDATION_ID must be exported from /tmp/m02-fixtures.env').toBeTruthy();

  await gotoAuthed(page, `/trainer/admissions/${validationId}`);
  await expect(page.getByText('Review pipeline', { exact: true })).toBeVisible();
  await expectNoAssessment(page);

  // Submit with no scores.
  await submitEvaluation(page);
  const alert = page.getByRole('alert');
  await expect(alert.first()).toBeVisible();
  const alertText = await alert.first().innerText();
  expect(alertText).toMatch(/Conformation score/i);

  // The admission stays in TRAINER_REVIEW and the form stays editable.
  await expect(page.getByRole('button', { name: 'Submit evaluation' })).toBeEnabled();
  await expect(page.getByText('Evaluation submitted to the Club Manager.').first()).toHaveCount(0);

  await capture(page, testInfo, 'c', 'TRN_TC53', 'validation-alert');

  // Expected: three inline field errors naming each missing score.
  // Actual (UI-D02): a single form-level alert that names only the first missing score.
  const inlineErrors = await page.locator('p[role="alert"]').count();
  expect(
    inlineErrors,
    `UI-D02: expected three inline field errors naming Conformation, Temperament and Gait quality; actual form alert is "${alertText}" and inline field errors = ${inlineErrors}`,
  ).toBe(3);
});

test('TRN_TC54 months until race-ready accepts 60 and rejects 61', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  const validationId = process.env.M02_VALIDATION_ID;
  expect(validationId, 'M02_VALIDATION_ID must be exported from /tmp/m02-fixtures.env').toBeTruthy();

  // TC53 intentionally leaves this admission pending, so it is fresh here.
  await gotoAuthed(page, `/trainer/admissions/${validationId}`);
  await expect(page.getByText('Review pipeline', { exact: true })).toBeVisible();
  await expectNoAssessment(page);

  await fillReadinessForm(page, {
    status: 'Ready for Racing',
    scores: [
      RULES.readinessScoreMax - 4,
      RULES.readinessScoreMax - 3,
      RULES.readinessScoreMax - 2,
    ],
    months: RULES.monthsMax + 1,
    remarks: uniqueName('TRN_TC54', 'boundary'),
  });
  await submitEvaluation(page);

  const rangeMessage = `Estimated months must be between ${RULES.monthsMin} and ${RULES.monthsMax}.`;
  const alert = page.getByRole('alert');
  await expect(alert.first()).toBeVisible();
  await expect(alert.first()).toContainText(rangeMessage);
  await expect(page.getByRole('button', { name: 'Submit evaluation' })).toBeVisible();
  await capture(page, testInfo, 'c', 'TRN_TC54', 'months-61-rejected');

  await page.getByLabel('Months until race-ready').fill(String(RULES.monthsMax));
  await submitEvaluation(page);

  const readOnly = page.getByText('Head Trainer evaluation (submitted)', { exact: true });
  await expect(readOnly).toBeVisible({ timeout: 25_000 });
  const readOnlyText = await readOnly.locator('xpath=..').innerText();
  expect(readOnlyText).toContain(`${RULES.monthsMax} months`);
  await expectManagerReviewCurrent(page);
  await capture(page, testInfo, 'c', 'TRN_TC54', 'months-60-saved');
});

test('TRN_TC55 foreign admission is absent from the queue and denied by direct URL', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  const foreignId = process.env.M02_FOREIGN_ID;
  expect(foreignId, 'M02_FOREIGN_ID must be exported from /tmp/m02-fixtures.env').toBeTruthy();

  await openAdmissions(page);
  await expect(admissionsTable(page).locator(`a[href^="/trainer/admissions/${foreignId}"]`)).toHaveCount(0);
  expect(await page.locator(`a[href^="/trainer/admissions/${foreignId}"]`).count()).toBe(0);

  await gotoAuthed(page, `/trainer/admissions/${foreignId}`);
  await expect(page.getByText('Cannot load admission details', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Try Again' })).toBeVisible();
  await expect(page.getByText('Racing readiness assessment', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Submit evaluation' })).toHaveCount(0);

  await capture(page, testInfo, 'c', 'TRN_TC55', 'foreign-admission-denied');
});

// ──────────────────────────────── horses ──────────────────────────────────────

test('TRN_TC58 trainer horse list: table, filters, search, paging and legacy URL', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openHorses(page);

  const headers = (await horsesTable(page).locator('thead th').allInnerTexts()).map(cleanHeader);
  expect(headers).toEqual([
    'Horse',
    'Registration number',
    'Breed',
    'Sex',
    'Specialty',
    'Training',
    'Status',
    'Actions',
  ]);

  const total = await paginationTotal(page);
  expect(total).toBeGreaterThan(0);
  expect(await horsesTable(page).locator('tbody tr').count()).toBe(Math.min(total, RULES.pageSize));

  // Search a registration number read from the first row.
  const firstRegistration = (await horsesTable(page).locator('tbody tr').first().locator('td').nth(1).innerText()).trim();
  expect(firstRegistration).not.toBe('');
  await page.getByLabel('Search horses').fill(firstRegistration);
  await page.getByRole('button', { name: 'Apply', exact: true }).click();
  await page.waitForTimeout(700);
  const searchedRows = horsesTable(page).locator('tbody tr');
  expect(await searchedRows.count()).toBe(1);
  expect(await searchedRows.first().innerText()).toContain(firstRegistration);
  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await page.waitForTimeout(700);

  // Status filter.
  await page.getByLabel('Status', { exact: true }).selectOption('ELIGIBLE');
  await page.getByRole('button', { name: 'Apply', exact: true }).click();
  await page.waitForTimeout(700);
  const eligibleRows = horsesTable(page).locator('tbody tr');
  const eligibleCount = await eligibleRows.count();
  for (let index = 0; index < eligibleCount; index += 1) {
    expect(await eligibleRows.nth(index).innerText()).toContain('Eligible');
  }
  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await page.waitForTimeout(700);
  expect(await paginationTotal(page)).toBe(total);

  // Page 2 exists only when the list is larger than the page size.
  if (total > RULES.pageSize) {
    await pagination(page).getByRole('button', { name: 'Next', exact: true }).click();
    await page.waitForTimeout(700);
    expect(await pagination(page).innerText()).toMatch(/Page 2 of/);
    expect(await horsesTable(page).locator('tbody tr').count()).toBe(total - RULES.pageSize);
    await page.getByRole('button', { name: 'Clear', exact: true }).click();
    await page.waitForTimeout(700);
  }

  // Row detail and the legacy ?horseId redirect.
  const horseId = await firstHorseId(page);
  await horsesTable(page).locator('tbody tr').first().getByRole('link', { name: 'View' }).click();
  await expect(page).toHaveURL(new RegExp(`/trainer/horses/${horseId}$`));
  await expect(page.getByRole('tab', { name: 'Overview' })).toBeVisible();

  await capture(page, testInfo, 'c', 'TRN_TC58', 'horse-list-filters');

  await gotoAuthed(page, `/trainer/horses?horseId=${horseId}`);
  await expect(page).toHaveURL(new RegExp(`/trainer/horses/${horseId}$`));
  await expect(page.getByRole('tab', { name: 'Overview' })).toBeVisible();
});

test('TRN_TC59 horse detail tabs render schedule, course runs, trends and incidents', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');

  // Precondition: a horse in the trainer area with a course run, completed
  // sessions and an incident. Discover it from the live incident list.
  await openIncidents(page);
  const incidentHorse = (await incidentsTable(page).locator('tbody tr').first().locator('td').nth(1).innerText()).trim();
  await openHorses(page);
  await page.getByLabel('Search horses').fill(incidentHorse);
  await page.getByRole('button', { name: 'Apply', exact: true }).click();
  await page.waitForTimeout(800);
  const horseHref = await horsesTable(page).locator('tbody tr').first().getByRole('link', { name: 'View' }).getAttribute('href');
  await gotoAuthed(page, horseHref);

  for (const tab of ['Overview', 'Training schedule', 'Course runs', 'Fitness & Trends', 'Completed sessions', 'Incident Reports']) {
    // "Completed sessions" is a section inside Fitness & Trends, not a tab.
    if (tab === 'Completed sessions') continue;
    await expect(page.getByRole('tab', { name: new RegExp(tab) })).toBeVisible();
  }

  await expect(page.getByText('Profile details', { exact: true })).toBeVisible();
  await expect(page.getByText('Registration number', { exact: true })).toBeVisible();

  await clickHorseTab(page, 'Training schedule');
  await expect(page.getByText('This week', { exact: true })).toBeVisible();
  await expect(page.getByText(/\d{2}\/\d{2} – \d{2}\/\d{2}\/\d{4}/)).toBeVisible();

  await clickHorseTab(page, 'Course runs');
  await expect(page.getByText(/\d+ of \d+ ran/).first()).toBeVisible();

  await clickHorseTab(page, 'Fitness & Trends');
  await expect(page.getByText(new RegExp(`Last ${RULES.performanceWindowDays} days`))).toBeVisible();
  await expect(page.getByText(/completed sessions/i).first()).toBeVisible();
  const completedSessions = await page.getByText(/Completed session history \(\d+\)/).innerText();
  const sessionCount = Number(completedSessions.match(/\((\d+)\)/)[1]);
  expect(sessionCount).toBeGreaterThan(0);
  for (const column of ['Date', 'Exercise', 'Distance', 'Speed (avg / max)', 'Performance']) {
    expect(await page.locator('table thead').last().innerText()).toContain(column);
  }

  await clickHorseTab(page, 'Incident Reports');
  await expect(page.getByText('Incident & Safety Reports', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: /Report Incident to Vet/ })).toBeVisible();
  await expect(page.getByText(/Reported on /).first()).toBeVisible();

  await capture(page, testInfo, 'c', 'TRN_TC59', 'horse-detail-tabs');
});

test('TRN_TC60 trainer cannot read performance data for another trainer horse', async ({ page, browser }, testInfo) => {
  const foreign = await newSession(browser, TRAINER2, PASSWORD);
  let foreignHorseId;
  let foreignHorseName;
  try {
    await foreign.page.waitForURL((url) => url.pathname === '/trainer', { timeout: 30_000 });
    await openHorses(foreign.page, TRAINER2, '/trainer');
    foreignHorseId = await firstHorseId(foreign.page);
    foreignHorseName = await foreign.page
      .getByRole('table', { name: 'Horses' })
      .locator('tbody tr')
      .first()
      .locator('td')
      .first()
      .innerText()
      .then(lastLine);
  } finally {
    await foreign.context.close();
  }

  await loginAs(page, TRAINER1, '/trainer');
  await openHorse(page, foreignHorseId);

  await expect(page.getByText('You can only view horses in the areas you manage', { exact: true })).toBeVisible();
  await expect(page.getByText(foreignHorseName, { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Registration number', { exact: true })).toBeVisible();
  await expect(page.getByText('Veterinary clearance', { exact: true })).toBeVisible();

  await clickHorseTab(page, 'Fitness & Trends');
  await expect(page.getByText('You can only view horses in the areas you manage', { exact: true }).first()).toBeVisible();
  const fitnessText = await page.locator('main').first().innerText();
  expect(fitnessText).toContain('COMPLETED SESSIONS');
  expect(fitnessText).toMatch(/COMPLETED SESSIONS\s*\n\s*0/);
  expect(fitnessText).toContain('No completed sessions in this date range');

  await capture(page, testInfo, 'c', 'TRN_TC60', 'foreign-horse-performance-blocked');

  // The foreign horse is not in trainer1's list either.
  await openHorses(page);
  await page.getByLabel('Search horses').fill(foreignHorseName);
  await page.getByRole('button', { name: 'Apply', exact: true }).click();
  await page.waitForTimeout(700);
  expect(await horsesTable(page).locator('tbody tr').filter({ hasText: foreignHorseName }).count()).toBe(0);
});

// ─────────────────────────────── incidents ────────────────────────────────────

test('TRN_TC61 trainer incident list: status chips, columns, order, search and paging', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openIncidents(page);

  await expect(page.getByRole('button', { name: 'Refresh' })).toBeVisible();
  for (const chip of ['All', 'Awaiting veterinary review', 'Under review', 'Resolved', 'Closed']) {
    await expect(page.getByRole('radio', { name: new RegExp(`^${chip}`) })).toBeVisible();
  }

  const headers = (await incidentsTable(page).locator('thead th').allInnerTexts()).map(cleanHeader);
  expect(headers).toEqual(['Reported', 'Horse', 'Incident', 'Reported by', 'Veterinarian', 'Severity', 'Status', 'Actions']);

  const rows = incidentsTable(page).locator('tbody tr');
  const rowCount = await rows.count();
  expect(rowCount).toBeGreaterThan(0);

  // Newest first.
  const timestamps = [];
  for (let index = 0; index < rowCount; index += 1) {
    const text = await rows.nth(index).locator('td').first().innerText();
    const match = text.trim().match(/(\d{2})\/(\d{2})\/(\d{4}),?\s+(\d{2}):(\d{2})/);
    if (match) timestamps.push(new Date(Number(match[3]), Number(match[2]) - 1, Number(match[1]), Number(match[4]), Number(match[5])).getTime());
  }
  for (let index = 1; index < timestamps.length; index += 1) {
    expect(timestamps[index - 1], 'incident rows should be newest first').toBeGreaterThanOrEqual(timestamps[index]);
  }

  // Status chips filter the table.
  await page.getByRole('radio', { name: /^Resolved/ }).click();
  await page.waitForTimeout(900);
  const resolvedRows = incidentsTable(page).locator('tbody tr');
  for (let index = 0; index < (await resolvedRows.count()); index += 1) {
    expect(await resolvedRows.nth(index).innerText()).toContain('Resolved');
  }
  await page.getByRole('radio', { name: /^Under review/ }).click();
  await page.waitForTimeout(900);
  const reviewRows = incidentsTable(page).locator('tbody tr');
  for (let index = 0; index < (await reviewRows.count()); index += 1) {
    expect(await reviewRows.nth(index).innerText()).toContain('Under Veterinary Review');
  }
  await page.getByRole('radio', { name: /^Closed/ }).click();
  await page.waitForTimeout(900);
  const closedRows = incidentsTable(page).locator('tbody tr');
  for (let index = 0; index < (await closedRows.count()); index += 1) {
    expect(await closedRows.nth(index).innerText()).toContain('Closed');
  }
  await page.getByRole('radio', { name: /^All/ }).click();
  await page.waitForTimeout(900);

  // Horse search in the table toolbar.
  const firstHorse = lastLine(await incidentsTable(page).locator('tbody tr').first().locator('td').nth(1).innerText());
  await page.getByLabel('Search records').fill(firstHorse);
  await page.waitForTimeout(700);
  const searchRows = incidentsTable(page).locator('tbody tr');
  expect(await searchRows.count()).toBeGreaterThan(0);
  for (let index = 0; index < (await searchRows.count()); index += 1) {
    expect(await searchRows.nth(index).innerText()).toContain(firstHorse);
  }
  await page.getByLabel('Search records').fill('');
  await page.waitForTimeout(700);

  // Paging: page 2 exists only when the list is larger than the page size.
  const total = await paginationTotal(page);
  if (total > RULES.pageSize) {
    await pagination(page).getByRole('button', { name: 'Next', exact: true }).click();
    await page.waitForTimeout(700);
    expect(await pagination(page).innerText()).toMatch(/Page 2 of/);
  } else {
    expect(await pagination(page).innerText()).toMatch(/Page 1 of 1/);
  }

  await capture(page, testInfo, 'c', 'TRN_TC61', 'incident-list');
});

test('TRN_TC62 incident attachment should render but shows the generic load error', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openIncidents(page);

  const rows = incidentsTable(page).locator('tbody tr');
  const rowCount = await rows.count();
  let selectedRow = null;
  for (let index = 0; index < rowCount; index += 1) {
    const row = rows.nth(index);
    await row.getByRole('button', { name: 'Details' }).click();
    const modal = page.getByRole('dialog');
    await expect(modal).toBeVisible();
    await page.waitForTimeout(1500);
    const modalText = await modal.innerText();
    const hasAttachment = modalText.includes('The image could not be loaded.') || (await modal.locator('img[alt^="Incident image"]').count()) > 0;
    const hasDecision = /Veterinary assessment & guidance|Closed \(/.test(modalText);
    if (hasAttachment && hasDecision) {
      selectedRow = row;
      break;
    }
    await closeIncidentModal(page);
  }
  expect(selectedRow, 'an incident with an attachment should exist').toBeTruthy();

  let modal = page.getByRole('dialog');
  const modalText = await modal.innerText();
  expect(modalText).toMatch(/^#\d+ .+/);
  expect(modalText).toContain('Horse:');
  expect(modalText).toContain('Reported');
  expect(modalText).toContain('Description');
  expect(modalText).toMatch(/Veterinary assessment & guidance|Closed \(/);

  await capture(page, testInfo, 'c', 'TRN_TC62', 'incident-attachment-error');

  // Close returns to the unchanged list, then reopen for the attachment check.
  await closeIncidentModal(page);
  const listTextBefore = await incidentsTable(page).innerText();
  await selectedRow.getByRole('button', { name: 'Details' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await closeIncidentModal(page);
  expect(await incidentsTable(page).innerText()).toBe(listTextBefore);
  await selectedRow.getByRole('button', { name: 'Details' }).click();
  modal = page.getByRole('dialog');
  await expect(modal).toBeVisible();
  await page.waitForTimeout(1200);

  // Expected: the authenticated Cloudinary image is displayed.
  // Actual (UI-D01): the fallback text "The image could not be loaded." is shown.
  await expect(
    modal.locator('img[alt^="Incident image"]').first(),
    'UI-D01: the incident attachment should load, but the modal shows "The image could not be loaded."',
  ).toBeVisible({ timeout: 5_000 });
});

test('TRN_TC63 new LOW incident is auto-assigned instead of awaiting veterinary assignment', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');

  // Discover a trainer1 horse with no incident at all (strictly no open case).
  // The incident list is paginated, so collect the horse names from every page.
  await openIncidents(page);
  const horsesWithIncidents = new Set();
  for (;;) {
    const incidentRows = incidentsTable(page).locator('tbody tr');
    for (let index = 0; index < (await incidentRows.count()); index += 1) {
      const row = incidentRows.nth(index);
      if ((await row.locator('td').count()) < 6) continue; // empty-state row
      horsesWithIncidents.add(lastLine(await row.locator('td').nth(1).innerText()));
    }
    const next = pagination(page).getByRole('button', { name: 'Next', exact: true });
    if (await next.isDisabled()) break;
    await next.click();
    await page.waitForTimeout(500);
  }

  await openHorses(page);
  let targetHref = null;
  let targetName = null;
  for (;;) {
    const horseRows = horsesTable(page).locator('tbody tr');
    for (let index = 0; index < (await horseRows.count()); index += 1) {
      const row = horseRows.nth(index);
      if ((await row.locator('td').count()) < 6) continue; // empty-state row
      const name = lastLine(await row.locator('td').first().innerText());
      if (!horsesWithIncidents.has(name)) {
        targetHref = await row.getByRole('link').first().getAttribute('href');
        targetName = name;
        break;
      }
    }
    if (targetHref) break;
    const next = pagination(page).getByRole('button', { name: 'Next', exact: true });
    if (await next.isDisabled()) break;
    await next.click();
    await page.waitForTimeout(500);
  }
  expect(targetHref, 'a trainer horse with no incident should exist').toBeTruthy();

  await gotoAuthed(page, targetHref);
  await clickHorseTab(page, 'Incident Reports');
  await expect(page.getByText('No incident reports', { exact: true })).toBeVisible();
  await expect(page.getByText(targetName, { exact: true }).first()).toBeVisible();

  const title = uniqueName('TRN_TC63', 'low incident');
  const description = `${uniqueName('TRN_TC63', 'description')} — small scrape noticed after grooming, no lameness.`;
  await page.getByRole('button', { name: /Report Incident to Vet/ }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  const dialogText = await dialog.innerText();
  expect(dialogText).toContain(targetName);
  expect(dialogText).toContain('Low — Next routine vet slot, keeps training');
  expect(dialogText).toContain('Medium — Next routine vet slot, keeps training');

  await dialog.getByLabel('Severity level').selectOption('LOW');
  await dialog.getByLabel(/Incident title/).fill(title);
  await dialog.getByLabel(/Incident description/).fill(description);
  await dialog.getByRole('button', { name: 'Send incident report' }).click();
  await expectToast(page, /Incident reported/);
  await expect(dialog).toBeHidden({ timeout: 15_000 });

  // New report appears at the top of the horse tab.
  await expect(page.getByText(title, { exact: true })).toBeVisible({ timeout: 20_000 });
  const horseTabText = await page.locator('main').first().innerText();
  const tabActualStatus = /Reported — Awaiting Vet Review/.test(horseTabText)
    ? 'Reported — Awaiting Vet Review'
    : /Under Vet Review/.test(horseTabText)
      ? 'Under Vet Review'
      : 'unknown';
  await capture(page, testInfo, 'c', 'TRN_TC63', 'new-incident-horse-tab');

  // The same report in the trainer incident list.
  await openIncidents(page);
  await page.getByLabel('Search records').fill(targetName);
  await page.waitForTimeout(1000);
  const newRow = incidentsTable(page).locator('tbody tr').filter({ hasText: title }).first();
  await expect(newRow).toBeVisible();
  expect((await incidentsTable(page).locator('tbody tr').first().innerText())).toContain(title);
  const status = (await newRow.locator('td').nth(6).innerText()).trim();
  const veterinarian = (await newRow.locator('td').nth(4).innerText()).trim();
  await capture(page, testInfo, 'c', 'TRN_TC63', 'new-incident-list-auto-assigned');

  // Expected: a newly reported LOW incident starts as Reported / Awaiting assignment.
  // Actual (UI-D03): it is auto-assigned to a vet and starts as Under Veterinary Review.
  expect(
    status,
    `UI-D03: expected "Reported - Awaiting Veterinary Review" with Veterinarian "Awaiting assignment"; actual status "${status}" (horse tab "${tabActualStatus}"), veterinarian "${veterinarian}"`,
  ).toBe('Reported - Awaiting Veterinary Review');
});

test('TRN_TC64 incident form validation: required text and file size boundary', async ({ page }, testInfo) => {
  await loginAs(page, TRAINER1, '/trainer');
  await openHorses(page);
  const horseId = await firstHorseId(page);
  await openHorse(page, horseId);
  await clickHorseTab(page, 'Incident Reports');
  const incidentCountBefore = await page.getByText(/Reported on /).count();

  await page.getByRole('button', { name: /Report Incident to Vet/ }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  const send = dialog.getByRole('button', { name: 'Send incident report' });
  const titleInput = dialog.getByLabel(/Incident title/);
  const descriptionInput = dialog.getByLabel(/Incident description/);

  // Blank title: the browser blocks the submit and reports the required field.
  await send.click();
  await expect(dialog).toBeVisible();
  expect(await titleInput.evaluate((input) => input.validationMessage)).not.toBe('');

  // Whitespace-only title: the form-level required error.
  await titleInput.fill('   ');
  await descriptionInput.fill('Description present.');
  await send.click();
  await expect(dialog.getByText('Title is required.', { exact: true })).toBeVisible();

  // Blank description: the browser reports the required field.
  await titleInput.fill(uniqueName('TRN_TC64', 'validation'));
  await descriptionInput.fill('');
  await send.click();
  await expect(dialog).toBeVisible();
  expect(await descriptionInput.evaluate((input) => input.validationMessage)).not.toBe('');

  // Whitespace-only description: the form-level required error.
  await descriptionInput.fill('   ');
  await send.click();
  await expect(dialog.getByText('Description is required.', { exact: true })).toBeVisible();

  // File size: limit + 1 MB is rejected, limit is accepted.
  const fileInput = dialog.locator('input[type="file"]');
  const oversized = '/tmp/opencode/m02-fixtures/m02-11mb.png';
  const atLimit = '/tmp/opencode/m02-fixtures/m02-10mb.png';
  await fileInput.setInputFiles(oversized);
  await expect(dialog.getByText(`Image file must not exceed ${RULES.fileMaxMB} MB.`, { exact: true })).toBeVisible();
  await capture(page, testInfo, 'c', 'TRN_TC64', 'oversized-file-rejected');

  await fileInput.setInputFiles(atLimit);
  const selected = await fileInput.evaluate((input) => Array.from(input.files || []).map((file) => ({ name: file.name, size: file.size })));
  expect(selected).toHaveLength(1);
  expect(selected[0].size).toBe(RULES.fileMaxMB * 1024 * 1024);
  await capture(page, testInfo, 'c', 'TRN_TC64', 'limit-file-accepted');

  // Cancel discards the draft and creates no incident.
  await dialog.getByRole('button', { name: 'Cancel' }).click();
  await expect(dialog).toBeHidden();
  expect(await page.getByText(/Reported on /).count()).toBe(incidentCountBefore);
});

// ─────────────────────────────── security ─────────────────────────────────────

async function expectRoleRedirect(browser, email, destination, testInfo, caseId, label) {
  const { context, page } = await newSession(browser, email, PASSWORD);
  try {
    await page.waitForURL((url) => url.pathname === destination, { timeout: 30_000 });
    const admissionId = process.env.M02_VALIDATION_ID;
    const horseId = await discoverTrainerHorseId(browser);
    const routes = [
      '/trainer',
      '/trainer/dashboard',
      '/trainer/admissions',
      `/trainer/admissions/${admissionId}`,
      '/trainer/horses',
      `/trainer/horses/${horseId}`,
      '/trainer/incidents',
      '/trainer/stable',
    ];
    for (const route of routes) {
      await gotoAuthed(page, route, email, destination);
      await page.waitForURL((url) => url.pathname === destination, { timeout: 30_000 });
      expect(new URL(page.url()).pathname).toBe(destination);
      expect(await page.locator('a[href^="/trainer"]').count(), `${route} should not render trainer navigation`).toBe(0);
      expect((await page.locator('body').innerText())).not.toContain('Fitness and training overview');
    }
    await capture(page, testInfo, 'c', caseId, label);
  } finally {
    await context.close();
  }
}

test('TRN_TC65 horse owner is redirected away from every trainer route', async ({ browser }, testInfo) => {
  const horseId = await discoverTrainerHorseId(browser);
  expect(horseId).toBeGreaterThan(0);
  await expectRoleRedirect(browser, OWNER, '/owner', testInfo, 'TRN_TC65', 'owner-redirected');
});

test('TRN_TC66 groom is redirected away from every trainer route', async ({ browser }, testInfo) => {
  const horseId = await discoverTrainerHorseId(browser);
  expect(horseId).toBeGreaterThan(0);
  await expectRoleRedirect(browser, GROOM, '/groom', testInfo, 'TRN_TC66', 'groom-redirected');
});

test('TRN_TC67 anonymous visitor is sent to login before trainer data loads', async ({ browser }, testInfo) => {
  const horseId = await discoverTrainerHorseId(browser);
  const { context, page } = await newSession(browser, null);
  try {
    for (const route of ['/trainer', '/trainer/admissions', `/trainer/horses/${horseId}`]) {
      await page.goto(route, { waitUntil: 'domcontentloaded' });
      await page.waitForURL((url) => url.pathname === '/login', { timeout: 30_000 });
      expect(new URL(page.url()).pathname).toBe('/login');
      const body = await page.locator('body').innerText();
      expect(body).not.toContain('Fitness and training overview');
      expect(body).not.toContain('TOTAL HORSES');
      expect(body).not.toContain('Admission records');
    }

    // Sign in through the UI and reopen the dashboard.
    await loginAs(page, TRAINER1, '/trainer');
    await gotoAuthed(page, '/trainer');
    await expect(page.getByRole('heading', { name: 'Fitness and training overview' })).toBeVisible({ timeout: 25_000 });
    await expect(page.getByText(/TOTAL HORSES/i).first()).toBeVisible();
    await capture(page, testInfo, 'c', 'TRN_TC67', 'anonymous-login-dashboard');
  } finally {
    await context.close();
  }
});
