# Reviewed cloud demo seed

Manual operation for the reviewed local demo dataset. Does not run on application startup or normal tests. Uses the same `DemoSeedWorkflow` as the strictly local runner, with a separate explicitly enabled Neon/Cloudinary runner.

## Target and backup

- Reads `backend/.env.cloud` inside the invoking process; credentials never appear in CLI argument values.
- Requires SSL Neon, the exact configured JDBC target and a configured Cloudinary account. Local supplement mode is forbidden.
- Creates a PostgreSQL 18 custom-format backup in ignored `target/cloud-seed/backups`, validates its table of contents and SHA-256 before any seed operation. PostgreSQL 18 client image is required (`postgres:18`).
- Flyway is disabled in the seed process. Run the normal cloud backend first to apply migrations, then pause it for the historical seed so live schedulers cannot expire simulated past reservations. The seed context mocks maintenance schedulers only; business services and after-commit Trainer assignment remain real.
- Do not blindly rerun after a failure. Inspect the backup, checkpoint and actual DB state; preparation and completed intakes may already be committed.

## Commands

From `backend/`:

```powershell
.\scripts\seed-cloud-demo.ps1 -AssetsRoot 'C:\Users\kieth\Documents\Codex\RTMS_Seed_Review_2026-10-17' -Mode Preflight
.\scripts\seed-cloud-demo.ps1 -AssetsRoot 'C:\Users\kieth\Documents\Codex\RTMS_Seed_Review_2026-10-17' -Mode Seed
.\scripts\seed-cloud-demo.ps1 -AssetsRoot 'C:\Users\kieth\Documents\Codex\RTMS_Seed_Review_2026-10-17' -Mode Verify
```

Preflight checks fixture identity/dependencies and all 324 original file checksums, then uploads/reads/deletes one JPG and one PDF with the real private storage service. It does not seed DB rows.

Seed refuses an already populated dataset. It removes only the identified migration fixtures (Horses 5/6/7, their legacy care/pedigree/Admission snapshot cascade), preserves 4 Subjects and existing account IDs/passwords, normalizes 9 Staff, registers 7 additional Owners and creates 21 Grooms. It calls real Stable/Admission services to create and assign Areas/Stalls, upload each dossier and advance it to the reviewed target stage. Seed-only chronology/identity normalization is scoped to the manual workflow; no public backdating or ownership-transfer API is added. Snapshot protection triggers remain enabled.

## Result

- 48 Horses: 42 eligible in regular Areas, 6 candidates in Q-A.
- 54 Admissions, 30 Staff, 8 Owners, 6 Areas, 72 Stalls.
- 324 private Cloudinary assets: 54 JPGs and 270 PDFs. No `local:` storage keys are copied from the local DB.
- 46 initial health records and 44 Trainer intake assessments support the Admission workflow. No training plans, lots, workouts, metrics, racing or daily tasks are seeded.
- Every stored asset is downloaded through `FileStorageService` and checked against the original SHA-256; 48 Horse photo API checks, five actor detail checks and 56 Owner scope checks follow.
- Reports/checkpoints/logs/backups are under ignored `target/cloud-seed`. Logs redact configured connection/storage credentials, including JDBC URLs which can embed a password. They are not included in the distributable PDF archive.

Default new demo account password: `RtmsDemo@2026`, overridable with `-NewAccountPassword`. Existing passwords remain unchanged. New Owners use `owner2@rtms.com` through `owner8@rtms.com`; legacy Owner `owner@rtms.com` keeps ID 5. Trainer/Vet emails become `trainer1-3@rtms.com` / `vet1-3@rtms.com`; the legacy Groom becomes `alex.carter@demo.rtms.example`.

Public horse identity references, stock photographs and fictional clinical/workflow/ownership bindings are explicitly distinguished in the dossier. These documents are demo data, not official ownership or clinical certificates.

## Verified execution — 17 October 2026

Completed on the reviewed Neon target with 324 authenticated Cloudinary assets. All source SHA-256 checks, 48 Horse photo API checks, five actor detail checks and 56 Owner scope requests passed. The four existing Subjects were preserved. Real Admission services also created their initial examination schedules and in-app assignment notifications; no manual routine Groom tasks or training data were added.

Browser verification passed for Manager Horses, Stable, filtered Admissions and Horse Detail, plus 16 pages across all eight Owners. Private PDF download passed for Manager, the correct Owner and assigned Trainer/Groom/Vet; foreign Owner and anonymous access were rejected. PDF iframe and photo previews both loaded.

The Manager list exposed a pre-existing per-row query cost on the cloud DB. Its related snapshots/stalls/photos now load in batches (up to four list queries); eight AdmissionQueryService tests passed. The real 54-row HTTP request measured 36.9 seconds before and 2.5 seconds after the change. Other actor queue methods and authorization remain unchanged.

The cloud backend was restarted and left running for review. The local demo database remains separate. Credentials for newly created demo accounts are recorded in ignored `target/cloud-seed/accounts.csv`; existing account passwords are marked unchanged. No commit or push was performed.
