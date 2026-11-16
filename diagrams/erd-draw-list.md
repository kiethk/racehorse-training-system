# ERD — Danh sách vẽ (xếp theo tổng liên kết giảm dần)

**Quy ước:** mỗi FK là **1:N** (parent 1 → child N). Không có 1:1. **N:N** đi qua bảng trung gian.

**N:N:** `roles` ↔ `permissions` (qua `role_permissions`) · `courses` ↔ `exercises` (qua `course_exercises`) · `courses` ↔ `training_groups` (qua `course_runs`) · `training_groups` ↔ `horses` (qua `training_group_members`)

## 1. `users` — 27 liên kết (Parent 1 · Child 26)
- **N:1** (bảng này nhiều → 1 parent): `roles`
- **1:N** (bảng này 1 → nhiều child): `admission_applications`, `ai_conversations`, `areas`, `audit_logs`, `care_schedule`, `course_runs`, `courses`, `exercises`, `groom_daily_tasks`, `groom_profiles`, `groom_task_completions`, `health_records`, `horse_edit_requests`, `horse_incident_reports`, `horses`, `notifications`, `pending_agent_actions`, `racing_readiness_assessments`, `refresh_tokens`, `stable_stalls`, `trainer_profiles`, `training_group_members`, `training_groups`, `training_sessions`, `training_workouts`, `veterinarian_profiles`

## 2. `horses` — 16 liên kết (Parent 2 · Child 14)
- **N:1** (bảng này nhiều → 1 parent): `stable_stalls`, `users`
- **1:N** (bảng này 1 → nhiều child): `admission_applications`, `care_schedule`, `groom_daily_tasks`, `health_conditions`, `health_records`, `horse_edit_request_documents`, `horse_edit_requests`, `horse_health_metrics`, `horse_incident_reports`, `horse_pedigrees`, `racing_readiness_assessments`, `training_group_members`, `training_sessions`, `training_workouts`

## 3. `health_records` — 8 liên kết (Parent 5 · Child 3)
- **N:1** (bảng này nhiều → 1 parent): `care_schedule`, `horses`, `training_sessions`, `training_workouts`, `users`
- **1:N** (bảng này 1 → nhiều child): `health_conditions`, `horse_health_metrics`, `treatment_plans`

## 4. `training_sessions` — 7 liên kết (Parent 5 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `course_exercises`, `course_runs`, `exercises`, `horses`, `users`
- **1:N** (bảng này 1 → nhiều child): `health_records`, `training_workouts`

## 5. `admission_applications` — 7 liên kết (Parent 3 · Child 4)
- **N:1** (bảng này nhiều → 1 parent): `horses`, `stable_stalls`, `users`
- **1:N** (bảng này 1 → nhiều child): `admission_documents`, `candidate_horse_profiles`, `care_schedule`, `racing_readiness_assessments`

## 6. `training_workouts` — 6 liên kết (Parent 5 · Child 1)
- **N:1** (bảng này nhiều → 1 parent): `course_exercises`, `exercises`, `horses`, `training_sessions`, `users`
- **1:N** (bảng này 1 → nhiều child): `health_records`

## 7. `care_schedule` — 5 liên kết (Parent 3 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `admission_applications`, `horses`, `users`
- **1:N** (bảng này 1 → nhiều child): `health_records`, `horse_incident_reports`

## 8. `course_runs` — 4 liên kết (Parent 3 · Child 1)
- **N:1** (bảng này nhiều → 1 parent): `courses`, `training_groups`, `users`
- **1:N** (bảng này 1 → nhiều child): `training_sessions`
- **N:N** — là bảng trung gian giữa `courses` và `training_groups`

## 9. `course_exercises` — 4 liên kết (Parent 2 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `courses`, `exercises`
- **1:N** (bảng này 1 → nhiều child): `training_sessions`, `training_workouts`
- **N:N** — là bảng trung gian giữa `courses` và `exercises`

## 10. `stable_stalls` — 4 liên kết (Parent 2 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `areas`, `users`
- **1:N** (bảng này 1 → nhiều child): `admission_applications`, `horses`

## 11. `exercises` — 4 liên kết (Parent 1 · Child 3)
- **N:1** (bảng này nhiều → 1 parent): `users`
- **1:N** (bảng này 1 → nhiều child): `course_exercises`, `training_sessions`, `training_workouts`

## 12. `groom_daily_tasks` — 3 liên kết (Parent 3 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `groom_task_completions`, `horses`, `users`

## 13. `health_conditions` — 3 liên kết (Parent 3 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `health_records`, `horse_body_regions`, `horses`

## 14. `horse_incident_reports` — 3 liên kết (Parent 3 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `care_schedule`, `horses`, `users`

## 15. `racing_readiness_assessments` — 3 liên kết (Parent 3 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `admission_applications`, `horses`, `users`

## 16. `training_group_members` — 3 liên kết (Parent 3 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `horses`, `training_groups`, `users`
- **N:N** — là bảng trung gian giữa `training_groups` và `horses`

## 17. `horse_edit_requests` — 3 liên kết (Parent 2 · Child 1)
- **N:1** (bảng này nhiều → 1 parent): `horses`, `users`
- **1:N** (bảng này 1 → nhiều child): `horse_edit_request_documents`

## 18. `courses` — 3 liên kết (Parent 1 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `users`
- **1:N** (bảng này 1 → nhiều child): `course_exercises`, `course_runs`

## 19. `groom_task_completions` — 3 liên kết (Parent 1 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `users`
- **1:N** (bảng này 1 → nhiều child): `groom_daily_tasks`, `groom_task_completion_photos`

## 20. `training_groups` — 3 liên kết (Parent 1 · Child 2)
- **N:1** (bảng này nhiều → 1 parent): `users`
- **1:N** (bảng này 1 → nhiều child): `course_runs`, `training_group_members`

## 21. `horse_edit_request_documents` — 2 liên kết (Parent 2 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `horse_edit_requests`, `horses`

## 22. `horse_health_metrics` — 2 liên kết (Parent 2 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `health_records`, `horses`

## 23. `prescriptions` — 2 liên kết (Parent 2 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `preventive_protocols`, `treatment_plans`

## 24. `role_permissions` — 2 liên kết (Parent 2 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `permissions`, `roles`
- **N:N** — là bảng trung gian giữa `roles` và `permissions`

## 25. `ai_conversations` — 2 liên kết (Parent 1 · Child 1)
- **N:1** (bảng này nhiều → 1 parent): `users`
- **1:N** (bảng này 1 → nhiều child): `ai_chat_messages`

## 26. `areas` — 2 liên kết (Parent 1 · Child 1)
- **N:1** (bảng này nhiều → 1 parent): `users`
- **1:N** (bảng này 1 → nhiều child): `stable_stalls`

## 27. `treatment_plans` — 2 liên kết (Parent 1 · Child 1)
- **N:1** (bảng này nhiều → 1 parent): `health_records`
- **1:N** (bảng này 1 → nhiều child): `prescriptions`

## 28. `roles` — 2 liên kết (Parent 0 · Child 2)
- **1:N** (bảng này 1 → nhiều child): `role_permissions`, `users`

## 29. `admission_documents` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `admission_applications`

## 30. `ai_chat_messages` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `ai_conversations`

## 31. `audit_logs` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 32. `candidate_horse_profiles` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `admission_applications`

## 33. `groom_profiles` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 34. `groom_task_completion_photos` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `groom_task_completions`

## 35. `horse_pedigrees` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `horses`

## 36. `notifications` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 37. `pending_agent_actions` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 38. `rag_knowledge_embeddings` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `rag_knowledge_documents`

## 39. `refresh_tokens` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 40. `trainer_profiles` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 41. `veterinarian_profiles` — 1 liên kết (Parent 1 · Child 0)
- **N:1** (bảng này nhiều → 1 parent): `users`

## 42. `horse_body_regions` — 1 liên kết (Parent 0 · Child 1)
- **1:N** (bảng này 1 → nhiều child): `health_conditions`

## 43. `permissions` — 1 liên kết (Parent 0 · Child 1)
- **1:N** (bảng này 1 → nhiều child): `role_permissions`

## 44. `preventive_protocols` — 1 liên kết (Parent 0 · Child 1)
- **1:N** (bảng này 1 → nhiều child): `prescriptions`

## 45. `rag_knowledge_documents` — 1 liên kết (Parent 0 · Child 1)
- **1:N** (bảng này 1 → nhiều child): `rag_knowledge_embeddings`

## 46. `medical_symptom_mappings` — 0 liên kết (Parent 0 · Child 0)
