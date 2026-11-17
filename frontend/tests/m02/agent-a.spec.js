// M02 relative UI system test — Agent A (Course Library + Training Groups)
// Scope: TRN_TC01–TRN_TC24. UI-only: no API calls, no SQL.
const { test, expect } = require('@playwright/test');
const {
  RULES,
  RUN_ID,
  login,
  goto,
  capture,
  uniqueName,
  newSession,
  expectToast,
  noHorizontalScroll,
} = require('./lib/helpers');

// A per-execution token keeps the generated names unique when the same RUN_ID is
// replayed. Names still follow M02_<case>_<RUN_ID>_<token>.
const ATTEMPT = process.env.M02_ATTEMPT || String(Date.now()).slice(-6);
const nameFor = (caseId, suffix = '') => uniqueName(caseId, suffix ? `${suffix}_${ATTEMPT}` : ATTEMPT);

const TC23_SESSIONS = 6; // plan: a six-session course run for the disband-with-history case

/** Login plus a short settle so the auth context is ready before the first route change. */
async function loginAs(page, email) {
  await login(page, email);
  await page.waitForTimeout(1200);
  page.__m02Email = email;
  if (page.url().includes('/login')) throw new Error(`login did not persist for ${email}`);
}

async function newRoleSession(browser, email) {
  const session = await newSession(browser, email);
  await session.page.waitForTimeout(1000);
  session.page.__m02Email = email;
  return session;
}

/**
 * Full-page navigation restores the session through the refresh cookie. The dev
 * server occasionally drops that one refresh, so retry the login once if the
 * guard sends us back to /login before asserting anything about the page.
 */
async function gotoAuthed(page, path, email) {
  const account = email || page.__m02Email || 'trainer1@rtms.com';
  for (let attempt = 0; attempt < 2; attempt += 1) {
    await goto(page, path);
    if (!new URL(page.url()).pathname.startsWith('/login')) return;
    await loginAs(page, account);
  }
  await goto(page, path);
}

/* ------------------------------------------------------------------ */
/* Course library helpers                                              */
/* ------------------------------------------------------------------ */

async function openCourses(page) {
  await gotoAuthed(page, '/trainer/courses');
  await expect(page.getByRole('heading', { name: 'Course library', exact: true }).first()).toBeVisible();
  await expect(page.getByText(/^Courses \(\d+\)$/).first()).toBeVisible();
}

async function openExercises(page) {
  await openCourses(page);
  await page.getByRole('tab', { name: 'Exercises', exact: true }).click();
  await expect(page.getByText(/^Exercise library \(\d+\)$/).first()).toBeVisible();
}

async function courseCount(page) {
  const text = await page.getByText(/^Courses \(\d+\)$/).first().innerText();
  return Number(/\d+/.exec(text)[0]);
}

async function exerciseCount(page) {
  const text = await page.getByText(/^Exercise library \(\d+\)$/).first().innerText();
  return Number(/\d+/.exec(text)[0]);
}

function courseCard(page, name) {
  return page
    .getByRole('heading', { name, exact: true })
    .locator("xpath=ancestor::div[contains(@class,'shadow-[var(--shadow-panel)]')][1]");
}

function exerciseCard(page, name) {
  return page
    .getByRole('heading', { name, exact: true })
    .locator("xpath=ancestor::div[contains(@class,'shadow-[var(--shadow-panel)]')][1]");
}

async function openExerciseForm(page) {
  const button = page.getByRole('button', { name: 'Create exercise', exact: true });
  if (await button.isVisible().catch(() => false)) await button.click();
  await expect(page.getByRole('heading', { name: 'Create exercise', exact: true })).toBeVisible();
}

async function fillExerciseForm(page, { name, duration, distance, description }) {
  await page.getByLabel('Exercise name').fill(name);
  await page.getByLabel('Category').selectOption({ label: 'Fitness & endurance' });
  await page.getByLabel('Track surface').selectOption({ label: 'Turf' });
  await page.getByLabel('Intensity').selectOption({ label: 'Medium' });
  await page.getByLabel('Target distance (metres)').fill(String(distance));
  await page.getByLabel('Estimated duration (minutes)').fill(String(duration));
  if (description) await page.getByLabel('Description').fill(description);
}

async function saveExercise(page, name, { expectCreated = true } = {}) {
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();
  if (expectCreated) {
    await expectToast(page, /Exercise created\./);
    await expect(page.getByRole('heading', { name, exact: true }).first()).toBeVisible();
  }
}

async function createExercise(page, { caseId, suffix, duration = 45, distance = 1200, description }) {
  const name = nameFor(caseId, suffix);
  await openExercises(page);
  await openExerciseForm(page);
  await fillExerciseForm(page, { name, duration, distance, description });
  await saveExercise(page, name);
  return name;
}

/** Top the trainer's exercise library up to a required count through the Create exercise form. */
async function ensureExerciseLibraryCount(page, target, caseId) {
  await openExercises(page);
  let count = await exerciseCount(page);
  let index = 0;
  while (count < target) {
    index += 1;
    const name = nameFor(caseId, `bulk-${index}`);
    await openExerciseForm(page);
    await fillExerciseForm(page, { name, duration: 45, distance: 1200 });
    await saveExercise(page, name);
    count = await exerciseCount(page);
  }
  return count;
}

async function openCourseForm(page) {
  const button = page.getByRole('button', { name: 'Create course', exact: true });
  if (await button.isVisible().catch(() => false)) await button.click();
  await expect(page.getByRole('heading', { name: 'Create course with exercise rotation', exact: true })).toBeVisible();
}

async function fillCourseBasics(page, { name, sessions, goal }) {
  await page.getByLabel('Course name').fill(name);
  await page.getByLabel('Total sessions').fill(String(sessions));
  if (goal) await page.getByLabel('Course goal').fill(goal);
}

function exerciseLibrary(page) {
  return page.locator('aside[aria-label="Exercise library"]');
}

async function selectedExercises(page) {
  const label = page.getByText(/^Exercise rotation \(\d+ selected\)$/).first();
  const text = await label.innerText();
  return Number(/\((\d+)/.exec(text)[1]);
}

async function addExercisesFromLibrary(page, count) {
  const library = exerciseLibrary(page);
  const before = await selectedExercises(page);
  for (let i = 0; i < count; i += 1) {
    const add = library.locator('button[aria-label^="Add "]').first();
    await add.scrollIntoViewIfNeeded();
    await add.click();
    await expect(page.getByText(new RegExp(`^Exercise rotation \\(${before + i + 1} selected\\)$`))).toBeVisible();
  }
}

async function addExerciseByName(page, name) {
  const library = exerciseLibrary(page);
  const search = library.getByLabel('Search exercises');
  await search.fill(name);
  await expect(library.getByText(name, { exact: true }).first()).toBeVisible();
  await library.getByRole('button', { name: `Add ${name} to course`, exact: true }).click();
  await search.fill('');
}

async function saveCourse(page, name) {
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expectToast(page, /Course created\./);
  await expect(page.getByRole('heading', { name, exact: true }).first()).toBeVisible();
}

async function createThreeExerciseCourse(page, caseId, suffix) {
  const exercises = [];
  for (const letter of ['A', 'B', 'C']) {
    exercises.push(await createExercise(page, { caseId, suffix: `${suffix}_${letter}` }));
  }
  const courseName = nameFor(caseId, suffix);
  await openCourses(page);
  await openCourseForm(page);
  await fillCourseBasics(page, { name: courseName, sessions: 6, goal: `${suffix} goal` });
  for (const exercise of exercises) await addExerciseByName(page, exercise);
  await saveCourse(page, courseName);
  return { courseName, exercises };
}

/* ------------------------------------------------------------------ */
/* Training-group helpers                                              */
/* ------------------------------------------------------------------ */

function activeGroupCard(page, name) {
  return page
    .getByRole('heading', { name, exact: true })
    .locator("xpath=ancestor::div[contains(@class,'shadow-[var(--shadow-panel)]')][1]");
}

function disbandedGroupCard(page, name) {
  return page
    .getByRole('heading', { name, exact: true })
    .locator("xpath=ancestor::div[contains(@class,'rounded-[var(--radius-md)]')][1]");
}

function horseLibrary(page) {
  return page.locator('aside').filter({ hasText: 'Horse Library' }).first();
}

async function openGroups(page) {
  await gotoAuthed(page, '/trainer/groups');
  await expect(page.getByRole('heading', { name: 'Groups', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'New group', exact: true })).toBeVisible();
}

async function createGroup(page, name) {
  await page.getByRole('button', { name: 'New group', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: 'New group', exact: true })).toBeVisible();
  await dialog.locator('input').first().fill(name);
  await dialog.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
}

async function disbandGroup(page, name) {
  const card = activeGroupCard(page, name);
  if ((await card.count()) === 0) return;
  await card.getByRole('button', { name: 'Disband group', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: 'Disband group', exact: true })).toBeVisible();
  await dialog.getByRole('button', { name: 'Disband', exact: true }).click();
  await expect(page.getByRole('heading', { name, exact: true })).toHaveCount(0);
}

async function cleanupGroups(page, names) {
  try {
    await page.keyboard.press('Escape');
  } catch {
    // no dialog open
  }
  for (let round = 0; round < 2; round += 1) {
    for (const name of names) {
      try {
        await disbandGroup(page, name);
      } catch {
        // Cleanup must not turn a passing assertion into a failure.
      }
    }
    let stillThere = 0;
    for (const name of names) {
      stillThere += await page.getByRole('heading', { name, exact: true }).count();
    }
    if (!stillThere) return;
    try {
      await gotoAuthed(page, '/trainer/groups');
    } catch {
      return;
    }
  }
}

/** Remove active groups left by an earlier failed attempt of this suite (UI-only). */
async function cleanupStaleSuiteGroups(page) {
  try {
    await page.keyboard.press('Escape');
  } catch {
    // no dialog open
  }
  const headings = await page.locator('h3').allInnerTexts();
  const ownCase = /^M02_TRN_TC(0[1-9]|1[0-9]|2[0-4])_/;
  const stale = headings
    .map((text) => text.trim())
    .filter((text) => ownCase.test(text) || text.startsWith('M02PROBE_') || text.startsWith('ZZPROBE_'));
  for (const name of stale) {
    try {
      await disbandGroup(page, name);
    } catch {
      // best-effort cleanup
    }
  }
}

async function openAddHorse(page, groupName) {
  await activeGroupCard(page, groupName).getByRole('button', { name: 'Add horse', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: `Add a horse to ${groupName}`, exact: true })).toBeVisible();
  return dialog;
}

async function addHorseViaDialog(page, groupName, horseName, expectedCount) {
  const dialog = await openAddHorse(page, groupName);
  const row = dialog.locator('li').filter({ hasText: horseName }).first();
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: 'Add', exact: true }).click();
  await expect(dialog.locator('li').filter({ hasText: horseName })).toHaveCount(0);
  await dialog.getByRole('button', { name: 'Close', exact: true }).click();
  await expect(dialog).toBeHidden();
  if (expectedCount !== undefined) {
    await expect(activeGroupCard(page, groupName)).toContainText(`${expectedCount}/${RULES.groupCapacity} horses`);
  }
}

async function countDisbanded(page) {
  const button = page.getByRole('button', { name: /^Disbanded \(\d+\)$/ });
  if ((await button.count()) === 0) return 0;
  return Number(/\d+/.exec(await button.innerText())[0]);
}

async function expandDisbanded(page) {
  const toggle = page.getByRole('button', { name: /^Disbanded \(\d+\)$/ });
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await toggle.click();
  await expect(toggle).toHaveAttribute('aria-expanded', 'true');
}

/** HTML5 drag & drop with both endpoints scrolled into view (sticky library stays visible). */
async function dragLocator(page, source, target) {
  await target.scrollIntoViewIfNeeded();
  await page.waitForTimeout(250);
  await source.scrollIntoViewIfNeeded();
  await page.waitForTimeout(250);
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  if (!sourceBox || !targetBox) throw new Error('drag source or target has no bounding box');
  const sx = sourceBox.x + sourceBox.width / 2;
  const sy = sourceBox.y + sourceBox.height / 2;
  const tx = targetBox.x + targetBox.width / 2;
  const ty = targetBox.y + targetBox.height / 2;
  await page.mouse.move(sx, sy);
  await page.mouse.down();
  for (let step = 1; step <= 18; step += 1) {
    await page.mouse.move(sx + ((tx - sx) * step) / 18, sy + ((ty - sy) * step) / 18);
    await page.waitForTimeout(25);
  }
  await page.mouse.up();
  await page.waitForTimeout(900);
}

async function removeMember(page, groupName, horseName) {
  const card = activeGroupCard(page, groupName);
  const member = card.locator('li').filter({ hasText: horseName }).first();
  await dragLocator(page, member, horseLibrary(page));
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: 'Remove horse', exact: true })).toBeVisible();
  await dialog.getByRole('button', { name: 'Remove', exact: true }).click();
  await expect(dialog).toBeHidden();
}

async function moveMember(page, fromGroup, toGroup, horseName) {
  const member = activeGroupCard(page, fromGroup).locator('li').filter({ hasText: horseName }).first();
  await dragLocator(page, member, activeGroupCard(page, toGroup));
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: 'Move horse', exact: true })).toBeVisible();
  await dialog.getByRole('button', { name: 'Move', exact: true }).click();
  await expect(dialog).toBeHidden();
}

async function dragLibraryHorseToGroup(page, horseName, groupName) {
  const library = horseLibrary(page);
  await library.getByLabel('Search horses').fill(horseName);
  const source = library.locator('div[draggable="true"]').filter({ hasText: horseName }).first();
  await expect(source).toBeVisible();
  await dragLocator(page, source, activeGroupCard(page, groupName));
  await library.getByLabel('Search horses').fill('');
}

async function firstAvailableHorse(page) {
  const source = horseLibrary(page).locator('div[draggable="true"]').first();
  await expect(source).toBeVisible();
  return (await source.locator('p').first().innerText()).trim();
}

/* ------------------------------------------------------------------ */
/* Tests                                                              */
/* ------------------------------------------------------------------ */

test('TRN_TC01 course library layout, tabs and responsive grid', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openCourses(page);

  await expect(page.getByRole('tab', { name: 'Courses', exact: true })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Exercises', exact: true })).toBeVisible();
  await expect(page.getByText(/^Courses \(\d+\)$/)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create course', exact: true })).toBeVisible();

  const count = await courseCount(page);
  expect(count).toBeGreaterThan(0);
  const firstCourse = (await page.locator('h3').first().innerText()).trim();
  const card = courseCard(page, firstCourse);
  await expect(card).toBeVisible();
  await expect(card.getByText(/\d+ sessions/)).toBeVisible();
  await expect(card.getByRole('button', { name: 'View course details', exact: true })).toBeVisible();

  const grid = card.locator("xpath=ancestor::div[contains(@class,'lg:grid-cols-3')][1]");
  const columnsAt = async () =>
    (await grid.evaluate((el) => getComputedStyle(el).gridTemplateColumns)).split(' ').filter(Boolean).length;
  expect(await columnsAt()).toBe(3);
  await capture(page, testInfo, 'a', 'TRN_TC01', 'course-library');
  const widthInfo = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }));
  expect(
    widthInfo.scrollWidth,
    `page scrollWidth ${widthInfo.scrollWidth} exceeds clientWidth ${widthInfo.clientWidth} at 1440`,
  ).toBeLessThanOrEqual(widthInfo.clientWidth + 1);

  await page.setViewportSize({ width: 1280, height: 1000 });
  await page.waitForTimeout(300);
  expect(await columnsAt()).toBe(3);
  expect(await noHorizontalScroll(page)).toBe(true);
  await page.setViewportSize({ width: 1440, height: 1000 });

  await capture(page, testInfo, 'a', 'TRN_TC01', 'course-library-1280');
});

test('TRN_TC02 exercise library cards and detail modal', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openExercises(page);

  const count = await exerciseCount(page);
  expect(count).toBeGreaterThanOrEqual(2);
  await expect(page.getByRole('button', { name: 'Create exercise', exact: true })).toBeVisible();

  const cards = page.locator('h3');
  const first = (await cards.nth(0).innerText()).trim();
  const second = (await cards.nth(1).innerText()).trim();
  expect(first).not.toBe(second);
  const firstCard = exerciseCard(page, first);
  await expect(firstCard.getByText(/Endurance|Speed|Stamina|Recovery|Technique|Strength/i).first()).toBeVisible();
  await expect(firstCard.getByText(/Low|Medium|High/).first()).toBeVisible();
  await expect(firstCard.getByRole('button', { name: 'View details', exact: true })).toBeVisible();

  await firstCard.getByRole('button', { name: 'View details', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: first, exact: true })).toBeVisible();
  await expect(dialog.getByText('Intensity', { exact: true })).toBeVisible();
  await expect(dialog.getByText('Duration', { exact: true })).toBeVisible();
  await expect(dialog.getByText('Distance', { exact: true })).toBeVisible();
  await expect(dialog.getByText('Surface', { exact: true })).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Close', exact: true }).last()).toBeVisible();

  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await capture(page, testInfo, 'a', 'TRN_TC02', 'exercise-library');
});

test('TRN_TC03 create an exercise with all fields', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openExercises(page);

  const before = await exerciseCount(page);
  const name = nameFor('TRN_TC03', 'all-fields');
  await openExerciseForm(page);
  await fillExerciseForm(page, {
    name,
    duration: 45,
    distance: 1200,
    description: 'Full-field M02 exercise created by the relative UI suite.',
  });
  await saveExercise(page, name);

  expect(await exerciseCount(page)).toBe(before + 1);
  const card = exerciseCard(page, name);
  await expect(card).toBeVisible();
  await card.getByRole('button', { name: 'View details', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByText('45 min', { exact: true })).toBeVisible();
  await expect(dialog.getByText('1,200 m', { exact: true })).toBeVisible();
  await capture(page, testInfo, 'a', 'TRN_TC03', 'exercise-created');
  await page.keyboard.press('Escape');
});

test('TRN_TC04 exercise duration and distance limits', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openExercises(page);

  const before = await exerciseCount(page);
  const firstName = nameFor('TRN_TC04', 'min-valid');
  const secondName = nameFor('TRN_TC04', 'max-valid');

  await openExerciseForm(page);
  await fillExerciseForm(page, { name: firstName, duration: 45, distance: 1200 });
  const durationError = `Must be between ${RULES.exerciseDurationMin} and ${RULES.exerciseDurationMax} minutes.`;
  const distanceError = `Must be between ${RULES.exerciseDistanceMin} and ${RULES.exerciseDistanceMax.toLocaleString('en-US')} metres.`;

  // duration min-1 / max+1
  await page.getByLabel('Estimated duration (minutes)').fill(String(RULES.exerciseDurationMin - 1));
  await expect(page.getByText(durationError, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();
  await expect(page.getByRole('heading', { name: firstName, exact: true })).toHaveCount(0);

  await page.getByLabel('Estimated duration (minutes)').fill(String(RULES.exerciseDurationMax + 1));
  await expect(page.getByText(durationError, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();

  // distance min-1 / max+1
  await page.getByLabel('Estimated duration (minutes)').fill('60');
  await page.getByLabel('Target distance (metres)').fill(String(RULES.exerciseDistanceMin - 1));
  await expect(page.getByText(distanceError, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();

  await page.getByLabel('Target distance (metres)').fill(String(RULES.exerciseDistanceMax + 1));
  await expect(page.getByText(distanceError, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();
  await expect(page.getByRole('heading', { name: firstName, exact: true })).toHaveCount(0);
  expect(await exerciseCount(page)).toBe(before);

  // exact minima
  await page.getByLabel('Target distance (metres)').fill(String(RULES.exerciseDistanceMin));
  await page.getByLabel('Estimated duration (minutes)').fill(String(RULES.exerciseDurationMin));
  await saveExercise(page, firstName);

  // exact maxima
  await openExerciseForm(page);
  await fillExerciseForm(page, {
    name: secondName,
    duration: RULES.exerciseDurationMax,
    distance: RULES.exerciseDistanceMax,
  });
  await saveExercise(page, secondName);

  expect(await exerciseCount(page)).toBe(before + 2);
  await capture(page, testInfo, 'a', 'TRN_TC04', 'duration-distance-boundaries');
});

test('TRN_TC05 exercise name required and unique per trainer', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openExercises(page);

  const before = await exerciseCount(page);
  const existing = (await page.locator('h3').first().innerText()).trim();

  await openExerciseForm(page);
  // UI-D15 (known cosmetic): a truly blank required name is stopped by Chromium
  // native validation, so assert it is blocked instead of expecting the styled error.
  const nameInput = page.getByLabel('Exercise name');
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();
  expect(await nameInput.evaluate((el) => el.validity.valid)).toBe(false);
  expect(await nameInput.evaluate((el) => el.validationMessage)).not.toBe('');

  // Whitespace-only reaches the app validator and shows the styled error.
  await nameInput.fill('   ');
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();
  await expect(page.getByText('Enter an exercise name.', { exact: true })).toBeVisible();

  await nameInput.fill(`  ${existing.toLowerCase()}  `);
  await page.getByRole('button', { name: 'Save exercise', exact: true }).click();
  await expect(page.getByText(new RegExp(`An exercise named '.*' already exists for this trainer!`))).toBeVisible();
  await expect(page.getByRole('heading', { name: existing, exact: true })).toHaveCount(1);
  expect(await exerciseCount(page)).toBe(before);

  await capture(page, testInfo, 'a', 'TRN_TC05', 'blank-duplicate');
});

test('TRN_TC06 create a course with an ordered exercise rotation', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  const courseName = nameFor('TRN_TC06', 'rotation');
  const goal = 'Three-step M02 relative rotation goal';

  const exercises = [];
  for (const letter of ['A', 'B', 'C']) {
    exercises.push(await createExercise(page, { caseId: 'TRN_TC06', suffix: `ex-${letter}` }));
  }

  await openCourses(page);
  const before = await courseCount(page);
  await openCourseForm(page);
  await fillCourseBasics(page, { name: courseName, sessions: 6, goal });
  for (const exercise of exercises) await addExerciseByName(page, exercise);
  await expect(page.getByText(/^Exercise rotation \(3 selected\)$/)).toBeVisible();

  const rotationItems = page.locator('ol > li');
  await expect(rotationItems).toHaveCount(3);
  for (let index = 0; index < 3; index += 1) {
    await expect(rotationItems.nth(index).getByText(String(index + 1), { exact: true })).toBeVisible();
    await expect(rotationItems.nth(index)).toContainText(exercises[index]);
  }
  await saveCourse(page, courseName);

  expect(await courseCount(page)).toBe(before + 1);
  const card = courseCard(page, courseName);
  await expect(card).toContainText('6 sessions');
  await expect(card).toContainText(goal);
  await capture(page, testInfo, 'a', 'TRN_TC06', 'course-rotation');
});

test('TRN_TC07 course total sessions boundaries 1 and 60', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  const exerciseOne = await createExercise(page, { caseId: 'TRN_TC07', suffix: 'min' });
  const exerciseSixty = await createExercise(page, { caseId: 'TRN_TC07', suffix: 'max' });

  await openCourses(page);
  const before = await courseCount(page);
  const message = `Enter a whole number from ${RULES.courseSessionsMin} to ${RULES.courseSessionsMax}.`;

  // min-1
  await openCourseForm(page);
  const invalidLow = nameFor('TRN_TC07', 'zero');
  await fillCourseBasics(page, { name: invalidLow, sessions: RULES.courseSessionsMin - 1 });
  await addExerciseByName(page, exerciseOne);
  await expect(page.getByText(message, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(page.getByRole('heading', { name: invalidLow, exact: true })).toHaveCount(0);

  // max+1
  await page.getByLabel('Total sessions').fill(String(RULES.courseSessionsMax + 1));
  await expect(page.getByText(message, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  expect(await courseCount(page)).toBe(before);

  // minimum accepted
  const minName = nameFor('TRN_TC07', 'one');
  await page.getByLabel('Course name').fill(minName);
  await page.getByLabel('Total sessions').fill(String(RULES.courseSessionsMin));
  await saveCourse(page, minName);

  // maximum accepted
  const maxName = nameFor('TRN_TC07', 'sixty');
  await openCourseForm(page);
  await fillCourseBasics(page, { name: maxName, sessions: RULES.courseSessionsMax });
  await addExerciseByName(page, exerciseSixty);
  await saveCourse(page, maxName);

  expect(await courseCount(page)).toBe(before + 2);
  // UI-D11 cosmetic: assert the count value, not the singular/plural wording.
  await expect(courseCard(page, minName)).toContainText(`${RULES.courseSessionsMin} sessions`);
  await expect(courseCard(page, maxName)).toContainText(`${RULES.courseSessionsMax} sessions`);
  await capture(page, testInfo, 'a', 'TRN_TC07', 'session-boundaries');
});

test('TRN_TC08 course name required and unique per trainer', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  const existingCourse = (await (async () => {
    await openCourses(page);
    return (await page.locator('h3').first().innerText()).trim();
  })());

  const exercise = await createExercise(page, { caseId: 'TRN_TC08', suffix: 'rotation' });
  await openCourses(page);
  const before = await courseCount(page);

  await openCourseForm(page);
  await addExerciseByName(page, exercise);

  // UI-D15 (known cosmetic): a truly blank required name is stopped by Chromium
  // native validation; whitespace-only reaches the app validator.
  const courseNameInput = page.getByLabel('Course name');
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  expect(await courseNameInput.evaluate((el) => el.validity.valid)).toBe(false);
  await expect(page.getByRole('heading', { name: existingCourse, exact: true })).toHaveCount(1);

  await courseNameInput.fill('   ');
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(page.getByText('Enter a course name and a valid number of sessions.', { exact: true })).toBeVisible();

  await courseNameInput.fill(`  ${existingCourse.toLowerCase()}  `);
  await page.getByLabel('Total sessions').fill('6');
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(page.getByText(new RegExp(`A course named '.*' already exists!`))).toBeVisible();
  expect(await courseCount(page)).toBe(before);

  await capture(page, testInfo, 'a', 'TRN_TC08', 'blank-duplicate');
});

test('TRN_TC09 maximum 30 exercises per course', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  // Fresh exercise makes this attempt's rotation distinct from any earlier run
  // that used the same first N library exercises.
  const fresh = await createExercise(page, { caseId: 'TRN_TC09', suffix: 'fresh' });
  const maxExercises = RULES.courseExercisesMax;
  // The clean official seed has fewer than 31 exercises, so build the library up
  // through the UI before exercising the 30/31 boundary.
  await ensureExerciseLibraryCount(page, maxExercises + 1, 'TRN_TC09');
  await openCourses(page);
  const before = await courseCount(page);

  // 30 exercises: accepted on a 60-session course.
  const maxName = nameFor('TRN_TC09', 'max');
  await openCourseForm(page);
  await fillCourseBasics(page, { name: maxName, sessions: RULES.courseSessionsMax });
  await addExerciseByName(page, fresh);
  await addExercisesFromLibrary(page, maxExercises - 1);
  await expect(page.getByText(new RegExp(`^Exercise rotation \\(${maxExercises} selected\\)$`))).toBeVisible();
  await saveCourse(page, maxName);
  expect(await courseCount(page)).toBe(before + 1);

  // 31 exercises: refused.
  const tooManyName = nameFor('TRN_TC09', 'too-many');
  await openCourseForm(page);
  await fillCourseBasics(page, { name: tooManyName, sessions: RULES.courseSessionsMax });
  await addExerciseByName(page, fresh);
  await addExercisesFromLibrary(page, maxExercises);
  await expect(page.getByText(new RegExp(`^Exercise rotation \\(${maxExercises + 1} selected\\)$`))).toBeVisible();
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(
    page.getByText(`A course can have at most ${maxExercises} exercises.`, { exact: true }),
  ).toBeVisible();
  await expect(page.getByRole('heading', { name: tooManyName, exact: true })).toHaveCount(0);
  expect(await courseCount(page)).toBe(before + 1);

  await capture(page, testInfo, 'a', 'TRN_TC09', 'max-30');
});

test('TRN_TC10 rotation rules: empty, longer than sessions, duplicate, identical', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');

  // Three fresh exercises guarantee a rotation that no older course already uses.
  const rotation = [];
  for (const letter of ['A', 'B', 'C']) {
    rotation.push(await createExercise(page, { caseId: 'TRN_TC10', suffix: `rot-${letter}` }));
  }
  const baseName = nameFor('TRN_TC10', 'base');
  await openCourses(page);
  await openCourseForm(page);
  await fillCourseBasics(page, { name: baseName, sessions: 6, goal: 'Rotation base course' });
  for (const exercise of rotation) await addExerciseByName(page, exercise);
  await saveCourse(page, baseName);

  const before = await courseCount(page);

  // Empty rotation.
  await openCourseForm(page);
  const emptyName = nameFor('TRN_TC10', 'empty');
  await fillCourseBasics(page, { name: emptyName, sessions: 6 });
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(page.getByText('Select at least one exercise for the course rotation.', { exact: true })).toBeVisible();

  // More exercises than sessions (six over five).
  await page.getByLabel('Total sessions').fill('5');
  await addExercisesFromLibrary(page, 6);
  await expect(page.getByText(/A course cannot have more exercises \(6\) than sessions \(5\)\./)).toBeVisible();
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(page.getByText(/A course cannot have more exercises \(6\) than sessions \(5\)\./).first()).toBeVisible();
  expect(await selectedExercises(page)).toBe(6);

  // Clicking an already-selected card must not duplicate it.
  const selectedCard = exerciseLibrary(page).locator('div').filter({ hasText: /In course/ }).first();
  await selectedCard.click();
  expect(await selectedExercises(page)).toBe(6);

  // Identical A,B,C rotation is refused (UI-D16 duplicate notice is known/waived).
  await page.getByRole('button', { name: 'Clear all', exact: true }).click();
  await page.getByLabel('Total sessions').fill('6');
  for (const exercise of rotation) await addExerciseByName(page, exercise);
  await page.getByLabel('Course name').fill(nameFor('TRN_TC10', 'identical'));
  await page.getByRole('button', { name: 'Save course', exact: true }).click();
  await expect(page.getByText(new RegExp(`Course '${baseName}' already has exactly these exercises in the same order!`))).toBeVisible();
  expect(await courseCount(page)).toBe(before);

  await capture(page, testInfo, 'a', 'TRN_TC10', 'rotation-rules');
});

test('TRN_TC11 course detail modal and enrollment shortcut', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openCourses(page);

  let courseName = nameFor('TRN_TC06', 'rotation');
  if ((await courseCard(page, courseName).count()) === 0) {
    const created = await createThreeExerciseCourse(page, 'TRN_TC11', 'fallback');
    courseName = created.courseName;
  }

  const card = courseCard(page, courseName);
  await card.getByRole('button', { name: 'View course details', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: courseName, exact: true })).toBeVisible();
  await expect(dialog).toContainText(/Total sessions: \d+/);
  await expect(dialog).toContainText(/Exercises \(3\)/);
  await expect(dialog.getByText('Exercise 1', { exact: true })).toBeVisible();
  await expect(dialog.getByText('Exercise 2', { exact: true })).toBeVisible();
  await expect(dialog.getByText('Exercise 3', { exact: true })).toBeVisible();
  const enroll = dialog.getByRole('link', { name: 'Enroll a group in this course', exact: true });
  await expect(enroll).toHaveAttribute('href', /\/trainer\/course-runs\/new\?courseId=\d+/);

  await capture(page, testInfo, 'a', 'TRN_TC11', 'course-detail');
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
});

test('TRN_TC12 course library role and ownership checks', async ({ page, browser }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openCourses(page);
  const trainer1Course = (await page.locator('h3').first().innerText()).trim();
  await page.getByRole('tab', { name: 'Exercises', exact: true }).click();
  await expect(page.getByText(/^Exercise library \(\d+\)$/)).toBeVisible();
  const trainer1Exercise = (await page.locator('h3').first().innerText()).trim();

  const roles = [
    { email: 'admin@rtms.com', landing: '/manager', label: 'manager' },
    { email: 'groom1@rtms.com', landing: '/groom', label: 'groom' },
    { email: 'owner@rtms.com', landing: '/owner', label: 'owner' },
    { email: 'vet1@rtms.com', landing: '/veterinarian', label: 'vet' },
  ];

  for (const role of roles) {
    const session = await newRoleSession(browser, role.email);
    try {
      await gotoAuthed(session.page, '/trainer/courses', role.email);
      await session.page.waitForURL((url) => !url.pathname.startsWith('/trainer'), { timeout: 30_000 });
      expect(new URL(session.page.url()).pathname.startsWith(role.landing)).toBe(true);
      await expect(session.page.getByRole('heading', { name: 'Course library', exact: true })).toHaveCount(0);
      await expect(session.page.getByRole('button', { name: 'Create course', exact: true })).toHaveCount(0);
    } finally {
      await session.context.close();
    }
  }

  const trainer2 = await newRoleSession(browser, 'trainer2@rtms.com');
  try {
    await gotoAuthed(trainer2.page, '/trainer/courses', 'trainer2@rtms.com');
    await expect(trainer2.page.getByRole('heading', { name: 'Course library', exact: true }).first()).toBeVisible();
    await expect(trainer2.page.getByText(/^Courses \(\d+\)$/)).toBeVisible();
    await expect(trainer2.page.getByRole('heading', { name: trainer1Course, exact: true })).toHaveCount(0);
    await trainer2.page.getByRole('tab', { name: 'Exercises', exact: true }).click();
    await expect(trainer2.page.getByText(/^Exercise library \(\d+\)$/)).toBeVisible();
    await expect(trainer2.page.getByRole('heading', { name: trainer1Exercise, exact: true })).toHaveCount(0);
    await capture(trainer2.page, testInfo, 'a', 'TRN_TC12', 'trainer2-isolation');
  } finally {
    await trainer2.context.close();
  }
});

test('TRN_TC13 groups screen, cards, horse library and disbanded section', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);

  const groupHeadings = page.locator('h3');
  expect(await groupHeadings.count()).toBeGreaterThan(0);
  const firstName = (await groupHeadings.first().innerText()).trim();
  const firstCard = activeGroupCard(page, firstName);
  await expect(firstCard).toContainText(/\d+\/6 horses/);
  await expect(firstCard).toContainText('Current Course');
  await expect(firstCard).toContainText(/Horses \(\d+\/6\)/);

  const library = horseLibrary(page);
  await expect(library.getByText('Horse Library', { exact: true })).toBeVisible();
  await expect(library.getByText(/^Available \(\d+\)$/)).toBeVisible();
  await expect(library.getByText(/^Blocked \(\d+\)$|^In a group \(\d+\)$/).first()).toBeVisible();

  const available = library.locator('div[draggable="true"]').first();
  const horseName = (await available.locator('p').first().innerText()).trim();
  await library.getByLabel('Search horses').fill(horseName);
  await expect(library.getByText(horseName, { exact: true })).toBeVisible();
  await library.getByLabel('Search horses').fill('');

  const disbanded = await countDisbanded(page);
  if (disbanded > 0) {
    await expandDisbanded(page);
    await expect(page.getByRole('heading', { level: 4 }).first()).toBeVisible();
  }

  expect(await noHorizontalScroll(page)).toBe(true);
  await page.setViewportSize({ width: 1280, height: 1000 });
  await page.waitForTimeout(300);
  expect(await noHorizontalScroll(page)).toBe(true);
  await page.setViewportSize({ width: 1440, height: 1000 });

  await capture(page, testInfo, 'a', 'TRN_TC13', 'groups-layout');
});

test('TRN_TC14 create an empty training group', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  const name = nameFor('TRN_TC14', 'empty');

  await page.getByRole('button', { name: 'New group', exact: true }).click();
  const dialog = page.getByRole('dialog');
  const save = dialog.getByRole('button', { name: 'Save', exact: true });
  await expect(save).toBeDisabled();
  await dialog.locator('input').first().fill(name);
  await expect(save).toBeEnabled();
  try {
    await save.click();
    await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
    const card = activeGroupCard(page, name);
    await expect(card).toContainText(`0/${RULES.groupCapacity} horses`);
    await expect(card).toContainText('No active course');
    await capture(page, testInfo, 'a', 'TRN_TC14', 'empty-group');
  } finally {
    await cleanupGroups(page, [name]);
  }
});

test('TRN_TC15 group name maximum length 100', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);

  const prefix = `M02_TRN_TC15_${RUN_ID}_${ATTEMPT}_`;
  const longName = (prefix + 'X'.repeat(RULES.groupNameMax)).slice(0, RULES.groupNameMax);
  expect(longName).toHaveLength(RULES.groupNameMax);

  await page.getByRole('button', { name: 'New group', exact: true }).click();
  const dialog = page.getByRole('dialog');
  const input = dialog.locator('input').first();
  await input.fill(longName);
  await input.pressSequentially('Y');
  expect(await input.inputValue()).toHaveLength(RULES.groupNameMax);
  expect(await input.inputValue()).toBe(longName);

  try {
    await dialog.getByRole('button', { name: 'Save', exact: true }).click();
    const heading = page.getByRole('heading', { name: longName, exact: true });
    await expect(heading).toBeVisible();
    expect((await heading.innerText()).length).toBe(RULES.groupNameMax);
    await capture(page, testInfo, 'a', 'TRN_TC15', 'max-length');
  } finally {
    await cleanupGroups(page, [longName]);
  }
});

test('TRN_TC16 blank and duplicate group names on create and rename', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  const groupA = nameFor('TRN_TC16', 'A');
  const groupB = nameFor('TRN_TC16', 'B');

  try {
    await createGroup(page, groupA);
    await createGroup(page, groupB);

    // Blank and whitespace-only names keep Save disabled.
    await page.getByRole('button', { name: 'New group', exact: true }).click();
    let dialog = page.getByRole('dialog');
    const save = dialog.getByRole('button', { name: 'Save', exact: true });
    await expect(save).toBeDisabled();
    await dialog.locator('input').first().fill('   ');
    await expect(save).toBeDisabled();

    // Case-variant duplicate create is refused.
    await dialog.locator('input').first().fill(`  ${groupA.toLowerCase()}  `);
    await expect(save).toBeEnabled();
    await save.click();
    await expect(dialog.getByText(new RegExp(`You already have a group named '.*'!`))).toBeVisible();
    await dialog.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(page.getByRole('heading', { name: groupA, exact: true })).toHaveCount(1);

    // Case-variant duplicate rename is refused and leaves the name unchanged.
    await activeGroupCard(page, groupB).getByRole('button', { name: 'Rename group', exact: true }).click();
    dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: 'Rename group', exact: true })).toBeVisible();
    await dialog.locator('input').first().fill(groupA.toLowerCase());
    await dialog.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(dialog.getByText(new RegExp(`You already have a group named '.*'!`))).toBeVisible();
    await dialog.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(page.getByRole('heading', { name: groupB, exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: groupA.toLowerCase(), exact: true })).toHaveCount(0);

    await capture(page, testInfo, 'a', 'TRN_TC16', 'blank-duplicate');
  } finally {
    await cleanupGroups(page, [groupA, groupB]);
  }
});

test('TRN_TC17 rename a group', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  const original = nameFor('TRN_TC17', 'original');
  const renamed = nameFor('TRN_TC17', 'renamed');
  const upper = renamed.toUpperCase();
  expect(upper).not.toBe(renamed);

  try {
    await createGroup(page, original);
    await activeGroupCard(page, original).getByRole('button', { name: 'Rename group', exact: true }).click();
    let dialog = page.getByRole('dialog');
    await dialog.locator('input').first().fill(renamed);
    await dialog.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByRole('heading', { name: renamed, exact: true })).toBeVisible();

    await gotoAuthed(page, '/trainer/groups');
    await expect(page.getByRole('heading', { name: renamed, exact: true })).toBeVisible();

    await activeGroupCard(page, renamed).getByRole('button', { name: 'Rename group', exact: true }).click();
    dialog = page.getByRole('dialog');
    await dialog.locator('input').first().fill(upper);
    await dialog.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByRole('heading', { name: upper, exact: true })).toBeVisible();
    await capture(page, testInfo, 'a', 'TRN_TC17', 'renamed');
  } finally {
    await cleanupGroups(page, [original, renamed, upper]);
  }
});

test('TRN_TC18 add a horse through the dialog and by drag and drop', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer3@rtms.com');
  await openGroups(page);
  await cleanupStaleSuiteGroups(page);
  const groupName = nameFor('TRN_TC18', 'group');

  try {
    await createGroup(page, groupName);
    await addHorseViaDialog(page, groupName, 'M02FIX_ALPHA', 1);
    await expect(activeGroupCard(page, groupName)).toContainText('M02FIX_ALPHA');

    await dragLibraryHorseToGroup(page, 'M02FIX_BRAVO', groupName);
    const card = activeGroupCard(page, groupName);
    await expect(card).toContainText(`2/${RULES.groupCapacity} horses`);
    await expect(card).toContainText('M02FIX_ALPHA');
    await expect(card).toContainText('M02FIX_BRAVO');

    const library = horseLibrary(page);
    await expect(library.locator('div[draggable="true"]').filter({ hasText: 'M02FIX_ALPHA' })).toHaveCount(0);
    await expect(library.locator('div[draggable="true"]').filter({ hasText: 'M02FIX_BRAVO' })).toHaveCount(0);
    await expect(library).toContainText('M02FIX_ALPHA');
    await expect(library).toContainText('M02FIX_BRAVO');
    await capture(page, testInfo, 'a', 'TRN_TC18', 'dialog-and-drag');
  } finally {
    await cleanupGroups(page, [groupName]);
  }
});

test('TRN_TC19 horse membership rules: no groom, no stall, blocked, already grouped', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer3@rtms.com');
  await openGroups(page);
  await cleanupStaleSuiteGroups(page);
  const groupName = nameFor('TRN_TC19', 'group');

  try {
    await createGroup(page, groupName);
    let dialog = await openAddHorse(page, groupName);
    await expect(dialog.getByText('M02FIX_NO_STALL', { exact: true })).toHaveCount(0);
    await expect(dialog.getByRole('button', { name: 'Add', exact: true }).first()).toBeDisabled();

    const tooltipFor = async (horseName, expected) => {
      await page.mouse.move(10, 10);
      await page.waitForTimeout(500);
      if (!(await dialog.isVisible().catch(() => false))) dialog = await openAddHorse(page, groupName);
      const row = dialog.locator('li').filter({ hasText: horseName }).first();
      await expect(row).toBeVisible();
      await expect(row.getByRole('button', { name: 'Add', exact: true })).toBeDisabled();
      const trigger = row.locator('span.cursor-help');
      await trigger.hover();
      await page.waitForTimeout(900);
      if ((await page.getByRole('tooltip').filter({ hasText: expected }).count()) === 0) {
        const box = await trigger.boundingBox();
        if (box) await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      }
      await expect(page.getByRole('tooltip').filter({ hasText: expected })).toBeVisible();
      await dialog.getByRole('button', { name: 'Close', exact: true }).click();
      await expect(dialog).toBeHidden();
    };

    await tooltipFor('M02FIX_NO_GROOM', 'Its stall has no groom');
    await tooltipFor('M02FIX_BLOCKED', 'Not allowed to train');
    await page.mouse.move(10, 10);
    dialog = await openAddHorse(page, groupName);
    const amnesiaRow = dialog.locator('li').filter({ hasText: 'AMNESIA' }).first();
    await expect(amnesiaRow).toBeVisible();
    await expect(amnesiaRow.getByRole('button', { name: 'Add', exact: true })).toBeDisabled();
    await amnesiaRow.locator('span.cursor-help').hover();
    await page.waitForTimeout(900);
    if ((await page.getByRole('tooltip').filter({ hasText: 'Already in another group' }).count()) === 0) {
      const box = await amnesiaRow.locator('span.cursor-help').boundingBox();
      if (box) await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
    }
    await expect(page.getByRole('tooltip').filter({ hasText: 'Already in another group' })).toBeVisible();

    await capture(page, testInfo, 'a', 'TRN_TC19', 'candidate-reasons');
    await dialog.getByRole('button', { name: 'Close', exact: true }).click();
  } finally {
    await cleanupGroups(page, [groupName]);
  }
});

test('TRN_TC20 group capacity of six horses', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer3@rtms.com');
  await openGroups(page);
  await cleanupStaleSuiteGroups(page);
  const groupName = nameFor('TRN_TC20', 'capacity');
  const fixtureHorses = [
    'M02FIX_ALPHA',
    'M02FIX_BRAVO',
    'M02FIX_CHARLIE',
    'M02FIX_DELTA',
    'M02FIX_ECHO',
    'M02FIX_FOXTROT',
  ];

  try {
    await createGroup(page, groupName);
    for (let index = 0; index < RULES.groupCapacity; index += 1) {
      await addHorseViaDialog(page, groupName, fixtureHorses[index], index + 1);
    }

    const fullCard = activeGroupCard(page, groupName);
    await expect(fullCard).toContainText(`${RULES.groupCapacity}/${RULES.groupCapacity} horses`);
    await expect(fullCard.getByText('Full', { exact: true })).toBeVisible();
    await expect(fullCard.getByRole('button', { name: 'Add horse', exact: true })).toBeDisabled();
    await capture(page, testInfo, 'a', 'TRN_TC20', 'full-six');

    await removeMember(page, groupName, 'M02FIX_FOXTROT');
    await expect(activeGroupCard(page, groupName)).toContainText(`5/${RULES.groupCapacity} horses`);
    await expect(
      activeGroupCard(page, groupName).getByRole('button', { name: 'Add horse', exact: true }),
    ).toBeEnabled();

    await addHorseViaDialog(page, groupName, 'M02FIX_GOLF', RULES.groupCapacity);
    const refilled = activeGroupCard(page, groupName);
    await expect(refilled).toContainText(`${RULES.groupCapacity}/${RULES.groupCapacity} horses`);
    await expect(refilled.getByText('Full', { exact: true })).toBeVisible();
    await capture(page, testInfo, 'a', 'TRN_TC20', 'seventh-after-remove');
  } finally {
    await cleanupGroups(page, [groupName]);
  }
});

test('TRN_TC21 remove a horse and move a horse between groups', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  await cleanupStaleSuiteGroups(page);
  const groupA = nameFor('TRN_TC21', 'A');
  const groupB = nameFor('TRN_TC21', 'B');
  let horseName;

  try {
    await createGroup(page, groupA);
    await createGroup(page, groupB);
    horseName = await firstAvailableHorse(page);

    await dragLibraryHorseToGroup(page, horseName, groupA);
    await expect(activeGroupCard(page, groupA)).toContainText(`1/${RULES.groupCapacity} horses`);
    await expect(activeGroupCard(page, groupA)).toContainText(horseName);

    await removeMember(page, groupA, horseName);
    await expect(activeGroupCard(page, groupA)).toContainText(`0/${RULES.groupCapacity} horses`);
    await expect(
      horseLibrary(page).locator('div[draggable="true"]').filter({ hasText: horseName }),
    ).toHaveCount(1);

    await dragLibraryHorseToGroup(page, horseName, groupA);
    await moveMember(page, groupA, groupB, horseName);
    await expect(activeGroupCard(page, groupA)).toContainText(`0/${RULES.groupCapacity} horses`);
    await expect(activeGroupCard(page, groupB)).toContainText(`1/${RULES.groupCapacity} horses`);
    await expect(activeGroupCard(page, groupB)).toContainText(horseName);
    await expect(activeGroupCard(page, groupA).locator('li').filter({ hasText: horseName })).toHaveCount(0);

    // Dropping a member onto its current group is a no-op: no dialog, no duplicate.
    const member = activeGroupCard(page, groupB).locator('li').filter({ hasText: horseName }).first();
    await dragLocator(page, member, activeGroupCard(page, groupB));
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(activeGroupCard(page, groupB)).toContainText(`1/${RULES.groupCapacity} horses`);
    await expect(activeGroupCard(page, groupB).locator('li').filter({ hasText: horseName })).toHaveCount(1);

    await capture(page, testInfo, 'a', 'TRN_TC21', 'remove-move');
  } finally {
    await cleanupGroups(page, [groupA, groupB]);
  }
});

test('TRN_TC22 disband a group with no course history', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  await cleanupStaleSuiteGroups(page);
  const groupName = nameFor('TRN_TC22', 'no-history');
  const horseName = await firstAvailableHorse(page);
  const beforeDisbanded = await countDisbanded(page);

  await createGroup(page, groupName);
  await addHorseViaDialog(page, groupName, horseName, 1);

  await activeGroupCard(page, groupName).getByRole('button', { name: 'Disband group', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: 'Disband group', exact: true })).toBeVisible();
  await expect(dialog).toContainText(/no history and will be removed/i);
  await expect(dialog).toContainText('Its horses leave the group.');
  await dialog.getByRole('button', { name: 'Disband', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeHidden();

  await expect(page.getByRole('heading', { name: groupName, exact: true })).toHaveCount(0);
  expect(await countDisbanded(page)).toBe(beforeDisbanded);

  const library = horseLibrary(page);
  await library.getByLabel('Search horses').fill(horseName);
  await expect(library.locator('div[draggable="true"]').filter({ hasText: horseName })).toHaveCount(1);
  await library.getByLabel('Search horses').fill('');
  await expect(page.getByText(groupName, { exact: true })).toHaveCount(0);

  await capture(page, testInfo, 'a', 'TRN_TC22', 'disband-no-history');
});

test('TRN_TC23 disband a group with course history', async ({ page }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  await cleanupStaleSuiteGroups(page);
  const groupName = nameFor('TRN_TC23', 'history');
  const horseName = await firstAvailableHorse(page);

  await createGroup(page, groupName);
  await addHorseViaDialog(page, groupName, horseName, 1);

  const exerciseName = await createExercise(page, { caseId: 'TRN_TC23', suffix: 'exercise' });
  const courseName = nameFor('TRN_TC23', 'course');
  await openCourses(page);
  await openCourseForm(page);
  await fillCourseBasics(page, { name: courseName, sessions: TC23_SESSIONS, goal: 'Disband history course' });
  await addExerciseByName(page, exerciseName);
  await saveCourse(page, courseName);

  await gotoAuthed(page, '/trainer/course-runs/new');
  await page.getByLabel('Group').selectOption({ label: `${groupName} (1)` });
  await page.getByLabel('Course').selectOption({ label: `${courseName} (${TC23_SESSIONS})` });
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expect(page.getByRole('heading', { name: `2. Dates for ${groupName}`, exact: true })).toBeVisible();
  const createRun = page.getByRole('button', { name: 'Create course run', exact: true });
  await expect(createRun).toBeEnabled();
  await createRun.click();
  await page.waitForURL(/\/trainer\/course-runs\/\d+/, { timeout: 30_000 });
  const runUrl = page.url();
  await expect(page.getByText('Upcoming', { exact: true }).first()).toBeVisible();

  await openGroups(page);
  await activeGroupCard(page, groupName).getByRole('button', { name: 'Disband group', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading', { name: 'Disband group', exact: true })).toBeVisible();
  await expect(dialog).toContainText(/1 course run/i);
  await expect(dialog).toContainText(new RegExp(`${TC23_SESSIONS} sessions`));
  await dialog.getByRole('button', { name: 'Disband', exact: true }).click();

  await expect(page.getByRole('heading', { name: groupName, exact: true })).toHaveCount(0);
  await expandDisbanded(page);
  const disbanded = disbandedGroupCard(page, groupName);
  await expect(disbanded).toBeVisible();
  await expect(disbanded).toContainText('Disbanded');
  await expect(disbanded.getByRole('button', { name: /Rename group|Add horse|Disband group/ })).toHaveCount(0);
  await expect(disbanded.getByRole('button', { name: /Horses when disbanded/ })).toBeVisible();
  await capture(page, testInfo, 'a', 'TRN_TC23', 'disbanded-card');

  await gotoAuthed(page, runUrl);
  await expect(page.getByText('Cancelled', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Disbanded', { exact: true }).first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cancel course run', exact: true })).toHaveCount(0);
  const sessionRows = page.locator('table tbody tr');
  expect(await sessionRows.count()).toBeGreaterThanOrEqual(TC23_SESSIONS);
  for (let index = 0; index < TC23_SESSIONS; index += 1) {
    await expect(sessionRows.nth(index)).toContainText('Cancelled');
  }
  await capture(page, testInfo, 'a', 'TRN_TC23', 'run-cancelled');
});

test('TRN_TC24 group role and trainer ownership checks', async ({ page, browser }, testInfo) => {
  await loginAs(page, 'trainer1@rtms.com');
  await openGroups(page);
  const trainer1Group = (await page.locator('h3').first().innerText()).trim();

  const roles = [
    { email: 'admin@rtms.com', landing: '/manager', label: 'manager' },
    { email: 'groom1@rtms.com', landing: '/groom', label: 'groom' },
    { email: 'owner@rtms.com', landing: '/owner', label: 'owner' },
    { email: 'vet1@rtms.com', landing: '/veterinarian', label: 'vet' },
  ];

  for (const role of roles) {
    const session = await newRoleSession(browser, role.email);
    try {
      await gotoAuthed(session.page, '/trainer/groups', role.email);
      await session.page.waitForURL((url) => !url.pathname.startsWith('/trainer'), { timeout: 30_000 });
      expect(new URL(session.page.url()).pathname.startsWith(role.landing)).toBe(true);
      await expect(session.page.getByRole('heading', { name: 'Groups', exact: true })).toHaveCount(0);
      await expect(session.page.getByRole('button', { name: 'New group', exact: true })).toHaveCount(0);
    } finally {
      await session.context.close();
    }
  }

  const trainer2 = await newRoleSession(browser, 'trainer2@rtms.com');
  try {
    await gotoAuthed(trainer2.page, '/trainer/groups', 'trainer2@rtms.com');
    await expect(trainer2.page.getByRole('heading', { name: 'Groups', exact: true })).toBeVisible();
    await expect(trainer2.page.getByRole('button', { name: 'New group', exact: true })).toBeVisible();
    await expect(trainer2.page.getByRole('heading', { name: trainer1Group, exact: true })).toHaveCount(0);
    await expect(trainer2.page.getByText(trainer1Group, { exact: true })).toHaveCount(0);
    await capture(trainer2.page, testInfo, 'a', 'TRN_TC24', 'trainer2-isolation');
  } finally {
    await trainer2.context.close();
  }
});
