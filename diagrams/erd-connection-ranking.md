# ERD — Bảng xếp theo số liên kết (nhiều → ít)

`out` = số FK đi ra (bảng này là child) · `in` = số FK đi vào (bảng này là parent) · `total` = out + in

## Hubs (≥10)
### `users` — total **27** (out 1, in 26)
- **→** `roles`
- **←** `admission_applications`, `ai_conversations`, `areas`, `audit_logs`, `care_schedule`, `course_runs`, `courses`, `exercises`, `groom_daily_tasks`, `groom_profiles`, `groom_task_completions`, `health_records`, `horse_edit_requests`, `horse_incident_reports`, `horses`, `notifications`, `pending_agent_actions`, `racing_readiness_assessments`, `refresh_tokens`, `stable_stalls`, `trainer_profiles`, `training_group_members`, `training_groups`, `training_sessions`, `training_workouts`, `veterinarian_profiles`

### `horses` — total **16** (out 2, in 14)
- **→** `stable_stalls`, `users`
- **←** `admission_applications`, `care_schedule`, `groom_daily_tasks`, `health_conditions`, `health_records`, `horse_edit_request_documents`, `horse_edit_requests`, `horse_health_metrics`, `horse_incident_reports`, `horse_pedigrees`, `racing_readiness_assessments`, `training_group_members`, `training_sessions`, `training_workouts`

## Cao (6–9)
### `health_records` — total **8** (out 5, in 3)
- **→** `care_schedule`, `horses`, `training_sessions`, `training_workouts`, `users`
- **←** `health_conditions`, `horse_health_metrics`, `treatment_plans`

### `training_sessions` — total **7** (out 5, in 2)
- **→** `course_exercises`, `course_runs`, `exercises`, `horses`, `users`
- **←** `health_records`, `training_workouts`

### `admission_applications` — total **7** (out 3, in 4)
- **→** `horses`, `stable_stalls`, `users`
- **←** `admission_documents`, `candidate_horse_profiles`, `care_schedule`, `racing_readiness_assessments`

### `training_workouts` — total **6** (out 5, in 1)
- **→** `course_exercises`, `exercises`, `horses`, `training_sessions`, `users`
- **←** `health_records`

## Trung bình (3–5)
### `care_schedule` — total **5** (out 3, in 2)
- **→** `admission_applications`, `horses`, `users`
- **←** `health_records`, `horse_incident_reports`

### `course_runs` — total **4** (out 3, in 1)
- **→** `courses`, `training_groups`, `users`
- **←** `training_sessions`

### `course_exercises` — total **4** (out 2, in 2)
- **→** `courses`, `exercises`
- **←** `training_sessions`, `training_workouts`

### `stable_stalls` — total **4** (out 2, in 2)
- **→** `areas`, `users`
- **←** `admission_applications`, `horses`

### `exercises` — total **4** (out 1, in 3)
- **→** `users`
- **←** `course_exercises`, `training_sessions`, `training_workouts`

### `groom_daily_tasks` — total **3** (out 3, in 0)
- **→** `groom_task_completions`, `horses`, `users`

### `health_conditions` — total **3** (out 3, in 0)
- **→** `health_records`, `horse_body_regions`, `horses`

### `horse_incident_reports` — total **3** (out 3, in 0)
- **→** `care_schedule`, `horses`, `users`

### `racing_readiness_assessments` — total **3** (out 3, in 0)
- **→** `admission_applications`, `horses`, `users`

### `training_group_members` — total **3** (out 3, in 0)
- **→** `horses`, `training_groups`, `users`

### `horse_edit_requests` — total **3** (out 2, in 1)
- **→** `horses`, `users`
- **←** `horse_edit_request_documents`

### `courses` — total **3** (out 1, in 2)
- **→** `users`
- **←** `course_exercises`, `course_runs`

### `groom_task_completions` — total **3** (out 1, in 2)
- **→** `users`
- **←** `groom_daily_tasks`, `groom_task_completion_photos`

### `training_groups` — total **3** (out 1, in 2)
- **→** `users`
- **←** `course_runs`, `training_group_members`

## Thấp (1–2)
### `horse_edit_request_documents` — total **2** (out 2, in 0)
- **→** `horse_edit_requests`, `horses`

### `horse_health_metrics` — total **2** (out 2, in 0)
- **→** `health_records`, `horses`

### `prescriptions` — total **2** (out 2, in 0)
- **→** `preventive_protocols`, `treatment_plans`

### `role_permissions` — total **2** (out 2, in 0)
- **→** `permissions`, `roles`

### `ai_conversations` — total **2** (out 1, in 1)
- **→** `users`
- **←** `ai_chat_messages`

### `areas` — total **2** (out 1, in 1)
- **→** `users`
- **←** `stable_stalls`

### `treatment_plans` — total **2** (out 1, in 1)
- **→** `health_records`
- **←** `prescriptions`

### `roles` — total **2** (out 0, in 2)
- **←** `role_permissions`, `users`

### `admission_documents` — total **1** (out 1, in 0)
- **→** `admission_applications`

### `ai_chat_messages` — total **1** (out 1, in 0)
- **→** `ai_conversations`

### `audit_logs` — total **1** (out 1, in 0)
- **→** `users`

### `candidate_horse_profiles` — total **1** (out 1, in 0)
- **→** `admission_applications`

### `groom_profiles` — total **1** (out 1, in 0)
- **→** `users`

### `groom_task_completion_photos` — total **1** (out 1, in 0)
- **→** `groom_task_completions`

### `horse_pedigrees` — total **1** (out 1, in 0)
- **→** `horses`

### `notifications` — total **1** (out 1, in 0)
- **→** `users`

### `pending_agent_actions` — total **1** (out 1, in 0)
- **→** `users`

### `rag_knowledge_embeddings` — total **1** (out 1, in 0)
- **→** `rag_knowledge_documents`

### `refresh_tokens` — total **1** (out 1, in 0)
- **→** `users`

### `trainer_profiles` — total **1** (out 1, in 0)
- **→** `users`

### `veterinarian_profiles` — total **1** (out 1, in 0)
- **→** `users`

### `horse_body_regions` — total **1** (out 0, in 1)
- **←** `health_conditions`

### `permissions` — total **1** (out 0, in 1)
- **←** `role_permissions`

### `preventive_protocols` — total **1** (out 0, in 1)
- **←** `prescriptions`

### `rag_knowledge_documents` — total **1** (out 0, in 1)
- **←** `rag_knowledge_embeddings`

## Không có FK (0)
### `medical_symptom_mappings` — total **0** (out 0, in 0)
