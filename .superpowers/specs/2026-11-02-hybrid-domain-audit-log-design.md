# Hybrid Domain Audit Log System Design

**Date:** 2026-11-02  
**Author:** Antigravity (Pair Programming with Manager / Senior BA)  
**Status:** Pending Approval  
**Target File:** `.superpowers/specs/2026-11-02-hybrid-domain-audit-log-design.md`

---

## 1. Intent & The Why

A racehorse training center manages elite animal assets valued from tens of thousands to millions of dollars. Operations are subject to stringent regulations from international Racing Authorities (Jockey Clubs), Stud Books, and strict anti-doping controls.

The current Audit Log system was designed with an HTTP interceptor (`HttpAuditInterceptor`) and URL pattern matcher (`AuditActivityResolver`). While this succeeded in masking raw technical HTTP queries (`POST /api/admissions/1/vet-review`), it remains a **shallow audit log**:
- It records *that* an event happened, but cannot show **what changed** (no before/after diff snapshot).
- It cannot capture **why** an action was taken (missing business justification/reason, e.g., clinical diagnosis or rejection notes).
- It lacks deep-linking back to the affected entity (e.g., clicking on `#12` to open the horse admission dossier).
- Filters are generic (search and date only), lacking horse-industry domain modules (Medical, Stable, Admissions, Lifecycle).

This project elevates the Audit Log into a **Production-Grade, Legally Compliant Activity & Audit Log** tailored to executive oversight, racing compliance, and dispute resolution with horse owners.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **Full Operational Traceability**: Provide Before vs After diff snapshots and business reasons for critical operational decisions.
- **Zero-Migration Backward Compatibility**: Store rich structured event payloads within the existing `details` (TEXT) column of the `audit_logs` table, allowing seamless coexistence with legacy text-based logs.
- **Phase 1 Priority on 4 High-Risk Domains**:
  1. *Veterinary & Medication*: Preventive protocols, care reschedule, clinical examinations, and treatment plans.
  2. *Intake & Admissions*: Groom review, vet intake exam, trainer fitness assessment, manager final approval, and arrival confirmation.
  3. *Stable & Stalls*: Stall reallocations, quarantine movements, and trainer/groom area assignments.
  4. *Horse Lifecycle & Staff*: Horse lifecycle transitions (`ACTIVE`, `INJURED`, `RETIRED`, `DECEASED`) and staff deactivations.
- **Executive UI with Modal Dialog & Entity Navigation**:
  - Filter by Domain Module (`Admissions`, `Medical & Health`, `Stable & Stalls`, `Horses`, `Staff & Security`, `Training`).
  - Filter by Actor Role (`Club Manager`, `Head Trainer`, `Veterinarian`, `Groom`, `Horse Owner`).
  - Clickable entity links to navigate directly to admissions, horse profiles, or staff directories (`Universal Entity Redirection`).
  - Modal Dialog for deep inspection: Actor card, event summary, business reason, and a 2-column Before/After comparison table with highlighted delta values.
  - Export Audit Trail to CSV for racing stewards and auditing.

### Non-Goals
- Altering the PostgreSQL database schema (no new tables or column alterations).
- Audit logging of read operations (`GET` requests), to avoid log bloating and performance degradation.
- Full-database automated change-data-capture (CDC) for low-level internal columns (e.g. system timestamps, temporary cache tokens).

### Anti-Goals
- Breaking existing audit queries or failing write transactions if audit serialization fails (audit must remain fail-safe and asynchronous/isolated via `@Transactional(propagation = Propagation.REQUIRES_NEW)`).
- Hardcoding user-facing Vietnamese strings in frontend component templates (strict compliance with project rule: English UI text, Vietnamese conversational communication).

---

## 3. Constraints

- **Language & Runtime**: Java 21 / Spring Boot 3 on backend; Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS on frontend.
- **Database**: PostgreSQL with existing Flyway migrations. Zero DDL alterations required.
- **Code Conventions**:
  - Follow `.agents/rules/java-import-style.md` (no wildcard imports, explicit imports).
  - Follow `.agents/rules/universal-entity-redirection.md` (consistent entity links across roles).
  - Strictly English UI text, tooltips, and badges.

---

## 4. Evaluated Approaches & Selected Architecture

### Approaches Considered

1. **Option A: Pure HTTP Interceptor (Current)**
   - *Pros*: Completely automatic, zero maintenance for developers.
   - *Cons*: Cannot read request body streams safely without caching filters; cannot know database state before the mutation; lacks domain context and business reasons.
   
2. **Option B: Database-Level CDC / Hibernate Envers**
   - *Pros*: Automatically diffs all database tables.
   - *Cons*: Generates overwhelming noise (internal foreign keys, version numbers); lacks business intent (e.g., cannot capture why a vet rejected an intake); requires heavy schema alterations and multiple audit shadow tables.

3. **Option C: Hybrid 2-Tier Architecture (Selected & Approved)**
   - *Tier 1 (Broad Coverage)*: Retain `HttpAuditInterceptor` + `AuditActivityResolver` as a catch-all safety net for standard CRUD operations across the system.
   - *Tier 2 (Deep Domain Audit)*: Introduce a lightweight `DomainAuditPublisher` (or custom event listener) at the Service layer for the 4 critical domains. Services pass domain snapshots (`before`, `after`, `reason`, `targetUrl`) which are serialized into clean structured JSON inside `details`.

---

## 5. Detailed Design & Decisions

### 5.1. Backend Architecture

#### Data Contract in `audit_logs.details`:
When recorded via Tier-2 Domain Audit, the `details` field contains a JSON payload adhering to:
```json
{
  "summary": "Veterinarian completed intake exam and cleared horse for training",
  "reason": "Normal cardiac auscultation, negative flexion tests, vaccinations valid",
  "module": "MEDICAL",
  "targetUrl": "/manager/admissions/12",
  "before": {
    "status": "VET_REVIEW",
    "healthStatus": "PENDING",
    "quarantineStall": "Q-01"
  },
  "after": {
    "status": "TRAINER_REVIEW",
    "healthStatus": "CLEARED",
    "quarantineStall": "Q-01"
  }
}
```
*Legacy logs or Tier-1 fallback logs remain plain English text.*

#### Domain Event Publication:
- Create `DomainAuditEvent`:
  - `entityName`: e.g. `"AdmissionApplication"`, `"CareSchedule"`, `"StableStall"`, `"Horse"`, `"User"`
  - `entityId`: e.g. `12L`
  - `action`: e.g. `"VET_INTAKE_EXAM"`, `"STALL_REALLOCATION"`, `"CARE_RESCHEDULE"`, `"HORSE_STATUS_CHANGE"`
  - `module`: `"ADMISSIONS" | "MEDICAL" | "STABLE" | "HORSES" | "STAFF" | "TRAINING"`
  - `summary`: Short business sentence.
  - `reason`: Operator's provided notes or reason.
  - `before`: Map/DTO of key pre-change attributes.
  - `after`: Map/DTO of key post-change attributes.
  - `targetUrl`: Relative app link for quick access.
- `AuditLogService.recordDomainEvent(...)` runs in `Propagation.REQUIRES_NEW` with try-catch isolation.

#### Controller & Query Enhancements:
- `ManagerAuditLogController`:
  - Support query parameter `@RequestParam(required = false) String module` (filters by entity categories or JSON module tag).
  - Support query parameter `@RequestParam(required = false) String role` (filters by user role).

---

### 5.2. Frontend Architecture (`ManagerAuditLogView.tsx`)

#### State & Types:
- Define `AuditDetailPayload`:
  ```typescript
  export interface AuditDetailPayload {
    summary?: string;
    reason?: string;
    module?: 'ADMISSIONS' | 'MEDICAL' | 'STABLE' | 'HORSES' | 'STAFF' | 'TRAINING';
    targetUrl?: string;
    before?: Record<string, unknown>;
    after?: Record<string, unknown>;
  }
  ```
- Parse helper `parseAuditDetails(log: AuditLogItem)`: Detects if `log.details` starts with `{`, parses JSON safely into `AuditDetailPayload`, otherwise wraps text into `{ summary: log.details }`.

#### UI Components & Interactions:
1. **FilterBar Enhancements**:
   - `Module Filter`: All Modules, Admissions, Medical & Health, Stable & Stalls, Horses & Pedigree, Staff & Security, Training.
   - `Role Filter`: All Roles, Club Manager, Head Trainer, Veterinarian, Groom, Horse Owner.
   - `Search Input`: Free-text search by actor, summary, or ID.
   - `Date Range`: From date - To date.
   - `Result Status`: All, Success, Failed.
   - `Export Button`: "Export CSV" triggering browser download of filtered records formatted with timestamp, actor, role, module, action, summary, outcome.

2. **Audit Table Enhancement**:
   - Clean column layout: `Timestamp`, `Actor`, `Role`, `Module / Activity`, `Summary & Target`, `Outcome`.
   - Clickable Entity Tag: If `targetUrl` is present (or derived from `entityName` and `entityId`), renders a distinct link badge with icon.
   - Row click or "Inspect" button opens the **Inspection Modal Dialog**.

3. **Inspection Modal Dialog (`AuditLogDetailModal.tsx`)**:
   - **Header**: Event Action name, outcome badge (Success / Failed), precise timestamp.
   - **Actor Card**: Full name, email, role badge, avatar.
   - **Target Card**: Entity Name, Entity ID, and a primary button *"Open Related Record"* navigating directly to `targetUrl`.
   - **Reason Box**: Rendered when `reason` is present (styled in neutral tinted callout with quotation icon).
   - **Before vs After Diff Table**:
     - 2-column comparative table (`Field`, `Previous State`, `New State`).
     - Rows where `before[key] !== after[key]` are highlighted with soft amber/emerald badges for fast scanning.
     - Formats dates, booleans, and null values gracefully (e.g. `None` / `Empty`).

---

## 6. Architecture & Implementation Decisions (Resolved from Builder Check)

1. **Deduplication Between Tier-1 (HTTP Interceptor) & Tier-2 (Domain Event)**:
   - When a service records a Tier-2 `DomainAuditEvent` in the current HTTP request thread, `AuditLogService` sets a request attribute `request.setAttribute("DOMAIN_AUDIT_LOGGED", Boolean.TRUE)`.
   - `HttpAuditInterceptor.afterCompletion` checks `request.getAttribute("DOMAIN_AUDIT_LOGGED")`. If `TRUE`, it silently skips recording Tier-1 to prevent duplicate audit entries for the same mutation.
   - If an unhandled error occurs before the service can execute or for non-tier-2 routes, Tier-1 records the failed attempt as a safety net.

2. **Backend Query Filtering Without Schema Alteration**:
   - `AuditLogService.getAuditLogs` maps the incoming `module` filter string to corresponding `entityName` sets:
     - `ADMISSIONS` -> `AdmissionApplication`
     - `MEDICAL` -> `CareSchedule`, `HealthRecord`, `TreatmentPlan`, `PreventiveCareSchedule`
     - `STABLE` -> `StableStall`, `Area`, `GroomDailyTask`
     - `HORSES` -> `Horse`
     - `STAFF` -> `User`
     - `TRAINING` -> `Course`, `HorseTrainingPlan`, `Subject`, `TrainingLot`, `TrainingWorkout`, `TrainingRule`
   - It additionally queries `details LIKE '%"module":"<MODULE>"%'` so both new JSON logs and historical/legacy logs are accurately returned.
   - For `role` filtering, it queries `user.role.name = :role` via JPA Specifications.

3. **Entity Target Routing for Manager**:
   - Follows `.agents/rules/universal-entity-redirection.md`:
     - Admissions: `/manager/admissions/{id}`
     - Horses: `/manager/horses/{id}`
     - Stalls / Barns: `/manager/stable`
     - Users / Staff: `/manager/accounts?userId={id}`
     - Medical / Care Schedules: Links to the associated horse profile `/manager/horses/{horseId}` or admission dossier `/manager/admissions/{admissionId}`.

---

## 7. What's Left to the Builder

- Implementing the Spring Event Publisher `DomainAuditPublisher` or direct helper calls in the designated 4 service domains.
- Adding unit tests for JSON serialization and parsing in backend and frontend.
- Implementing the CSV exporter utility with proper escaping of quotes and commas.

