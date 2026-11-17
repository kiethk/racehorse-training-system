# M02 — Head Trainer UI Test: Reproduction Guide

This guide reproduces the UI system-test results for the lecturer. The main run has 67 in-scope cases (64 Pass, 3 Fail); TC68 is documented as out of scope in Appendix A. The test run used the official demo seed in a separate database, not hand-injected rows.

## 1. Environment

| Item | Value |
|---|---|
| Frontend | `http://localhost:3000` |
| Backend | `http://localhost:8080` |
| Database | `rtms_system_test` on `localhost:5432` |
| Accounts | `trainer1@rtms.com`, `trainer2@rtms.com`, `trainer3@rtms.com`, `owner@rtms.com`, `groom1@rtms.com`, `admin@rtms.com` — password `admin123` |
| Seed | `backend/scripts/seed-database.ps1` (official demo seed) |
| UI driver | Playwright + installed Chrome (`channel: 'chrome'`), 1440x1000 |
| Evidence | `evidence/screenshots/ui-1..3` (103 screenshots) |
| Specs | `evidence/specs/ui-1..3` |

## 2. Reset the seed

Stop the backend, then run from `backend`:

```powershell
.\scripts\seed-database.ps1 -Yes -Database rtms_system_test -User postgres -Password 1 `
  -PsqlPath "C:\Users\Phu\AppData\Roaming\DBeaverData\drivers\clients\postgresql\win\17\psql.exe"
```

The seed prints the row counts and `All 68 business-rule checks passed.` It wipes and recreates the demo data, including 55 admissions, 11 groups, 61 course runs, 429 sessions and 1,565 workouts.

## 3. Start the app

Backend (from `backend`):

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/rtms_system_test'
$env:SPRING_DATASOURCE_USERNAME='postgres'
$env:SPRING_DATASOURCE_PASSWORD='1'
.\mvnw.cmd spring-boot:run
```

If `target` contains stale resources from another branch, run `.\mvnw.cmd clean` first. The `pick up` schema must have `prescriptions.withdrawal_days`; the current migration set does.

Frontend (from `frontend`):

```powershell
npm run dev
```

## 4. Run the UI suites

Each suite needs `@playwright/test` and the config that uses installed Chrome. Example:

```powershell
$dir="$env:TEMP\rtms-ui-1"
Copy-Item "D:\swp\racehorse-training-system\docs\bmad\testing\evidence\specs\ui-1\*" $dir -Force
cd $dir
npm init -y
npm install --no-audit --no-fund @playwright/test
npx playwright test --reporter=line
```

Use `ui-1`, `ui-2`, `ui-3` for the three suites. For TC18–TC20, apply `evidence/fixtures/m02_group_fixtures.sql` after the official seed; it adds Area F, seven eligible ungrouped horses, one no-groom stall, one no-stall horse and one blocked horse. Re-seed between suites when replaying because the suites consume seeded admissions, held works and create records.

## 5. Reproduce the three failing cases

### TC53 — readiness validation presentation

1. Log in as `trainer1@rtms.com`.
2. Open `Operations > Admissions`; click **Evaluate** on a pending admission.
3. Click **Submit evaluation** with no scores.
4. Observe: one form-level alert, not three inline field errors.
5. Add only Conformation and submit; only Temperament is reported. Add Temperament; only Gait quality is reported.
6. Expected by the case: three inline errors naming all missing scores at once.

Screenshot: `evidence/screenshots/ui-3/TRN_TC53.png`.

### TC62 — incident attachment image

1. Log in as `trainer1@rtms.com`.
2. Open `Operations > Incidents`.
3. Click **Details** on a report that has an image (e.g. FC NAJAA).
4. Observe `The image could not be loaded.` in the modal; the same failure appears on the horse's Incident Reports tab.
5. Expected: the protected image loads.

Screenshot: `evidence/screenshots/ui-3/TRN_TC62.png`.

### TC63 — LOW incident routing

1. Log in as `trainer1@rtms.com`.
2. Open `Horses` → `FELICITY` (#18) → `Incident Reports`.
3. Click **Report Incident to Vet**.
4. Enter a title and description, choose **Low**, send.
5. Observe: the report is immediately assigned to Emily Wilson and starts **Under Vet Review**.
6. Expected by the case: `Reported — Awaiting Veterinary Review` with `Awaiting assignment`.

Screenshot: `evidence/screenshots/ui-3/TRN_TC63.png`.

## 6. Why the four cases are Blocked

| Case | Why it cannot be executed with the official seed through the UI |
|---|---|
| TC68 | The expected `403 FORBIDDEN` is a backend response. The UI only redirects owner/groom and hides trainer actions; the response code is not observable through the UI. This case belongs in an API/security suite, not a UI system-test suite. |

`Blocked` is a valid ISTQB result when the precondition cannot be met without changing the test environment. The report does not mark these as Pass. TC18–TC20 were retested with the M02 fixture SQL and now pass; they are in the 67-case main run. TC68 is kept in the `Out of Scope` sheet and Appendix A. The executed count is **67** (64 Pass + 3 Fail), so the minimum 60 executed cases is met.

## 7. Defect status

14 Jira-loggable UI findings in `M02-head-trainer-defect-list.md` are **open / not fixed**. UI-D09 is an automation-only observation (3 px drag) and is not logged; UI-D10 is closed by decision (the 2000-character cap is intended, option A). The findings are test findings, not changes to the application. The only code change made outside this UI run was the earlier API role-guard fix in `AdmissionTrainerReviewController`; it is unrelated to these UI defects and remains uncommitted.
