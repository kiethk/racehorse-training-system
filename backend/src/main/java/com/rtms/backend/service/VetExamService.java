package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.dto.CompleteVetExamRequest;
import com.rtms.backend.dto.CreateVetExamRequest;
import com.rtms.backend.dto.HorseHealthMetricRequest;
import com.rtms.backend.dto.VetExamResponse;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.*;
import com.rtms.backend.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VetExamService {
    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final int SCHEDULING_HORIZON_DAYS = 30;
    private static final Set<VetExamStatus> ACTIVE_SCHEDULE_STATUSES =
            EnumSet.of(VetExamStatus.SCHEDULED, VetExamStatus.IN_PROGRESS);
    private static final Set<VetExamStatus> ACTIVE_EXAM_STATUSES =
            EnumSet.of(VetExamStatus.REQUESTED, VetExamStatus.SCHEDULED, VetExamStatus.IN_PROGRESS);

    private final VetExamRepository exams;
    private final HorseRepository horses;
    private final AdmissionApplicationRepository admissions;
    private final StableStallRepository stalls;
    private final HealthRecordRepository healthRecords;
    private final HorseHealthMetricRepository metrics;
    private final UserRepository users;

    public VetExamService(VetExamRepository exams, HorseRepository horses,
            AdmissionApplicationRepository admissions, StableStallRepository stalls,
            HealthRecordRepository healthRecords, HorseHealthMetricRepository metrics,
            UserRepository users) {
        this.exams = exams;
        this.horses = horses;
        this.admissions = admissions;
        this.stalls = stalls;
        this.healthRecords = healthRecords;
        this.metrics = metrics;
        this.users = users;
    }

    @Transactional
    public VetExamResponse createInitialExam(Long admissionId, Long createdByUserId) {
        AdmissionApplication admission = admissions.findByIdForUpdate(admissionId)
                .orElseThrow(() -> notFound("Admission not found"));
        if (admission.getStatus() != AdmissionStatus.VET_REVIEW
                || admission.getHorseId() == null || admission.getQuarantineStallId() == null) {
            throw conflict("Admission is not ready for its initial examination");
        }
        List<VetExam> active = exams.findActiveForAdmissionForUpdate(admissionId,
                VetExamType.INITIAL, ACTIVE_EXAM_STATUSES);
        if (!active.isEmpty()) return VetExamResponse.from(active.get(0));

        Horse horse = horses.findByIdForUpdate(admission.getHorseId())
                .orElseThrow(() -> notFound("Horse not found"));
        if (horse.getCurrentStatus() != HorseStatus.CANDIDATE
                || !Objects.equals(horse.getCurrentStallId(), admission.getQuarantineStallId())) {
            throw conflict("Candidate horse is not in the assigned quarantine stall");
        }
        lockTraining(horse, "Initial admission examination is pending", null, null);
        return VetExamResponse.from(exams.save(newExam(horse.getId(), admissionId,
                VetExamType.INITIAL, "Initial admission examination", createdByUserId, null)));
    }

    @Transactional
    public VetExamResponse createExam(CreateVetExamRequest request, Long createdByUserId) {
        if (request.examType() != VetExamType.URGENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "The public request endpoint only accepts URGENT exams");
        }
        Horse horse = horses.findByIdForUpdate(request.horseId())
                .orElseThrow(() -> notFound("Horse not found"));
        VetExam exam = newExam(horse.getId(), null, request.examType(), request.reason(),
                createdByUserId, request.requestedForDate());
        if (request.examType() == VetExamType.URGENT) {
            lockTraining(horse, request.reason(), null, null);
        }
        return VetExamResponse.from(exams.save(exam));
    }

    @Transactional(readOnly = true)
    public Page<VetExamResponse> list(VetExamStatus status, VetExamType type, Pageable pageable) {
        int boundedSize = Math.max(1, Math.min(pageable.getPageSize(), 50));
        Pageable boundedPageable = PageRequest.of(
                Math.max(0, pageable.getPageNumber()), boundedSize, pageable.getSort());
        Page<VetExam> page = status != null && type != null
                ? exams.findByStatusAndExamType(status, type, boundedPageable)
                : status != null ? exams.findByStatus(status, boundedPageable)
                : type != null ? exams.findByExamType(type, boundedPageable)
                : exams.findAll(boundedPageable);
        return page.map(VetExamResponse::from);
    }

    @Transactional(readOnly = true)
    public VetExamResponse get(Long id) {
        return VetExamResponse.from(exams.findById(id)
                .orElseThrow(() -> notFound("Vet exam not found")));
    }

    @Transactional
    public VetExamResponse start(Long id, Long vetId) {
        VetExam exam = exams.findByIdForUpdate(id)
                .orElseThrow(() -> notFound("Vet exam not found"));
        if (exam.getStatus() != VetExamStatus.SCHEDULED
                && !(exam.getStatus() == VetExamStatus.REQUESTED
                    && exam.getExamType() == VetExamType.URGENT)) {
            throw conflict("Only a scheduled exam, or an urgent request, can be started");
        }
        if (exam.getAssignedVetId() != null && !exam.getAssignedVetId().equals(vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the assigned veterinarian can start this exam");
        }
        exam.setAssignedVetId(vetId);
        if (exam.getScheduledAt() == null) exam.setScheduledAt(LocalDateTime.now());
        exam.setStatus(VetExamStatus.IN_PROGRESS);
        return VetExamResponse.from(exam);
    }

    @Transactional
    public VetExamResponse complete(Long id, CompleteVetExamRequest request, Long vetId) {
        validateCompletion(request);
        VetExam exam = exams.findByIdForUpdate(id)
                .orElseThrow(() -> notFound("Vet exam not found"));
        if (exam.getStatus() != VetExamStatus.IN_PROGRESS) {
            throw conflict("Vet exam must be IN_PROGRESS before completion");
        }
        if (!Objects.equals(exam.getAssignedVetId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the assigned veterinarian can complete this exam");
        }
        if (exam.getHealthRecordId() != null) {
            throw conflict("Vet exam already has a HealthRecord");
        }

        Horse horse = horses.findByIdForUpdate(exam.getHorseId())
                .orElseThrow(() -> notFound("Horse not found"));
        AdmissionApplication admission = exam.getAdmissionId() == null ? null
                : admissions.findByIdForUpdate(exam.getAdmissionId())
                    .orElseThrow(() -> notFound("Admission not found"));

        // Reserve/release stalls before saving the medical result. Any failure rolls the
        // whole transaction back, including the HealthRecord and exam transition.
        applyDecision(exam, horse, admission, request, vetId);

        LocalDateTime now = LocalDateTime.now();
        HealthRecord record = new HealthRecord();
        record.setHorseId(horse.getId());
        record.setVeterinarianId(vetId);
        record.setRecordType(exam.getExamType().name());
        record.setExaminedAt(now);
        record.setSymptoms(trim(request.getSymptoms()));
        record.setFindings(request.getFindings().trim());
        record.setDiagnosis(trim(request.getDiagnosis()));
        record.setTreatment(trim(request.getTreatment()));
        record.setVetDecision(request.getVetDecision());
        record.setRejectionReason(trim(request.getRejectionReason()));
        record.setFollowUpDate(request.getFollowUpDate());
        record.setNotes(trim(request.getNote()));
        record = healthRecords.save(record);
        saveMetrics(request.getMetrics(), horse.getId(), record.getId(), now);

        exam.setHealthRecordId(record.getId());
        exam.setStatus(VetExamStatus.COMPLETED);

        if (request.getVetDecision() == VetDecision.RECHECK_REQUIRED) {
            // The partial unique index permits one active follow-up per Admission.
            // Flush the completed transition before inserting its successor.
            exams.flush();
            VetExam followUp = newExam(horse.getId(), exam.getAdmissionId(), VetExamType.FOLLOW_UP,
                    "Follow-up for vet exam #" + exam.getId(), vetId, request.getFollowUpDate());
            followUp.setPreferredVetId(vetId);
            exams.save(followUp);
        }
        return VetExamResponse.from(exam);
    }

    @Transactional
    public int scheduleRequestedBatch(int batchSize) {
        if (!exams.trySchedulerLock()) return 0;
        int safeBatchSize = Math.max(1, Math.min(batchSize, 100));
        List<User> activeVets = users.findActiveVeterinarians();
        if (activeVets.isEmpty()) return 0;
        int assigned = 0;
        for (VetExam exam : exams.lockNextRequestedBatch(safeBatchSize)) {
            Assignment assignment = findAssignment(exam, activeVets);
            if (assignment == null) continue;
            exam.setAssignedVetId(assignment.vetId());
            exam.setScheduledAt(assignment.at());
            exam.setStatus(VetExamStatus.SCHEDULED);
            assigned++;
        }
        return assigned;
    }

    private void applyDecision(VetExam exam, Horse horse, AdmissionApplication admission,
            CompleteVetExamRequest request, Long vetId) {
        VetDecision decision = request.getVetDecision();
        LocalDateTime now = LocalDateTime.now();
        if (admission != null) {
            if (admission.getHorseId() == null || !admission.getHorseId().equals(horse.getId())
                    || (admission.getStatus() != AdmissionStatus.VET_REVIEW
                        && admission.getStatus() != AdmissionStatus.PENDING_RECHECK)) {
                throw conflict("Admission is not awaiting a veterinary decision");
            }
            admission.setVeterinarianId(vetId);
            admission.setVetDecision(decision);
            admission.setVetFeedback(decision == VetDecision.REJECTED
                    ? trim(request.getRejectionReason()) : trim(request.getNote()));
            admission.setVetReviewedAt(now);

            if (decision == VetDecision.APPROVED) {
                assertCandidateInQuarantine(admission, horse);
                lockTraining(horse, "Admission pending trainer and manager review", null, vetId);
                admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
            } else if (decision == VetDecision.RECHECK_REQUIRED) {
                assertCandidateInQuarantine(admission, horse);
                lockTraining(horse, "Veterinary recheck required", request.getFollowUpDate(), vetId);
                admission.setStatus(AdmissionStatus.PENDING_RECHECK);
            } else {
                StableStall quarantine = lockQuarantine(admission);
                quarantine.setStatus(StallStatus.AVAILABLE);
                horse.setCurrentStallId(null);
                horse.setCurrentStatus(HorseStatus.REJECTED);
                lockTraining(horse, request.getRejectionReason(), null, vetId);
                admission.setStatus(AdmissionStatus.REJECTED);
            }
            return;
        }

        if (decision == VetDecision.APPROVED) {
            unlockTraining(horse, vetId);
            if (horse.getCurrentStatus() == HorseStatus.INJURED
                    || horse.getCurrentStatus() == HorseStatus.MONITORING) {
                horse.setCurrentStatus(HorseStatus.ELIGIBLE);
            }
        } else {
            String reason = decision == VetDecision.REJECTED
                    ? request.getRejectionReason() : "Veterinary recheck required";
            lockTraining(horse, reason, request.getFollowUpDate(), vetId);
        }
    }

    private StableStall lockQuarantine(AdmissionApplication admission) {
        if (admission.getQuarantineStallId() == null) throw conflict("Admission has no quarantine stall");
        return stalls.findQuarantineStallByIdForUpdate(admission.getQuarantineStallId())
                .orElseThrow(() -> conflict("Quarantine stall not found"));
    }

    private void assertCandidateInQuarantine(AdmissionApplication admission, Horse horse) {
        StableStall quarantine = lockQuarantine(admission);
        if (horse.getCurrentStatus() != HorseStatus.CANDIDATE
                || !Objects.equals(horse.getCurrentStallId(), quarantine.getId())
                || quarantine.getStatus() != StallStatus.OCCUPIED) {
            throw conflict("Candidate horse is no longer in the assigned quarantine stall");
        }
    }

    private VetExam newExam(Long horseId, Long admissionId, VetExamType type, String reason,
            Long createdByUserId, LocalDate requestedForDate) {
        VetExam exam = new VetExam();
        exam.setHorseId(horseId);
        exam.setAdmissionId(admissionId);
        exam.setExamType(type);
        exam.setStatus(VetExamStatus.REQUESTED);
        exam.setPriority(type.getDefaultPriority());
        exam.setReason(reason == null ? null : reason.trim());
        exam.setCreatedByUserId(createdByUserId);
        exam.setRequestedForDate(requestedForDate);
        exam.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        return exam;
    }

    private Assignment findAssignment(VetExam exam, List<User> activeVets) {
        if (exam.getExamType() == VetExamType.URGENT) {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            return orderedVets(activeVets, exam.getPreferredVetId(), now.toLocalDate()).stream()
                    .filter(vet -> isFree(exam, vet.getId(), now))
                    .map(vet -> new Assignment(vet.getId(), now)).findFirst().orElse(null);
        }
        LocalDate startDate = exam.getRequestedForDate() == null
                ? LocalDate.now() : max(LocalDate.now(), exam.getRequestedForDate());
        for (int day = 0; day < SCHEDULING_HORIZON_DAYS; day++) {
            LocalDate date = startDate.plusDays(day);
            Assignment preferred = findInWindow(exam, activeVets, date,
                    FarmSchedulePolicy.VET_WINDOW_START, FarmSchedulePolicy.VET_WINDOW_END);
            if (preferred != null) return preferred;
            // 13:30-15:30 is preferred, not mandatory. Search the remainder of
            // ordinary working hours before moving to the next day.
            Assignment morning = findInWindow(exam, activeVets, date,
                    LocalTime.of(8, 0), FarmSchedulePolicy.VET_WINDOW_START);
            if (morning != null) return morning;
            Assignment afternoon = findInWindow(exam, activeVets, date,
                    FarmSchedulePolicy.VET_WINDOW_END, LocalTime.of(17, 0));
            if (afternoon != null) return afternoon;
        }
        return null;
    }

    private Assignment findInWindow(VetExam exam, List<User> activeVets, LocalDate date,
            LocalTime windowStart, LocalTime windowEnd) {
        LocalDateTime now = LocalDateTime.now();
        for (LocalDateTime slot = date.atTime(windowStart);
                !slot.plusMinutes(exam.getDurationMinutes()).isAfter(date.atTime(windowEnd));
                slot = slot.plusMinutes(DEFAULT_DURATION_MINUTES)) {
            if (slot.isBefore(now)) continue;
            for (User vet : orderedVets(activeVets, exam.getPreferredVetId(), date)) {
                if (isFree(exam, vet.getId(), slot)) return new Assignment(vet.getId(), slot);
            }
        }
        return null;
    }

    private List<User> orderedVets(List<User> activeVets, Long preferredVetId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return activeVets.stream().sorted(Comparator
                .comparing((User vet) -> !Objects.equals(vet.getId(), preferredVetId))
                .thenComparingLong(vet -> exams.findVetSchedule(vet.getId(),
                        ACTIVE_SCHEDULE_STATUSES, from, to).size())
                .thenComparing(User::getId)).toList();
    }

    private boolean isFree(VetExam request, Long vetId, LocalDateTime start) {
        LocalDateTime end = start.plusMinutes(request.getDurationMinutes());
        LocalDateTime from = start.minusDays(1);
        LocalDateTime to = end.plusDays(1);
        return exams.findVetSchedule(vetId, ACTIVE_SCHEDULE_STATUSES, from, to).stream()
                    .noneMatch(existing -> overlaps(start, end, existing))
                && exams.findHorseSchedule(request.getHorseId(), ACTIVE_SCHEDULE_STATUSES, from, to).stream()
                    .noneMatch(existing -> overlaps(start, end, existing));
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, VetExam existing) {
        if (existing.getScheduledAt() == null) return false;
        LocalDateTime existingEnd = existing.getScheduledAt().plusMinutes(existing.getDurationMinutes());
        return start.isBefore(existingEnd) && end.isAfter(existing.getScheduledAt());
    }

    private void validateCompletion(CompleteVetExamRequest request) {
        if (request.getVetDecision() == null || request.getFindings() == null
                || request.getFindings().isBlank() || !request.isRejectionValid()
                || !request.isFollowUpValid()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid veterinary examination result");
        }
    }

    private void saveMetrics(List<HorseHealthMetricRequest> inputs, Long horseId,
            Long healthRecordId, LocalDateTime now) {
        for (HorseHealthMetricRequest input : inputs == null
                ? List.<HorseHealthMetricRequest>of() : inputs) {
            HorseHealthMetric metric = new HorseHealthMetric();
            metric.setHorseId(horseId);
            metric.setHealthRecordId(healthRecordId);
            metric.setRecordedAt(now);
            metric.setHeartRate(input.getHeartRate());
            metric.setTemperature(input.getTemperature());
            metric.setWeight(input.getWeight());
            metric.setRespiratoryRate(input.getRespiratoryRate());
            metric.setHydrationStatus(input.getHydrationStatus());
            metric.setBodyConditionScore(input.getBodyConditionScore());
            metric.setNotes(input.getNotes());
            metrics.save(metric);
        }
    }

    private void lockTraining(Horse horse, String reason, LocalDate reviewDate, Long vetId) {
        horse.setTrainingLocked(true);
        horse.setTrainingLockReason(trim(reason));
        horse.setTrainingLockReviewDate(reviewDate);
        horse.setTrainingLockVetId(vetId);
        horse.setTrainingLockUpdatedAt(LocalDateTime.now());
    }

    private void unlockTraining(Horse horse, Long vetId) {
        horse.setTrainingLocked(false);
        horse.setTrainingLockReason(null);
        horse.setTrainingLockReviewDate(null);
        horse.setTrainingLockVetId(vetId);
        horse.setTrainingLockUpdatedAt(LocalDateTime.now());
    }

    private static LocalDate max(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", message);
    }

    private record Assignment(Long vetId, LocalDateTime at) { }
}
