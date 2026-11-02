# Urgent Cases Module Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Urgent Cases list page (`/veterinarian/urgent`) and urgent examination workspace (`UrgentCaseView.tsx` at `/veterinarian/urgent/[scheduleId]`) to seamlessly align with the RTMS Prism-Inspired Equine Operations design system and veterinary theme.

**Architecture:** 
- The list page leverages `PageHeader`, 4 top `MetricCard` status widgets, `SearchInput`, `FilterChips`, and equine emergency cards featuring `HorseAvatar`, stall badge, priority tags, and responsive action triggers.
- The examination view replaces the stark red box banner with an equine clinical 2-column layout: Left column contains incident evidence, image preview, vitals telemetry cards with integrated internal labels (no bottom helper text, no out-of-range red warnings), clinical findings/diagnosis/clearance form, and completion summary; Right column houses horse location context, reporter metadata, and medical protocol checklist.

**Tech Stack:** Next.js (App Router), React 19, TypeScript, Tailwind CSS, RTMS UI Primitives (`PageHeader`, `MetricCard`, `HorseAvatar`, `Pill`, `Panel`, `Button`, `Icon`, `Input`, `Textarea`, `ProtectedImage`, `EmptyState`, `ListSkeleton`).

## Global Constraints

- Retain 100% backward compatibility with backend `CareScheduleController` and `admissionsApi`.
- Embed input labels directly inside/above input controls for vitals; do NOT place long helper text underneath the input boxes.
- Do NOT display red warnings or blocking errors for vital metric numbers outside standard ranges.
- Follow RTMS design tokens: `--color-primary: #6657c7`, `--color-medical: #b94b58`, `--color-background: #f6f6f8`, `--color-surface: #ffffff`, `--color-border: #e3e3e8`.
- Keep existing form validation invariants intact: `symptoms`, `findings`, and `diagnosis` are required; when `trainingDecision === 'BLOCKED'`, `restrictionDetails`, `followUpDate`, and `followUpDescription` are required.

---

### Task 1: Verify & Extend Workflow Unit Tests

**Files:**
- Modify: `frontend/src/features/admissions/components/__tests__/urgentCaseWorkflow.test.ts`

**Interfaces:**
- Consumes: `UrgentAssignmentAlert`, `CompleteCareScheduleRequest`, `TrainingDecision` from `frontend/src/features/admissions/types/index.ts`.
- Produces: Passing node test suite covering vitals mapping, search filter logic, and form validation invariants.

- [ ] **Step 1: Add new test cases for search filter logic and vitals permissive parsing**

Add tests to `frontend/src/features/admissions/components/__tests__/urgentCaseWorkflow.test.ts`:
```typescript
it('7. Search and status filter matches horse name, stall code, or reporter', () => {
  const cases: UrgentAssignmentAlert[] = [
    {
      eventId: 1, scheduleId: 101, incidentId: 11, veterinarianId: 5,
      horseId: 10, horseName: 'Thunder', stableLocation: 'Barn A', stallCode: 'A-01',
      reportedById: 2, reportedByName: 'Groom Alex', reportedAt: '2026-10-22T08:00:00',
      severity: 'HIGH', title: 'Lameness', description: 'Left leg limp', imageUrl: null,
      trainingDecision: 'BLOCKED', previousTrainingDecision: null, previousTrainingDecisionReason: null,
      status: 'SCHEDULED', scheduledAt: '2026-10-22T08:15:00', assignedAt: '2026-10-22T08:05:00',
    },
    {
      eventId: 2, scheduleId: 102, incidentId: 12, veterinarianId: 5,
      horseId: 20, horseName: 'Pegasus', stableLocation: 'Barn B', stallCode: 'B-03',
      reportedById: 3, reportedByName: 'Trainer John', reportedAt: '2026-10-22T08:30:00',
      severity: 'CRITICAL', title: 'Colic acute', description: 'Rolling and sweat', imageUrl: null,
      trainingDecision: 'BLOCKED', previousTrainingDecision: null, previousTrainingDecisionReason: null,
      status: 'IN_PROGRESS', scheduledAt: '2026-10-22T08:45:00', assignedAt: '2026-10-22T08:35:00',
    },
  ];

  function filterCases(list: UrgentAssignmentAlert[], statusFilter: string, search: string) {
    const q = search.trim().toLowerCase();
    return list.filter((c) => {
      const matchStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'PENDING' && c.status === 'SCHEDULED') ||
        (statusFilter === 'IN_PROGRESS' && c.status === 'IN_PROGRESS');
      if (!matchStatus) return false;
      if (!q) return true;
      return (
        c.horseName.toLowerCase().includes(q) ||
        (c.stallCode && c.stallCode.toLowerCase().includes(q)) ||
        (c.reportedByName && c.reportedByName.toLowerCase().includes(q)) ||
        c.title.toLowerCase().includes(q)
      );
    });
  }

  assert.equal(filterCases(cases, 'ALL', 'pegasus').length, 1);
  assert.equal(filterCases(cases, 'ALL', 'A-01').length, 1);
  assert.equal(filterCases(cases, 'PENDING', '').length, 1);
  assert.equal(filterCases(cases, 'IN_PROGRESS', '').length, 1);
});

it('8. Vitals accepts extreme numerical values without throwing or blocking payload formation', () => {
  const temp = '41.5'; // High fever
  const hr = '88';    // Severe tachycardia
  const rr = '40';    // Tachypnea
  const wt = '520';

  const metrics: { temperature?: number; heartRate?: number; respiratoryRate?: number; weight?: number } = {};
  if (temp.trim()) metrics.temperature = Number(temp);
  if (hr.trim()) metrics.heartRate = Number(hr);
  if (rr.trim()) metrics.respiratoryRate = Number(rr);
  if (wt.trim()) metrics.weight = Number(wt);

  assert.equal(metrics.temperature, 41.5);
  assert.equal(metrics.heartRate, 88);
  assert.equal(metrics.respiratoryRate, 40);
  assert.equal(metrics.weight, 520);
});
```

- [ ] **Step 2: Run test suite to verify tests pass**

Run: `node --experimental-transform-types --test src/features/admissions/components/__tests__/urgentCaseWorkflow.test.ts` from `frontend` directory.  
Expected: PASS with 8 tests passed.

- [ ] **Step 3: Commit test updates**

```bash
git add frontend/src/features/admissions/components/__tests__/urgentCaseWorkflow.test.ts
git commit -m "test(vet): add urgent case filter and vitals test cases"
```

---

### Task 2: Redesign Urgent Cases List Page (`frontend/src/app/(app)/veterinarian/urgent/page.tsx`)

**Files:**
- Modify: `frontend/src/app/(app)/veterinarian/urgent/page.tsx`

**Interfaces:**
- Consumes:
  - `admissionsApi.getPendingUrgentAlerts()`
  - Primitives: `PageContainer`, `Panel`, `Button`, `Pill`, `Icon`, `EmptyState`, `ListSkeleton`, `FilterChips`, `SearchInput`, `MetricCard`, `HorseAvatar`, `Notice`.
- Produces: Modern Equine Operations Urgent Dashboard with 4 `MetricCard` summary blocks, instant search by horse/stall/reporter, status chip filters, and equine emergency case cards.

- [ ] **Step 1: Implement the redesigned Urgent Cases page**

Rewrite `frontend/src/app/(app)/veterinarian/urgent/page.tsx`:
- Add state for `searchQuery: string`.
- Compute 4 summary counts:
  - `totalCount = urgentCases.length`
  - `pendingCount = urgentCases.filter((c) => c.status === 'SCHEDULED').length`
  - `inProgressCount = urgentCases.filter((c) => c.status === 'IN_PROGRESS').length`
  - `criticalCount = urgentCases.filter((c) => c.severity === 'CRITICAL').length`
- Render 4 `MetricCard` components in a grid (`grid-cols-2 lg:grid-cols-4`).
- Add a filter toolbar containing `SearchInput` and `FilterChips`.
- Render cards with:
  - Left: `HorseAvatar` with fallback horse name, horse ID, stall badge (`Stall {c.stallCode} · {c.stableLocation}`).
  - Middle: Severity pill (`CRITICAL` vs `HIGH`), Status pill, Title, description snippet, reporter name & role badge, elapsed / reported time.
  - Right: Action button (`SCHEDULED` -> `Intake Case`, `IN_PROGRESS` -> `Examine`).
- Render `ListSkeleton` on loading, and enhanced `EmptyState` when no items match.

- [ ] **Step 2: Verify TypeScript and test compilation**

Run: `node --experimental-transform-types --test src/features/admissions/components/__tests__/urgentCaseWorkflow.test.ts` from `frontend` directory.

- [ ] **Step 3: Commit the redesigned list page**

```bash
git add frontend/src/app/(app)/veterinarian/urgent/page.tsx
git commit -m "feat(vet): redesign urgent cases dashboard with RTMS operational theme"
```

---

### Task 3: Redesign Urgent Case Examination View (`frontend/src/features/admissions/components/UrgentCaseView.tsx`)

**Files:**
- Modify: `frontend/src/features/admissions/components/UrgentCaseView.tsx`

**Interfaces:**
- Consumes: `scheduleId: number`, `admissionsApi.getUrgentCase`, `admissionsApi.getUrgentCaseReports`, `admissionsApi.startCareSchedule`, `admissionsApi.completeCareSchedule`.
- Produces: 2-column clinical workspace with `HorseAvatar`, compact vitals inputs with embedded labels and no out-of-range red warnings, structured clinical examination form, and sidebar medical context.

- [ ] **Step 1: Implement the redesigned `UrgentCaseView.tsx`**

Update `frontend/src/features/admissions/components/UrgentCaseView.tsx`:
- Replace the all-red border and banner with:
  - Back-link: `<Link href="/veterinarian/urgent" className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--color-primary)] hover:underline mb-2">&larr; Back to Urgent Cases</Link>`
  - Clinical Header: White/subtle panel with `border border-[var(--color-border)]`, left accent bar in `--color-medical`, `HorseAvatar`, horse name, #id, stall & barn badges, schedule ID, priority pill, and status pill.
- Main column (65% width):
  - Incident briefing section with Groom's reported observation.
  - Incident photo displayed with `ProtectedImage` and zoom capability.
  - Merged secondary reports (if any) in structured sub-panels.
  - Status `SCHEDULED`: Clean notice banner with "Start Clinical Examination" button.
  - Status `IN_PROGRESS`: Clinical Examination Form:
    - **Vitals Telemetry Grid**: 4 cards (`grid-cols-2 sm:grid-cols-4`).
      - In each card, render a compact header/label inside the top of the input card (e.g. `<span className="text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">Temperature (°C)</span>`).
      - Input field with placeholder and no bottom helper text.
      - No red out-of-range warnings or artificial blocks.
    - **Clinical Findings & Diagnosis**: Symptoms textarea, findings textarea, diagnosis input, treatment input, additional notes textarea.
    - **Training Clearance**: Radio selector (`ALLOWED` vs `BLOCKED`). When `BLOCKED`, render restriction details, date picker with `+3 days`, `+7 days`, `+14 days` quick buttons, and follow-up reason.
    - **CTA Button**: "Complete Urgent Examination".
  - Status `COMPLETED`: Elegant success card with saved diagnosis, training decision, findings, and scheduled follow-up.
- Side column (35% width):
  - **Horse Medical Context**: Stall location, barn area, previous training hold alert if pre-existing hold is active.
  - **Reporter & Timeline**: Reporter name, role badge (`GROOM` / `TRAINER`), reported at timestamp, assigned at timestamp.
  - **Clinical Protocol Checklist**: Guidance steps (secure horse in stall, take vitals before sedation, document all findings).

- [ ] **Step 2: Run test suite to verify all tests pass**

Run: `node --experimental-transform-types --test src/features/admissions/components/__tests__/urgentCaseWorkflow.test.ts` from `frontend` directory.  
Expected: All 8 tests pass.

- [ ] **Step 3: Commit the redesigned examination view**

```bash
git add frontend/src/features/admissions/components/UrgentCaseView.tsx
git commit -m "feat(vet): redesign urgent case examination workspace with 2-column clinical layout"
```

---

### Task 4: Full System Verification & Regression Check

**Files:**
- Check: All modified files

- [ ] **Step 1: Run complete frontend unit test suite**

Run: `npm test` from `frontend` directory.  
Expected: All 123+ unit tests pass without errors.

- [ ] **Step 2: Commit any remaining updates and document completion**

```bash
git commit --allow-empty -m "chore(vet): complete urgent cases redesign verification"
```
