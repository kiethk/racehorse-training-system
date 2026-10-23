RTMS PostgreSQL data export
Source: dump-rtms_db-202610230855.sql (PostgreSQL 16.15; pg_dump 17.0; 2026-10-23 08:55)

CONTENTS
- One UTF-8 CSV per PostgreSQL table, with the header row containing exact source column names/order.
- 46 COPY blocks exported; 0 INSERT statements found.
- NULL values use the unique marker __RTMS_NULL_20261023__; empty text remains an empty string. In DBeaver's CSV import settings set this exact value as the NULL string.
- CSV uses comma separator, double-quote quoting, and UTF-8 BOM for Excel/DBeaver detection.
- manifest.json lists columns and row counts.

DBEAVER IMPORT
1. Create/migrate the target schema first; CSV files contain data only and do not create tables, columns, types, constraints, extensions, or sequences.
2. In DBeaver select the target schema, then use Tools > Import Data (or right-click table > Import Data) and choose the matching CSV. Ensure first row is header, delimiter comma, quote ", encoding UTF-8, and NULL marker is __RTMS_NULL_20261023__. Map columns by name and review the preview before finishing.
3. Import application tables in the dependency order below when target foreign keys are active. Skip flyway_schema_history.csv for a database already managed by Flyway. The order lists parent tables before dependent tables.
4. After importing ID values, update each serial/identity sequence to at least MAX(id) before creating new rows.

IMPORT ORDER (foreign-key parents before dependent tables; skip flyway_schema_history unless intentionally restoring Flyway metadata)
1. flyway_schema_history
2. horse_body_regions
3. medical_symptom_mappings
4. permissions
5. preventive_protocols
6. rag_knowledge_documents
7. roles
8. rag_knowledge_embeddings
9. role_permissions
10. users
11. ai_conversations
12. areas
13. audit_logs
14. courses
15. groom_profiles
16. groom_task_completions
17. notifications
18. pending_agent_actions
19. refresh_tokens
20. subjects
21. trainer_profiles
22. veterinarian_profiles
23. ai_chat_messages
24. course_subjects
25. groom_task_completion_photos
26. stable_stalls
27. training_lots
28. horses
29. admission_applications
30. financial_reports
31. groom_daily_tasks
32. horse_pedigrees
33. horse_training_plans
34. race_registrations
35. admission_documents
36. candidate_horse_profiles
37. care_schedule
38. racing_readiness_assessments
39. training_workouts
40. deprecated_vet_offers
41. health_records
42. horse_incident_reports
43. horse_health_metrics
44. injury_records
45. treatment_plans
46. prescriptions

SCHEMA AND DATA CAVEATS
- This is a data-only export from the source snapshot. Import succeeds only if destination tables and columns match these CSV headers and PostgreSQL types accept the values. Compare manifest.json against the new database schema first.
- The dump was made at 2026-10-23 08:55 using pg_dump 17.0 from PostgreSQL 16.15. A newer or older application migration set may have renamed/dropped/added columns, enum values, constraints, or tables; CSV cannot reconcile those differences.
- flyway_schema_history.csv is included because it was present in COPY. Usually do not import it into a database already managed by Flyway: it can make Flyway believe migrations have already run. Keep the destination's own migration history.
- Reference/seed tables (roles, permissions, role_permissions, preventive_protocols, medical_symptom_mappings, horse_body_regions) may already contain rows created by migrations. Importing may hit duplicate keys or conflict with target seed values. Choose whether to preserve target seed rows or replace them before importing.
- The source snapshot has a self-referencing foreign key in care_schedule (source_schedule_id). If rows reference another row imported later, that table may need a second pass or a controlled import with constraints temporarily deferred/disabled by a database administrator, followed by constraint validation. Do not use TRUNCATE/CASCADE on a shared database.
- Explicit IDs are preserved. Reset affected sequences after import (for each serial ID column, set sequence to max(id), or use pg_get_serial_sequence/setval).
- Review the manifest and a backup of the target before importing. The CSV files contain the source database's actual records, including account and audit data.
