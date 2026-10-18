# Horse sex

- JSON property: `sex`, values `MALE` or `FEMALE`. A gelding is `MALE`; castration is not represented as a third sex.
- New Owner Admission submissions and direct Horse creation require this property. Missing/null, numeric ordinals and unsupported labels return HTTP 400 before saving or uploading files.
- Stored Horse records and Admission snapshots may have `sex: null` for legacy data. The UI shows **Not recorded**; the migration does not invent values.
- Owner submission persists the sex in `candidate_horse_profiles`. Groom arrival copies it to `horses`, including readmission of a rejected horse. A legacy null snapshot never clears an existing Horse sex.
- Admission snapshot locking still applies to this field after Groom starts review. Historical snapshots are not rewritten from the current Horse record.
- Horse list/detail JSON exposes `sex` alongside the existing fields. Shared HorseTable, HorseOverview and AdmissionDetailHeader display it. Actor-specific Horse screens can use `formatHorseSex` from the horses feature; their layouts and business rules are unchanged.
- No changes to eligibility, medical decisions, training plans, stable capacity, assignments or permissions.

## Deployment and OCR

Apply `V202610171503__add_horse_sex.sql` through Flyway and deploy the backend and frontend together. Existing API clients creating Horses or Admissions must add `sex`.

OCR must fill the editable `sex` field, using the same request DTO. Use an empty selection when extraction is uncertain; the Owner must choose before submitting. Do not infer sex from the name or a stock photo.

Seed manifests still need the sourced sex mapped into the request JSON before cloud seeding. No cloud data is changed by this implementation.
