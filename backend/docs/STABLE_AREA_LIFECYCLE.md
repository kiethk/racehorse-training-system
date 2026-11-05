# Stable Area deletion and restoration

Manager Stable supports deleting one unused Area at a time. The operation soft-deletes the Area and all 12 stalls; it does not change `StallStatus` or remove database rows. Bulk deletion and retiring facilities with historical usage are outside this feature.

## Rules

- Both Regular and Quarantine Areas are supported.
- Exactly 12 stalls, numbered 1–12, must be present and active.
- No Trainer on the Area, no Groom on any stall, all stalls `AVAILABLE`.
- No horse may reference a stall, regardless of its displayed status.
- No Admission may reference a quarantine stall, including closed/rejected Admissions.
- Deletion never automatically unassigns staff, moves horses or edits Admission history.

## API

| Request | Permission | Result |
|---|---|---|
| `GET /api/areas/{id}/deletion-check` | `AREA_DELETE` | `{ areaId, areaCode, stallCount, canDelete, reasons }` |
| `DELETE /api/areas/{id}` | `AREA_DELETE` | `{ areaId, areaCode, stallCount }` |
| Existing `POST /api/areas` | `AREA_CREATE` | Existing Area response shape; restores an eligible deleted Area of the requested type before creating a new one |

All responses use `ApiResponse`. A deletion check is informational; DELETE always revalidates under locks. A blocked deletion returns 409. Missing/deleted IDs return 404. The migration grants `AREA_DELETE` to `CLUB_MANAGER`.

## Restoration

Add area picks an eligible archived Area of the same type, ordered by code length then alphabetically. It clears `deleted_at` on the Area and its original 12 stalls in one transaction. IDs, codes and stall numbering are retained. Inconsistent or newly referenced archived Areas are skipped. If none qualifies, the existing generator allocates a new code using all previously allocated codes, including archived ones. Regular codes skip Q and continue Z → AA; Quarantine codes continue Q-Z → Q-AA.

## Shared backend behavior

- `Area` and `StableStall` use `@SQLRestriction("deleted_at IS NULL")`. The timestamp is read-only in JSON. Normal ORM lookups hide deleted records. Repository `delete()` is **not** the lifecycle API; call `AreaLifecycleService`.
- Native allocation, capacity, staff-block and assignment queries explicitly exclude deleted rows. Hibernate does not rewrite handwritten SQL.
- Deletion/restoration use `STABLE_ASSIGNMENTS_LOCK`, then Area locks and stall locks in ID order. Creation also retains its per-type advisory lock.
- Admission queries lock only stall rows (`FOR UPDATE OF ss`) to avoid reversing the Area → stall lock order. The service checks status and references again after acquiring locks.
- Moving a horse into a stall locks an active target through `findActiveByIdForUpdate`; a stale screen cannot assign into an archived stall.
- Lifecycle SQL flushes pending ORM writes and clears the persistence context after mutation. These are request-boundary operations; reload managed entities after calling them within a larger transaction.

Trainer/Groom UI and daily-task behavior are unchanged. Team regression checks should cover floor plans, available stall pickers, Trainer/Groom auto-assignment and moving horses. Admission checks should cover capacity, reservation, arrival and Regular stall allocation.

## Manager UI

### Trainer reassignment

- Manager Stable reads `GET /api/areas/trainer-assignment-checks` (`AREA_UPDATE`) for each Area's current Trainer, eligibility and blocking reasons. These checks are advisory; assignment writes revalidate after taking the existing Area/stall locks.
- Assigning, changing or removing a Trainer requires an empty Regular Area: no horse references and no `OCCUPIED`/`RESERVED` stalls.
- Automatic Area allocation when creating a Trainer follows the same rule: it locks candidate stalls and skips blocked Areas, while still allowing the Staff account to be created without an Area.
- The outgoing Trainer must have no `UPCOMING`/`ACTIVE` plans, `SCHEDULED` lots or unfinished workouts in non-cancelled lots. Lots may span several Areas, so this check covers the outgoing Trainer's entire schedule rather than guessing which Area owns a lot. An incoming Trainer may keep their existing work; nothing is transferred.
- Completed/cancelled history remains unchanged. A no-op Trainer edit does not block Groom changes. Quarantine Areas still have no Trainer.
- Occupied or busy Trainer slots cannot be dragged, replaced or removed. A lock icon marks unavailable slots; failed writes report blocking reasons. Local draft validation also checks both ends of a drag operation. Direct API calls receive 409, and a blocked Trainer edit rejects the entire assignment batch before any Groom/workout changes.

### Area deletion

Each Area has a top-right options menu with **Delete area**. A confirmation checks the server's eligibility and shows blocking reasons. Pending assignment edits must be saved or explicitly discarded first. Successful deletion removes the card and its stalls locally before refreshing; a failed refresh offers GET retry and cannot repeat the committed deletion. Add area may restore an archived Area, so success wording is **added**.

### Automatic allocation when creating Staff

- Staff creation keeps automatic allocation. Trainer creation takes up to two eligible empty Regular Areas in generated code order (`A..Z, AA..`). Quarantine Areas are excluded.
- Groom creation considers unassigned consecutive three-stall blocks in both Regular and Quarantine Areas. Regular Areas require an active Trainer. Q Groom profiles always have `trainer_id = null`.
- Blocks with horses take priority. Remaining ties use the fewest assigned Grooms in the reporting group, then the most free blocks. A Regular reporting group is a Trainer's Areas; each Q Area forms its own group. Regular groups win an exact Regular/Q tie; Trainer ID, generated Area code and stall order keep the result deterministic.
- Creation takes `STABLE_ASSIGNMENTS_LOCK` before reading candidates, then re-reads locked entities before assigning. Concurrent creations cannot overwrite each other's assignments. No capacity still creates an active Staff account with no allocation.
- Basic fields and role-specific credentials validate before persistence. Invalid input returns 400; duplicate email, including concurrent submissions, returns 409. Account, role profile and allocation commit or roll back together.
- The create dialog shows field errors and prevents repeat submissions. Its result reports the actual Area, three stalls and Regular Trainer, or an unassigned state. Q results show the Area type instead of a Trainer. `Open Stable` leads to the existing board.
- `StaffCreationPostgresTest` runs only with `RTMS_STAFF_TEST_DB_URL=jdbc:postgresql://localhost:5433/rtms_staff_test_<unique-name>`; it commits disposable fixtures and must never target an application DB.

Migration: `V202610162219__soft_delete_stable_areas_and_stalls.sql`. Restart the backend to apply it before using the new UI. Do not edit previously applied migrations.

## Verification

`AreaLifecyclePostgresTest` exercises real Hibernate restrictions, native SQL, permission checks, restoration, rollback, stale ORM state and both orders of concurrent deletion/allocation. Like `StableAssignmentPostgresTest`, enable it only with `RTMS_STABLE_TEST_DB_URL=jdbc:postgresql://localhost:5433/rtms_stable_test_<unique-name>` pointing to a disposable database. These tests commit fixtures; never point them at an application database.
