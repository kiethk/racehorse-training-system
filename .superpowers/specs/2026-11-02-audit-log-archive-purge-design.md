# Specification: Audit Log Archival & Database Purge (Archive & Purge)

## 1. Problem Statement & Motivation
In an active racehorse training management facility, audit logging generates substantial telemetry (HTTP requests) and domain state snapshots. Over time, table bloat in `audit_logs` degrades PostgreSQL query performance and inflates backup sizes.

Rather than relying solely on automated silent background cron jobs that lack visibility during academic defense or compliance reviews, this feature provides an **interactive, auditable, enterprise-grade "Archive & Purge" maintenance capability** for Club Managers.

## 2. Core User Requirements & Workflow
1. **Always-Accessible Maintenance Tool**:
   - The `Archive & Purge` button is permanently present on the `/manager/audit-log` toolbar.
   - **Normal State**: Clean neutral appearance (`bg-white` / `dark:bg-zinc-900`, standard border and text).
   - **Alert State**: When logs older than 30 days exist (or log volume exceeds threshold), the button highlights in rose/red border with a red dot indicator. Hovering displays a tooltip indicating the count of aged logs requiring archival.
2. **Flexible Range Selection in Modal**:
   - **Preset 1 (Default)**: Logs older than 30 days (`before cutoff = NOW() - 30 days`).
   - **Preset 2**: Logs older than 60 days.
   - **Preset 3**: Custom date range (`From` date to `To` date).
   - Live counter shows the exact number of records eligible for archival.
3. **Safety & Data Preservation Invariant**:
   - **Mandatory Download Before Deletion**: The system downloads the comprehensive CSV archive to the Manager's local machine before deleting any database records.
   - **Immutable Audit Trail Retention**: After deleting old records, the system writes a permanent Domain Audit Event documenting who purged the logs, how many records were purged, the date range, and the timestamp.

---

## 3. Technical Architecture

### 3.1 Backend Components (`com.rtms.backend.audit`)
1. **DTOs**:
   - `AuditArchiveStatusResponse`:
     - `totalCount`: Total logs in database.
     - `agedCount`: Count of logs older than 30 days.
     - `oldestLogDate`: Timestamp of earliest record.
     - `requiresMaintenance`: `true` if `agedCount > 0` or `totalCount > 1000`.
   - `AuditPurgeRequest`:
     - `beforeDate`: Upper bound timestamp (exclusive).
     - `fromDate`: Optional lower bound timestamp.
     - `reason`: Operational note (e.g., "Monthly maintenance archive").
   - `AuditPurgeResponse`:
     - `purgedCount`: Number of records deleted.
     - `purgedAt`: Timestamp of execution.
2. **Repository Query (`AuditLogRepository.java`)**:
   - `long countByCreatedAtBefore(LocalDateTime cutoff)`
   - `long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to)`
   - `List<AuditLog> findByCreatedAtBeforeOrderByCreatedAtAsc(LocalDateTime cutoff)`
   - `List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to)`
   - `@Modifying @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoff") int deleteByCreatedAtBefore(...)`
   - `@Modifying @Query("DELETE FROM AuditLog a WHERE a.createdAt >= :from AND a.createdAt <= :to") int deleteByCreatedAtBetween(...)`
3. **Service Layer (`AuditLogService.java`)**:
   - `getArchiveStatus()`: Computes status, aged counts, and maintenance flags.
   - `exportLogsForArchive(LocalDateTime from, LocalDateTime to, LocalDateTime before)`: Queries logs to build CSV rows.
   - `purgeLogs(Long actorUserId, AuditPurgeRequest request)`:
     - Deletes matching records within a transactional boundary.
     - Records 1 `DomainAuditEvent`:
       - `action`: `"ARCHIVE_AND_PURGE_AUDIT_LOGS"`
       - `entityName`: `"AuditLog"`
       - `module`: `"SYSTEM"`
       - `summary`: `"Manager executed audit log archive & purge (" + purgedCount + " records removed)"`
       - `reason`: `request.reason()`
4. **Controller Endpoints (`ManagerAuditLogController.java`)**:
   - `GET /api/manager/audit-logs/archive-status`: Returns `AuditArchiveStatusResponse`.
   - `GET /api/manager/audit-logs/export-archive`: Streams CSV file download (`Content-Disposition: attachment; filename="audit_archive_..."`).
   - `POST /api/manager/audit-logs/purge`: Executes the deletion and records the audit marker.

---

### 3.2 Frontend Components (`src/features/audit-log`)
1. **API Client (`services/api.ts`)**:
   - `getArchiveStatus()`: Calls status endpoint.
   - `downloadArchiveCsv(params)`: Triggers blob download directly from backend or local items.
   - `purgeAuditLogs(request)`: Sends purge request.
2. **Modal Component (`AuditLogArchiveModal.tsx`)**:
   - Displays selectable range presets (30 days, 60 days, custom).
   - Displays real-time matching count.
   - Disables submit if 0 records match.
   - Two-step execution:
     - Step 1: Downloads CSV file to user's computer.
     - Step 2: Executes API purge and closes modal on success.
3. **Toolbar Integration (`ManagerAuditLogView.tsx`)**:
   - Displays `Archive & Purge` button next to `Export CSV`.
   - Dynamic style:
     - When `requiresMaintenance` is true: Red/rose alert border, red status dot, tooltip with aged log count.
     - When false: Clean neutral white button.
   - Refresh table on successful purge.

---

## 4. Verification & Testing Strategy
1. **Unit Tests (Backend)**:
   - Verify `getArchiveStatus` returns correct counts and flags.
   - Verify `deleteByCreatedAtBefore` correctly removes aged records while preserving records newer than cutoff.
   - Verify the audit marker event is generated upon purge.
2. **Unit Tests (Frontend)**:
   - Verify `AuditLogArchiveModal` range calculations.
   - Verify modal downloads CSV before triggering delete API.
3. **End-to-End Verification**:
   - Create logs with past timestamps $\rightarrow$ check alert indicator $\rightarrow$ execute archive & purge $\rightarrow$ verify CSV file downloaded $\rightarrow$ verify database table reduced $\rightarrow$ verify single new audit marker row created.

