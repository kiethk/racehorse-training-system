# Implementation Plan: Audit Log Archival & Purge (Archive & Purge)

## Phase 1: Backend DTOs, Repository Queries & Service Logic
1. **Task 1.1: Create DTOs**
   - `AuditArchiveStatusResponse.java` (`totalCount`, `agedCount`, `oldestLogDate`, `requiresMaintenance`).
   - `AuditPurgeRequest.java` (`beforeDate`, `fromDate`, `reason`).
   - `AuditPurgeResponse.java` (`purgedCount`, `purgedAt`).
2. **Task 1.2: Extend `AuditLogRepository.java`**
   - Add counting methods: `countByCreatedAtBefore(LocalDateTime cutoff)`, `countByCreatedAtBetween(LocalDateTime from, LocalDateTime to)`.
   - Add delete methods with `@Modifying` and `@Transactional`:
     - `int deleteByCreatedAtBefore(LocalDateTime cutoff)`.
     - `int deleteByCreatedAtBetween(LocalDateTime from, LocalDateTime to)`.
   - Add query methods for export:
     - `List<AuditLog> findByCreatedAtBeforeOrderByCreatedAtAsc(LocalDateTime cutoff)`.
     - `List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to)`.
3. **Task 1.3: Implement Service Methods in `AuditLogService.java`**
   - `getArchiveStatus()`: Computes status, calculates aged logs (> 30 days), and checks threshold.
   - `exportLogsForArchive(LocalDateTime from, LocalDateTime to, LocalDateTime before)`: Prepares CSV formatted text.
   - `purgeLogs(Long actorUserId, AuditPurgeRequest request)`:
     - Executes deletion in transaction.
     - Automatically calls `recordDomainEvent` to log an immutable record of the archival action.
4. **Task 1.4: Add Endpoints to `ManagerAuditLogController.java`**
   - `GET /api/manager/audit-logs/archive-status`
   - `GET /api/manager/audit-logs/export-archive`
   - `POST /api/manager/audit-logs/purge`
5. **Task 1.5: Unit Tests for Backend**
   - Write `AuditLogArchivePurgeTest.java` covering counting, status check, CSV export, and safe purging with audit trail creation.

---

## Phase 2: Frontend API, Modal & Toolbar Integration
1. **Task 2.1: Extend Frontend Types & API**
   - Update `src/features/audit-log/types/index.ts` with `AuditArchiveStatus`, `AuditPurgeRequest`, `AuditPurgeResponse`.
   - Update `src/features/audit-log/services/api.ts` with:
     - `getArchiveStatus()`
     - `downloadArchiveCsv(params)`
     - `purgeLogs(request)`
2. **Task 2.2: Build `AuditLogArchiveModal.tsx`**
   - Clean enterprise design (no aggressive AI styling, neutral borders).
   - Range selection:
     - Preset 1: Older than 30 days (default)
     - Preset 2: Older than 60 days
     - Custom: Date pickers (from -> to)
   - Real-time count of matching logs.
   - Two-step handler:
     1. Downloads CSV archive (`audit_archive_...csv`).
     2. Calls purge API.
   - Toast feedback upon completion.
3. **Task 2.3: Integrate into `ManagerAuditLogView.tsx`**
   - Query archive status on page load.
   - Render `Archive & Purge` button on FilterBar:
     - Normal: Clean white background (`bg-white dark:bg-zinc-900 border border-slate-300 dark:border-zinc-700`).
     - Alert: Rose/red border with red dot when `requiresMaintenance` is true, plus tooltip indicating aged log count.
   - Click opens `AuditLogArchiveModal`.
   - On purge success: Reloads audit log table.
4. **Task 2.4: Frontend Unit Tests**
   - Unit tests for date range calculations and archive status handling.

---

## Phase 3: Verification & Polish
1. Run backend tests: `cd backend; .\mvnw.cmd test -Dtest="*Audit*"`
2. Run frontend tests: `cd frontend; npm run test`
3. Verify clean code style and import rules.

