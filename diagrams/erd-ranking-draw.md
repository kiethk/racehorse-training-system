# ERD — Xếp theo tổng liên kết giảm dần

**Parent** = số bảng cha (FK đi ra, bảng này là child) · **Child** = số bảng con (FK đi vào, bảng này là parent) · **Tổng** = Parent + Child

| # | Bảng | Parent | Child | Tổng |
|---:|---|---:|---:|---:|
| 1 | `users` | 1 | 26 | **27** |
| 2 | `horses` | 2 | 14 | **16** |
| 3 | `health_records` | 5 | 3 | **8** |
| 4 | `training_sessions` | 5 | 2 | **7** |
| 5 | `admission_applications` | 3 | 4 | **7** |
| 6 | `training_workouts` | 5 | 1 | **6** |
| 7 | `care_schedule` | 3 | 2 | **5** |
| 8 | `course_runs` | 3 | 1 | **4** |
| 9 | `course_exercises` | 2 | 2 | **4** |
| 10 | `stable_stalls` | 2 | 2 | **4** |
| 11 | `exercises` | 1 | 3 | **4** |
| 12 | `groom_daily_tasks` | 3 | 0 | **3** |
| 13 | `health_conditions` | 3 | 0 | **3** |
| 14 | `horse_incident_reports` | 3 | 0 | **3** |
| 15 | `racing_readiness_assessments` | 3 | 0 | **3** |
| 16 | `training_group_members` | 3 | 0 | **3** |
| 17 | `horse_edit_requests` | 2 | 1 | **3** |
| 18 | `courses` | 1 | 2 | **3** |
| 19 | `groom_task_completions` | 1 | 2 | **3** |
| 20 | `training_groups` | 1 | 2 | **3** |
| 21 | `horse_edit_request_documents` | 2 | 0 | **2** |
| 22 | `horse_health_metrics` | 2 | 0 | **2** |
| 23 | `prescriptions` | 2 | 0 | **2** |
| 24 | `role_permissions` | 2 | 0 | **2** |
| 25 | `ai_conversations` | 1 | 1 | **2** |
| 26 | `areas` | 1 | 1 | **2** |
| 27 | `treatment_plans` | 1 | 1 | **2** |
| 28 | `roles` | 0 | 2 | **2** |
| 29 | `admission_documents` | 1 | 0 | **1** |
| 30 | `ai_chat_messages` | 1 | 0 | **1** |
| 31 | `audit_logs` | 1 | 0 | **1** |
| 32 | `candidate_horse_profiles` | 1 | 0 | **1** |
| 33 | `groom_profiles` | 1 | 0 | **1** |
| 34 | `groom_task_completion_photos` | 1 | 0 | **1** |
| 35 | `horse_pedigrees` | 1 | 0 | **1** |
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

## Chi tiết theo thứ tự trên

### 1. `users` — tổng **27** (Parent 1, Child 26)
- **→ Parent (N:1):** `roles`
- **← Child (1:N):** `admission_applications`, `ai_conversations`, `areas`, `audit_logs`, `care_schedule`, `course_runs`, `courses`, `exercises`, `groom_daily_tasks`, `groom_profiles`, `groom_task_completions`, `health_records`, `horse_edit_requests`, `horse_incident_reports`, `horses`, `notifications`, `pending_agent_actions`, `racing_readiness_assessments`, `refresh_tokens`, `stable_stalls`, `trainer_profiles`, `training_group_members`, `training_groups`, `training_sessions`, `training_workouts`, `veterinarian_profiles`

### 2. `horses` — tổng **16** (Parent 2, Child 14)
- **→ Parent (N:1):** `stable_stalls`, `users`
- **← Child (1:N):** `admission_applications`, `care_schedule`, `groom_daily_tasks`, `health_conditions`, `health_records`, `horse_edit_request_documents`, `horse_edit_requests`, `horse_health_metrics`, `horse_incident_reports`, `horse_pedigrees`, `racing_readiness_assessments`, `training_group_members`, `training_sessions`, `training_workouts`

### 3. `health_records` — tổng **8** (Parent 5, Child 3)
- **→ Parent (N:1):** `care_schedule`, `horses`, `training_sessions`, `training_workouts`, `users`
- **← Child (1:N):** `health_conditions`, `horse_health_metrics`, `treatment_plans`

### 4. `training_sessions` — tổng **7** (Parent 5, Child 2)
- **→ Parent (N:1):** `course_exercises`, `course_runs`, `exercises`, `horses`, `users`
- **← Child (1:N):** `health_records`, `training_workouts`

### 5. `admission_applications` — tổng **7** (Parent 3, Child 4)
- **→ Parent (N:1):** `horses`, `stable_stalls`, `users`
- **← Child (1:N):** `admission_documents`, `candidate_horse_profiles`, `care_schedule`, `racing_readiness_assessments`

### 6. `training_workouts` — tổng **6** (Parent 5, Child 1)
- **→ Parent (N:1):** `course_exercises`, `exercises`, `horses`, `training_sessions`, `users`
- **← Child (1:N):** `health_records`

### 7. `care_schedule` — tổng **5** (Parent 3, Child 2)
- **→ Parent (N:1):** `admission_applications`, `horses`, `users`
- **← Child (1:N):** `health_records`, `horse_incident_reports`

### 8. `course_runs` — tổng **4** (Parent 3, Child 1)
- **→ Parent (N:1):** `courses`, `training_groups`, `users`
- **← Child (1:N):** `training_sessions`

### 9. `course_exercises` — tổng **4** (Parent 2, Child 2)
- **→ Parent (N:1):** `courses`, `exercises`
- **← Child (1:N):** `training_sessions`, `training_workouts`

### 10. `stable_stalls` — tổng **4** (Parent 2, Child 2)
- **→ Parent (N:1):** `areas`, `users`
- **← Child (1:N):** `admission_applications`, `horses`

### 11. `exercises` — tổng **4** (Parent 1, Child 3)
- **→ Parent (N:1):** `users`
- **← Child (1:N):** `course_exercises`, `training_sessions`, `training_workouts`

### 12. `groom_daily_tasks` — tổng **3** (Parent 3, Child 0)
- **→ Parent (N:1):** `groom_task_completions`, `horses`, `users`

### 13. `health_conditions` — tổng **3** (Parent 3, Child 0)
- **→ Parent (N:1):** `health_records`, `horse_body_regions`, `horses`

### 14. `horse_incident_reports` — tổng **3** (Parent 3, Child 0)
- **→ Parent (N:1):** `care_schedule`, `horses`, `users`

### 15. `racing_readiness_assessments` — tổng **3** (Parent 3, Child 0)
- **→ Parent (N:1):** `admission_applications`, `horses`, `users`

### 16. `training_group_members` — tổng **3** (Parent 3, Child 0)
- **→ Parent (N:1):** `horses`, `training_groups`, `users`

### 17. `horse_edit_requests` — tổng **3** (Parent 2, Child 1)
- **→ Parent (N:1):** `horses`, `users`
- **← Child (1:N):** `horse_edit_request_documents`

### 18. `courses` — tổng **3** (Parent 1, Child 2)
- **→ Parent (N:1):** `users`
- **← Child (1:N):** `course_exercises`, `course_runs`

### 19. `groom_task_completions` — tổng **3** (Parent 1, Child 2)
- **→ Parent (N:1):** `users`
- **← Child (1:N):** `groom_daily_tasks`, `groom_task_completion_photos`

### 20. `training_groups` — tổng **3** (Parent 1, Child 2)
- **→ Parent (N:1):** `users`
- **← Child (1:N):** `course_runs`, `training_group_members`

### 21. `horse_edit_request_documents` — tổng **2** (Parent 2, Child 0)
- **→ Parent (N:1):** `horse_edit_requests`, `horses`

### 22. `horse_health_metrics` — tổng **2** (Parent 2, Child 0)
- **→ Parent (N:1):** `health_records`, `horses`

### 23. `prescriptions` — tổng **2** (Parent 2, Child 0)
- **→ Parent (N:1):** `preventive_protocols`, `treatment_plans`

### 24. `role_permissions` — tổng **2** (Parent 2, Child 0)
- **→ Parent (N:1):** `permissions`, `roles`

### 25. `ai_conversations` — tổng **2** (Parent 1, Child 1)
- **→ Parent (N:1):** `users`
- **← Child (1:N):** `ai_chat_messages`

### 26. `areas` — tổng **2** (Parent 1, Child 1)
- **→ Parent (N:1):** `users`
- **← Child (1:N):** `stable_stalls`

### 27. `treatment_plans` — tổng **2** (Parent 1, Child 1)
- **→ Parent (N:1):** `health_records`
- **← Child (1:N):** `prescriptions`

### 28. `roles` — tổng **2** (Parent 0, Child 2)
- **← Child (1:N):** `role_permissions`, `users`

### 29. `admission_documents` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `admission_applications`

### 30. `ai_chat_messages` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `ai_conversations`

### 31. `audit_logs` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 32. `candidate_horse_profiles` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `admission_applications`

### 33. `groom_profiles` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 34. `groom_task_completion_photos` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `groom_task_completions`

### 35. `horse_pedigrees` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `horses`

### 36. `notifications` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 37. `pending_agent_actions` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 38. `rag_knowledge_embeddings` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `rag_knowledge_documents`

### 39. `refresh_tokens` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 40. `trainer_profiles` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 41. `veterinarian_profiles` — tổng **1** (Parent 1, Child 0)
- **→ Parent (N:1):** `users`

### 42. `horse_body_regions` — tổng **1** (Parent 0, Child 1)
- **← Child (1:N):** `health_conditions`

### 43. `permissions` — tổng **1** (Parent 0, Child 1)
- **← Child (1:N):** `role_permissions`

### 44. `preventive_protocols` — tổng **1** (Parent 0, Child 1)
- **← Child (1:N):** `prescriptions`

### 45. `rag_knowledge_documents` — tổng **1** (Parent 0, Child 1)
- **← Child (1:N):** `rag_knowledge_embeddings`

### 46. `medical_symptom_mappings` — tổng **0** (Parent 0, Child 0)
