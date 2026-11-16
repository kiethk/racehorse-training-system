# Relation table — draw.io

Arrow: `startArrow=ERone; endArrow=ERzeroToMany; startFill=0; endFill=0` (parent 1 → child 0..N).

Sắp xếp theo số FK giữa cặp (giảm dần), rồi theo tổng liên kết của parent/child.

| Relation (draw.io) | Ref1 (parent) | Ref2 (child) |
|---|---|---|
| `ERone → ERzeroToMany` | `users` | `admission_applications` |
| `ERone → ERzeroToMany` | `horses` | `horse_pedigrees` |
| `ERone → ERzeroToMany` | `users` | `care_schedule` |
| `ERone → ERzeroToMany` | `users` | `courses` |
| `ERone → ERzeroToMany` | `users` | `horse_edit_requests` |
| `ERone → ERzeroToMany` | `users` | `horse_incident_reports` |
| `ERone → ERzeroToMany` | `users` | `groom_profiles` |
| `ERone → ERzeroToMany` | `users` | `horses` |
| `ERone → ERzeroToMany` | `users` | `health_records` |
| `ERone → ERzeroToMany` | `users` | `course_runs` |
| `ERone → ERzeroToMany` | `users` | `stable_stalls` |
| `ERone → ERzeroToMany` | `users` | `training_sessions` |
| `ERone → ERzeroToMany` | `users` | `exercises` |
| `ERone → ERzeroToMany` | `users` | `groom_daily_tasks` |
| `ERone → ERzeroToMany` | `users` | `groom_task_completions` |
| `ERone → ERzeroToMany` | `users` | `racing_readiness_assessments` |
| `ERone → ERzeroToMany` | `users` | `training_group_members` |
| `ERone → ERzeroToMany` | `users` | `training_groups` |
| `ERone → ERzeroToMany` | `users` | `training_workouts` |
| `ERone → ERzeroToMany` | `users` | `ai_conversations` |
| `ERone → ERzeroToMany` | `users` | `areas` |
| `ERone → ERzeroToMany` | `users` | `audit_logs` |
| `ERone → ERzeroToMany` | `users` | `notifications` |
| `ERone → ERzeroToMany` | `users` | `pending_agent_actions` |
| `ERone → ERzeroToMany` | `users` | `refresh_tokens` |
| `ERone → ERzeroToMany` | `users` | `trainer_profiles` |
| `ERone → ERzeroToMany` | `users` | `veterinarian_profiles` |
| `ERone → ERzeroToMany` | `horses` | `admission_applications` |
| `ERone → ERzeroToMany` | `horses` | `care_schedule` |
| `ERone → ERzeroToMany` | `horses` | `health_records` |
| `ERone → ERzeroToMany` | `horses` | `horse_edit_requests` |
| `ERone → ERzeroToMany` | `horses` | `horse_incident_reports` |
| `ERone → ERzeroToMany` | `horses` | `groom_daily_tasks` |
| `ERone → ERzeroToMany` | `horses` | `health_conditions` |
| `ERone → ERzeroToMany` | `horses` | `racing_readiness_assessments` |
| `ERone → ERzeroToMany` | `horses` | `training_group_members` |
| `ERone → ERzeroToMany` | `horses` | `training_workouts` |
| `ERone → ERzeroToMany` | `horses` | `horse_edit_request_documents` |
| `ERone → ERzeroToMany` | `horses` | `horse_health_metrics` |
| `ERone → ERzeroToMany` | `admission_applications` | `care_schedule` |
| `ERone → ERzeroToMany` | `admission_applications` | `racing_readiness_assessments` |
| `ERone → ERzeroToMany` | `admission_applications` | `admission_documents` |
| `ERone → ERzeroToMany` | `admission_applications` | `candidate_horse_profiles` |
| `ERone → ERzeroToMany` | `care_schedule` | `care_schedule` |
| `ERone → ERzeroToMany` | `care_schedule` | `health_records` |
| `ERone → ERzeroToMany` | `care_schedule` | `horse_incident_reports` |
| `ERone → ERzeroToMany` | `health_records` | `health_conditions` |
| `ERone → ERzeroToMany` | `health_records` | `horse_health_metrics` |
| `ERone → ERzeroToMany` | `health_records` | `treatment_plans` |
| `ERone → ERzeroToMany` | `stable_stalls` | `horses` |
| `ERone → ERzeroToMany` | `stable_stalls` | `admission_applications` |
| `ERone → ERzeroToMany` | `course_runs` | `training_sessions` |
| `ERone → ERzeroToMany` | `courses` | `course_runs` |
| `ERone → ERzeroToMany` | `training_sessions` | `training_workouts` |
| `ERone → ERzeroToMany` | `courses` | `course_exercises` |
| `ERone → ERzeroToMany` | `horse_edit_requests` | `horse_edit_request_documents` |
| `ERone → ERzeroToMany` | `exercises` | `training_sessions` |
| `ERone → ERzeroToMany` | `training_groups` | `course_runs` |
| `ERone → ERzeroToMany` | `groom_task_completions` | `groom_daily_tasks` |
| `ERone → ERzeroToMany` | `training_groups` | `training_group_members` |
| `ERone → ERzeroToMany` | `exercises` | `course_exercises` |
| `ERone → ERzeroToMany` | `groom_task_completions` | `groom_task_completion_photos` |
| `ERone → ERzeroToMany` | `roles` | `users` |
| `ERone → ERzeroToMany` | `areas` | `stable_stalls` |
| `ERone → ERzeroToMany` | `roles` | `role_permissions` |
| `ERone → ERzeroToMany` | `treatment_plans` | `prescriptions` |
| `ERone → ERzeroToMany` | `ai_conversations` | `ai_chat_messages` |
| `ERone → ERzeroToMany` | `horse_body_regions` | `health_conditions` |
| `ERone → ERzeroToMany` | `permissions` | `role_permissions` |
| `ERone → ERzeroToMany` | `preventive_protocols` | `prescriptions` |
| `ERone → ERzeroToMany` | `rag_knowledge_documents` | `rag_knowledge_embeddings` |
