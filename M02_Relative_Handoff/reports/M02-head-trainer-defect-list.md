# M02 — Head Trainer UI System-Test Defect List

Executed UI-only against `rtms_system_test` with the official demo seed on 2026-11-11.
No API calls and no direct database writes were used by the test suites.

| ID | Severity | Status | Area | Summary |
|---|---|---|---|---|
| UI-D01 | High | Open | Incidents | Incident attachment images fail to load in the detail modal and horse Incident Reports tab ("The image could not be loaded."). |
| UI-D02 | Medium | Open | Admissions | Readiness validation shows one form-level alert and only the first missing score per submit, not three inline field errors. |
| UI-D03 | Medium | Open | Incidents | A newly reported LOW incident is auto-assigned and starts as Under Vet Review, not Reported / Awaiting Veterinary Review. |
| UI-D04 | Medium | Open | Horse scope | trainer1 can open a trainer2-area horse detail and see identity, status and vet clearance; only specialty/performance data is blocked. |
| UI-D05 | Medium | Open | Course-run wizard | After an invalid past date or no weekdays, the live preview keeps the previous valid schedule instead of clearing or marking itself stale. |
| UI-D06 | Medium | Open | Course-run wizard | With no weekday selected, Create is silently disabled; no inline message explains the requirement. |
| UI-D07 | Medium | Open | Schedule | Cancelled sessions are filtered out of the Today view; they are only visible in Week/detail. |
| UI-D09 | Low | Automation-only | Schedule drag | A 3 px drag opens the Session dialog. A human cannot reproduce a 3 px movement reliably; opening the dialog is normal click behaviour. Not logged in Jira. |
| UI-D10 | Low | Closed | Result dialog | Feedback is capped at 2000 characters. Option A accepts the cap as intended; the expected result was updated and UI-D10 is closed. |
| UI-D11 | Low | Open | Copy | "1 sessions" and "1 horses" pluralisation. |
| UI-D12 | Low | Open | Report incident | An oversized file remains selected; the 10 MB error remains after choosing a valid file. |
| UI-D13 | Low | Open | Copy | Foreign-admission access-denied message is Vietnamese in an otherwise English UI. |
| UI-D14 | Low | Open | Incidents | "Reported by" is "—" for seeded and newly created reports. |
| UI-D15 | Low | Open | Forms | Blank required name uses Chromium native validation, not the app-styled error. |
| UI-D16 | Cosmetic | Open | Course form | "More exercises than sessions" is rendered twice. |

**13 Jira-loggable defects are Open. UI-D09 is an automation-only observation; UI-D10 is closed by decision (maxlength 2000 is intended). UI-D08 (Copilot overlay) is removed/waived by decision. None of the three is in the Jira list.**

## Image files (exact names for Jira attachments)

| ID | Test case | Folder | Exact file name(s) |
|---|---|---|---|
| UI-D01 | TC62 | `ui-3` | `TRN_TC62.png` |
| UI-D02 | TC53 | `ui-3` | `TRN_TC53.png` |
| UI-D03 | TC63 | `ui-3` | `TRN_TC63.png` |
| UI-D04 | TC60 | `ui-3` | `TRN_TC60.png` |
| UI-D05 | TC27, TC28 | `ui-2` | `TRN_TC27.png`, `TRN_TC28.png` |
| UI-D06 | TC28 | `ui-2` | `TRN_TC28.png` |
| UI-D07 | TC38 | `ui-2` | `TRN_TC38.png` |
| UI-D09 | TC36 | `ui-2` | `TRN_TC36.png` (not attached — automation-only) |
| UI-D10 | TC45 | `ui-2` | `TRN_TC45.png` (not attached — closed) |
| UI-D11 | TC07, TC23 | `ui-1` | `TRN_TC07_sessions-boundary.png`, `TRN_TC23_course-run-created.png` |
| UI-D12 | TC64 | `ui-3` | `TRN_TC64.png` |
| UI-D13 | TC55 | `ui-3` | `TRN_TC55.png` |
| UI-D14 | TC61, TC62 | `ui-3` | `TRN_TC61.png`, `TRN_TC62.png` |
| UI-D15 | TC05, TC08 | `ui-1` | `TRN_TC05_duplicate-name.png`, `TRN_TC08_duplicate-course.png` |
| UI-D16 | TC10 | `ui-1` | `TRN_TC10_empty-and-too-many.png`, `TRN_TC10_identical-rotation-refused.png` |

Base path for every image: `docs/bmad/testing/evidence/screenshots/<folder>/<file name>`. The Jira list attaches 18 images (all Jira rows; UI-D08, UI-D09 and UI-D10 excluded).

## Evidence

- Screenshots: `evidence/screenshots/ui-1`, `ui-2`, `ui-3`, plus `ui-1-rerun` for TC18–TC20 (107 files).
- Test cases: `M02-head-trainer-test-plan.md`, 67 pure-UI in-scope cases (64 Pass / 3 Fail / 0 Blocked) + TC68 out of scope.
- Case audit: `M02-test-case-audit.md` - 67 pure UI, 0 API assertions in the UI cases, 1 automation-only observation (UI-D09), 1 closed by decision (UI-D10), 1 waived (UI-D08).
- Failing cases: TC53 (UI-D02), TC62 (UI-D01), TC63 (UI-D03).
- Jira export: `RTMS_M02_DefectList.xlsx` — sheet `Jira Mapping` (CSV → Jira field mapping + wizard steps), `Repro Check` (16 findings; UI-D08 waived, UI-D09 rejected, UI-D10 closed), `Jira Defects` (13 manually reproducible open rows) and `Attachments` (18 image rows). Import `RTMS_M02_Jira_Import_Mapped.csv`; `RTMS_M02_Jira_Import.csv` is the full master copy.
