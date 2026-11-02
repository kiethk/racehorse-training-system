# Hybrid Domain Audit Log Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a production-grade, 2-Tier Hybrid Audit Log system capturing deep Before/After snapshots, change reasons, and entity links for critical operations across 4 high-risk domains, complete with module/role filtering, CSV export, and an inspection Modal Dialog.

**Architecture:** 
- **Tier 1 (Broad HTTP Safety Net)**: `HttpAuditInterceptor` + `AuditActivityResolver` captures general write requests; skips logging when a Tier-2 domain audit has already been recorded for the same request via a request context attribute `DOMAIN_AUDIT_LOGGED`.
- **Tier 2 (Deep Domain Audit)**: `AuditLogService.recordDomainEvent` records structured JSON into the existing `details` TEXT column containing `summary`, `reason`, `module`, `before`, `after`, and `targetUrl`.
- **Executive Frontend UI**: `ManagerAuditLogView.tsx` with enhanced `FilterBar` (Module, Role, Status, Dates, Search), CSV Export button, clickable entity redirection badges, and an `AuditLogDetailModal` featuring actor profile, target deep-link, reason callout, and a 2-column Before vs After diff table with highlighted changes.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS, RTMS Design System Primitives.

**Spec:** `.superpowers/specs/2026-11-02-hybrid-domain-audit-log-design.md`

## Global Constraints

- Zero database schema alterations (no new tables or DDL migrations; reuse `audit_logs.details`).
- Backward compatibility: plain text legacy `details` and Tier-1 logs must continue to render gracefully alongside new structured JSON logs.
- Strictly English UI text, tooltips, and badges; conversational Vietnamese for user communication.
- Adhere to `.agents/rules/java-import-style.md` (no wildcard imports).
- Adhere to `.agents/rules/universal-entity-redirection.md` (deep-linking to entity records).

## Review Focus

1. **Deduplication Invariant**: A single user action within the 4 core domains must produce exactly one audit record (Tier-2 enriched), never two (duplicate Tier-1 + Tier-2).
2. **Fail-Safe Logging**: An error in audit serialization or logging must never crash the primary business transaction.
3. **Module Filter Precision**: Filtering by module (e.g. `MEDICAL`) must return both new JSON records and historical plain-text records matching medical entity types.
4. **Graceful Diff Rendering**: The detail modal must gracefully handle null values, booleans, dates, and non-JSON legacy details without throwing render exceptions.
5. **CSV Safety**: The CSV export must properly quote and escape strings with commas, quotes, or newlines to prevent CSV injection or broken rows.

---

### Task 1: Domain Event Data Model & Tier-1 Deduplication Engine

**Files:**
- Create: `backend/src/main/java/com/rtms/backend/audit/dto/DomainAuditEvent.java`
- Modify: `backend/src/main/java/com/rtms/backend/audit/service/AuditLogService.java`
- Modify: `backend/src/main/java/com/rtms/backend/interceptor/HttpAuditInterceptor.java`
- Test: `backend/src/test/java/com/rtms/backend/audit/service/DomainAuditLogServiceTest.java`

- [ ] **Step 1: Write failing unit test for `recordDomainEvent` and deduplication**
  Create `backend/src/test/java/com/rtms/backend/audit/service/DomainAuditLogServiceTest.java` verifying that:
  - Calling `auditLogService.recordDomainEvent(...)` serializes before/after/reason/module to JSON in `details`.
  - Sets `request.setAttribute("DOMAIN_AUDIT_LOGGED", Boolean.TRUE)` on the current `HttpServletRequest`.
  - `HttpAuditInterceptor` skips logging when `DOMAIN_AUDIT_LOGGED` is `true`.

- [ ] **Step 2: Run test to confirm it fails**
  Run: `cd backend; .\mvnw.cmd test -Dtest=DomainAuditLogServiceTest`

- [ ] **Step 3: Implement `DomainAuditEvent` and `recordDomainEvent`**
  - Create `DomainAuditEvent.java` with builder/constructor for `entityName`, `entityId`, `action`, `module`, `summary`, `reason`, `before`, `after`, `targetUrl`, `userId`.
  - In `AuditLogService.java`, implement `recordDomainEvent`:
    - Serialize payload to JSON using `ObjectMapper`.
    - Set `log.setAction(event.action())`, `log.setEntityName(event.entityName())`, `log.setEntityId(event.entityId())`, `log.setDetails(jsonDetails)`.
    - Save to `AuditLogRepository`.
    - Mark `HttpServletRequest` with `DOMAIN_AUDIT_LOGGED = Boolean.TRUE`.
  - In `HttpAuditInterceptor.java`, inspect `request.getAttribute("DOMAIN_AUDIT_LOGGED")`; if `Boolean.TRUE`, return early without saving duplicate Tier-1 log.

- [ ] **Step 4: Verify test passes**
  Run: `cd backend; .\mvnw.cmd test -Dtest=DomainAuditLogServiceTest`

- [ ] **Step 5: Commit changes**
  Commit: `feat(audit): introduce DomainAuditEvent and Tier-1 deduplication in HttpAuditInterceptor`

---

### Task 2: Backend Query Filtering by Module and Role

**Files:**
- Modify: `backend/src/main/java/com/rtms/backend/audit/controller/ManagerAuditLogController.java`
- Modify: `backend/src/main/java/com/rtms/backend/audit/service/AuditLogService.java`
- Modify: `backend/src/main/java/com/rtms/backend/audit/dto/AuditLogResponse.java`
- Test: `backend/src/test/java/com/rtms/backend/audit/service/AuditLogServiceReadTest.java`

- [ ] **Step 1: Write failing test for module and role filtering**
  In `AuditLogServiceReadTest.java`, add tests:
  - Filtering by `module = "MEDICAL"` returns entries with `entityName IN ('CareSchedule', 'HealthRecord', 'TreatmentPlan', 'PreventiveCareSchedule')` as well as JSON `details` with `"module":"MEDICAL"`.
  - Filtering by `role = "VETERINARIAN"` returns only records created by veterinarians.
  - `AuditLogResponse` includes `module` derived from JSON or `entityName`.

- [ ] **Step 2: Run test to confirm failure**
  Run: `cd backend; .\mvnw.cmd test -Dtest=AuditLogServiceReadTest`

- [ ] **Step 3: Implement module and role query specifications in `AuditLogService`**
  - Add query params `String module` and `String role` to `ManagerAuditLogController.getAuditLogs`.
  - In `AuditLogService.java`, build JPA Specification matching:
    - `module`: maps `ADMISSIONS`, `MEDICAL`, `STABLE`, `HORSES`, `STAFF`, `TRAINING` to corresponding entity names OR `details LIKE '%"module":"<MODULE>"%'`.
    - `role`: checks `user.role.name = :role`.
  - In `AuditLogResponse.java`, add `private String module;` and populate it in `from(AuditLog log)`.

- [ ] **Step 4: Verify test passes**
  Run: `cd backend; .\mvnw.cmd test -Dtest=AuditLogServiceReadTest`

- [ ] **Step 5: Commit changes**
  Commit: `feat(audit): support module and role filtering in audit log queries and DTO`

---

### Task 3: Instrument Domain Events in the 4 Critical Business Services

**Files:**
- Modify: `backend/src/main/java/com/rtms/backend/admission/service/AdmissionReviewService.java` (Vet review)
- Modify: `backend/src/main/java/com/rtms/backend/admission/service/AdmissionManagerReviewService.java` (Manager admission approval)
- Modify: `backend/src/main/java/com/rtms/backend/medical/service/CareRescheduleService.java` (Care rescheduling)
- Modify: `backend/src/main/java/com/rtms/backend/horse/service/HorseService.java` (Horse status transition)
- Modify: `backend/src/main/java/com/rtms/backend/identity/service/StaffManagementService.java` (Staff status update)

- [ ] **Step 1: Write integration tests verifying domain events firing**
  Write tests in respective service test classes ensuring `auditLogService.recordDomainEvent` is called with before, after, reason, and module.

- [ ] **Step 2: Instrument `AdmissionReviewService` & `AdmissionManagerReviewService`**
  - When vet reviews: capture before (`AdmissionStatus.WAITING_FOR_VET_REVIEW`), after (`WAITING_FOR_TRAINER_REVIEW` or `BLOCKED`), diagnosis, training decision, and reason notes.
  - When manager approves: capture allocated stall, before status, after status (`APPROVED`), and approval notes.

- [ ] **Step 3: Instrument `CareRescheduleService`**
  - Refactor direct `auditLogs.save(audit)` to call `auditLogService.recordDomainEvent(...)` with before (`oldDate`, `oldStatus`), after (`newDate`, `REQUESTED`), and operator reason.

- [ ] **Step 4: Instrument `HorseService` & `StaffManagementService`**
  - In `HorseService.updateStatus`: capture before (`horse.getCurrentStatus()`), after (`targetStatus`), `reasonCode`, and `statusReason`.
  - In `StaffManagementService.updateStaffStatus`: capture before (`active`), after (`newActive`), and deactivation reason.

- [ ] **Step 5: Verify all backend tests compile and pass**
  Run: `cd backend; .\mvnw.cmd test`

- [ ] **Step 6: Commit changes**
  Commit: `feat(audit): instrument domain audit events across Admissions, Medical, Horse, and Staff domains`

---

### Task 4: Frontend Types, API Client, and CSV Export Utility

**Files:**
- Modify: `frontend/src/features/audit-log/types.ts`
- Modify: `frontend/src/features/audit-log/services/api.ts`
- Create: `frontend/src/features/audit-log/lib/auditExport.ts`
- Create: `frontend/src/features/audit-log/lib/__tests__/auditExport.test.ts`

- [ ] **Step 1: Write unit tests for CSV export**
  Create `frontend/src/features/audit-log/lib/__tests__/auditExport.test.ts` verifying:
  - Generates valid CSV header: `Timestamp,Actor,Role,Module,Activity,Summary,Target,Outcome`.
  - Correctly quotes fields containing commas, quotes, and newlines.
  - Extracts clean summary and target identifier from both legacy and JSON logs.

- [ ] **Step 2: Run test to confirm it fails**
  Run: `cd frontend; node --experimental-transform-types --test src/features/audit-log/lib/__tests__/auditExport.test.ts`

- [ ] **Step 3: Implement `types.ts`, `api.ts`, and `auditExport.ts`**
  - In `types.ts`: add `module` and `role` to `AuditLogFilters`; add `module?: string` to `AuditLogItem`; define `AuditDetailPayload`.
  - In `api.ts`: pass `module` and `role` params to `/api/manager/audit-logs`.
  - In `auditExport.ts`: implement `exportAuditLogsToCsv(items: AuditLogItem[])` and trigger browser file download.

- [ ] **Step 4: Verify test passes**
  Run: `cd frontend; node --experimental-transform-types --test src/features/audit-log/lib/__tests__/auditExport.test.ts`

- [ ] **Step 5: Commit changes**
  Commit: `feat(audit): add audit filters, DTO module support, and CSV export utility`

---

### Task 5: Frontend Before/After Diff Parser and Detail Modal Component

**Files:**
- Create: `frontend/src/features/audit-log/lib/auditDiff.ts`
- Create: `frontend/src/features/audit-log/lib/__tests__/auditDiff.test.ts`
- Create: `frontend/src/features/audit-log/components/AuditLogDetailModal.tsx`

- [ ] **Step 1: Write unit tests for `auditDiff.ts`**
  Create `frontend/src/features/audit-log/lib/__tests__/auditDiff.test.ts` verifying:
  - Parses JSON `details` safely into `AuditDetailPayload`.
  - Fallback parses legacy string into `{ summary: text }`.
  - `computeDiffRows(before, after)` extracts all unique keys, marks keys where `before !== after` as changed (`isChanged: true`), and formats boolean/null/date values.

- [ ] **Step 2: Run test to confirm it fails**
  Run: `cd frontend; node --experimental-transform-types --test src/features/audit-log/lib/__tests__/auditDiff.test.ts`

- [ ] **Step 3: Implement `auditDiff.ts` and `AuditLogDetailModal.tsx`**
  - Implement `parseAuditDetails` and `computeDiffRows` in `auditDiff.ts`.
  - Create `AuditLogDetailModal.tsx` using RTMS design tokens:
    - Modal backdrop and center container.
    - Header: Event Title, Outcome Badge, formatted ISO date.
    - Actor section: Name, email, role badge.
    - Target section: Entity name, ID, and primary link button `"Open Related Record ↗"` pointing to `targetUrl` or derived universal entity route.
    - Reason callout: Rendered if `reason` is present.
    - 2-column Before vs After table: Field name, Previous State, New State, with soft amber/emerald background for rows with `isChanged === true`.

- [ ] **Step 4: Verify tests pass**
  Run: `cd frontend; node --experimental-transform-types --test src/features/audit-log/lib/__tests__/auditDiff.test.ts`

- [ ] **Step 5: Commit changes**
  Commit: `feat(audit): implement audit diff parser and AuditLogDetailModal component`

---

### Task 6: Integrate Executive Audit Log View with Filters, Export & Deep Links

**Files:**
- Modify: `frontend/src/features/audit-log/components/ManagerAuditLogView.tsx`

- [ ] **Step 1: Update `ManagerAuditLogView.tsx`**
  - Add `Module` Select filter (`All Modules`, `Admissions`, `Medical & Health`, `Stable & Stalls`, `Horses`, `Staff & Security`, `Training`).
  - Add `Role` Select filter (`All Roles`, `Club Manager`, `Head Trainer`, `Veterinarian`, `Groom`, `Horse Owner`).
  - Add "Export CSV" Button in filter bar or header.
  - In Table body:
    - Render clickable Entity Tag linking to `targetUrl` or universal entity route (e.g. `/manager/admissions/12`, `/manager/horses/49`).
    - Add "Inspect" action button on each row and allow clicking row to open `AuditLogDetailModal`.
  - Connect state for `selectedLog: AuditLogItem | null` to render `AuditLogDetailModal`.

- [ ] **Step 2: Run TypeScript and test validation**
  Run: `cd frontend; npx tsc --noEmit; npm run test`

- [ ] **Step 3: Commit changes**
  Commit: `feat(audit): integrate module/role filters, CSV export, and detail modal in ManagerAuditLogView`

---

### Task 7: Full System Verification, Trail Check & Final Commit Review

- [ ] **Step 1: Verify backend compiles and tests pass**
  Run: `cd backend; .\mvnw.cmd test`

- [ ] **Step 2: Verify frontend builds and tests pass**
  Run: `cd frontend; npx tsc --noEmit; npm run test`

- [ ] **Step 3: Run trail check**
  Run: `npx trail check`

- [ ] **Step 4: Record learning note**
  Record key architecture decisions (Tier 1 vs Tier 2 deduplication, zero-migration JSON details, Before/After diff model) via `trail note`.

- [ ] **Step 5: Prepare final review report**
  Provide clear walk-through of changes and verification evidence to user.

