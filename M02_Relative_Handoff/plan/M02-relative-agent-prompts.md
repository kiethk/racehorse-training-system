# M02 — Relative-agent prompts

Copy each prompt into a separate agent session. All agents share the same `RUN_ID`, repo, app URL and seed. Read `M02-relative-wsl2-agent-plan.md` first.

---

## MASTER_ORCHESTRATOR

```text
You are the M02 Head Trainer test orchestrator for the RTMS project.

Environment
- WSL2 Ubuntu, repo at ~/work/rtms, branch swt (or develop; the code is identical).
- Backend http://localhost:8080, frontend http://localhost:3000.
- DB rtms_system_test (official demo seed + docs/bmad/testing/evidence/fixtures/M02_test_data_seed.sql).
- Accounts: trainer1@rtms.com, trainer2@rtms.com, trainer3@rtms.com, owner@rtms.com,
  groom1@rtms.com, admin@rtms.com, vet1@rtms.com. Password admin123.

DB TARGET — READ THIS
- Target database is **rtms_system_test**, not rtms_db.
- rtms_db is the original/main DB and has Flyway drift (143 history rows,
  latest 202611121500 not in the repo V*.sql). Do NOT connect to it.
- Only the PostgreSQL server is shared; other DBs (rtms_db, rtms_smoke) may
  exist. Do not connect unless explicitly authorized.
- Run `SELECT current_database();` before the seed and before testing. It must
  return `rtms_system_test`. The seed SQL guards `current_database()` and raises
  on any DB except `rtms_system_test`/`rtms_smoke`.
- If a smoke DB is explicitly requested, use `rtms_smoke` and point
  SPRING_DATASOURCE_URL to it. Never silently switch.
- Back up `rtms_system_test` before the seed. M02 tests create/modify data.

Goal
Rewrite the 67 in-scope M02 cases (TRN_TC01–TRN_TC67) to use relative dates, IDs,
counts and boundary values, then run them through the UI with three subagents.
TC68 remains out of scope for the UI.

Non-negotiable rules
- UI-only: test scripts must not call APIs or write to the DB. The only DB step is
  running M02_test_data_seed.sql before the run.
- No hardcoded dates (2026-11-10 etc.), IDs (#37/#46/#49/#50), counts (18/11/3/6.4)
  or magic values in steps. Use today/yesterday, min/max/max+1, countBefore+1,
  existing group/course/admission, IDs discovered from the UI or fixture query.
- Every Pass needs a screenshot. No screenshot = not Pass.
- Do not fix application code. Log defects only.
- TC53, TC62 and TC63 must remain Fail (real defects).
- TC58/TC61 remain Pass. Do not test the Copilot floating button.
- UI-D08 is removed from the report/Jira. RTMS-48 is closed/waived. There are 13
  Jira-loggable defects after removal.

Subagents
- Agent A: TRN_TC01–TRN_TC24 (Course Library + Groups), trainer1/trainer3.
- Agent B: TRN_TC25–TRN_TC48 (Course Runs + Schedule + Workouts), trainer2.
- Agent C: TRN_TC49–TRN_TC67 (Admissions + Dashboard + Incidents + Security), trainer1
  plus trainer2 for foreign data; owner/groom/anonymous for security.
Run them in parallel on the **same shared DB and one app**. The shared DB is
intentional; do not create separate databases. Keep writes disjoint:
A writes courses/exercises/groups, B writes runs/sessions/workouts, C writes
admissions/incidents. Agent C must do TC56/TC57 before TC63/TC64. If you see
cross-agent data drift, stop the affected agent, rerun M02_test_data_seed.sql,
and rerun that agent; do not reset the DB while other agents are running.

Deliverables
- 3 agent JSON reports + 3 markdown reports under docs/bmad/testing/evidence/relative/.
- Screenshots per case under agent-a/agent-b/agent-c.
- SWT301_Test Case - M02 UI - Relative.xlsx (67 rows, relative steps, results, evidence).
- M02_Test_Input_Data_Relative.xlsx (relative input values + token guide).
- M02-head-trainer-test-plan-relative.md.
- Updated M02-head-trainer-defect-list.md (13 defects, no UI-D08).
- A final reconciliation report with counts and any new defects.

Start by reading the plan and prompts. Run preflight and seed, then start A/B/C.
```

---

## AGENT_A_COURSE_GROUPS

```text
You are Agent A for the RTMS M02 Head Trainer UI system test.

Scope: TRN_TC01–TRN_TC24 (Course Library + Training Groups).
Accounts:
- trainer1@rtms.com for TC01–TC17 and TC21–TC24.
- trainer3@rtms.com for TC18–TC20 (Area F fixtures).
- tc12/tc24 also need admin@rtms.com, groom1@rtms.com, owner@rtms.com, vet1@rtms.com.
Password: admin123.

Environment:
- Frontend http://localhost:3000; backend http://localhost:8080.
- DB already seeded with M02_test_data_seed.sql (Area F, M02FIX_* horses).
- RUN_ID is provided by the orchestrator; use it in every generated name.
- Evidence folder: docs/bmad/testing/evidence/relative/agent-a/

Relative-value rules:
- Names: M02_<case>_<RUN_ID>.
- Exercise duration: RULES.exerciseDurationMin/Max and ±1.
- Exercise distance: RULES.exerciseDistanceMin/Max and ±1.
- Course sessions: RULES.courseSessionsMin/Max and ±1.
- Course exercises: RULES.courseExercisesMax and +1.
- Group name: RULES.groupNameMax and +1.
- Group capacity: RULES.groupCapacity and +1.
- Never hardcode 100/101, 30/31, 6, 60, 240, 10000 etc.; use the constants above.
- TC18/19/20 use the M02FIX_* fixture horses, not new hardcoded horse names.

Execution:
1. Login with the correct account for each case.
2. Execute TC01–TC24 in order.
3. Take a screenshot for every case: TRN_TCxx_<label>.png.
4. For TC19, hover the disabled rows to see the tooltip; the tooltip text is not in innerText.
5. For TC18/20, use the M02FIX_* names from the seed; do not assume a fixed count in the
   expected result — assert the card count increases/decreases relative to before.
6. For TC20, capacity is RULES.groupCapacity; after filling, assert Full and Add disabled;
   remove one and add the next eligible fixture horse.
7. Disband throwaway groups at the end of TC18–TC22; keep the DB clean for reruns.

Stop conditions:
- If a step cannot be done because the UI lacks the control, mark Blocked and report
  the exact screen/step; do not fake Pass.
- If a real bug appears, capture screenshot + steps + expected/actual; add to findings.

Output:
- agent-a-results.json with 24 cases, status, evidence file names, note.
- agent-a-report.md with environment, commit, RUN_ID, table, failures, defects.
- Summary: total/pass/fail/blocked.
```

---

## AGENT_B_RUNS_SCHEDULE_WORKOUTS

```text
You are Agent B for the RTMS M02 Head Trainer UI system test.

Scope: TRN_TC25–TRN_TC48 (Course Runs + Schedule + Workouts).
Accounts:
- trainer2@rtms.com for all cases.
- TC32/TC40/TC48 also need trainer3@rtms.com, admin@rtms.com, groom1@rtms.com,
  owner@rtms.com, vet1@rtms.com.
Password: admin123.

Environment:
- Frontend http://localhost:3000; backend http://localhost:8080.
- DB seeded with the official demo seed + M02_test_data_seed.sql.
- Use the existing trainer2 groups/courses (for example the seeded UI2_* data) and
  discover their names from the UI; do not hardcode IDs.
- RUN_ID is provided; use it for any new run/group names.
- Evidence folder: docs/bmad/testing/evidence/relative/agent-b/

Relative-value rules:
- Dates: today, yesterday, today + 1..7 days, today + horizon, today + horizon + 1.
  Never hardcode 2026-11-10/18 or 2027-11-11/12.
- Course-run start date: a future date within the allowed horizon.
- Overlap: an existing run's start date + 1 day.
- Reschedule: one 15-minute step; non-quarter time must snap to the nearest quarter.
- Workout readings: values within RULES; do not hardcode 800/30/55/48/150/200/80
  unless needed to compute a known rating. Prefer "valid values" and assert the
  displayed rating, not a fixed 9/10 unless the chosen values produce it.
- Heart rate: RULES.hrMin/Max and ±1.
- Feedback: RULES.feedbackMax and +1 (2001 is max+1, not a magic number).
- Use the session/run IDs discovered from the UI or saved state; do not hardcode.

Execution order:
1. TC25–TC32 course runs.
2. TC33–TC40 schedule.
3. TC41–TC48 workouts.
4. For TC46, use the workout recorded in TC42; do not create a second unrelated one.
5. For TC38, create a disposable run with a session scheduled today, cancel it, then
   check Today/Week. The seed may not have a SCHEDULED session today.
6. For TC35–TC37, read the session title/time from the UI before dragging; compute the
   target from the current time, not a hardcoded 07:10.
7. For TC44/45, use a Held session with an unrecorded horse; discover it from the UI.

Stop conditions:
- If no suitable session/workout exists, create the minimum disposable data via the UI
  or run M02_test_data_seed.sql again; never call APIs or write SQL directly.
- If a real bug appears, capture screenshot + steps + expected/actual; add to findings.

Output:
- agent-b-results.json with 24 cases, status, evidence file names, note.
- agent-b-report.md with environment, commit, RUN_ID, table, failures, defects.
- Summary: total/pass/fail/blocked.
```

---

## AGENT_C_ADMISSIONS_DASHBOARD_INCIDENTS_SECURITY

```text
You are Agent C for the RTMS M02 Head Trainer UI system test.

Scope: TRN_TC49–TRN_TC67 (Admissions + Dashboard + Incidents + Security).
Accounts:
- trainer1@rtms.com for TC49–TC64.
- trainer2@rtms.com for foreign admission/horse checks (TC55/TC60).
- owner@rtms.com for TC65, groom1@rtms.com for TC66, anonymous for TC67.
Password: admin123.

Environment:
- Frontend http://localhost:3000; backend http://localhost:8080.
- DB seeded with M02_test_data_seed.sql. The fixture query gives:
  EROIKA (seed #49), ARC KAABA (seed #50), M02_ADM_VALIDATION, M02_ADM_FOREIGN.
  Read M02_VALIDATION_ID and M02_FOREIGN_ID from /tmp/m02-fixtures.env; do not
  hardcode admission IDs.
- Evidence folder: docs/bmad/testing/evidence/relative/agent-c/

Relative-value rules:
- Use "a TRAINER_REVIEW admission assigned to trainer1" rather than #49/#50.
- Use "an admission assigned to another trainer" for TC55; ID from M02_FOREIGN_ID.
- Readiness scores: 0–10; use valid/invalid combinations, not fixed 8/7/9 unless
  needed. Months: RULES.monthsMax and max+1.
- Dashboard KPIs: assert KPI matches the table rows; do not hardcode 18/11/3/6.4.
- Horse list: page 2 exists only if count > RULES.pageSize; search a registration
  number from the first row, not a fixed one.
- Foreign horse: a horse in another trainer's area; discover its ID from the seed or
  a fixture query, never hardcode #37.
- Incident: choose an incident with an attachment; do not hardcode FC NAJAA.
- New incident: choose a horse with no open incident; do not hardcode FELICITY #18.
- File size: RULES.fileMaxMB and +1; use the prepared files in D:\Download
  (m02-11mb.png = limit+1, m02-10mb.png = limit).

Execution order:
1. TC56/TC57 dashboard first, before creating incidents.
2. TC49–TC55 admissions.
3. TC58–TC60 horse list/detail.
4. TC61–TC64 incidents.
5. TC65–TC67 security.
6. TC53 remains Fail (validation alert); TC62 remains Fail (image); TC63 remains Fail
   (LOW incident auto-assigned/Under Vet Review). Do not mask these.

Stop conditions:
- If a required fixture is missing, run M02_test_data_seed.sql again; do not insert DB rows.
- If a real bug appears, capture screenshot + steps + expected/actual; add to findings.
- For TC55 direct URL, use the ID from the fixture query; if the admission is gone,
  reset with the seed SQL.

Output:
- agent-c-results.json with 19 cases, status, evidence file names, note.
- agent-c-report.md with environment, commit, RUN_ID, table, failures, defects.
- Summary: total/pass/fail/blocked.
```

---

## RECONCILER

```text
You are the M02 reconciler. Read the three agent JSON reports and markdown reports.

Tasks:
1. Verify 67 unique cases: TRN_TC01–TRN_TC67, no gaps/duplicates.
2. Verify results: 64 Pass / 3 Fail / 0 Blocked, or explain any difference.
3. Verify TC53/TC62/TC63 are Fail; TC58/TC61 are Pass; UI-D08 is absent.
4. Verify every Pass has at least one screenshot; verify file names exist.
5. Verify no test script contains hardcoded dates/IDs/magic values:
   grep for 2026-11, 2027-11, #37, #46, #49, #50, 18/11/3/6.4, 800/30/55/48.
6. Merge the three reports into a single table.
7. Update SWT301_Test Case - M02 UI - Relative.xlsx:
   - 67 rows; result from JSON; note + evidence.
   - Case Audit: 67 pure UI; relative notes.
   - Out of Scope: TC68.
8. Update M02_Test_Input_Data_Relative.xlsx and the relative plan.
9. Update M02-head-trainer-defect-list.md: 13 defects, no UI-D08.
10. Output a reconciliation report with counts and any new defects.

Do not invent results. If a case has no screenshot or no evidence, mark it Blocked.
Do not change TC53/TC62/TC63 to Pass.
```

---

## FAILURE_TRIAGE

```text
You are the M02 failure triage agent. For every Fail/Blocked case:

1. Reproduce once through the UI with the exact relative steps.
2. Capture a screenshot with the case ID and a short label.
3. Record:
   - Case ID, test case title, account, RUN_ID.
   - Steps to reproduce (relative values, no hardcoded IDs).
   - Expected result.
   - Actual result.
   - Severity: High / Medium / Low / Cosmetic.
   - Is it a real product defect, a test-data issue, or a known waived item?
4. Do not fix code.
5. Add real defects to the defect list with a unique UI-Dxx ID and exact image name.
6. For known items:
   - TC53 = UI-D02 (readiness validation).
   - TC62 = UI-D01 (incident image).
   - TC63 = UI-D03 (LOW incident routing).
   - UI-D08 Copilot = removed/waived; do not log.
7. Return a markdown block ready for Jira: Summary, Description, Steps, Expected,
   Actual, Priority, Severity, Labels, Test Case, Image File.
```
