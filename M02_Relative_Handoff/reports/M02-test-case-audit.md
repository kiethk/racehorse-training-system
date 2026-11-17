# M02 — Test case audit (all 67 pure-UI cases + TC68)

Executed test package: `SWT301_Test Case - M02 UI - Pure UI.xlsx`, sheet `M02_Head_Trainer`.
Audit date: 2026-11-12. Criteria: human reproducibility through the UI, UI-only observability, setup/fixture dependency, and consistency between expected output and recorded result.

## Summary

| Metric | Count |
|---|---|
| In-scope cases | 67 |
| Out-of-scope case | 1 (TC68) |
| Pure UI, human-reproducible | 67 |
| API calls or HTTP status codes in UI cases | 0 |
| Network-only assertions in UI cases | 0 |
| Automation-only sub-step | 1 (TC36 — removed) |
| Closed by decision | 1 (UI-D10 — option A) |
| Status conflicts | 0 |
| Jira-loggable defects | 14 |
| Failing cases | 3 (TC53, TC62, TC63) |
| Cases with no screenshot | 0 |
| Duplicate/missing IDs | 0 |

**Conclusion:** all 67 in-scope cases are pure UI. Every procedure is browser-only, and every expected result is visible on screen. The API/security assertions that were in the earlier draft were moved to a separate API suite. UI-D09 (3 px drag) is rejected as automation-only; UI-D10 (feedback maxlength) is closed because option A accepts the 2000-character cap as intended.

## Findings after conversion

### 1. 24 cases were rewritten to pure UI

The earlier draft contained 22 UI+API cases and 2 UI+network cases. All 24 were rewritten: API calls were removed from the procedures, and HTTP status codes/error codes were replaced by UI-observable outcomes (field errors, toasts, redirects, empty states, disabled/hidden controls, row/status changes). TC16 also had an "or 400" alternative in its expected output; it was cleaned to the UI-only disabled/duplicate message. In total, 25 cases were touched.

Rewritten cases: TC12, TC15, TC21, TC23, TC24, TC27, TC28, TC29, TC30, TC31, TC32, TC35, TC36, TC37, TC38, TC40, TC42, TC43, TC45, TC47, TC48, TC53, TC55, TC60; TC16 (wording cleanup).

### 2. UI-D09 — 3 px drag (automation-only, removed from Jira)

TC36 no longer contains the 3 px sub-step. A human cannot reliably perform or verify a 3 px pointer movement, and opening the Session dialog is normal click behaviour. UI-D09 is Rejected and not logged. TC36 keeps the human-verifiable snap and clamp checks.

### 3. UI-D10 — feedback maxlength (closed by option A)

Feedback stops at 2000 characters. Option A accepts this as the intended behaviour; TC45's expected output now says the field stops at 2000, and UI-D10 is Closed. The Jira list has 14 defects.

### 4. Three real failures remain

| Case | Defect | Observation |
|---|---|---|
| TC53 | UI-D02 | Readiness validation shows one form-level alert and only the first missing score per submit |
| TC62 | UI-D01 | Incident attachment image fails to load in the modal and the horse Incident Reports tab |
| TC63 | UI-D03 | A new LOW incident is auto-assigned and starts Under Vet Review |

### 5. Setup-dependent cases

Reproducible, but need a fixture/reseed/special data: TC09, TC10, TC18–TC20, TC23, TC26, TC29, TC31, TC35–TC38, TC42, TC44–TC47, TC49–TC55, TC57–TC60, TC64. TC18–TC20 need `m02_group_fixtures.sql`; TC51–TC54 consume fresh admissions, so reseed before replaying.

### 6. TC68 remains out of scope

TC68's expected HTTP 403 is a backend response. The UI only redirects owner/groom and hides trainer actions. Keep it in the API/security suite; it is not part of the 67 UI cases.

## Recommendations

1. Use the pure-UI workbook and plan for submission; keep the API harness as a separate report.
2. Reseed `rtms_system_test` and apply the M02 fixture SQL before replaying the setup-dependent cases.
3. Keep TC53, TC62 and TC63 as Fail; do not mark them Pass.
4. Do not log UI-D09 or UI-D10 in Jira.

## Full case audit

| Case | Result | Category | Human repro | Issue | Recommendation |
|---|---|---|---|---|---|

| TRN_TC01 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC02 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC03 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC04 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC05 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC06 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC07 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC08 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC09 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC10 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC11 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC12 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC13 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC14 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC15 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC16 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC17 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC18 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC19 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC20 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC21 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC22 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC23 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC24 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC25 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC26 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC27 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC28 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC29 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC30 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC31 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC32 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC33 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC34 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC35 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC36 | **Pass** | Pure UI | Yes* | 3 px sub-step removed; UI-D09 rejected. | Keep. |
| TRN_TC37 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC38 | **Pass** | Pure UI | Yes* | Side finding UI-D07 (Today hides cancelled sessions). | Keep Pass; UI-D07 remains. |
| TRN_TC39 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC40 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC41 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC42 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC43 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC44 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC45 | **Pass** | Pure UI | Yes* | Maxlength 2000 is intended; UI-D10 closed (option A). | Keep; expected updated. |
| TRN_TC46 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC47 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC48 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC49 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC50 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC51 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC52 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC53 | **Fail** | Pure UI | Yes* | Fail — UI-D02 (one form-level alert, one score per submit). | Keep Fail. |
| TRN_TC54 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC55 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC56 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC57 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC58 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC59 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC60 | **Pass** | Pure UI | Yes* | Side finding UI-D04 (basic profile visible for a foreign horse). | Keep Pass; UI-D04 remains. |
| TRN_TC61 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC62 | **Fail** | Pure UI | Yes | Fail — UI-D01 (incident image fails to load). | Keep Fail. |
| TRN_TC63 | **Fail** | Pure UI | Yes | Fail — UI-D03 (LOW incident auto-assigned / Under Vet Review). | Keep Fail. |
| TRN_TC64 | **Pass** | Pure UI | Yes* | None. | Keep. |
| TRN_TC65 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC66 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC67 | **Pass** | Pure UI | Yes | None. | Keep. |
| TRN_TC68 | Out of scope | Out of scope | N/A | Expected HTTP 403 is an API-level response; the UI only redirects. | Keep in the API/security suite. |
