# Design Spec: RTMS Urgent Cases Module Redesign

**Date**: 2026-10-22  
**Status**: Draft (Pending User Review)  
**Target Pages**:
1. Urgent Cases Dashboard: `frontend/src/app/(app)/veterinarian/urgent/page.tsx`
2. Urgent Case Examination Workspace: `frontend/src/features/admissions/components/UrgentCaseView.tsx` (rendered at `frontend/src/app/(app)/veterinarian/urgent/[scheduleId]/page.tsx`)

---

## 1. Objective & Context

Synchronize the Urgent Cases module with the RTMS Prism-Inspired Operational Design System and equine veterinary theme.
Replace the generic, stark red borders and plain list cards with a desktop-first, equine-focused clinical operations dashboard and workspace.

### Key Requirements
1. **List Page (`/veterinarian/urgent`)**:
   - Standard RTMS `PageHeader` with title, active count pill, and manual + 15s auto-refresh.
   - 4 operational `MetricCard` summary blocks (Active Urgent, Pending Intake, In Progress, Critical Priority).
   - Fast client-side filtering via `SearchInput` (horse name, stall code, reporter name) and `FilterChips` (All, Pending Intake, In Progress).
   - Equine Emergency Cards featuring `HorseAvatar`, horse name, ID, stall/barn location, severity badges, reporter role badge, and intuitive intake/resume CTA buttons.
   - Refined `ListSkeleton` and `EmptyState` matching RTMS design tokens.

2. **Examination Page (`/veterinarian/urgent/[scheduleId]`)**:
   - Breadcrumb navigation back to the Urgent Cases list.
   - Modern clinical header displaying `HorseAvatar`, horse identity, stall location, schedule ID, priority pill, and current lifecycle state.
   - Responsive 2-column layout:
     - **Main Column (65%)**:
       - Incident description and photo evidence (`ProtectedImage` with click-to-preview).
       - Merged incident reports (if multiple reporters submitted incidents for this emergency).
       - Intake Action Bar (when `SCHEDULED`) with prominent "Start Clinical Examination" button.
       - Clinical Examination Form (when `IN_PROGRESS`):
         - **Vitals Telemetry**: Compact input boxes with labels integrated directly inside/above each input (NO helper text underneath; NO out-of-range red warnings for flexible veterinary input).
         - **Findings & Diagnosis**: Symptoms, physical findings, diagnosis, treatment/first-aid, and additional notes.
         - **Training Clearance**: Radio options (`ALLOWED` vs `BLOCKED`). When `BLOCKED`, prompt for restriction details, follow-up date (+3d, +7d, +14d shortcuts), and instructions.
         - Submit CTA: "Complete Urgent Examination" with loading state.
       - Completed Examination Summary (when `COMPLETED`): Clean success presentation of saved diagnosis, findings, and training decision.
     - **Side Column (35%)**:
       - Horse Location & Medical Context (current stall, barn, pre-existing training block alert).
       - Reporter & Timeline Meta (reporter name, role, reported timestamp, assigned timestamp).
       - Equine Clinical Protocol Checklist.

---

## 2. Component Architecture & UI Specifications

### 2.1 List Page: `frontend/src/app/(app)/veterinarian/urgent/page.tsx`
- **Imports**: `PageHeader`, `MetricCard`, `SearchInput`, `FilterChips`, `HorseAvatar`, `Pill`, `Button`, `Panel`, `Icon`, `EmptyState`, `ListSkeleton`, `Notice`, `admissionsApi`.
- **Top Metrics Grid**:
  - `Active Urgent`: `urgentCases.length`
  - `Pending Intake`: count of `SCHEDULED` cases
  - `In Progress`: count of `IN_PROGRESS` cases
  - `Critical Priority`: count of cases with `severity === 'CRITICAL'`
- **Search & Filter Bar**:
  - `SearchInput` updates local state `searchQuery`.
  - `FilterChips` selects status (`ALL`, `PENDING`, `IN_PROGRESS`).
- **Emergency Case Cards**:
  - Container: `Panel` with subtle border, left status accent border (`--color-danger` for `SCHEDULED` / Critical, `--color-warning` or `--color-primary` for `IN_PROGRESS`).
  - Left: `HorseAvatar` (size `lg` or `md`), horse name (bold), `#{horseId}`, stall badge (`Stall {stallCode} · {stableLocation}`).
  - Middle: Incident title, description snippet, `Pill` for severity (`CRITICAL`, `HIGH`), `Pill` for status, reporter badge (`GROOM` / `TRAINER`) and formatted time.
  - Right: Button variant based on status (`SCHEDULED` -> destructive/red "Intake Case", `IN_PROGRESS` -> primary "Examine").

### 2.2 Examination View: `frontend/src/features/admissions/components/UrgentCaseView.tsx`
- **Header**:
  - Replaces the 100% red background header with a clean white/subtle panel with a left accent and top breadcrumb.
  - Displays `HorseAvatar` next to horse name, ID, stall location, schedule ID, severity pill, and status pill.
- **Vitals Inputs (User specific constraint)**:
  - Container: 4-column responsive grid (`grid-cols-2 sm:grid-cols-4`).
  - Each input card has an embedded top label (e.g. `Temperature (°C)`, `Heart Rate (bpm)`, `Respiratory Rate (rpm)`, `Weight (kg)`).
  - No text hanging below the input fields.
  - No out-of-range red error warnings when entering high/low numbers. Vets have full clinical discretion.
- **Examination & Training Form**:
  - Retains all existing validation: `symptoms`, `findings`, `diagnosis` required; `restrictionDetails`, `followUpDate`, `followUpDescription` required if `trainingDecision === 'BLOCKED'`.
  - Date helper buttons (+3 days, +7 days, +14 days) preserved.
- **Side Panel**:
  - Horse context and previous training hold notice.
  - Reporter metadata with role tag.
  - Protocol guidance checklist.

---

## 3. Data Flow & Backend Compatibility

- Fully compatible with current backend APIs in `CareScheduleController` and `admissionsApi`:
  - `getPendingUrgentAlerts()`
  - `getUrgentCase(scheduleId)`
  - `getUrgentCaseReports(scheduleId)`
  - `startCareSchedule(scheduleId)`
  - `completeCareSchedule(scheduleId, payload)`
- No payload modifications; existing tests and API contracts remain intact.

---

## 4. Testing & Verification

1. **Unit / Integration Tests**:
   - Run `urgentCaseWorkflow.test.ts` to ensure existing form workflow, validation logic, and submit handlers are undisturbed.
2. **Build Check**:
   - Run `pnpm run build` or Next.js type-check to confirm zero TypeScript / lint regressions.
3. **Visual Verification**:
   - Verify desktop and mobile responsiveness of both `/veterinarian/urgent` and `/veterinarian/urgent/[scheduleId]`.
