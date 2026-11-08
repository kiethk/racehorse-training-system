---
title: Auto-reassign pending tasks on Trainer or Vet deactivation
author: Kiethk
date: 2026-11-08
branch: feature/kiet-manager-horses
id: l-f9953536
scope: repo
status: active
src: agent
touches:
  - backend/src/main/java/com/rtms/backend/admission/repository/AdmissionApplicationRepository.java
  - backend/src/main/java/com/rtms/backend/admission/service/TrainerAssignmentService.java
  - backend/src/main/java/com/rtms/backend/identity/service/AccountManagementService.java
  - backend/src/main/java/com/rtms/backend/identity/service/StaffManagementService.java
  - backend/src/test/java/com/rtms/backend/admission/service/TrainerAssignmentServiceTest.java
  - backend/src/test/java/com/rtms/backend/identity/service/AccountManagementServiceTest.java
  - backend/src/test/java/com/rtms/backend/identity/service/StaffManagementServiceTest.java
  - frontend/src/features/accounts/components/AccountTable.tsx
  - frontend/src/features/staff/components/StaffTable.tsx
---

## Decided
- On Head Trainer deactivation: if trainer has pending admission applications in TRAINER_REVIEW, auto-reassign them to the least-loaded active Head Trainer via TrainerAssignmentService. Only block with 409 Conflict if no other active Head Trainer exists.
- On Veterinarian deactivation: if veterinarian has open care schedules (SCHEDULED, IN_PROGRESS), auto-reassign them to other active veterinarians via CareScheduleService, resetting any IN_PROGRESS exams back to SCHEDULED for medical safety. Only block with 409 Conflict if no other active Veterinarian exists.
- In TrainerAssignmentService.assignIfNeeded: self-heal admissions if assigned trainerId belongs to an inactive trainer by clearing trainerId and assigning to the least-loaded active trainer.

## Tried and ruled out
- Hard-blocking deactivation whenever any tasks are assigned: caused deadlock/lockout for managers because automated assignment services continuously assign incoming work.
