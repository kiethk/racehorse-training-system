# ERD — Loại quan hệ (1:N / N:N)

Quy ước: mỗi FK `child → parent` là **1:N** khi đọc theo hướng `parent 1 —— N child` (một parent có nhiều child).
Bảng trung gian (junction) gộp 2 FK lại thành **N:N**.

## Quan hệ N:N (qua bảng trung gian)

| Bảng A | Bảng B | Qua bảng |
|---|---|---|
| `roles` | `permissions` | `role_permissions` |
| `courses` | `exercises` | `course_exercises` |
| `training_groups` | `horses` | `training_group_members` |

## Tất cả quan hệ 1:N

| Parent (1) | Child (N) | Loại |
|---|---|---|
| `admission_applications` | `admission_documents` | 1:N |
| `admission_applications` | `candidate_horse_profiles` | 1:N |
| `admission_applications` | `care_schedule` | 1:N |
| `admission_applications` | `racing_readiness_assessments` | 1:N |
| `ai_conversations` | `ai_chat_messages` | 1:N |
| `areas` | `stable_stalls` | 1:N |
| `care_schedule` | `health_records` | 1:N |
| `care_schedule` | `horse_incident_reports` | 1:N |
| `course_exercises` | `training_sessions` | 1:N |
| `course_exercises` | `training_workouts` | 1:N |
| `course_runs` | `training_sessions` | 1:N |
| `courses` | `course_exercises` | 1:N |
| `courses` | `course_runs` | 1:N |
| `exercises` | `course_exercises` | 1:N |
| `exercises` | `training_sessions` | 1:N |
| `exercises` | `training_workouts` | 1:N |
| `groom_task_completions` | `groom_daily_tasks` | 1:N |
| `groom_task_completions` | `groom_task_completion_photos` | 1:N |
| `health_records` | `health_conditions` | 1:N |
| `health_records` | `horse_health_metrics` | 1:N |
| `health_records` | `treatment_plans` | 1:N |
| `horse_body_regions` | `health_conditions` | 1:N |
| `horse_edit_requests` | `horse_edit_request_documents` | 1:N |
| `horses` | `admission_applications` | 1:N |
| `horses` | `care_schedule` | 1:N |
| `horses` | `groom_daily_tasks` | 1:N |
| `horses` | `health_conditions` | 1:N |
| `horses` | `health_records` | 1:N |
| `horses` | `horse_edit_request_documents` | 1:N |
| `horses` | `horse_edit_requests` | 1:N |
| `horses` | `horse_health_metrics` | 1:N |
| `horses` | `horse_incident_reports` | 1:N |
| `horses` | `horse_pedigrees` | 1:N |
| `horses` | `racing_readiness_assessments` | 1:N |
| `horses` | `training_group_members` | 1:N |
| `horses` | `training_sessions` | 1:N |
| `horses` | `training_workouts` | 1:N |
| `permissions` | `role_permissions` | 1:N |
| `preventive_protocols` | `prescriptions` | 1:N |
| `rag_knowledge_documents` | `rag_knowledge_embeddings` | 1:N |
| `roles` | `role_permissions` | 1:N |
| `roles` | `users` | 1:N |
| `stable_stalls` | `admission_applications` | 1:N |
| `stable_stalls` | `horses` | 1:N |
| `training_groups` | `course_runs` | 1:N |
| `training_groups` | `training_group_members` | 1:N |
| `training_sessions` | `health_records` | 1:N |
| `training_sessions` | `training_workouts` | 1:N |
| `training_workouts` | `health_records` | 1:N |
| `treatment_plans` | `prescriptions` | 1:N |
| `users` | `admission_applications` | 1:N |
| `users` | `ai_conversations` | 1:N |
| `users` | `areas` | 1:N |
| `users` | `audit_logs` | 1:N |
| `users` | `care_schedule` | 1:N |
| `users` | `course_runs` | 1:N |
| `users` | `courses` | 1:N |
| `users` | `exercises` | 1:N |
| `users` | `groom_daily_tasks` | 1:N |
| `users` | `groom_profiles` | 1:N |
| `users` | `groom_task_completions` | 1:N |
| `users` | `health_records` | 1:N |
| `users` | `horse_edit_requests` | 1:N |
| `users` | `horse_incident_reports` | 1:N |
| `users` | `horses` | 1:N |
| `users` | `notifications` | 1:N |
| `users` | `pending_agent_actions` | 1:N |
| `users` | `racing_readiness_assessments` | 1:N |
| `users` | `refresh_tokens` | 1:N |
| `users` | `stable_stalls` | 1:N |
| `users` | `trainer_profiles` | 1:N |
| `users` | `training_group_members` | 1:N |
| `users` | `training_groups` | 1:N |
| `users` | `training_sessions` | 1:N |
| `users` | `training_workouts` | 1:N |
| `users` | `veterinarian_profiles` | 1:N |

## Theo bảng con (child)

- **`admission_applications`** — N:1 với: `horses`, `stable_stalls`, `users`
- **`admission_documents`** — N:1 với: `admission_applications`
- **`ai_chat_messages`** — N:1 với: `ai_conversations`
- **`ai_conversations`** — N:1 với: `users`
- **`areas`** — N:1 với: `users`
- **`audit_logs`** — N:1 với: `users`
- **`candidate_horse_profiles`** — N:1 với: `admission_applications`
- **`care_schedule`** — N:1 với: `admission_applications`, `horses`, `users`
- **`course_exercises`** — N:1 với: `courses`, `exercises`
- **`course_runs`** — N:1 với: `courses`, `training_groups`, `users`
- **`courses`** — N:1 với: `users`
- **`exercises`** — N:1 với: `users`
- **`groom_daily_tasks`** — N:1 với: `groom_task_completions`, `horses`, `users`
- **`groom_profiles`** — N:1 với: `users`
- **`groom_task_completion_photos`** — N:1 với: `groom_task_completions`
- **`groom_task_completions`** — N:1 với: `users`
- **`health_conditions`** — N:1 với: `health_records`, `horse_body_regions`, `horses`
- **`health_records`** — N:1 với: `care_schedule`, `horses`, `training_sessions`, `training_workouts`, `users`
- **`horse_edit_request_documents`** — N:1 với: `horse_edit_requests`, `horses`
- **`horse_edit_requests`** — N:1 với: `horses`, `users`
- **`horse_health_metrics`** — N:1 với: `health_records`, `horses`
- **`horse_incident_reports`** — N:1 với: `care_schedule`, `horses`, `users`
- **`horse_pedigrees`** — N:1 với: `horses`
- **`horses`** — N:1 với: `stable_stalls`, `users`
- **`notifications`** — N:1 với: `users`
- **`pending_agent_actions`** — N:1 với: `users`
- **`prescriptions`** — N:1 với: `preventive_protocols`, `treatment_plans`
- **`racing_readiness_assessments`** — N:1 với: `admission_applications`, `horses`, `users`
- **`rag_knowledge_embeddings`** — N:1 với: `rag_knowledge_documents`
- **`refresh_tokens`** — N:1 với: `users`
- **`role_permissions`** — N:1 với: `permissions`, `roles`
- **`stable_stalls`** — N:1 với: `areas`, `users`
- **`trainer_profiles`** — N:1 với: `users`
- **`training_group_members`** — N:1 với: `horses`, `training_groups`, `users`
- **`training_groups`** — N:1 với: `users`
- **`training_sessions`** — N:1 với: `course_exercises`, `course_runs`, `exercises`, `horses`, `users`
- **`training_workouts`** — N:1 với: `course_exercises`, `exercises`, `horses`, `training_sessions`, `users`
- **`treatment_plans`** — N:1 với: `health_records`
- **`users`** — N:1 với: `roles`
- **`veterinarian_profiles`** — N:1 với: `users`

## Theo bảng cha (parent)

- **`admission_applications`** — 1:N với: `admission_documents`, `candidate_horse_profiles`, `care_schedule`, `racing_readiness_assessments`
- **`ai_conversations`** — 1:N với: `ai_chat_messages`
- **`areas`** — 1:N với: `stable_stalls`
- **`care_schedule`** — 1:N với: `health_records`, `horse_incident_reports`
- **`course_exercises`** — 1:N với: `training_sessions`, `training_workouts`
- **`course_runs`** — 1:N với: `training_sessions`
- **`courses`** — 1:N với: `course_exercises`, `course_runs`
- **`exercises`** — 1:N với: `course_exercises`, `training_sessions`, `training_workouts`
- **`groom_task_completions`** — 1:N với: `groom_daily_tasks`, `groom_task_completion_photos`
- **`health_records`** — 1:N với: `health_conditions`, `horse_health_metrics`, `treatment_plans`
- **`horse_body_regions`** — 1:N với: `health_conditions`
- **`horse_edit_requests`** — 1:N với: `horse_edit_request_documents`
- **`horses`** — 1:N với: `admission_applications`, `care_schedule`, `groom_daily_tasks`, `health_conditions`, `health_records`, `horse_edit_request_documents`, `horse_edit_requests`, `horse_health_metrics`, `horse_incident_reports`, `horse_pedigrees`, `racing_readiness_assessments`, `training_group_members`, `training_sessions`, `training_workouts`
- **`permissions`** — 1:N với: `role_permissions`
- **`preventive_protocols`** — 1:N với: `prescriptions`
- **`rag_knowledge_documents`** — 1:N với: `rag_knowledge_embeddings`
- **`roles`** — 1:N với: `role_permissions`, `users`
- **`stable_stalls`** — 1:N với: `admission_applications`, `horses`
- **`training_groups`** — 1:N với: `course_runs`, `training_group_members`
- **`training_sessions`** — 1:N với: `health_records`, `training_workouts`
- **`training_workouts`** — 1:N với: `health_records`
- **`treatment_plans`** — 1:N với: `prescriptions`
- **`users`** — 1:N với: `admission_applications`, `ai_conversations`, `areas`, `audit_logs`, `care_schedule`, `course_runs`, `courses`, `exercises`, `groom_daily_tasks`, `groom_profiles`, `groom_task_completions`, `health_records`, `horse_edit_requests`, `horse_incident_reports`, `horses`, `notifications`, `pending_agent_actions`, `racing_readiness_assessments`, `refresh_tokens`, `stable_stalls`, `trainer_profiles`, `training_group_members`, `training_groups`, `training_sessions`, `training_workouts`, `veterinarian_profiles`