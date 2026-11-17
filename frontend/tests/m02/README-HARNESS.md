# M02 relative UI harness — agent contract

Environment (already prepared by the orchestrator):

- Repo: `/home/passla1/project/racehorse-training-system` (branch `swt`)
- Frontend: `http://localhost:3000`, backend: `http://localhost:8080` (already running)
- DB: `rtms_system_test` (already seeded with `M02_test_data_seed.sql`; do not write SQL/API)
- Node: `export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"` (v22)
- Playwright + Chromium installed in `frontend/`
- Run ID and fixture IDs: `/tmp/m02-fixtures.env`
- Evidence root: `docs/bmad/testing/evidence/relative/agent-<x>/`

## Files

- Config: `frontend/playwright.m02.config.js`
- Helpers: `frontend/tests/m02/lib/helpers.js`
- Summariser: `frontend/tests/m02/lib/summarize.js`
- Write exactly one spec per agent: `frontend/tests/m02/agent-a.spec.js` (or b/c).

## Helper API

```js
const {
  RULES, RUN_ID, login, logout, newSession, goto, capture, shot, bodyText,
  todayIso, yesterdayIso, tomorrowIso, plusDaysIso, uniqueName, tableRows,
} = require('./lib/helpers');
```

- `login(page, email)` — real UI login (password `admin123`).
- `newSession(browser, email)` — separate cookie jar for role checks; `email = null` for anonymous.
- `capture(page, testInfo, 'a', 'TRN_TC01', 'course-library')` — full-page screenshot,
  attaches + annotates it. Returns the file name. **Call this in every test.**
- `RULES` — business boundaries (`groupCapacity: 6`, `groupNameMax: 100`, etc.).
- `uniqueName('TRN_TC14', 'empty')` — `M02_TRN_TC14_<RUN_ID>_empty`.
- Dates: `todayIso()`, `yesterdayIso()`, `plusDaysIso(365)`.

## Test rules

1. Each test title must start with `TRN_TCxx`, e.g.
   `test('TRN_TC01 course library layout', async ({ page }, testInfo) => { ... })`.
2. Run in file order, one worker. Do **not** use `test.describe.configure({ mode: 'serial' })`;
   a known failure must not skip later cases.
3. UI-only: no `fetch`/`request`/axios to the app API, no SQL, no DB driver in the spec.
4. No hardcoded dates, IDs, counts or magic values. Use helpers/`RULES`, discover IDs
   from links/rows, or use the fixture IDs given in your prompt.
5. Every Pass needs `capture()` evidence. For known fails, capture the actual state
   **before** the failing assertion.
6. Known fails: TC53 = UI-D02, TC62 = UI-D01, TC63 = UI-D03. Let the expected-vs-actual
   assertion fail; do not mark them Pass.
7. TC58/TC61 are Pass. Do not click/test the Copilot floating button (UI-D08 waived).
8. Never edit application code. If a new defect appears, capture screenshot + steps +
   expected/actual in the report; do not fix it.

## Run

```bash
cd frontend
export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"
set -a; . /tmp/m02-fixtures.env; set +a
M02_AGENT=a PLAYWRIGHT_BASE_URL=http://localhost:3000 \
  npx playwright test -c playwright.m02.config.js tests/m02/agent-a.spec.js \
  --workers=1 --output=../docs/bmad/testing/evidence/relative/agent-a/playwright-output
node tests/m02/lib/summarize.js --agent A --run-id "$RUN_ID"
```

Then write `docs/bmad/testing/evidence/relative/agent-a/agent-a-report.md` with:
environment, commit, RUN_ID, case/status/evidence/note table, failures, defects,
and summary counts. Do not claim Pass without an image file on disk.
