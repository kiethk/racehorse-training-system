# ERD — Kiểm tra lại từ DB thật (nguồn chính xác)

Nguồn: PostgreSQL `rtms_db` (localhost:5432), lọc theo 46 bảng đang dùng trong code (41 JPA entity + 5 bảng join/aux).
Số FK thực tế: **82** (không phải 76 như bản trích từ migration trước).

## 1. 46 bảng theo domain

**Auth** (5): `users`, `roles`, `permissions`, `role_permissions`, `refresh_tokens`
**Horse** (6): `horses`, `horse_pedigrees`, `horse_health_metrics`, `horse_body_regions`, `horse_edit_requests`, `horse_edit_request_documents`
**Training** (8): `courses`, `exercises`, `course_exercises`, `course_runs`, `training_groups`, `training_group_members`, `training_sessions`, `training_workouts`
**Admission** (3): `admission_applications`, `candidate_horse_profiles`, `admission_documents`
**Veterinary** (7): `health_records`, `health_conditions`, `treatment_plans`, `prescriptions`, `preventive_protocols`, `care_schedule`, `medical_symptom_mappings`
**Groom** (5): `groom_profiles`, `groom_daily_tasks`, `groom_task_completions`, `groom_task_completion_photos`, `horse_incident_reports`
**Stable** (2): `areas`, `stable_stalls`
**Racing** (1): `racing_readiness_assessments`
**Profiles** (2): `veterinarian_profiles`, `trainer_profiles`
**AI & System** (7): `notifications`, `audit_logs`, `pending_agent_actions`, `ai_conversations`, `ai_chat_messages`, `rag_knowledge_documents`, `rag_knowledge_embeddings`

## 2. 82 FK thật (child.column → parent)

| # | Child | Cột FK | Parent |
|---:|---|---|---|
| 1 | `admission_applications` | `groom_id` | `users` |
| 2 | `admission_applications` | `horse_id` | `horses` |
| 3 | `admission_applications` | `manager_id` | `users` |
| 4 | `admission_applications` | `owner_id` | `users` |
| 5 | `admission_applications` | `quarantine_stall_id` | `stable_stalls` |
| 6 | `admission_applications` | `trainer_id` | `users` |
| 7 | `admission_applications` | `veterinarian_id` | `users` |
| 8 | `admission_documents` | `admission_id` | `admission_applications` |
| 9 | `ai_chat_messages` | `conversation_id` | `ai_conversations` |
| 10 | `ai_conversations` | `user_id` | `users` |
| 11 | `areas` | `trainer_id` | `users` |
| 12 | `audit_logs` | `user_id` | `users` |
| 13 | `candidate_horse_profiles` | `admission_id` | `admission_applications` |
| 14 | `care_schedule` | `admission_id` | `admission_applications` |
| 15 | `care_schedule` | `horse_id` | `horses` |
| 16 | `care_schedule` | `requested_by_id` | `users` |
| 17 | `care_schedule` | `source_schedule_id` | `care_schedule` |
| 18 | `care_schedule` | `veterinarian_id` | `users` |
| 19 | `course_exercises` | `course_id` | `courses` |
| 20 | `course_exercises` | `exercise_id` | `exercises` |
| 21 | `course_runs` | `course_id` | `courses` |
| 22 | `course_runs` | `group_id` | `training_groups` |
| 23 | `course_runs` | `trainer_id` | `users` |
| 24 | `courses` | `created_by_id` | `users` |
| 25 | `courses` | `trainer_id` | `users` |
| 26 | `exercises` | `trainer_id` | `users` |
| 27 | `groom_daily_tasks` | `completion_id` | `groom_task_completions` |
| 28 | `groom_daily_tasks` | `groom_id` | `users` |
| 29 | `groom_daily_tasks` | `horse_id` | `horses` |
| 30 | `groom_profiles` | `trainer_id` | `users` |
| 31 | `groom_profiles` | `user_id` | `users` |
| 32 | `groom_task_completion_photos` | `completion_id` | `groom_task_completions` |
| 33 | `groom_task_completions` | `groom_id` | `users` |
| 34 | `health_conditions` | `body_region_id` | `horse_body_regions` |
| 35 | `health_conditions` | `health_record_id` | `health_records` |
| 36 | `health_conditions` | `horse_id` | `horses` |
| 37 | `health_records` | `care_schedule_id` | `care_schedule` |
| 38 | `health_records` | `horse_id` | `horses` |
| 39 | `health_records` | `veterinarian_id` | `users` |
| 40 | `horse_edit_request_documents` | `horse_id` | `horses` |
| 41 | `horse_edit_request_documents` | `request_id` | `horse_edit_requests` |
| 42 | `horse_edit_requests` | `horse_id` | `horses` |
| 43 | `horse_edit_requests` | `owner_id` | `users` |
| 44 | `horse_edit_requests` | `reviewer_id` | `users` |
| 45 | `horse_health_metrics` | `health_record_id` | `health_records` |
| 46 | `horse_health_metrics` | `horse_id` | `horses` |
| 47 | `horse_incident_reports` | `care_schedule_id` | `care_schedule` |
| 48 | `horse_incident_reports` | `handled_by_id` | `users` |
| 49 | `horse_incident_reports` | `horse_id` | `horses` |
| 50 | `horse_incident_reports` | `reported_by_id` | `users` |
| 51 | `horse_pedigrees` | `dam_id` | `horses` |
| 52 | `horse_pedigrees` | `horse_id` | `horses` |
| 53 | `horse_pedigrees` | `sire_id` | `horses` |
| 54 | `horses` | `current_stall_id` | `stable_stalls` |
| 55 | `horses` | `owner_id` | `users` |
| 56 | `notifications` | `recipient_id` | `users` |
| 57 | `pending_agent_actions` | `user_id` | `users` |
| 58 | `prescriptions` | `preventive_protocol_id` | `preventive_protocols` |
| 59 | `prescriptions` | `treatment_plan_id` | `treatment_plans` |
| 60 | `racing_readiness_assessments` | `admission_id` | `admission_applications` |
| 61 | `racing_readiness_assessments` | `horse_id` | `horses` |
| 62 | `racing_readiness_assessments` | `trainer_id` | `users` |
| 63 | `rag_knowledge_embeddings` | `document_id` | `rag_knowledge_documents` |
| 64 | `refresh_tokens` | `user_id` | `users` |
| 65 | `role_permissions` | `permission_id` | `permissions` |
| 66 | `role_permissions` | `role_id` | `roles` |
| 67 | `stable_stalls` | `area_id` | `areas` |
| 68 | `stable_stalls` | `groom_id` | `users` |
| 69 | `trainer_profiles` | `user_id` | `users` |
| 70 | `training_group_members` | `groom_id` | `users` |
| 71 | `training_group_members` | `group_id` | `training_groups` |
| 72 | `training_group_members` | `horse_id` | `horses` |
| 73 | `training_groups` | `trainer_id` | `users` |
| 74 | `training_sessions` | `course_run_id` | `course_runs` |
| 75 | `training_sessions` | `exercise_id` | `exercises` |
| 76 | `training_sessions` | `trainer_id` | `users` |
| 77 | `training_workouts` | `assigned_to_id` | `users` |
| 78 | `training_workouts` | `horse_id` | `horses` |
| 79 | `training_workouts` | `session_id` | `training_sessions` |
| 80 | `treatment_plans` | `health_record_id` | `health_records` |
| 81 | `users` | `role_id` | `roles` |
| 82 | `veterinarian_profiles` | `user_id` | `users` |

## 3. Xếp theo tổng liên kết giảm dần (để vẽ)

| # | Bảng | Out (FK đi ra) | In (FK trỏ vào) | Tổng |
|---:|---|---:|---:|---:|
| 1 | `users` | 1 | 35 | **36** |
| 2 | `horses` | 2 | 15 | **17** |
| 3 | `admission_applications` | 7 | 4 | **11** |
| 4 | `care_schedule` | 5 | 3 | **8** |
| 5 | `health_records` | 3 | 3 | **6** |
| 6 | `horse_incident_reports` | 4 | 0 | **4** |
| 7 | `course_runs` | 3 | 1 | **4** |
| 8 | `horse_edit_requests` | 3 | 1 | **4** |
| 9 | `training_sessions` | 3 | 1 | **4** |
| 10 | `courses` | 2 | 2 | **4** |
| 11 | `stable_stalls` | 2 | 2 | **4** |
| 12 | `groom_daily_tasks` | 3 | 0 | **3** |
| 13 | `health_conditions` | 3 | 0 | **3** |
| 14 | `horse_pedigrees` | 3 | 0 | **3** |
| 15 | `racing_readiness_assessments` | 3 | 0 | **3** |
| 16 | `training_group_members` | 3 | 0 | **3** |
| 17 | `training_workouts` | 3 | 0 | **3** |
| 18 | `exercises` | 1 | 2 | **3** |
| 19 | `groom_task_completions` | 1 | 2 | **3** |
| 20 | `training_groups` | 1 | 2 | **3** |
| 21 | `course_exercises` | 2 | 0 | **2** |
| 22 | `groom_profiles` | 2 | 0 | **2** |
| 23 | `horse_edit_request_documents` | 2 | 0 | **2** |
| 24 | `horse_health_metrics` | 2 | 0 | **2** |
| 25 | `prescriptions` | 2 | 0 | **2** |
| 26 | `role_permissions` | 2 | 0 | **2** |
| 27 | `ai_conversations` | 1 | 1 | **2** |
| 28 | `areas` | 1 | 1 | **2** |
| 29 | `treatment_plans` | 1 | 1 | **2** |
| 30 | `roles` | 0 | 2 | **2** |
| 31 | `admission_documents` | 1 | 0 | **1** |
| 32 | `ai_chat_messages` | 1 | 0 | **1** |
| 33 | `audit_logs` | 1 | 0 | **1** |
| 34 | `candidate_horse_profiles` | 1 | 0 | **1** |
| 35 | `groom_task_completion_photos` | 1 | 0 | **1** |
| 36 | `notifications` | 1 | 0 | **1** |
| 37 | `pending_agent_actions` | 1 | 0 | **1** |
| 38 | `rag_knowledge_embeddings` | 1 | 0 | **1** |
| 39 | `refresh_tokens` | 1 | 0 | **1** |
| 40 | `trainer_profiles` | 1 | 0 | **1** |
| 41 | `veterinarian_profiles` | 1 | 0 | **1** |
| 42 | `horse_body_regions` | 0 | 1 | **1** |
| 43 | `permissions` | 0 | 1 | **1** |
| 44 | `preventive_protocols` | 0 | 1 | **1** |
| 45 | `rag_knowledge_documents` | 0 | 1 | **1** |
| 46 | `medical_symptom_mappings` | 0 | 0 | **0** |

## 4. Chi tiết theo bảng (out/in)

### 1. `users` — 36
- **→** `role_id` → `roles`
- **←** `assigned_to_id` ← `training_workouts`, `created_by_id` ← `courses`, `groom_id` ← `admission_applications`, `groom_id` ← `groom_daily_tasks`, `groom_id` ← `groom_task_completions`, `groom_id` ← `stable_stalls`, `groom_id` ← `training_group_members`, `handled_by_id` ← `horse_incident_reports`, `manager_id` ← `admission_applications`, `owner_id` ← `admission_applications`, `owner_id` ← `horse_edit_requests`, `owner_id` ← `horses`, `recipient_id` ← `notifications`, `reported_by_id` ← `horse_incident_reports`, `requested_by_id` ← `care_schedule`, `reviewer_id` ← `horse_edit_requests`, `trainer_id` ← `admission_applications`, `trainer_id` ← `areas`, `trainer_id` ← `course_runs`, `trainer_id` ← `courses`, `trainer_id` ← `exercises`, `trainer_id` ← `groom_profiles`, `trainer_id` ← `racing_readiness_assessments`, `trainer_id` ← `training_groups`, `trainer_id` ← `training_sessions`, `user_id` ← `ai_conversations`, `user_id` ← `audit_logs`, `user_id` ← `groom_profiles`, `user_id` ← `pending_agent_actions`, `user_id` ← `refresh_tokens`, `user_id` ← `trainer_profiles`, `user_id` ← `veterinarian_profiles`, `veterinarian_id` ← `admission_applications`, `veterinarian_id` ← `care_schedule`, `veterinarian_id` ← `health_records`

### 2. `horses` — 17
- **→** `current_stall_id` → `stable_stalls`, `owner_id` → `users`
- **←** `dam_id` ← `horse_pedigrees`, `horse_id` ← `admission_applications`, `horse_id` ← `care_schedule`, `horse_id` ← `groom_daily_tasks`, `horse_id` ← `health_conditions`, `horse_id` ← `health_records`, `horse_id` ← `horse_edit_request_documents`, `horse_id` ← `horse_edit_requests`, `horse_id` ← `horse_health_metrics`, `horse_id` ← `horse_incident_reports`, `horse_id` ← `horse_pedigrees`, `horse_id` ← `racing_readiness_assessments`, `horse_id` ← `training_group_members`, `horse_id` ← `training_workouts`, `sire_id` ← `horse_pedigrees`

### 3. `admission_applications` — 11
- **→** `groom_id` → `users`, `horse_id` → `horses`, `manager_id` → `users`, `owner_id` → `users`, `quarantine_stall_id` → `stable_stalls`, `trainer_id` → `users`, `veterinarian_id` → `users`
- **←** `admission_id` ← `admission_documents`, `admission_id` ← `candidate_horse_profiles`, `admission_id` ← `care_schedule`, `admission_id` ← `racing_readiness_assessments`

### 4. `care_schedule` — 8
- **→** `admission_id` → `admission_applications`, `horse_id` → `horses`, `requested_by_id` → `users`, `source_schedule_id` → `care_schedule`, `veterinarian_id` → `users`
- **←** `care_schedule_id` ← `health_records`, `care_schedule_id` ← `horse_incident_reports`, `source_schedule_id` ← `care_schedule`

### 5. `health_records` — 6
- **→** `care_schedule_id` → `care_schedule`, `horse_id` → `horses`, `veterinarian_id` → `users`
- **←** `health_record_id` ← `health_conditions`, `health_record_id` ← `horse_health_metrics`, `health_record_id` ← `treatment_plans`

### 6. `horse_incident_reports` — 4
- **→** `care_schedule_id` → `care_schedule`, `handled_by_id` → `users`, `horse_id` → `horses`, `reported_by_id` → `users`

### 7. `course_runs` — 4
- **→** `course_id` → `courses`, `group_id` → `training_groups`, `trainer_id` → `users`
- **←** `course_run_id` ← `training_sessions`

### 8. `horse_edit_requests` — 4
- **→** `horse_id` → `horses`, `owner_id` → `users`, `reviewer_id` → `users`
- **←** `request_id` ← `horse_edit_request_documents`

### 9. `training_sessions` — 4
- **→** `course_run_id` → `course_runs`, `exercise_id` → `exercises`, `trainer_id` → `users`
- **←** `session_id` ← `training_workouts`

### 10. `courses` — 4
- **→** `created_by_id` → `users`, `trainer_id` → `users`
- **←** `course_id` ← `course_exercises`, `course_id` ← `course_runs`

### 11. `stable_stalls` — 4
- **→** `area_id` → `areas`, `groom_id` → `users`
- **←** `current_stall_id` ← `horses`, `quarantine_stall_id` ← `admission_applications`

### 12. `groom_daily_tasks` — 3
- **→** `completion_id` → `groom_task_completions`, `groom_id` → `users`, `horse_id` → `horses`

### 13. `health_conditions` — 3
- **→** `body_region_id` → `horse_body_regions`, `health_record_id` → `health_records`, `horse_id` → `horses`

### 14. `horse_pedigrees` — 3
- **→** `dam_id` → `horses`, `horse_id` → `horses`, `sire_id` → `horses`

### 15. `racing_readiness_assessments` — 3
- **→** `admission_id` → `admission_applications`, `horse_id` → `horses`, `trainer_id` → `users`

### 16. `training_group_members` — 3
- **→** `groom_id` → `users`, `group_id` → `training_groups`, `horse_id` → `horses`

### 17. `training_workouts` — 3
- **→** `assigned_to_id` → `users`, `horse_id` → `horses`, `session_id` → `training_sessions`

### 18. `exercises` — 3
- **→** `trainer_id` → `users`
- **←** `exercise_id` ← `course_exercises`, `exercise_id` ← `training_sessions`

### 19. `groom_task_completions` — 3
- **→** `groom_id` → `users`
- **←** `completion_id` ← `groom_daily_tasks`, `completion_id` ← `groom_task_completion_photos`

### 20. `training_groups` — 3
- **→** `trainer_id` → `users`
- **←** `group_id` ← `course_runs`, `group_id` ← `training_group_members`

### 21. `course_exercises` — 2
- **→** `course_id` → `courses`, `exercise_id` → `exercises`

### 22. `groom_profiles` — 2
- **→** `trainer_id` → `users`, `user_id` → `users`

### 23. `horse_edit_request_documents` — 2
- **→** `horse_id` → `horses`, `request_id` → `horse_edit_requests`

### 24. `horse_health_metrics` — 2
- **→** `health_record_id` → `health_records`, `horse_id` → `horses`

### 25. `prescriptions` — 2
- **→** `preventive_protocol_id` → `preventive_protocols`, `treatment_plan_id` → `treatment_plans`

### 26. `role_permissions` — 2
- **→** `permission_id` → `permissions`, `role_id` → `roles`

### 27. `ai_conversations` — 2
- **→** `user_id` → `users`
- **←** `conversation_id` ← `ai_chat_messages`

### 28. `areas` — 2
- **→** `trainer_id` → `users`
- **←** `area_id` ← `stable_stalls`

### 29. `treatment_plans` — 2
- **→** `health_record_id` → `health_records`
- **←** `treatment_plan_id` ← `prescriptions`

### 30. `roles` — 2
- **←** `role_id` ← `role_permissions`, `role_id` ← `users`

### 31. `admission_documents` — 1
- **→** `admission_id` → `admission_applications`

### 32. `ai_chat_messages` — 1
- **→** `conversation_id` → `ai_conversations`

### 33. `audit_logs` — 1
- **→** `user_id` → `users`

### 34. `candidate_horse_profiles` — 1
- **→** `admission_id` → `admission_applications`

### 35. `groom_task_completion_photos` — 1
- **→** `completion_id` → `groom_task_completions`

### 36. `notifications` — 1
- **→** `recipient_id` → `users`

### 37. `pending_agent_actions` — 1
- **→** `user_id` → `users`

### 38. `rag_knowledge_embeddings` — 1
- **→** `document_id` → `rag_knowledge_documents`

### 39. `refresh_tokens` — 1
- **→** `user_id` → `users`

### 40. `trainer_profiles` — 1
- **→** `user_id` → `users`

### 41. `veterinarian_profiles` — 1
- **→** `user_id` → `users`

### 42. `horse_body_regions` — 1
- **←** `body_region_id` ← `health_conditions`

### 43. `permissions` — 1
- **←** `permission_id` ← `role_permissions`

### 44. `preventive_protocols` — 1
- **←** `preventive_protocol_id` ← `prescriptions`

### 45. `rag_knowledge_documents` — 1
- **←** `document_id` ← `rag_knowledge_embeddings`

### 46. `medical_symptom_mappings` — 0

## 5. 4 cặp N:N (qua bảng trung gian)

| A | B | Qua bảng |
|---|---|---|
| `roles` | `permissions` | `role_permissions` |
| `courses` | `exercises` | `course_exercises` |
| `courses` | `training_groups` | `course_runs` |
| `training_groups` | `horses` | `training_group_members` |

## 6. Khác biệt so với bản trích migration trước

- `health_records` **không còn** FK tới `training_sessions` / `training_workouts` (cột đã bị bỏ).
- `training_sessions` **không có** `horse_id`, `course_exercise_id`; chỉ có `course_run_id`, `exercise_id`, `trainer_id`.
- `training_workouts` **không có** `exercise_id`, `course_exercise_id`; chỉ có `assigned_to_id`, `horse_id`, `session_id`.
- Nhiều FK cùng trỏ `users` (nhiều cột khác nhau): `admission_applications` (owner_id, trainer_id, manager_id, veterinarian_id, groom_id), `care_schedule` (requested_by_id, veterinarian_id), `courses` (created_by_id, trainer_id), `groom_profiles` (user_id, trainer_id), `horse_edit_requests` (owner_id, reviewer_id), `horse_incident_reports` (reported_by_id, handled_by_id)…
- `care_schedule.source_schedule_id` là **self-reference** (`care_schedule` → `care_schedule`).
- `horse_pedigrees` có **3 FK** tới `horses`: `dam_id`, `horse_id`, `sire_id`.
- `horses` dùng `current_stall_id`; `notifications` dùng `recipient_id`; `training_group_members` dùng `groom_id`.
