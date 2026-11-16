# Cardinality thật từ DB (82 FK)

Quy ước: Parent = bảng được tham chiếu, Child = bảng chứa FK.
- `ERone` = đúng 1 · `ERzeroToOne` = 0..1 · `ERzeroToMany` = 0..N
- Parent end: `ERone` nếu FK NOT NULL, `ERzeroToOne` nếu nullable.
- Child end: `ERzeroToMany`; nếu FK có UNIQUE (1 cột) thì `ERzeroToOne`.

| # | Child | Cột FK | Parent | Nullable | Unique | startArrow (Parent) | endArrow (Child) | Cardinality |
|---:|---|---|---|---|---|---|---|---|
| 1 | `admission_applications` | `groom_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 2 | `admission_applications` | `horse_id` | `horses` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 3 | `admission_applications` | `manager_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 4 | `admission_applications` | `owner_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 5 | `admission_applications` | `quarantine_stall_id` | `stable_stalls` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 6 | `admission_applications` | `trainer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 7 | `admission_applications` | `veterinarian_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 8 | `admission_documents` | `admission_id` | `admission_applications` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 9 | `ai_chat_messages` | `conversation_id` | `ai_conversations` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 10 | `ai_conversations` | `user_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 11 | `areas` | `trainer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 12 | `audit_logs` | `user_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 13 | `candidate_horse_profiles` | `admission_id` | `admission_applications` | NO | t | `ERone` | `ERzeroToOne` | **1 : 0..1** |
| 14 | `care_schedule` | `admission_id` | `admission_applications` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 15 | `care_schedule` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 16 | `care_schedule` | `requested_by_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 17 | `care_schedule` | `source_schedule_id` | `care_schedule` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 18 | `care_schedule` | `veterinarian_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 19 | `course_exercises` | `course_id` | `courses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 20 | `course_exercises` | `exercise_id` | `exercises` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 21 | `course_runs` | `course_id` | `courses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 22 | `course_runs` | `group_id` | `training_groups` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 23 | `course_runs` | `trainer_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 24 | `courses` | `created_by_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 25 | `courses` | `trainer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 26 | `exercises` | `trainer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 27 | `groom_daily_tasks` | `completion_id` | `groom_task_completions` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 28 | `groom_daily_tasks` | `groom_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 29 | `groom_daily_tasks` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 30 | `groom_profiles` | `trainer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 31 | `groom_profiles` | `user_id` | `users` | NO | t | `ERone` | `ERzeroToOne` | **1 : 0..1** |
| 32 | `groom_task_completion_photos` | `completion_id` | `groom_task_completions` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 33 | `groom_task_completions` | `groom_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 34 | `health_conditions` | `body_region_id` | `horse_body_regions` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 35 | `health_conditions` | `health_record_id` | `health_records` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 36 | `health_conditions` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 37 | `health_records` | `care_schedule_id` | `care_schedule` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 38 | `health_records` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 39 | `health_records` | `veterinarian_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 40 | `horse_edit_request_documents` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 41 | `horse_edit_request_documents` | `request_id` | `horse_edit_requests` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 42 | `horse_edit_requests` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 43 | `horse_edit_requests` | `owner_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 44 | `horse_edit_requests` | `reviewer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 45 | `horse_health_metrics` | `health_record_id` | `health_records` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 46 | `horse_health_metrics` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 47 | `horse_incident_reports` | `care_schedule_id` | `care_schedule` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 48 | `horse_incident_reports` | `handled_by_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 49 | `horse_incident_reports` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 50 | `horse_incident_reports` | `reported_by_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 51 | `horse_pedigrees` | `dam_id` | `horses` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 52 | `horse_pedigrees` | `horse_id` | `horses` | NO | t | `ERone` | `ERzeroToOne` | **1 : 0..1** |
| 53 | `horse_pedigrees` | `sire_id` | `horses` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 54 | `horses` | `current_stall_id` | `stable_stalls` | YES | t | `ERzeroToOne` | `ERzeroToOne` | **0..1 : 0..1** |
| 55 | `horses` | `owner_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 56 | `notifications` | `recipient_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 57 | `pending_agent_actions` | `user_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 58 | `prescriptions` | `preventive_protocol_id` | `preventive_protocols` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 59 | `prescriptions` | `treatment_plan_id` | `treatment_plans` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 60 | `racing_readiness_assessments` | `admission_id` | `admission_applications` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 61 | `racing_readiness_assessments` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 62 | `racing_readiness_assessments` | `trainer_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 63 | `rag_knowledge_embeddings` | `document_id` | `rag_knowledge_documents` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 64 | `refresh_tokens` | `user_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 65 | `role_permissions` | `permission_id` | `permissions` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 66 | `role_permissions` | `role_id` | `roles` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 67 | `stable_stalls` | `area_id` | `areas` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 68 | `stable_stalls` | `groom_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 69 | `trainer_profiles` | `user_id` | `users` | NO | t | `ERone` | `ERzeroToOne` | **1 : 0..1** |
| 70 | `training_group_members` | `groom_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 71 | `training_group_members` | `group_id` | `training_groups` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 72 | `training_group_members` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 73 | `training_groups` | `trainer_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 74 | `training_sessions` | `course_run_id` | `course_runs` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 75 | `training_sessions` | `exercise_id` | `exercises` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 76 | `training_sessions` | `trainer_id` | `users` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 77 | `training_workouts` | `assigned_to_id` | `users` | YES | f | `ERzeroToOne` | `ERzeroToMany` | **0..1 : 0..N** |
| 78 | `training_workouts` | `horse_id` | `horses` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 79 | `training_workouts` | `session_id` | `training_sessions` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 80 | `treatment_plans` | `health_record_id` | `health_records` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 81 | `users` | `role_id` | `roles` | NO | f | `ERone` | `ERzeroToMany` | **1 : 0..N** |
| 82 | `veterinarian_profiles` | `user_id` | `users` | NO | t | `ERone` | `ERzeroToOne` | **1 : 0..1** |

## Tổng hợp
- **0..1 : 0..N**: 33 FK
- **1 : 0..N**: 43 FK
- **1 : 0..1**: 5 FK
- **0..1 : 0..1**: 1 FK
