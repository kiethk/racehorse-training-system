# Arrow cho từng quan hệ (từ DB thật)

## Ký hiệu

| Arrow | Ký hiệu | Nghĩa |
|---|---|---|
| `ERone` | `\|` | đúng 1 |
| `ERzeroToOne` | `o\|` | 0 hoặc 1 |
| `ERzeroToMany` | `o<` | 0 hoặc nhiều |
| `ERmany` | `<` | nhiều |

## 82 quan hệ

| startArrow | endArrow | Minh họa | Bảng bắt đầu (1) | Bảng kết thúc |
|---|---|---|---|---|
| `ERone` | `ERzeroToMany` | `|————o<` | `admission_applications` | `admission_documents` (`admission_id`) |
| `ERone` | `ERzeroToOne` | `|————o|` | `admission_applications` | `candidate_horse_profiles` (`admission_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `admission_applications` | `care_schedule` (`admission_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `admission_applications` | `racing_readiness_assessments` (`admission_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `ai_conversations` | `ai_chat_messages` (`conversation_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `areas` | `stable_stalls` (`area_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `care_schedule` | `care_schedule` (`source_schedule_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `care_schedule` | `health_records` (`care_schedule_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `care_schedule` | `horse_incident_reports` (`care_schedule_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `course_runs` | `training_sessions` (`course_run_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `courses` | `course_exercises` (`course_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `courses` | `course_runs` (`course_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `exercises` | `course_exercises` (`exercise_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `exercises` | `training_sessions` (`exercise_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `groom_task_completions` | `groom_daily_tasks` (`completion_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `groom_task_completions` | `groom_task_completion_photos` (`completion_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `health_records` | `health_conditions` (`health_record_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `health_records` | `horse_health_metrics` (`health_record_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `health_records` | `treatment_plans` (`health_record_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `horse_body_regions` | `health_conditions` (`body_region_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horse_edit_requests` | `horse_edit_request_documents` (`request_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `horses` | `admission_applications` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `care_schedule` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `groom_daily_tasks` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `health_conditions` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `health_records` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `horse_edit_request_documents` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `horse_edit_requests` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `horse_health_metrics` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `horse_incident_reports` (`horse_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `horses` | `horse_pedigrees` (`dam_id`) |
| `ERone` | `ERzeroToOne` | `|————o|` | `horses` | `horse_pedigrees` (`horse_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `horses` | `horse_pedigrees` (`sire_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `racing_readiness_assessments` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `training_group_members` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `horses` | `training_workouts` (`horse_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `permissions` | `role_permissions` (`permission_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `preventive_protocols` | `prescriptions` (`preventive_protocol_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `rag_knowledge_documents` | `rag_knowledge_embeddings` (`document_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `roles` | `role_permissions` (`role_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `roles` | `users` (`role_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `stable_stalls` | `admission_applications` (`quarantine_stall_id`) |
| `ERzeroToOne` | `ERzeroToOne` | `o|———o|` | `stable_stalls` | `horses` (`current_stall_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `training_groups` | `course_runs` (`group_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `training_groups` | `training_group_members` (`group_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `training_sessions` | `training_workouts` (`session_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `treatment_plans` | `prescriptions` (`treatment_plan_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `admission_applications` (`groom_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `admission_applications` (`manager_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `admission_applications` (`owner_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `admission_applications` (`trainer_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `admission_applications` (`veterinarian_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `ai_conversations` (`user_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `areas` (`trainer_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `audit_logs` (`user_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `care_schedule` (`requested_by_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `care_schedule` (`veterinarian_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `course_runs` (`trainer_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `courses` (`created_by_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `courses` (`trainer_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `exercises` (`trainer_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `groom_daily_tasks` (`groom_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `groom_profiles` (`trainer_id`) |
| `ERone` | `ERzeroToOne` | `|————o|` | `users` | `groom_profiles` (`user_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `groom_task_completions` (`groom_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `health_records` (`veterinarian_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `horse_edit_requests` (`owner_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `horse_edit_requests` (`reviewer_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `horse_incident_reports` (`handled_by_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `horse_incident_reports` (`reported_by_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `horses` (`owner_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `notifications` (`recipient_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `pending_agent_actions` (`user_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `racing_readiness_assessments` (`trainer_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `refresh_tokens` (`user_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `stable_stalls` (`groom_id`) |
| `ERone` | `ERzeroToOne` | `|————o|` | `users` | `trainer_profiles` (`user_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `training_group_members` (`groom_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `training_groups` (`trainer_id`) |
| `ERone` | `ERzeroToMany` | `|————o<` | `users` | `training_sessions` (`trainer_id`) |
| `ERzeroToOne` | `ERzeroToMany` | `o|———o<` | `users` | `training_workouts` (`assigned_to_id`) |
| `ERone` | `ERzeroToOne` | `|————o|` | `users` | `veterinarian_profiles` (`user_id`) |
