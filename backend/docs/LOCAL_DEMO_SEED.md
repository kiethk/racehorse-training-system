# Local demo seed

Manual tool for the reviewed 48-Horse dataset. Does not run on backend startup or during normal tests.

## What is created

- 48 Horses: 42 approved in regular Areas; 6 Candidates in Q-A.
- 54 Admissions: 42 approved; 2 each in Vet/Trainer/Manager Review; 2 Groom Review; 2 Waiting for Arrival; 1 Arrival Expired; 1 Rejected.
- 6 Areas / 72 Stalls. A–D: 9 Horses each; E: 6; Q-A: 6 occupied + 2 reserved.
- 30 Staff: 3 Trainers, 3 Vets, 24 Grooms. Each Groom owns one 3-stall block. Q Grooms have no Trainer.
- 8 fictional Owners sharing 48 Horses: 9, 8, 7, 6, 5, 5, 4, 4 respectively. The original `owner@rtms.com` keeps ID 5, password and avatar. Seven accounts are registered through `AuthService.registerOwner`.
- 324 uploaded files: 54 real JPG files and 270 separate PDF files. Photos are illustrative; medical/workflow records are explicitly DEMO.
- Supporting Admission records only: 46 initial examination findings and 44 Trainer admission assessments. No training plans, lots, workouts, metrics, racing or daily tasks.

Horse creation, reservations, arrival, clinical review, Trainer assessment and Manager decision call the real services. Admission files pass the real file validation/storage implementation. Historical dates use thread-scoped simulated time in the seed JVM; no public backdate API or runtime application clock change.

The tool removes only the three identified migration Horses and their legacy Admission/care/pedigree fixture rows. It normalizes the nine existing Staff through their service/entity, preserves IDs/password hashes and the four Subjects, and creates 21 Grooms through `StaffManagementService`. The old migration accounts/Areas/Stalls are dated before the demo admissions. These fixture cleanup/metadata operations use targeted database statements; Horse and Admission creation are not SQL inserts.

## Run

From the backend directory, with Docker `rtms-postgres` running:

```powershell
.\scripts\seed-local-demo.ps1 -AssetsRoot 'C:\Users\kieth\Documents\Codex\RTMS_Seed_Review_2026-10-17'
```

- Hard restricted to PostgreSQL at `localhost:5433`, database `rtms_db` or an explicitly named local rehearsal database.
- Ignores `.env.cloud`, pins the local DB account, disables Cloudinary, stores files in `backend/uploads/admissions`.
- Backend may keep running on port 8080. Its storage service can read `local:` keys even when Cloudinary is configured.
- Saves a local database backup under `target/demo-seed` before execution.
- Validates all 324 source checksums before cleanup. Refuses data that differ from the expected migration fixtures, including an already seeded database.
- Does not truncate tables or disable snapshot protection triggers. Parent Admission deletion uses the existing cascade.
- Each intake's submission/reservation/arrival/examination commits as one transaction. Real after-commit Trainer assignment runs before Trainer/Manager review.
- A failure stops the run. Preparation rolls back as a unit; already completed intakes can remain. Inspect the log/backup before retrying; do not blindly rerun or clear another database.

## Verify without reseeding

```powershell
.\scripts\seed-local-demo.ps1 -AssetsRoot 'C:\Users\kieth\Documents\Codex\RTMS_Seed_Review_2026-10-17' -VerifyOnly
```

Checks row counts, assignment constraints, Admission chronology/placement, all stored file SHA-256 hashes, 48 authenticated Horse photo HTTP responses and Admission detail access for all five assigned actors. Results live in `target/demo-seed/rtms_db-result.json`; logs and database backups are ignored by Git.

With the eight-Owner pack, verification also checks 56 Owner API requests: exact admission lists and horse counts, own detail access, and forbidden foreign admission/document/horse/photo access. No submitted PDF or immutable snapshot is edited. PDF registration references contain a generic demo Owner association, not a named real owner's identity or proof of ownership.

## Refine an existing one-Owner local seed

```powershell
.\scripts\seed-local-demo.ps1 -AssetsRoot 'C:\Users\kieth\Documents\Codex\RTMS_Seed_Review_2026-10-17' -AddOwners
```

This is a one-time seed refinement, not a runtime ownership transfer. It requires the exact recorded 48-Horse/54-Admission local dataset with 32 existing accounts, a single Owner and no Owner notifications. It backs up first, registers seven Owners, fills the demo Owner's name/missing phone/address and corrects the planned Owner associations on Horses and Admissions in one transaction. IDs, passwords, avatar, admission states, timestamps, assignments, clinical records, audit logs, subjects, documents and source file contents are preserved. Structural and positive access checks run before commit; expected access-denial checks run after commit to avoid marking the mutation transaction rollback-only. A report is written only after successful commit/verification. A second AddOwners run is rejected; use VerifyOnly afterward.

The same owner mapping is used for future fresh local seeds; no re-creation of the current dataset is needed.

## Accounts

Existing migration passwords are unchanged (fresh migration default: `admin123`). Login email changes:

| Old email | New email | Name |
|---|---|---|
| trainer@rtms.com | trainer3@rtms.com | Daniel Foster |
| vet@rtms.com | vet3@rtms.com | Hannah Reed |
| groom@rtms.com | alex.carter@demo.rtms.example | Alex Carter |

Trainer ID 2 still owns the four existing Subjects. Manager `admin@rtms.com`, Owner `owner@rtms.com`, Trainer/Vet 1–2 and Groom 1–2 retain their emails.

The 21 newly created Grooms use the script's `-NewGroomPassword` value (local demo default `RtmsDemo@2026`). The reviewed JSON lists their names/emails. No email/invitation is sent.

Owners `owner2@rtms.com` through `owner8@rtms.com` use `-NewOwnerPassword` (local demo default `RtmsDemo@2026`). The existing `owner@rtms.com` password is unchanged (`admin123` on fresh fixtures). All Owner names/contact details/horse associations are fictional demo data; public horse identities remain sourced references. Per-profile `owner-binding.json`, `owners.json` and `OWNERS.md` record the mapping without putting credentials into the shareable asset archive.

## Cloud handoff

This local runner deliberately rejects Neon and does not upload to Cloudinary. Running `run-cloud.ps1` does not seed data automatically. After local approval, use the separate `scripts/seed-cloud-demo.ps1` runner described in `CLOUD_DEMO_SEED.md`; it uploads original files through the configured storage service. Do not import the local DB's `local:` file keys into Neon.
