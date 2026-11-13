package com.rtms.backend.medical.service;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.service.TrainerAssignmentTriggers;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.dto.CareScheduleDetailResponse;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.InjuryMarkerRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.dto.HorseHealthMetricRequest;
import com.rtms.backend.medical.dto.PrescriptionItemRequest;
import com.rtms.backend.medical.dto.UrgentCaseReportResponse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.entity.HorseBodyRegion;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.RescheduleReason;
import com.rtms.backend.medical.event.PrescriptionCreatedEvent;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.notification.dto.UrgentAssignmentAlert;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingDecisionService;


import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import com.rtms.backend.event.ClinicalRecordCreatedEvent;
import com.rtms.backend.event.InitialExamCompletedEvent;
import com.rtms.backend.event.UrgentAssignmentCommittedEvent;

@Service
public class CareScheduleService {

    public static final int ROUTINE_DAILY_QUOTA = 3;
    public static final int PREVENTIVE_DAILY_QUOTA = 3;
    public static final int ADMISSION_DAILY_QUOTA = 2;

    private static final int DEFAULT_DURATION_MINUTES = 30;
    /** PAUSED vẫn là ca đang khám dở: Vet quay lại sau ca khẩn cấp. */
    private static final List<CareScheduleStatus> OPEN_CASE_STATUSES = CareScheduleStatus.OPEN;
    private static final int ASSIGNMENT_BATCH_SIZE = 25;
    /**
     * Lịch thường chỉ được xếp vet khi còn trong cửa sổ này. Mốc ở xa (tiêm phòng +180 ngày)
     * nằm REQUESTED, chưa giữ chỗ của vet và không bị tính vào tải hiện tại.
     */
    static final int ASSIGNMENT_HORIZON_DAYS = 14;

    /**
     * AUTO cadence: a routine follow-up that ends without an explicit date continues this many days
     * after the completed visit. The offset is anchored to the PERSISTED completedAt (never a fresh now),
     * so a re-submit keeps the same fingerprint and resolves to the same row instead of an idempotency conflict.
     */
    static final int ROUTINE_CADENCE_DAYS = 14;
    static final String ROUTINE_CADENCE_DESCRIPTION = "Routine follow-up examination";
    static final String RETURN_FOLLOW_UP_DESCRIPTION = "Follow-up after the horse returned to the club";

    private final CareScheduleRepository careScheduleRepository;
    private final HorseRepository horseRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HorseHealthMetricRepository metricRepository;
    private final AdmissionApplicationRepository admissionRepository;
    private final StableStallRepository stallRepository;
    private final UserRepository userRepository;
    private final HorseIncidentReportRepository incidentReportRepository;
    private final VeterinarianProfileRepository veterinarianProfileRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final NotificationService notificationService;
    private final TrainingDecisionService trainingDecisionService;
    private final PreventiveVaccinationService preventiveVaccinationService;
    private final InjuryRecordRepository injuryRecordRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final PrescriptionRepository prescriptionRepository;

    public CareScheduleService(
            CareScheduleRepository careScheduleRepository,
            HorseRepository horseRepository,
            HealthRecordRepository healthRecordRepository,
            HorseHealthMetricRepository metricRepository,
            AdmissionApplicationRepository admissionRepository,
            StableStallRepository stallRepository,
            UserRepository userRepository,
            HorseIncidentReportRepository incidentReportRepository,
            VeterinarianProfileRepository veterinarianProfileRepository,
            AuditLogRepository auditLogRepository,
            ApplicationEventPublisher eventPublisher,
            EntityManager entityManager,
            NotificationService notificationService,
            TrainingDecisionService trainingDecisionService,
            PreventiveVaccinationService preventiveVaccinationService,
            InjuryRecordRepository injuryRecordRepository,
            TreatmentPlanRepository treatmentPlanRepository,
            PrescriptionRepository prescriptionRepository) {
        this.careScheduleRepository = careScheduleRepository;
        this.horseRepository = horseRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.metricRepository = metricRepository;
        this.admissionRepository = admissionRepository;
        this.stallRepository = stallRepository;
        this.userRepository = userRepository;
        this.incidentReportRepository = incidentReportRepository;
        this.veterinarianProfileRepository = veterinarianProfileRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.notificationService = notificationService;
        this.trainingDecisionService = trainingDecisionService;
        this.preventiveVaccinationService = preventiveVaccinationService;
        this.injuryRecordRepository = injuryRecordRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional
    public CareScheduleResponse createInitialSchedule(Long admissionId, Long horseId) {
        if (admissionId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "admissionId is required");
        }
        AdmissionApplication adm = admissionRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        if (adm.getStatus() != AdmissionStatus.VET_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Admission is not awaiting a vet examination");
        }
        if (horseId != null && !Objects.equals(horseId, adm.getHorseId())) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_MISMATCH", "Horse does not belong to this admission");
        }
        horseId = adm.getHorseId();
        if (horseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "HORSE_NOT_ASSIGNED", "Admission does not have a horse assigned yet");
        }

        // Khóa hàng ngựa để hai yêu cầu tạo lịch khám song song không cùng lọt qua bước kiểm tra trùng.
        horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        boolean activeExists = careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                horseId, CareType.INITIAL,
                CareScheduleStatus.OPEN);
        if (activeExists) {
            CareSchedule existing = careScheduleRepository
                    .findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admissionId, CareType.INITIAL)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "SCHEDULE_EXISTS", "Active schedule already exists"));
            return CareScheduleResponse.from(existing);
        }

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horseId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription("Initial admission physical examination in quarantine area");
        // Không khóa ngựa ở đây: ngựa CANDIDATE vốn không tập được (Horse.canTrain()),
        // kết luận y tế do chính lần khám này quyết định.
        CareSchedule saved = careScheduleRepository.save(schedule);
        assignRequestedSchedule(saved);
        return CareScheduleResponse.from(saved);
    }

    @Transactional
    public CareScheduleResponse createSchedule(Long horseId, CareType careType, String description, LocalDateTime requestedAt, Long sourceIncidentId) {
        return createSchedule(horseId, careType, description, requestedAt, sourceIncidentId, null);
    }

    /** `excludeFromOffload`: ca vừa bị hoãn vì ca mới này, không được đẩy sang Vet khác lúc gán. */
    @Transactional
    public CareScheduleResponse createSchedule(Long horseId, CareType careType, String description, LocalDateTime requestedAt,
                                               Long sourceIncidentId, Long excludeFromOffload) {
        Horse horse = horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horseId);
        schedule.setCareType(careType);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setRequestedAt(requestedAt);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription(description != null ? description.trim() : null);
        schedule.setSourceIncidentId(sourceIncidentId);

        CareSchedule saved = careScheduleRepository.save(schedule);
        assignRequestedSchedule(saved, saved.getRequestedById(), excludeFromOffload);
        return CareScheduleResponse.from(saved);
    }

    /**
     * Ca sự cố đang mở của ngựa: ca URGENT, hoặc ca ROUTINE do báo cáo tạo ra. Mỗi ngựa chỉ có một ca như vậy.
     * Lịch ROUTINE thường như tiêm chủng không tính.
     */
    @Transactional(readOnly = true)
    public Optional<CareSchedule> findOpenIncidentCase(Long horseId) {
        return careScheduleRepository.findByHorseIdAndStatusIn(horseId, OPEN_CASE_STATUSES).stream()
                .filter(CareScheduleService::isIncidentCase)
                .min(Comparator.comparing((CareSchedule cs) -> cs.getCareType() != CareType.URGENT));
    }

    /**
     * Ca ROUTINE bị ca URGENT chen ngang: trả ca về SCHEDULED và giữ nguyên Vet để khám tiếp khi xong ca khẩn cấp.
     * Không xóa ca, không đụng dữ liệu Vet đã nhập (FE giữ draft theo careScheduleId).
     */
    @Transactional
    public void interruptForUrgent(CareSchedule routineCase) {
        routineCase.setStatus(CareScheduleStatus.SCHEDULED);
        routineCase.setScheduledAt(null);
        routineCase.setRescheduleReason(RescheduleReason.URGENT_PREEMPTION);
        routineCase.setRescheduleNote("Interrupted by an urgent case; resume this visit when the urgent case is done");
        careScheduleRepository.save(routineCase);
        notificationService.sendAssignmentNotification(
                routineCase.getVeterinarianId(),
                NotificationTypes.REFERENCE_CARE_SCHEDULE,
                routineCase.getId(),
                routineCase.getHorseId(),
                NotificationTypes.CARE_SCHEDULE_PAUSED,
                "Visit paused for an urgent case",
                "Visit #" + routineCase.getId() + " for " + horseNameOf(routineCase)
                        + " was paused because a new incident report made it urgent. Resume it when the urgent case is done.");
    }

    /**
     * AD-022 / Mục 8 & 9: Khi ngựa chuyển INACTIVE, đóng các báo cáo sự cố còn mở của các ca chưa hoàn tất
     * (SCHEDULED hoặc IN_PROGRESS) sang DISMISSED kèm lý do, thông báo cho người gửi, và gỡ liên kết
     * để các ca đó được hủy hàng loạt sau đó.
     */
    @Transactional
    public void closeOpenIncidentCasesForHorse(Long horseId, String reason) {
        List<HorseIncidentReport> openReports = incidentReportRepository.findByHorseIdAndStatusIn(
                horseId, List.of(IncidentStatus.REPORTED, IncidentStatus.IN_REVIEW));
        if (openReports.isEmpty()) return;

        Set<Long> unexaminedCaseIds = careScheduleRepository.findByHorseIdAndStatusIn(
                        horseId, List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS))
                .stream()
                .map(CareSchedule::getId)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : "Horse left the club";
        List<HorseIncidentReport> closed = new ArrayList<>();
        for (HorseIncidentReport report : openReports) {
            Long caseId = report.getCareScheduleId();
            if (caseId != null && !unexaminedCaseIds.contains(caseId)) continue;
            report.setStatus(IncidentStatus.DISMISSED);
            report.setHandledAt(now);
            report.setHandlerNote("Closed without a completed examination: " + effectiveReason);
            report.setCareScheduleId(null);
            closed.add(report);
            notificationService.sendAssignmentNotification(
                    report.getReportedById(),
                    NotificationTypes.REFERENCE_CARE_SCHEDULE,
                    caseId,
                    horseId,
                    NotificationTypes.URGENT_CASE_CLOSED,
                    "Incident report closed",
                    "Your incident report \"" + report.getTitle() + "\" was closed without an examination. Reason: " + effectiveReason);
        }
        if (!closed.isEmpty()) {
            incidentReportRepository.saveAll(closed);
        }
    }

    private static boolean isIncidentCase(CareSchedule schedule) {
        return schedule.getCareType() == CareType.URGENT || schedule.getSourceIncidentId() != null;
    }

    /** Vet đã nhận ca: mọi báo cáo của ca chuyển IN_REVIEW, kể cả báo cáo gắn vào trước khi có Vet. */
    private HorseIncidentReport markCaseReportsInReview(CareSchedule schedule, Long vetId, LocalDateTime assignedAt) {
        HorseIncidentReport source = incidentReportRepository.findByIdForUpdate(schedule.getSourceIncidentId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "INCIDENT_NOT_FOUND", "Urgent incident not found"));
        List<HorseIncidentReport> reports = new ArrayList<>(
                incidentReportRepository.findByCareScheduleIdOrderByReportedAtAsc(schedule.getId()));
        if (reports.stream().noneMatch(report -> Objects.equals(report.getId(), source.getId()))) {
            reports.add(source);
        }
        for (HorseIncidentReport report : reports) {
            report.setStatus(IncidentStatus.IN_REVIEW);
            report.setHandledById(vetId);
            report.setHandledAt(assignedAt);
        }
        incidentReportRepository.saveAll(reports);
        return source;
    }

    private CareSchedule lockClinicalSchedule(Long scheduleId) {
        CareSchedule snapshot = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        // Match Manager review's admission -> horse -> schedule lock order.
        AdmissionApplication admission = snapshot.getAdmissionId() == null ? null
                : admissionRepository.findByIdForUpdate(snapshot.getAdmissionId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        Horse horse = horseRepository.findByIdForUpdate(snapshot.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        CareSchedule locked = careScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        // The compatibility adapter may already have these entities in its persistence context.
        // Preserve this transaction's writes, then read the state committed before we acquired the locks.
        entityManager.flush();
        if (admission != null) entityManager.refresh(admission);
        entityManager.refresh(horse);
        entityManager.refresh(locked);
        return locked;
    }

    private void ensureAvailable(CareSchedule schedule, Long vetId, LocalDateTime start) {
        List<CareSchedule> activeExams = new ArrayList<>(
                careScheduleRepository.findByVeterinarianIdAndStatus(vetId, CareScheduleStatus.IN_PROGRESS));
        boolean vetHasConflict = activeExams.stream()
                .anyMatch(other -> inProgressConflictsWithSlot(schedule, start, other));
        boolean horseBusy = careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS);
        if (horseBusy && schedule.getCareType() == CareType.URGENT) {
            // An emergency must reach a horse whose in-progress visit is stale (past
            // its booked duration); only a genuinely current examination blocks it.
            horseBusy = activeExams.stream()
                    .filter(cs -> Objects.equals(cs.getHorseId(), schedule.getHorseId()))
                    .anyMatch(other -> inProgressConflictsWithSlot(schedule, start, other));
        }
        if (vetHasConflict || horseBusy) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Veterinarian or horse has an examination in progress");
        }
        // Một ca đang khám vẫn giữ slot của nó, nên vẫn tính là đã đặt khi kiểm tra xung đột.
        List<CareSchedule> booked = new ArrayList<>(
                careScheduleRepository.findByVeterinarianIdAndStatusIn(vetId, CareScheduleStatus.BOOKED_OR_ACTIVE));
        if (schedule.getCareType() != CareType.URGENT) {
            booked.addAll(careScheduleRepository.findByHorseIdAndStatusIn(
                    schedule.getHorseId(), CareScheduleStatus.BOOKED_OR_ACTIVE));
        }
        for (CareSchedule other : booked) {
            if (Objects.equals(other.getId(), schedule.getId()) || !blocksSlot(schedule, start, other)) continue;
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Veterinarian or horse is already scheduled in this slot");
        }
    }

    /** Ca khẩn cấp được ưu tiên hơn lịch hẹn thường: chỉ một ca khẩn cấp khác mới cản được nó. */
    private boolean blocksSlot(CareSchedule schedule, LocalDateTime start, CareSchedule other) {
        boolean outranked = schedule.getCareType() == CareType.URGENT && other.getCareType() != CareType.URGENT;
        return !outranked && overlaps(schedule, start, other);
    }

    /** V2: San sẻ ca Routine trong ngày cho Vet đồng nghiệp nếu Vet chính bận ca cấp cứu. */
    private void offloadRoutineForUrgent(Long vetId, LocalDate date, boolean wasBusy, Long excludeFromOffload) {
        if (vetId == null) return;
        // Chỉ offload khi vet đang bận (preemption được dùng) — tránh offload không cần thiết khi vet rảnh.
        if (!wasBusy) return;
        List<CareSchedule> uncompletedRoutines = careScheduleRepository
                .findByVeterinarianIdAndScheduledDateAndCareTypeAndStatusInOrderByCreatedAtAsc(
                        vetId, date, CareType.ROUTINE, CareScheduleStatus.BOOKED_OR_ACTIVE);
        if (uncompletedRoutines.isEmpty()) return;

        List<User> activeVets = userRepository.findActiveVeterinarians();
        for (CareSchedule routine : uncompletedRoutines) {
            // Ca vừa bị chính ca cấp cứu này hoãn lại: Vet khám tiếp, không đẩy sang đồng nghiệp.
            if (Objects.equals(routine.getId(), excludeFromOffload)) continue;
            boolean isPreventive = routine.getPurpose() == CareSchedulePurpose.PREVENTIVE;
            int quota = isPreventive ? PREVENTIVE_DAILY_QUOTA : ROUTINE_DAILY_QUOTA;
            User peer = activeVets.stream()
                    .filter(v -> !Objects.equals(v.getId(), vetId))
                    .filter(v -> {
                        List<CareSchedule> peerRoutines = careScheduleRepository
                                .findByVeterinarianIdAndScheduledDateAndCareTypeAndStatusInOrderByCreatedAtAsc(
                                        v.getId(), date, CareType.ROUTINE, CareScheduleStatus.BOOKED_OR_ACTIVE);
                        long count = peerRoutines.stream()
                                .filter(cs -> isPreventive
                                        ? cs.getPurpose() == CareSchedulePurpose.PREVENTIVE
                                        : cs.getPurpose() != CareSchedulePurpose.PREVENTIVE)
                                .count();
                        if (count >= quota) return false;
                        return peerRoutines.stream()
                                .noneMatch(cs -> Objects.equals(cs.getHorseId(), routine.getHorseId()));
                    })
                    .min(Comparator.comparingLong(v -> careScheduleRepository
                            .findByVeterinarianIdAndScheduledDateAndCareTypeAndStatusInOrderByCreatedAtAsc(
                                    v.getId(), date, CareType.ROUTINE, CareScheduleStatus.BOOKED_OR_ACTIVE).size()))
                    .orElse(null);

            if (peer != null) {
                routine.setVeterinarianId(peer.getId());
                routine.setRescheduleReason(RescheduleReason.PEER_OFFLOAD);
                routine.setRescheduleNote("Offloaded to Dr. " + peer.getFullName() + " due to emergency case handling");
                careScheduleRepository.save(routine);

                notificationService.sendAssignmentNotification(
                        peer.getId(),
                        NotificationTypes.REFERENCE_CARE_SCHEDULE,
                        routine.getId(),
                        routine.getHorseId(),
                        NotificationTypes.ROUTINE_ASSIGNED,
                        "Routine visit offloaded to you",
                        "Routine visit for horse #" + routine.getHorseId() + " was transferred to you to support urgent care.");
            } else {
                routine.setRescheduleReason(RescheduleReason.URGENT_PREEMPTION);
                routine.setRescheduleNote("Priority shifted due to urgent case handling");
                careScheduleRepository.save(routine);
            }
        }
    }

    private boolean overlaps(CareSchedule schedule, LocalDateTime start, CareSchedule other) {
        if (other.getScheduledAt() == null) return false;
        LocalDateTime end = start.plusMinutes(schedule.getDurationMinutes() > 0 ? schedule.getDurationMinutes() : 30);
        LocalDateTime otherEnd = other.getScheduledAt().plusMinutes(other.getDurationMinutes() > 0 ? other.getDurationMinutes() : 30);
        return start.isBefore(otherEnd) && end.isAfter(other.getScheduledAt());
    }

    @Transactional
    public CareScheduleResponse startCareSchedule(Long scheduleId, Long vetId) {
        userRepository.findById(vetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VET_NOT_FOUND", "Veterinarian not found"));
        CareSchedule schedule = lockClinicalSchedule(scheduleId);

        if (schedule.getStatus() != CareScheduleStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "Only SCHEDULED care schedules can be started");
        }

        if (!Objects.equals(schedule.getVeterinarianId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can start this schedule");
        }
        // Một ngựa chỉ khám một ca tại một thời điểm: complete resolve mọi báo cáo đang mở của ngựa (AD-004),
        // nên ca thứ hai mở song song sẽ đóng oan báo cáo của ca còn lại.
        if (careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS)) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_EXAM_IN_PROGRESS",
                    "This horse already has an examination in progress");
        }
        validateRoutineScheduleDate(schedule);
        validateRoutineExamWindowForStart(schedule);

        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        return CareScheduleResponse.from(careScheduleRepository.save(schedule));
    }

    @Transactional
    public CareScheduleResponse completeCareSchedule(Long scheduleId, CompleteCareScheduleRequest request, Long vetId) {
        if (request.getFindings() == null || request.getFindings().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Findings are required");
        }
        if (request.getDiagnosis() == null || request.getDiagnosis().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Diagnosis is required");
        }
        if (request.getTrainingDecision() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Training decision is required");
        }
        if (request.getTrainingDecision() == TrainingDecision.BLOCKED) {
            if (request.getRestrictionDetails() == null || request.getRestrictionDetails().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Restriction details are required when training is BLOCKED");
            }
        }
        validatePrescriptions(request.getPrescriptions());

        CareSchedule schedule = lockClinicalSchedule(scheduleId);

        if (schedule.getStatus() == CareScheduleStatus.COMPLETED) {
            if (Objects.equals(schedule.getVeterinarianId(), vetId)) {
                // Idempotent: a completed schedule never creates a follow-up twice.
                return CareScheduleResponse.from(schedule);
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can access this completed schedule");
        }

        // A late exam is completed from IN_PROGRESS too, without a reopen step.
        // SCHEDULED is also accepted directly for routine and admission visits (no start-exam step needed).
        if (!CareScheduleStatus.ACTIVE_EXAM.contains(schedule.getStatus())
                && schedule.getStatus() != CareScheduleStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "Care schedule must be SCHEDULED or IN_PROGRESS to complete");
        }
        if (!Objects.equals(schedule.getVeterinarianId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can complete this schedule");
        }
        // Complete nhận cả SCHEDULED, nên phải chặn ở đây nữa: nếu ngựa đang khám ca khác thì
        // resolve-all (AD-004) sẽ đóng oan báo cáo của ca kia.
        if (schedule.getStatus() != CareScheduleStatus.IN_PROGRESS
                && careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS)) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_EXAM_IN_PROGRESS",
                    "This horse has another examination in progress; finish it first");
        }
        validateRoutineScheduleDate(schedule);

        validateMandatoryVitalsAndMarkers(request, schedule);

        boolean requiresFollowUp = schedule.getCareType() == CareType.URGENT
                || request.getTrainingDecision() == TrainingDecision.BLOCKED;
        if (requiresFollowUp) {
            if (request.getNextSchedule() == null) {
                String message = schedule.getCareType() == CareType.URGENT
                        ? "A follow-up examination (nextSchedule) is required for all urgent cases"
                        : "A follow-up examination (nextSchedule) is required when training is BLOCKED";
                throw new ApiException(HttpStatus.BAD_REQUEST, "FOLLOW_UP_REQUIRED", message);
            }
        }

        // Kiểm tra mũi tiêm trước mọi thay đổi trạng thái: nhập sai thì từ chối cả ca, không ghi dở.
        List<PreventiveVaccinationService.ValidatedVaccination> vaccinations =
                request.getVaccinations() == null || request.getVaccinations().isEmpty()
                        ? List.of()
                        : preventiveVaccinationService.validate(request.getVaccinations(), schedule.getCareType());
        Integer deferDays = request.getDeferVaccinationDays();
        if (deferDays != null) {
            if (schedule.getPurpose() != CareSchedulePurpose.PREVENTIVE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Vaccination can only be deferred on a preventive care visit");
            }
            if (!vaccinations.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Cannot defer vaccination and record vaccinations in the same visit");
            }
            if (deferDays < 7 || deferDays > 10) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Vaccination can be deferred by 7 to 10 days");
            }
        }

        AdmissionApplication linkedAdmission = schedule.getAdmissionId() == null ? null
                : admissionRepository.findByIdForUpdate(schedule.getAdmissionId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
        if (schedule.getCareType() == CareType.INITIAL && (linkedAdmission == null
                || linkedAdmission.getStatus() != AdmissionStatus.VET_REVIEW)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Admission is not awaiting a vet examination");
        }
        Horse horse = horseRepository.findByIdForUpdate(schedule.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));

        TrainingDecision decision = request.getTrainingDecision();
        List<CareScheduleStatus> activeStatuses = CareScheduleStatus.OPEN;
        boolean anotherActiveUrgent = schedule.getCareType() == CareType.URGENT
                ? careScheduleRepository.existsByHorseIdAndCareTypeAndStatusInAndIdNot(
                        horse.getId(), CareType.URGENT, activeStatuses, schedule.getId())
                : careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                        horse.getId(), CareType.URGENT, activeStatuses);
        // Ngựa chỉ có MỘT trạng thái khóa nhưng có thể có nhiều ca khám cùng lúc.
        // Còn một ca khẩn cấp khác chưa xong thì lần khám này không được mở khóa:
        // kết quả vẫn lưu vào bệnh án bên dưới, nhưng ngựa giữ nguyên BLOCKED.
        boolean preservedUrgentLock = anotherActiveUrgent
                && horse.getTrainingDecision() == TrainingDecision.BLOCKED
                && decision == TrainingDecision.ALLOWED;
        // Ca nhẹ từ báo cáo không mở được lệnh chặn có sẵn: lần khám đang chờ mới quyết định.
        // Không còn lịch nào khác thì Vet vẫn mở được, để ngựa không bị khóa vĩnh viễn.
        boolean preservedForOtherExam = schedule.getCareType() == CareType.ROUTINE
                && schedule.getSourceIncidentId() != null
                && horse.getTrainingDecision() == TrainingDecision.BLOCKED
                && decision == TrainingDecision.ALLOWED
                && careScheduleRepository.existsByHorseIdAndPurposeAndStatusInAndIdNot(
                        horse.getId(), CareSchedulePurpose.FOLLOW_UP, activeStatuses, schedule.getId());
        TrainingDecision previousDecision = horse.getTrainingDecision();
        if (!preservedUrgentLock && !preservedForOtherExam) {
            if (schedule.getCareType() == CareType.URGENT && decision == TrainingDecision.ALLOWED) {
                // WF-02 (Q11): URGENT never releases the training block.
                // Ngựa giữ nguyên BLOCKED từ lúc report incident tới khi ca tái khám (ROUTINE) quyết định.
                if (request.getRestrictionDetails() != null && !request.getRestrictionDetails().isBlank()) {
                    trainingDecisionService.block(horse, request.getRestrictionDetails().trim());
                } else if (horse.getTrainingDecision() != TrainingDecision.BLOCKED) {
                    trainingDecisionService.block(horse, "Post-urgent monitoring: awaiting a follow-up examination");
                }
            } else if (decision == TrainingDecision.ALLOWED) {
                trainingDecisionService.allow(horse);
                // Chỉ thông báo cho Trainer khi ngựa chuyển thật từ BLOCKED sang ALLOWED
                // (tránh làm phiền khi khám định kỳ ALLOWED -> ALLOWED).
                if (previousDecision == TrainingDecision.BLOCKED) {
                    horseRepository.findTrainerIdByHorseId(horse.getId()).ifPresent(trainerId -> {
                        String horseName = horse.getName() != null ? horse.getName() : ("#" + horse.getId());
                        String title = "Training resumed: " + horseName;
                        String message = "Horse " + horseName
                                + " has been cleared by the veterinarian and is now allowed to train.";
                        try {
                            notificationService.sendAssignmentNotification(
                                    trainerId,
                                    NotificationTypes.REFERENCE_HORSE,
                                    horse.getId(),
                                    horse.getId(),
                                    NotificationTypes.HORSE_TRAINING_ALLOWED,
                                    title,
                                    message
                            );
                        } catch (Exception ex) {
                            // Bỏ qua lỗi gửi notification để bảo toàn giao dịch khám bệnh
                        }
                    });
                }
            } else {
                trainingDecisionService.block(horse, request.getRestrictionDetails().trim());
            }
        }
        // The horse's own state is the effective decision: it stays blocked when another urgent
        // case is open or the last non-urgent exam concluded BLOCKED, so report that, not the conclusion.
        TrainingDecision effectiveDecision = horse.getTrainingDecision();
        String effectiveReason = horse.getTrainingDecisionReason();

        LocalDateTime now = LocalDateTime.now().minusNanos(1_000_000);
        HealthRecord record = new HealthRecord();
        record.setHorseId(horse.getId());
        record.setVeterinarianId(vetId);
        record.setCareScheduleId(schedule.getId());
        record.setRecordType(schedule.getCareType().name());
        record.setExaminedAt(now);
        record.setFindings(request.getFindings().trim());
        com.rtms.backend.medical.enums.ClinicalDiagnosis cd = com.rtms.backend.medical.enums.ClinicalDiagnosis.fromString(request.getDiagnosis());
        record.setDiagnosis(cd != null ? cd.getDisplayName() : request.getDiagnosis().trim());
        record.setTreatment(request.getTreatment() != null ? request.getTreatment().trim() : null);
        // Bệnh án ghi kết quả thực tế: ca bị giữ chặn thì ngựa vẫn BLOCKED dù Vet chọn ALLOWED.
        record.setTrainingDecision(preservedForOtherExam ? TrainingDecision.BLOCKED : request.getTrainingDecision());
        record.setRestrictionDetails(preservedForOtherExam ? horse.getTrainingDecisionReason()
                : request.getRestrictionDetails() != null ? request.getRestrictionDetails().trim() : null);

        record.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        HealthRecord savedRecord = healthRecordRepository.save(record);
        eventPublisher.publishEvent(new ClinicalRecordCreatedEvent(
                savedRecord.getId(),
                horse.getId(),
                horse.getRegistrationNumber(),
                savedRecord.getFindings(),
                savedRecord.getDiagnosis(),
                List.of("VETERINARIAN", "HEAD_TRAINER", "CLUB_MANAGER")
        ));
        if (!vaccinations.isEmpty()) {
            preventiveVaccinationService.record(savedRecord.getId(), vaccinations);
        }
        persistPrescriptions(request.getPrescriptions(), horse, savedRecord, request);

        // Persist injury markers captured on the 3D model (T4).
        // FE không gửi injuryType/severity: loại chấn thương lấy từ displayName của bodyRegion
        // (Q5-1, duyệt 2026-11-12), severity mặc định MODERATE — Vet sửa sau qua injury correction.
        if (request.getInjuryMarkers() != null) {
            for (InjuryMarkerRequest m : request.getInjuryMarkers()) {
                InjuryRecord ir = new InjuryRecord();
                ir.setHorseId(horse.getId());
                ir.setHealthRecordId(savedRecord.getId());
                HorseBodyRegion region = m.getBodyRegionId() != null
                        ? entityManager.find(HorseBodyRegion.class, m.getBodyRegionId())
                        : null;
                String resolvedType = (region != null && region.getDisplayName() != null
                        && !region.getDisplayName().isBlank())
                        ? region.getDisplayName().trim()
                        : (m.getZoneId() != null && !m.getZoneId().isBlank() ? m.getZoneId() : "INJURY");
                String note = m.getDescription() != null ? m.getDescription().trim() : "";
                ir.setInjuryType(resolvedType);
                ir.setBodyRegionId(m.getBodyRegionId());
                ir.setSeverity("MODERATE");
                ir.setDescription(note.isEmpty() ? null : note);
                ir.setDiagnosedAt(m.getDiagnosedAt() != null ? m.getDiagnosedAt() : now);
                ir.setStatus("ACTIVE");
                ir.setPositionX(m.getPositionX());
                ir.setPositionY(m.getPositionY());
                ir.setPositionZ(m.getPositionZ());
                ir.setZoneId(m.getZoneId());
                ir.setLocalX(m.getLocalX());
                ir.setLocalY(m.getLocalY());
                ir.setLocalZ(m.getLocalZ());
                ir.setNormalX(m.getNormalX());
                ir.setNormalY(m.getNormalY());
                ir.setNormalZ(m.getNormalZ());
                injuryRecordRepository.save(ir);
            }
        }

        // C7/B1: không auto-recovery hàng loạt. Vet tick điều kiện cụ thể (resolvedConditionIds)
        // thì điều kiện đó mới được đánh dấu RECOVERED; nếu decision == ALLOWED thì phác đồ OLD
        // của bệnh án liên quan cũng được đóng (COMPLETED); thuốc kê trong chính ca này luôn giữ ACTIVE (B1).
        if (request.getResolvedConditionIds() != null) {
            for (Long conditionId : request.getResolvedConditionIds()) {
                if (conditionId == null) continue;
                InjuryRecord ir = injuryRecordRepository.findById(conditionId)
                        .filter(x -> Objects.equals(x.getHorseId(), horse.getId()))
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CONDITION_NOT_FOUND",
                                "Health condition " + conditionId + " not found for this horse"));
                if ("RECOVERED".equalsIgnoreCase(ir.getStatus())) continue;
                ir.setStatus("RECOVERED");
                ir.setRecoveredAt(now);
                injuryRecordRepository.save(ir);
                if (ir.getHealthRecordId() != null && decision == TrainingDecision.ALLOWED) {
                    for (TreatmentPlan oldPlan : treatmentPlanRepository
                            .findByHealthRecordIdAndStatusIgnoreCase(ir.getHealthRecordId(), "ACTIVE")) {
                        oldPlan.setStatus("COMPLETED");
                        treatmentPlanRepository.save(oldPlan);
                    }
                }
            }
        }

        // Save metrics if present
        if (request.getMetrics() != null) {
            for (HorseHealthMetricRequest metricReq : request.getMetrics()) {
                HorseHealthMetric m = new HorseHealthMetric();
                m.setHorseId(horse.getId());
                m.setHealthRecordId(savedRecord.getId());
                m.setRecordedAt(now);
                m.setHeartRate(metricReq.getHeartRate());
                m.setTemperature(metricReq.getTemperature());
                m.setWeight(metricReq.getWeight());
                m.setRespiratoryRate(metricReq.getRespiratoryRate());
                m.setHydrationStatus(metricReq.getHydrationStatus());
                m.setBodyConditionScore(metricReq.getBodyConditionScore());
                m.setNotes(metricReq.getNotes());
                metricRepository.save(m);
            }
        }

        // If exam linked to AdmissionApplication, update admission status
        if (schedule.getAdmissionId() != null) {
            AdmissionApplication admission = linkedAdmission;
            if (admission != null) {
                boolean isInitial = schedule.getCareType() == CareType.INITIAL;
                // Only INITIAL examinations own the Admission workflow. ROUTINE care must
                // not move an Admission backward even when it references a legacy admission.
                if (isInitial) {
                    admission.setVeterinarianId(vetId);
                    admission.setVetReviewedAt(now);
                    admission.setVetFeedback(request.getNotes() != null && !request.getNotes().isBlank()
                            ? request.getNotes() : request.getFindings());
                    // Thú y không duyệt/từ chối đơn: đơn luôn đi tiếp sang Trainer, kèm
                    // kết luận được tập hay tạm nghỉ. Ngựa vẫn là CANDIDATE nên chưa tập được
                    // cho tới khi Manager duyệt — không cần khóa thêm.
                    admission.setVetTrainingDecision(request.getTrainingDecision());
                    admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
                    admissionRepository.save(admission);
                }
            }
        }

        schedule.setStatus(CareScheduleStatus.COMPLETED);
        schedule.setCompletedAt(now);
        CareSchedule completedSchedule = careScheduleRepository.save(schedule);

        // Sau khi ca này COMPLETED (không còn là mốc active) mới đặt mốc tiêm kế tiếp.
        // Ca tiêm phòng mà vet hoãn tiêm vẫn phải để lại một mốc mới cho ngựa.
        if (deferDays != null) {
            careScheduleRepository.flush();
            preventiveVaccinationService.deferMilestone(horse.getId(), savedRecord.getId(), vetId, deferDays);
        } else if (!vaccinations.isEmpty()) {
            careScheduleRepository.flush();
            preventiveVaccinationService.refreshMilestone(horse.getId(), savedRecord.getId(), vetId,
                    schedule.getCareType());
        } else if (schedule.getPurpose() == CareSchedulePurpose.PREVENTIVE) {
            // Auto-create a preventive (vaccination) milestone after an ALLOWED exam even when
            // no vaccine was administered, so the next vaccination checkpoint is booked.
            careScheduleRepository.flush();
            preventiveVaccinationService.refreshMilestone(horse.getId(), savedRecord.getId(), vetId,
                    schedule.getCareType());
        }

        // Gán Trainer chạy SAU khi transaction này commit (TrainerAssignmentTriggers):
        // lỗi lúc gán không thể làm rollback kết quả khám của Vet.
        if (schedule.getAdmissionId() != null && schedule.getCareType() == CareType.INITIAL) {
            eventPublisher.publishEvent(new InitialExamCompletedEvent(schedule.getAdmissionId()));
        }

        if (isIncidentCase(schedule)) {
            List<HorseIncidentReport> openIncidents = new ArrayList<>(
                    incidentReportRepository.findByHorseIdAndStatusIn(
                            horse.getId(),
                            List.of(IncidentStatus.REPORTED, IncidentStatus.IN_REVIEW)
                    )
            );
            if (schedule.getSourceIncidentId() != null) {
                incidentReportRepository.findById(schedule.getSourceIncidentId()).ifPresent(src -> {
                    boolean alreadyPresent = openIncidents.stream().anyMatch(i -> Objects.equals(i.getId(), src.getId()));
                    if (!alreadyPresent && (src.getStatus() == IncidentStatus.REPORTED || src.getStatus() == IncidentStatus.IN_REVIEW)) {
                        openIncidents.add(src);
                    }
                });
            }

            Set<Long> recipients = new HashSet<>();
            for (HorseIncidentReport inc : openIncidents) {
                inc.setStatus(IncidentStatus.RESOLVED);
                inc.setHandledById(vetId);
                inc.setHandledAt(now);
                inc.setHandlerNote(incidentOutcomeNote(request, effectiveDecision, effectiveReason));
                if (inc.getGroomId() != null) {
                    recipients.add(inc.getGroomId());
                }
            }
            if (!openIncidents.isEmpty()) {
                incidentReportRepository.saveAll(openIncidents);
            }

            // Gửi thông báo cho Trainer phụ trách khu chuồng của ngựa.
            // Ca ROUTINE đã có thông báo riêng cho Trainer ở khối bên dưới.
            if (schedule.getCareType() == CareType.URGENT) {
                horseRepository.findTrainerIdByHorseId(horse.getId()).ifPresent(recipients::add);
            }

            // Bắn thông báo URGENT_CASE_CLOSED về cho cả Groom và Trainer liên quan
            String horseName = horse.getName() != null ? horse.getName() : ("#" + horse.getId());
            String caseLabel = schedule.getCareType() == CareType.URGENT ? "Urgent case" : "Incident case";
            String title = caseLabel + " closed: " + horseName;
            String message = caseLabel + " closed for " + horseName
                    + ". Training decision: " + effectiveDecision
                    + (effectiveDecision == TrainingDecision.BLOCKED
                            && effectiveReason != null && !effectiveReason.isBlank()
                            ? " (" + effectiveReason + ")" : "");

            for (Long recipientId : recipients) {
                if (recipientId != null) {
                    try {
                        notificationService.sendAssignmentNotification(
                                recipientId,
                                NotificationTypes.REFERENCE_CARE_SCHEDULE,
                                schedule.getId(),
                                horse.getId(),
                                NotificationTypes.URGENT_CASE_CLOSED,
                                title,
                                message
                        );
                    } catch (Exception ex) {
                        // Bỏ qua lỗi gửi notification để không làm gián đoạn hoàn tất ca khám
                    }
                }
            }
        }

        // Bắn thông báo kết quả khám định kỳ / tái khám cho Trainer phụ trách ngựa
        if (schedule.getCareType() == CareType.ROUTINE) {
            horseRepository.findTrainerIdByHorseId(horse.getId()).ifPresent(trainerId -> {
                String horseName = horse.getName() != null ? horse.getName() : ("#" + horse.getId());
                String title = "Routine Examination Completed: " + horseName;
                String decisionText = effectiveDecision == TrainingDecision.ALLOWED
                        ? "Cleared for training (ALLOWED)."
                        : "Training on hold (BLOCKED)." + (effectiveReason != null && !effectiveReason.isBlank() ? " Note: " + effectiveReason : "");
                String message = "Veterinarian has completed routine examination for " + horseName + ". Decision: " + decisionText;
                try {
                    notificationService.sendAssignmentNotification(
                            trainerId,
                            NotificationTypes.REFERENCE_CARE_SCHEDULE,
                            schedule.getId(),
                            horse.getId(),
                            NotificationTypes.CARE_SCHEDULE_ROUTINE_COMPLETED,
                            title,
                            message
                    );
                } catch (Exception ex) {
                    // Bỏ qua lỗi gửi notification để bảo toàn giao dịch khám bệnh
                }
            });
        }

        // Atomic next schedule creation if provided. Multiple follow-ups let vets
        // rotate rechecks on consecutive days; each entry gets its own idempotency
        // key (source_schedule_id is one-to-one and cannot anchor them all).
        boolean createdFollowUp = false;
        if (request.getNextSchedule() != null) {
            createNextSchedule(request.getNextSchedule(), vetId, completedSchedule);
            createdFollowUp = true;
        }
        if (!createdFollowUp) {
            createRoutineCadenceSchedule(vetId, completedSchedule);
        }

        return CareScheduleResponse.from(completedSchedule);
    }

    /**
     * Ca khám định kỳ (ROUTINE) chỉ được phép bắt đầu hoặc hoàn thành vào đúng ngày hẹn hoặc sau đó.
     * Chặn các ca khám có ngày hẹn ở tương lai.
     */
    private void validateRoutineScheduleDate(CareSchedule schedule) {
        if (schedule.getCareType() == CareType.ROUTINE) {
            LocalDate today = LocalDate.now();
            LocalDate scheduledDate = schedule.getScheduledDate();
            if (scheduledDate == null && schedule.getScheduledAt() != null) {
                scheduledDate = schedule.getScheduledAt().toLocalDate();
            }
            if (scheduledDate == null && schedule.getRequestedAt() != null) {
                scheduledDate = schedule.getRequestedAt().toLocalDate();
            }
            if (scheduledDate != null && scheduledDate.isAfter(today)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ROUTINE_EXAM_TOO_EARLY",
                        "Routine examination can only be conducted on or after the scheduled date (" + scheduledDate + ")");
            }
        }
    }

    /**
     * Ca khám định kỳ (ROUTINE) chỉ được phép bấm bắt đầu (Examine) trong khung giờ 13:30 - 15:30.
     * Sau 15:30, ca nào đã kịp IN_PROGRESS vẫn được phép hoàn tất (completeCareSchedule).
     */
    private void validateRoutineExamWindowForStart(CareSchedule schedule) {
        if (schedule.getCareType() == CareType.ROUTINE) {
            LocalTime now = LocalTime.now();
            if (now.isBefore(FarmSchedulePolicy.VET_WINDOW_START) || now.isAfter(FarmSchedulePolicy.VET_WINDOW_END)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ROUTINE_EXAM_OUTSIDE_WINDOW",
                        "Routine examinations can only be started between " + FarmSchedulePolicy.VET_WINDOW_START
                                + " and " + FarmSchedulePolicy.VET_WINDOW_END);
            }
        }
    }

    /**
     * D3: khám nhập trại (INITIAL) và tái khám định kỳ (ROUTINE+FOLLOW_UP) bắt buộc ghi đủ
     * 6 chỉ số sinh hiệu trong phạm vi cho phép. D4: INITIAL kết luận BLOCKED phải có ít nhất
     * một marker chấn thương/bệnh lý. Không cho phép thay đổi trạng thái nếu thiếu.
     */
    private void validateMandatoryVitalsAndMarkers(CompleteCareScheduleRequest request, CareSchedule schedule) {
        boolean requiresFullVitals = schedule.getCareType() == CareType.INITIAL
                || (schedule.getCareType() == CareType.ROUTINE && schedule.getPurpose() == CareSchedulePurpose.FOLLOW_UP
                        && schedule.getSourceIncidentId() == null);
        if (requiresFullVitals) {
            if (request.getMetrics() == null || request.getMetrics().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Vital signs are required: heart rate, temperature, weight, respiratory rate, hydration status, and body condition score");
            }
            List<String> rowErrors = new ArrayList<>();
            int index = 0;
            for (HorseHealthMetricRequest m : request.getMetrics()) {
                List<String> missing = new ArrayList<>();
                if (m.getHeartRate() == null) missing.add("heart rate");
                if (m.getTemperature() == null) missing.add("temperature");
                if (m.getWeight() == null) missing.add("weight");
                if (m.getRespiratoryRate() == null) missing.add("respiratory rate");
                if (m.getHydrationStatus() == null) missing.add("hydration status");
                if (m.getBodyConditionScore() == null) missing.add("body condition score");

                // Giới hạn đo được, không phải khoảng bình thường: ngựa đang bệnh vẫn phải ghi nhận được.
                List<String> outOfRange = new ArrayList<>();
                if (m.getHeartRate() != null) {
                    double hr = m.getHeartRate().doubleValue();
                    if (hr < 20 || hr > 240) outOfRange.add("heart rate " + hr + " (20-240)");
                }
                if (m.getTemperature() != null) {
                    double t = m.getTemperature().doubleValue();
                    if (t < 30 || t > 45) outOfRange.add("temperature " + t + " (30-45)");
                }
                if (m.getRespiratoryRate() != null) {
                    double rr = m.getRespiratoryRate().doubleValue();
                    if (rr < 4 || rr > 100) outOfRange.add("respiratory rate " + rr + " (4-100)");
                }
                if (m.getWeight() != null) {
                    double w = m.getWeight().doubleValue();
                    if (w <= 0 || w >= 1500) outOfRange.add("weight " + w + " (0-1500)");
                }
                if (m.getBodyConditionScore() != null) {
                    double bcs = m.getBodyConditionScore().doubleValue();
                    if (bcs < 1 || bcs > 9) outOfRange.add("body condition score " + bcs + " (1-9)");
                }

                if (!missing.isEmpty() || !outOfRange.isEmpty()) {
                    rowErrors.add("metric[" + index + "]: "
                            + (missing.isEmpty() ? "" : "missing " + String.join(", ", missing)
                                    + (outOfRange.isEmpty() ? "" : ", "))
                            + (outOfRange.isEmpty() ? "" : "out of range " + String.join(", ", outOfRange)));
                }
                index++;
            }
            if (!rowErrors.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Incomplete or out-of-range vital signs: " + String.join(" | ", rowErrors));
            }
        }

        if (schedule.getCareType() == CareType.INITIAL && request.getTrainingDecision() == TrainingDecision.BLOCKED) {
            if (request.getInjuryMarkers() == null || request.getInjuryMarkers().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "An injury or condition record is required when blocking a newly admitted horse.");
            }
        }

        // Coupling marker => BLOCKED (duyệt 2026-11-12, mirror FE lock): ca INITIAL/ROUTINE
        // có marker cắm trong phiên khám thì không được phép ALLOWED. URGENT giữ rule riêng.
        boolean hasInjuryMarkers = request.getInjuryMarkers() != null && !request.getInjuryMarkers().isEmpty();
        if (hasInjuryMarkers && request.getTrainingDecision() == TrainingDecision.ALLOWED
                && (schedule.getCareType() == CareType.INITIAL
                        || schedule.getCareType() == CareType.ROUTINE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MARKERS_BLOCK_TRAINING",
                    "Injury markers require training to stay BLOCKED. Remove the markers or keep the horse blocked.");
        }
    }

    private void validatePrescriptions(List<PrescriptionItemRequest> prescriptions) {
        if (prescriptions == null || prescriptions.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        for (PrescriptionItemRequest item : prescriptions) {
            String medicationName = item.getMedicationName().trim();
            if (!seen.add(medicationName.toLowerCase(Locale.ROOT))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_MEDICATION",
                        "Medication " + medicationName + " is already prescribed in this examination");
            }
            if (item.getRoute() != null && item.getRoute().isInjection()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INJECTION_NOT_ALLOWED",
                        "Treatment plans cannot contain injections");
            }
        }
    }

    /** Lưu nguyên tử plan + đơn trong transaction hoàn tất ca khám (Luồng 1). */
    private void persistPrescriptions(List<PrescriptionItemRequest> prescriptions, Horse horse,
            HealthRecord record, CompleteCareScheduleRequest request) {
        if (prescriptions == null || prescriptions.isEmpty()) return;
        TreatmentPlan plan = new TreatmentPlan();
        plan.setHealthRecordId(record.getId());
        plan.setTreatmentName("Treatment course (" + prescriptions.size() + " medications)");
        String instructions = request.getGeneralCareInstructions() != null && !request.getGeneralCareInstructions().isBlank()
                ? request.getGeneralCareInstructions().trim()
                : (request.getTreatment() != null && !request.getTreatment().isBlank()
                        ? request.getTreatment().trim()
                        : "Follow dosage and instructions on each prescription.");
        plan.setInstructions(instructions);
        plan.setStartDate(prescriptions.stream().map(PrescriptionItemRequest::getStartDate)
                .min(LocalDate::compareTo).orElse(LocalDate.now()));
        plan.setEndDate(prescriptions.stream().map(PrescriptionItemRequest::getEndDate)
                .max(LocalDate::compareTo).orElse(null));
        plan.setStatus("ACTIVE");
        TreatmentPlan savedPlan = treatmentPlanRepository.save(plan);

        List<Long> rxIds = new ArrayList<>();
        for (PrescriptionItemRequest item : prescriptions) {
            Prescription rx = new Prescription();
            rx.setTreatmentPlanId(savedPlan.getId());
            rx.setMedicationName(item.getMedicationName().trim());
            rx.setDosage(item.getDosage());
            rx.setFrequency(item.getFrequency());
            rx.setRoute(item.getRoute().name());
            rx.setStartDate(item.getStartDate());
            rx.setEndDate(item.getEndDate());
            rx.setInstructions(item.getInstructions());
            rx.setStatus("ACTIVE");
            rx.setAdministeredByRole(item.getRoute().requiresVeterinarian() ? "VETERINARIAN" : "GROOM");
            rxIds.add(prescriptionRepository.save(rx).getId());
        }
        eventPublisher.publishEvent(new PrescriptionCreatedEvent(
                record.getCareScheduleId(), record.getId(), horse.getId(), rxIds));
    }

    /**
     * Lấy chi tiết ca khám (bao gồm horse, vet, health record liên quan).
     */
    @Transactional(readOnly = true)
    public CareScheduleDetailResponse getScheduleDetail(Long scheduleId) {
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
        User vet = schedule.getVeterinarianId() != null ? userRepository.findById(schedule.getVeterinarianId()).orElse(null) : null;
        HealthRecord hr = healthRecordRepository.findByCareScheduleId(scheduleId).orElse(null);

        LocalDateTime blockHeldUntil = schedule.getCareType() == CareType.ROUTINE && schedule.getSourceIncidentId() != null
                && schedule.getStatus() != CareScheduleStatus.COMPLETED
                && horse != null && horse.getTrainingDecision() == TrainingDecision.BLOCKED
                ? careScheduleRepository.findNextExamTime(horse.getId(), CareScheduleStatus.OPEN)
                : null;
        CareScheduleDetailResponse.WaitingFollowUp waitingFollowUp = findWaitingFollowUp(schedule.getHorseId())
                .filter(waiting -> !Objects.equals(waiting.getId(), scheduleId))
                .map(waiting -> new CareScheduleDetailResponse.WaitingFollowUp(plannedAt(waiting),
                        waiting.getVeterinarianId() == null ? null : userRepository.findById(waiting.getVeterinarianId())
                                .map(User::getFullName).orElse(null)))
                .orElse(null);
        return CareScheduleDetailResponse.of(schedule, horse, vet, hr, blockHeldUntil, waitingFollowUp);
    }

    @Transactional(readOnly = true)
    public CareScheduleDetailResponse getAssignedScheduleDetail(Long scheduleId, Long vetId) {
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        if (!Objects.equals(schedule.getVeterinarianId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can view this schedule");
        }
        return getScheduleDetail(scheduleId);
    }

    @Transactional(readOnly = true)
    public CareScheduleDetailResponse getScheduleDetailForAdmissionAssignee(Long scheduleId, Long userId,
            String role) {
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        AdmissionApplication admission = schedule.getAdmissionId() == null ? null
                : admissionRepository.findById(schedule.getAdmissionId()).orElse(null);
        boolean assigned = admission != null && "GROOM".equals(role) && Objects.equals(admission.getGroomId(), userId);
        if (!assigned) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This schedule is not assigned to you");
        }
        return getScheduleDetail(scheduleId);
    }

    @Transactional(readOnly = true)
    public Page<CareScheduleResponse> listSchedules(CareScheduleStatus status, CareType careType,
            Long horseId, Long vetId, Long requestedById, Long admissionId, LocalDate date, Pageable pageable) {
        LocalDateTime dayStart = date != null ? date.atStartOfDay() : null;
        LocalDateTime dayEnd = date != null ? dayStart.plusDays(1) : null;
        return careScheduleRepository.findFiltered(status, careType, horseId, vetId, requestedById, admissionId, dayStart, dayEnd, pageable)
                .map(CareScheduleResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CareScheduleResponse> listSchedules(CareScheduleStatus status, CareType careType,
            Long horseId, Long vetId, Long admissionId, LocalDate date, Pageable pageable) {
        return listSchedules(status, careType, horseId, vetId, null, admissionId, date, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CareScheduleResponse> listSchedules(CareScheduleStatus status, CareType careType,
            Long horseId, Long vetId, Long admissionId, Pageable pageable) {
        return listSchedules(status, careType, horseId, vetId, null, admissionId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CareScheduleResponse> listSchedulesForAdmissionAssignee(CareScheduleStatus status, CareType careType,
            Long horseId, Long vetId, Long admissionId, Long userId, String role, Pageable pageable) {
        if (!"GROOM".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only grooms can list care schedules by admission assignment");
        }
        return careScheduleRepository.findFilteredForGroom(status, careType, horseId, vetId,
                admissionId, userId, pageable).map(CareScheduleResponse::from);
    }

    @Transactional
    public CareScheduleResponse createNextSchedule(CreateNextScheduleRequest request, Long userId) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "nextSchedule is required");
        }
        if (request.getSourceScheduleId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "sourceScheduleId is required for standalone follow-up scheduling");
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VET_NOT_FOUND", "Veterinarian not found"));
        CareSchedule source = careScheduleRepository.findByIdForUpdate(request.getSourceScheduleId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOURCE_SCHEDULE_NOT_FOUND",
                        "Source examination not found"));
        if (source.getStatus() != CareScheduleStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_SOURCE_SCHEDULE",
                    "Source examination must be completed");
        }
        if (!Objects.equals(source.getVeterinarianId(), userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the veterinarian who completed the source examination may schedule its follow-up care");
        }
        return createNextSchedule(request, userId, source, null, false);
    }

    private CareScheduleResponse createNextSchedule(CreateNextScheduleRequest request, Long userId, CareSchedule source) {
        return createNextSchedule(request, userId, source, "completion:" + source.getId(), true);
    }

    /**
     * @param operationKeyOverride khóa idempotency của lịch tạo ra; null → lấy từ request
     *                             (standalone). Ca hoàn tất tạo 1 lịch dùng
     *                             "completion:<sourceId>" (giữ nguyên hành vi cũ); hoàn tất
     *                             tạo NHIỀU lịch tái khám đính kèm key của từng dòng để
     *                             mỗi lịch có khóa riêng.
     * @param linkedToSourceSchedule true khi lịch là follow-up DUY NHẤT của ca hoàn tất
     *                               (unique index trên source_schedule_id); các chế độ khác
     *                               để null vì không thể 2 dòng cùng source.
     */
    private CareScheduleResponse createNextSchedule(CreateNextScheduleRequest request, Long userId,
            CareSchedule source, String operationKeyOverride, boolean linkedToSourceSchedule) {
        if (request == null) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "nextSchedule is required");
        Long horseId = source == null ? request.getHorseId() : source.getHorseId();
        Long admissionId = source == null ? request.getAdmissionId() : source.getAdmissionId();
        if (horseId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "horseId is required");
        // Sau khi Admission đã APPROVED, không copy admissionId sang lịch ROUTINE/PREVENTIVE mới.
        if (admissionId != null) {
            AdmissionApplication adm = admissionRepository.findById(admissionId).orElse(null);
            if (adm != null && adm.getStatus() == AdmissionStatus.APPROVED) {
                admissionId = null;
            }
        }
        if (source != null) {
            if (request.getSourceScheduleId() != null && !Objects.equals(request.getSourceScheduleId(), source.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "SOURCE_SCHEDULE_MISMATCH",
                        "nextSchedule sourceScheduleId must match the completed examination");
            }
            if (request.getHorseId() != null && !Objects.equals(request.getHorseId(), horseId)) {
                throw new ApiException(HttpStatus.CONFLICT, "HORSE_MISMATCH", "nextSchedule horseId must match the completed examination");
            }
            if (request.getAdmissionId() != null
                    && !Objects.equals(request.getAdmissionId(), source.getAdmissionId())
                    && !Objects.equals(request.getAdmissionId(), admissionId)) {
                throw new ApiException(HttpStatus.CONFLICT, "ADMISSION_MISMATCH", "nextSchedule admissionId must match the completed examination");
            }
        }
        if (request.getCareType() != CareType.ROUTINE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CARE_TYPE", "Follow-up schedules must use careType ROUTINE");
        }
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        if (description == null || description.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Description is required for a follow-up schedule");
        }
        if (request.getScheduledAt() != null && request.getScheduledDate() != null && !request.getScheduledDate().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Provide either scheduledAt or scheduledDate, not both");
        }
        LocalDateTime requestedAt = request.getScheduledAt();
        if (requestedAt == null && request.getScheduledDate() != null && !request.getScheduledDate().isBlank()) {
            try {
                requestedAt = LocalDate.parse(request.getScheduledDate()).atTime(14, 0);
            } catch (DateTimeParseException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "scheduledDate must use ISO format yyyy-MM-dd");
            }
        }
        if (requestedAt == null || !requestedAt.isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "Requested schedule time must be in the future");
        }
        String operationKey = operationKeyOverride != null
                ? operationKeyOverride
                : (request.getIdempotencyKey() == null ? null : request.getIdempotencyKey().trim());
        if (operationKey == null || operationKey.isBlank() || operationKey.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A valid idempotencyKey is required");
        }
        // Serialize standalone idempotency keys per actor, including requests for
        // different horses, before checking the unique (actor,key) contract.
        Horse horse = horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        if (admissionId != null) {
            AdmissionApplication admission = admissionRepository.findById(admissionId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Admission not found"));
            if (!Objects.equals(admission.getHorseId(), horseId)) {
                throw new ApiException(HttpStatus.CONFLICT, "ADMISSION_MISMATCH", "Admission does not belong to the scheduled horse");
            }
        }
        Optional<CareSchedule> existing = linkedToSourceSchedule
                ? careScheduleRepository.findBySourceScheduleId(source.getId())
                : careScheduleRepository.findByRequestedByIdAndIdempotencyKey(userId, operationKey);
        if (existing.isPresent()) {
            boolean payloadMatches = Objects.equals(existing.get().getHorseId(), horseId)
                    && Objects.equals(existing.get().getAdmissionId(), admissionId)
                    && Objects.equals(existing.get().getDescription(), description);
            if (!payloadMatches) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency key was already used with a different payload");
            }
            return CareScheduleResponse.from(existing.get());
        }

        Optional<CareSchedule> waiting = findWaitingFollowUp(horseId);
        if (waiting.isPresent()) {
            return CareScheduleResponse.from(mergeIntoWaitingFollowUp(waiting.get(), requestedAt, description, userId));
        }

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horse.getId());
        schedule.setAdmissionId(admissionId);
        // sourceScheduleId on the entity is the one-to-one completion-operation key.
        // All other modes use (requestedById, idempotencyKey) so multiple distinct
        // follow-ups may be authorized by the same completed examination.
        schedule.setSourceScheduleId(linkedToSourceSchedule ? source.getId() : null);
        schedule.setRequestedById(userId);
        schedule.setIdempotencyKey(operationKey);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription(description);
        schedule.setRequestedAt(requestedAt);
        CareSchedule saved = careScheduleRepository.saveAndFlush(schedule);
        assignRequestedSchedule(saved, userId);
        return CareScheduleResponse.from(saved);
    }

    /**
     * AUTO cadence: a completed routine follow-up without an explicit date continues ROUTINE_CADENCE_DAYS
     * after the visit. The offset is anchored to the persisted completedAt so the request fingerprint stays
     * stable across re-submits and the follow-up row is resolved, not duplicated.
     */
    private void createRoutineCadenceSchedule(Long vetId, CareSchedule completed) {
        if (completed == null || completed.getId() == null) return;
        if (completed.getCareType() != CareType.ROUTINE) return;
        // A preventive milestone already books its own next visit (refreshMilestone/deferMilestone).
        if (completed.getPurpose() != CareSchedulePurpose.FOLLOW_UP) return;
        // A visit opened by an incident report ends with its exam; it is not a Follow-up series.
        if (completed.getSourceIncidentId() != null) return;
        if (findWaitingFollowUp(completed.getHorseId()).isPresent()) return;
        LocalDateTime completedAt = completed.getCompletedAt();
        if (completedAt == null) return;

        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setScheduledAt(completedAt.plusDays(ROUTINE_CADENCE_DAYS));
        next.setDescription(ROUTINE_CADENCE_DESCRIPTION);
        createNextSchedule(next, vetId, completed);
    }

    /** Lịch Follow-up đang chờ của ngựa. Ca từ báo cáo sự cố và mốc tiêm phòng không phải Follow-up. */
    private Optional<CareSchedule> findWaitingFollowUp(Long horseId) {
        return careScheduleRepository
                .findByHorseIdAndStatusIn(horseId, List.of(CareScheduleStatus.SCHEDULED)).stream()
                .filter(cs -> cs.getCareType() == CareType.ROUTINE && cs.getPurpose() == CareSchedulePurpose.FOLLOW_UP
                        && cs.getSourceIncidentId() == null)
                .min(Comparator.comparing(CareScheduleService::plannedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static LocalDateTime plannedAt(CareSchedule schedule) {
        return schedule.getScheduledAt() != null ? schedule.getScheduledAt() : schedule.getRequestedAt();
    }

    /** Mỗi ngựa chỉ có một Follow-up đang chờ: yêu cầu mới ghép vào lịch đó, giữ ngày sớm hơn. */
    private CareSchedule mergeIntoWaitingFollowUp(CareSchedule waiting, LocalDateTime requestedAt, String description, Long userId) {
        String current = waiting.getDescription();
        if (description != null && !description.isBlank() && (current == null || !current.contains(description))) {
            waiting.setDescription(current == null || current.isBlank() ? description : current + ". " + description);
        }
        LocalDateTime planned = plannedAt(waiting);
        if (planned != null && !requestedAt.isBefore(planned)) {
            return careScheduleRepository.save(waiting);
        }
        waiting.setRequestedAt(requestedAt);
        waiting.setVeterinarianId(null);
        waiting.setScheduledAt(null);
        waiting.setStatus(CareScheduleStatus.SCHEDULED);
        CareSchedule saved = careScheduleRepository.save(waiting);
        assignRequestedSchedule(saved, userId);
        return saved;
    }

    /**
     * Ngựa quay lại từ INACTIVE vẫn giữ quyết định tập cũ. Mọi lịch khám đã bị hủy lúc rời trại, nên ngựa
     * đang bị chặn cần một Follow-up mới, nếu không sẽ không còn ai mở được lệnh chặn.
     */
    @Transactional
    public void scheduleFollowUpIfBlocked(Horse horse, Long actorId) {
        if (horse.getTrainingDecision() != TrainingDecision.BLOCKED) return;
        if (findOpenIncidentCase(horse.getId()).isPresent() || findWaitingFollowUp(horse.getId()).isPresent()) return;

        LocalDateTime now = LocalDateTime.now();
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setHorseId(horse.getId());
        next.setCareType(CareType.ROUTINE);
        next.setScheduledAt(now.toLocalDate().plusDays(1).atTime(FarmSchedulePolicy.VET_WINDOW_START));
        next.setDescription(RETURN_FOLLOW_UP_DESCRIPTION);
        createNextSchedule(next, actorId, null, "return:" + horse.getId() + ":" + now.toLocalDate(), false);
    }

    // ------------------------------------------------------------------
    // D1 — careScheduleLifecycleSweep (ROADMAP v2.2 §4.5; WF-01 §3.1.3-5, WF-03 §3.2)
    // ------------------------------------------------------------------

    private static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(17, 0);
    private static final DateTimeFormatter SWEEP_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Job 60s:
     * - Midnight EOD rollover (00:00 - 05:00): dời tất cả các ca INITIAL/ROUTINE chưa xong của ngày hôm trước.
     *   Nếu bác sĩ có xử lý URGENT trong ngày -> URGENT_PREEMPTION, bỏ gán để sáng hôm sau gán đều lại.
     *   Nếu không có URGENT mà trễ -> TIME_EXCEEDED, giữ nguyên bác sĩ làm tiếp.
     * - URGENT quá hạn: escalate notification, không bao giờ dời lịch.
     */
    @Transactional
    public void careScheduleLifecycleSweep() {
        runLifecycleSweep(LocalDateTime.now());
    }

    /** Tách `now` thành tham số để test dùng clock cố định. */
    void runLifecycleSweep(LocalDateTime now) {
        LocalTime tod = now.toLocalTime();
        if (!tod.isBefore(WORKDAY_START) && tod.isBefore(WORKDAY_END)) {
            return;
        }
        if (tod.isBefore(WORKDAY_END)) {
            return; // trước 17:00: không rollover/escalate
        }

        List<CareScheduleStatus> openStatuses = CareScheduleStatus.ASSIGNED_OR_BOOKED;
        for (CareSchedule cs : careScheduleRepository.findRollableEodCandidates(openStatuses, now)) {
            rollOverEod(cs, now);
        }
        for (CareSchedule cs : careScheduleRepository.findStaleUrgentForEscalation(openStatuses, now)) {
            escalateStaleUrgent(cs);
        }
    }

    private boolean vetHandledUrgentToday(Long vetId, LocalDate date) {
        if (vetId == null) return false;
        return careScheduleRepository.existsByVeterinarianIdAndCareTypeAndStatusInAndScheduledDate(
                vetId, CareType.URGENT,
                List.of(CareScheduleStatus.IN_PROGRESS, CareScheduleStatus.COMPLETED, CareScheduleStatus.SCHEDULED),
                date);
    }

    private void cascadeIfTomorrowFull(CareSchedule cs, LocalDate targetDate) {
        if (cs.getVeterinarianId() == null) return;
        boolean isPreventive = cs.getCareType() == CareType.ROUTINE && cs.getPurpose() == CareSchedulePurpose.PREVENTIVE;
        int maxQuota = cs.getCareType() == CareType.INITIAL
                ? ADMISSION_DAILY_QUOTA
                : (isPreventive ? PREVENTIVE_DAILY_QUOTA : ROUTINE_DAILY_QUOTA);
        List<CareSchedule> allVisits = careScheduleRepository
                .findByVeterinarianIdAndScheduledDateAndCareTypeAndStatusInOrderByCreatedAtAsc(
                        cs.getVeterinarianId(), targetDate, cs.getCareType(), List.of(CareScheduleStatus.SCHEDULED));
        List<CareSchedule> targetVisits = allVisits.stream()
                .filter(visit -> cs.getCareType() != CareType.ROUTINE || (isPreventive
                        ? visit.getPurpose() == CareSchedulePurpose.PREVENTIVE
                        : visit.getPurpose() != CareSchedulePurpose.PREVENTIVE))
                .collect(Collectors.toList());
        if (targetVisits.size() >= maxQuota) {
            CareSchedule toCascade = targetVisits.stream()
                    .min(Comparator.comparingInt(CareSchedule::getRolloverCount)
                            .thenComparing(Comparator.comparing(CareSchedule::getCreatedAt).reversed()))
                    .orElse(targetVisits.get(targetVisits.size() - 1));

            if (!Objects.equals(toCascade.getId(), cs.getId())) {
                LocalDate nextDate = targetDate.plusDays(1);
                toCascade.setScheduledDate(nextDate);
                if (toCascade.getScheduledAt() != null) {
                    toCascade.setScheduledAt(nextDate.atTime(toCascade.getScheduledAt().toLocalTime()));
                }
                toCascade.setRescheduleReason(RescheduleReason.TIME_EXCEEDED);
                toCascade.setRescheduleNote("Cascaded to " + nextDate + " to prioritize rolled-over queue (FIFO)");
                careScheduleRepository.save(toCascade);
                if (toCascade.getVeterinarianId() != null) {
                    notificationService.sendAssignmentNotification(
                            toCascade.getVeterinarianId(),
                            NotificationTypes.REFERENCE_CARE_SCHEDULE,
                            toCascade.getId(),
                            toCascade.getHorseId(),
                            NotificationTypes.CARE_SCHEDULE_EOD_ROLLED,
                            "Examination rescheduled",
                            "Examination #" + toCascade.getId() + " was moved to " + nextDate + " to make room for a rolled-over visit.",
                            nextDate.toString());
                }
                cascadeIfTomorrowFull(toCascade, nextDate);
            }
        }
    }

    private void rollOverEod(CareSchedule cs, LocalDateTime now) {
        LocalDateTime slot = nextEodSlot(cs, now);
        if (slot == null) return; // shortcut: 7 ngày kề đều kín — thử lại ở tick job sau
        CareScheduleStatus previous = cs.getStatus();
        cs.setStatus(CareScheduleStatus.SCHEDULED);
        cs.setScheduledDate(slot.toLocalDate());
        cs.setScheduledAt(null);
        cs.setRolloverCount(cs.getRolloverCount() + 1);

        LocalDate examOriginalDate = cs.getScheduledDate() != null ? cs.getScheduledDate()
                : (cs.getScheduledAt() != null ? cs.getScheduledAt().toLocalDate() : now.toLocalDate());
        boolean handledUrgent = vetHandledUrgentToday(cs.getVeterinarianId(), examOriginalDate);
        RescheduleReason reason = handledUrgent
                ? RescheduleReason.URGENT_PREEMPTION
                : RescheduleReason.TIME_EXCEEDED;
        cs.setRescheduleReason(reason);
        cs.setRescheduleNote("Unfinished by end of day — auto-moved to " + slot.format(SWEEP_TIME_FMT)
                + (CareScheduleStatus.ACTIVE_EXAM.contains(previous) ? " (was in progress)" : "")
                + " (reason: " + reason + ")");

        Long previousVetId = cs.getVeterinarianId();
        // Nếu bị URGENT ngắt trong ngày -> gỡ gán để ngày hôm sau hệ thống tự động gán đều lại cho bác sĩ khác
        if (handledUrgent) {
            cs.setVeterinarianId(null);
            cs.setStatus(CareScheduleStatus.SCHEDULED);
        } else {
            cascadeIfTomorrowFull(cs, slot.toLocalDate());
        }

        careScheduleRepository.save(cs);
        if (previousVetId == null) return;
        notificationService.sendAssignmentNotification(
                previousVetId,
                NotificationTypes.REFERENCE_CARE_SCHEDULE,
                cs.getId(),
                cs.getHorseId(),
                NotificationTypes.CARE_SCHEDULE_EOD_ROLLED,
                "Examination rolled over to the next working day",
                "The " + cs.getCareType() + " examination for " + horseNameOf(cs) + " (schedule #" + cs.getId()
                        + ") did not finish by end of day and was moved to " + slot.format(SWEEP_TIME_FMT)
                        + (handledUrgent ? " (unassigned due to urgent case preemption)." : "."),
                String.valueOf(cs.getRolloverCount()));
    }

    /** URGENT quá hạn: ngựa đang BLOCKED — chỉ escalate, không bao giờ dời lịch im lặng. */
    private void escalateStaleUrgent(CareSchedule cs) {
        if (cs.getVeterinarianId() == null) return;
        notificationService.sendAssignmentNotification(
                cs.getVeterinarianId(),
                NotificationTypes.REFERENCE_CARE_SCHEDULE,
                cs.getId(),
                cs.getHorseId(),
                NotificationTypes.CARE_SCHEDULE_URGENT_ESCALATED,
                "Urgent case still open past its scheduled time",
                "The urgent case for " + horseNameOf(cs) + " (schedule #" + cs.getId() + ", scheduled "
                        + cs.getScheduledAt().format(SWEEP_TIME_FMT) + ") is still open. The horse stays "
                        + "blocked until the examination is completed.");
    }

    /** Khe EOD hợp lệ đầu tiên từ T+1..T+7, né lịch kín của vet và của ngựa. */
    LocalDateTime nextEodSlot(CareSchedule cs, LocalDateTime now) {
        List<LocalTime> times = cs.getCareType() == CareType.ROUTINE
                ? List.of(FarmSchedulePolicy.VET_WINDOW_START, LocalTime.of(14, 0),
                        LocalTime.of(14, 30), LocalTime.of(15, 0))
                : eodInitialGrid();
        // Ca đang khám vẫn chiếm lịch của ngựa, nên rollover phải né nó.
        List<CareSchedule> horseScheduled = careScheduleRepository.findByHorseIdAndStatusIn(
                cs.getHorseId(), CareScheduleStatus.BOOKED_OR_ACTIVE);
        LocalDate day = now.toLocalDate().plusDays(1);
        for (int d = 0; d < 7; d++, day = day.plusDays(1)) {
            LocalDate currentDay = day;
            List<CareSchedule> assignedInDay = careScheduleRepository.findAssignedInDay(
                    currentDay.atStartOfDay(), currentDay.atStartOfDay().plusDays(1),
                    CareScheduleStatus.BOOKED_OR_ACTIVE);
            // Quota check: vet đã đầy quota cho careType/purpose này trong ngày → skip ngày.
            if (cs.getVeterinarianId() != null) {
                boolean isPreventive = cs.getCareType() == CareType.ROUTINE && cs.getPurpose() == CareSchedulePurpose.PREVENTIVE;
                int maxQuota = cs.getCareType() == CareType.INITIAL
                        ? ADMISSION_DAILY_QUOTA
                        : (isPreventive ? PREVENTIVE_DAILY_QUOTA : ROUTINE_DAILY_QUOTA);
                long vetDayCount = assignedInDay.stream()
                        .filter(o -> Objects.equals(o.getVeterinarianId(), cs.getVeterinarianId())
                                && o.getCareType() == cs.getCareType()
                                && (cs.getCareType() != CareType.ROUTINE
                                        || (isPreventive ? o.getPurpose() == CareSchedulePurpose.PREVENTIVE
                                                         : o.getPurpose() != CareSchedulePurpose.PREVENTIVE)))
                        .count();
                if (vetDayCount >= maxQuota) continue;
            }
            for (LocalTime time : times) {
                LocalDateTime slot = currentDay.atTime(time);
                boolean horseBusy = horseScheduled.stream().anyMatch(o -> {
    if (Objects.equals(o.getId(), cs.getId())) return false;
    if (cs.getCareType() != CareType.URGENT && o.getCareType() != CareType.URGENT) {
        LocalDate otherDate = o.getScheduledDate() != null ? o.getScheduledDate()
                : (o.getScheduledAt() != null ? o.getScheduledAt().toLocalDate() : null);
        if (otherDate != null && otherDate.equals(currentDay)) return true;
    }
    return overlaps(cs, slot, o);
});
                if (horseBusy) continue;
                if (cs.getVeterinarianId() == null) return slot;
                boolean vetBusy = assignedInDay.stream()
                        .anyMatch(o -> Objects.equals(o.getVeterinarianId(), cs.getVeterinarianId())
                                && overlaps(cs, slot, o));
                if (vetBusy) continue;
                return slot;
            }
        }
        return null;
    }

    /** Lưới 08:00–17:00 ngoài khung routine trước, khe trong khung làm fallback — gương findAssignmentSlot. */
    private static List<LocalTime> eodInitialGrid() {
        List<LocalTime> times = new ArrayList<>();
        for (int hour = 8; hour < 17; hour++) {
            for (int minute : List.of(0, 30)) {
                LocalTime time = LocalTime.of(hour, minute);
                if (!time.isBefore(FarmSchedulePolicy.VET_WINDOW_START)
                        && time.isBefore(FarmSchedulePolicy.VET_WINDOW_END)) {
                    continue;
                }
                times.add(time);
            }
        }
        times.add(FarmSchedulePolicy.VET_WINDOW_START);
        times.add(LocalTime.of(14, 0));
        times.add(LocalTime.of(14, 30));
        times.add(LocalTime.of(15, 0));
        return times;
    }

    private String horseNameOf(CareSchedule cs) {
        return horseRepository.findById(cs.getHorseId())
                .map(Horse::getName).orElse("#" + cs.getHorseId());
    }

    @Transactional
    public void assignRequestedSchedules() {
        LocalDateTime horizon = LocalDateTime.now().plusDays(ASSIGNMENT_HORIZON_DAYS);
        for (CareSchedule schedule : careScheduleRepository.lockNextRequestedBatch(ASSIGNMENT_BATCH_SIZE, horizon)) {
            assignRequestedSchedule(schedule);
        }
    }

    public void assignRequestedSchedule(CareSchedule schedule) {
        assignRequestedSchedule(schedule, schedule.getRequestedById(), false);
    }

    @Transactional
    public void assignPendingSchedulesForHorse(Long horseId) {
        if (horseId == null) return;
        List<CareSchedule> pending = careScheduleRepository.findByHorseIdAndStatusIn(
                horseId, List.of(CareScheduleStatus.SCHEDULED));
        for (CareSchedule schedule : pending) {
            if (schedule.getVeterinarianId() == null) {
                assignRequestedSchedule(schedule, schedule.getRequestedById(), true);
            }
        }
    }

    /**
     * @param creatorVetId Vet who requested/created this schedule. Deprioritized so the visit
     *                     is balanced across other active vets; only assigned to the creator
     *                     if other vets are unavailable or at daily quota.
     */
    void assignRequestedSchedule(CareSchedule schedule, Long creatorVetId) {
        assignRequestedSchedule(schedule, creatorVetId, false);
    }

    void assignRequestedSchedule(CareSchedule schedule, Long creatorVetId, boolean ignoreHorizon) {
        if (schedule.getStatus() != CareScheduleStatus.SCHEDULED) return;
        if (schedule.getVeterinarianId() != null && (schedule.getScheduledDate() != null || schedule.getScheduledAt() != null)) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(ASSIGNMENT_HORIZON_DAYS);
        if (!ignoreHorizon && isBeyondAssignmentHorizon(schedule, horizon)) return;

        // Keep the global lock order admission -> horse -> schedule (same as lockClinicalSchedule)
        // so concurrent Vet actions and auto-assignment cannot deadlock.
        if (schedule.getAdmissionId() != null) {
            admissionRepository.findByIdForUpdate(schedule.getAdmissionId());
        }
        Horse horse = horseRepository.findByIdForUpdate(schedule.getHorseId()).orElse(null);
        if (horse == null) return;

        List<User> activeVets = Optional.ofNullable(userRepository.findActiveVeterinarians()).orElseGet(List::of);
        if (activeVets.isEmpty() && (schedule.getCareType() != CareType.INITIAL || schedule.getVeterinarianId() == null)) {
            return;
        }

        Map<Long, Integer> overdueAppointmentsByVet = new HashMap<>();
        Map<Long, Integer> scheduledMinutesByVet = new HashMap<>();
        for (CareSchedule assigned : careScheduleRepository
                .findByStatusInAndVeterinarianIdIsNotNull(CareScheduleStatus.OPEN)) {
            if (assigned.getVeterinarianId() == null) continue;
            LocalDateTime when = assigned.getScheduledAt() != null ? assigned.getScheduledAt() : assigned.getRequestedAt();
            if (when != null && !when.isBefore(horizon)) continue;
            scheduledMinutesByVet.merge(assigned.getVeterinarianId(),
                    assigned.getDurationMinutes() > 0 ? assigned.getDurationMinutes() : DEFAULT_DURATION_MINUTES,
                    Integer::sum);
            if (assigned.getRolloverCount() > 0) {
                overdueAppointmentsByVet.merge(assigned.getVeterinarianId(), assigned.getRolloverCount(), Integer::sum);
            }
        }

        // Admission INITIAL exams must get a named Vet as soon as the horse arrives,
        // even when that Vet is currently busy or has no schedulable slot yet.
        // Keep the schedule REQUESTED until a non-conflicting appointment time is found.
        if (schedule.getCareType() == CareType.INITIAL && schedule.getVeterinarianId() == null) {
            User assignedVet = assignInitialExamToLeastLoadedVet(activeVets, overdueAppointmentsByVet, scheduledMinutesByVet);
            if (assignedVet == null) return;
            schedule.setVeterinarianId(assignedVet.getId());
            careScheduleRepository.save(schedule);
            assignInitialAdmissionVet(schedule, assignedVet.getId());
        }

        List<User> candidateVets = schedule.getCareType() == CareType.INITIAL && schedule.getVeterinarianId() != null
                ? userRepository.findById(schedule.getVeterinarianId())
                        .or(() -> activeVets.stream().filter(v -> Objects.equals(v.getId(), schedule.getVeterinarianId())).findFirst())
                        .stream().toList()
                : activeVets;
        if (candidateVets.isEmpty()) return;

        Long effectiveCreatorId = creatorVetId != null ? creatorVetId : schedule.getRequestedById();
        VetScheduleCache vetCache = new VetScheduleCache(candidateVets, effectiveCreatorId);
        SlotAssignment assignment = findAssignmentSlot(schedule, vetCache, overdueAppointmentsByVet, scheduledMinutesByVet);
        boolean usedPreemption = (assignment == null || assignment.rankedVets().isEmpty()) && schedule.getCareType() == CareType.URGENT;
        if (usedPreemption) {
            assignment = attemptPreemptionForUrgent(schedule, candidateVets, overdueAppointmentsByVet, scheduledMinutesByVet);
        }
        if (assignment == null || assignment.rankedVets().isEmpty()) return;

        LocalDateTime slot = assignment.slot();
        List<User> ranked = assignment.rankedVets();
        for (User candidate : ranked) {
            User lockedVet = userRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (!isStillEligible(lockedVet)) continue;
            try {
                ensureAvailable(schedule, lockedVet.getId(), slot);
            } catch (ApiException conflict) {
                continue;
            }

            schedule.setVeterinarianId(lockedVet.getId());
            schedule.setScheduledDate(slot.toLocalDate());
            schedule.setScheduledAt(schedule.getCareType() == CareType.URGENT ? slot : null);
            schedule.setStatus(CareScheduleStatus.SCHEDULED);

            careScheduleRepository.save(schedule);
            assignInitialAdmissionVet(schedule, lockedVet.getId());

            boolean urgent = schedule.getCareType() == CareType.URGENT;
            if (urgent) {
                offloadRoutineForUrgent(lockedVet.getId(), slot.toLocalDate(), usedPreemption, excludeFromOffload);
            }
            String notificationType;
            String notificationTitle;
            String notificationMessage;
            if (urgent) {
                notificationType = NotificationTypes.CARE_SCHEDULE_URGENT_ASSIGNED;
                notificationTitle = "Urgent case assigned";
                notificationMessage = "You have been assigned to an urgent care case for horse " + horse.getName()
                        + " on care schedule #" + schedule.getId() + ".";
            } else if (schedule.getCareType() == CareType.ROUTINE) {
                notificationType = NotificationTypes.ROUTINE_ASSIGNED;
                notificationTitle = "Care schedule assigned";
                notificationMessage = "You have been assigned to a routine care visit for horse " + horse.getName()
                        + " on care schedule #" + schedule.getId() + ".";
            } else {
                notificationType = NotificationTypes.ADMISSION_VET_ASSIGNED;
                notificationTitle = "New horse assignment";
                notificationMessage = "You have been assigned to candidate horse " + horse.getName()
                        + " for care schedule #" + schedule.getId() + ".";
            }
            notificationService.sendAssignmentNotification(
                    lockedVet.getId(),
                    NotificationTypes.REFERENCE_CARE_SCHEDULE,
                    schedule.getId(),
                    horse.getId(),
                    notificationType,
                    notificationTitle,
                    notificationMessage
            );

            if (urgent || schedule.getSourceIncidentId() != null) {
                LocalDateTime assignedAt = LocalDateTime.now();
                HorseIncidentReport incident = markCaseReportsInReview(schedule, lockedVet.getId(), assignedAt);
                if (urgent) {
                    eventPublisher.publishEvent(new UrgentAssignmentCommittedEvent(
                            buildUrgentAlert(schedule, incident, horse, lockedVet.getId(), assignedAt)));
                }
            }

            recordAssignmentAudit(schedule, lockedVet);
            return;
        }
    }

    private SlotAssignment attemptPreemptionForUrgent(CareSchedule urgentSchedule,
            List<User> candidateVets,
            Map<Long, Integer> overdueAppointmentsByVet,
            Map<Long, Integer> scheduledMinutesByVet) {
        LocalDate today = LocalDate.now();

        // Tầng 2: Nếu không ai rảnh, tìm các bác sĩ đang có ca khám dở (IN_PROGRESS)
        // Chỉ chọn bác sĩ đang làm Routine (để đẩy Routine về SCHEDULED)
        // Tuyệt đối không chọn bác sĩ đang làm ca URGENT khác
        List<User> preemptible = candidateVets.stream()
                .filter(v -> {
                    List<CareSchedule> inProgress = careScheduleRepository.findByVeterinarianIdAndStatus(v.getId(), CareScheduleStatus.IN_PROGRESS);
                    boolean hasUrgentInProgress = inProgress.stream().anyMatch(cs -> cs.getCareType() == CareType.URGENT);
                    boolean hasRoutineInProgress = inProgress.stream().anyMatch(cs -> cs.getCareType() != CareType.URGENT);
                    return !hasUrgentInProgress && hasRoutineInProgress;
                })
                .toList();
        if (preemptible.isEmpty()) return null;

        // Xếp hạng: ưu tiên bác sĩ có ít ca hơn trong ngày hôm nay, sau đó theo ID
        User chosen = preemptible.stream()
                .min(Comparator
                        .comparingInt((User v) -> countAssignedSchedulesToday(v.getId(), today))
                        .thenComparing(User::getId))
                .orElse(null);
        if (chosen == null) return null;

        List<CareSchedule> inProgress = careScheduleRepository.findByVeterinarianIdAndStatus(chosen.getId(), CareScheduleStatus.IN_PROGRESS);
        for (CareSchedule exam : inProgress) {
            if (exam.getCareType() == CareType.URGENT) continue;
            exam.setStatus(CareScheduleStatus.SCHEDULED);
            exam.setVeterinarianId(null);
            exam.setScheduledAt(null);
            exam.setScheduledDate(null);
            exam.setRescheduleReason(RescheduleReason.URGENT_PREEMPTION);
            exam.setRescheduleNote("Deferred to make room for urgent case #" + urgentSchedule.getId());
            careScheduleRepository.save(exam);
            break;
        }

        notificationService.sendAssignmentNotification(
                chosen.getId(),
                NotificationTypes.REFERENCE_CARE_SCHEDULE,
                urgentSchedule.getId(),
                urgentSchedule.getHorseId(),
                NotificationTypes.CARE_SCHEDULE_PAUSED,
                "Examination deferred for urgent case",
                "An in-progress examination was deferred to handle urgent case #" + urgentSchedule.getId() + ".");

        return new SlotAssignment(LocalDateTime.now(), List.of(chosen));
    }

    private int countAssignedSchedulesToday(Long vetId, LocalDate date) {
        if (vetId == null) return 0;
        return careScheduleRepository.findByVeterinarianIdAndScheduledDateAndCareTypeAndStatusInOrderByCreatedAtAsc(
                vetId, date, CareType.ROUTINE, CareScheduleStatus.BOOKED_OR_ACTIVE).size()
                + careScheduleRepository.findScheduledForVet(vetId, CareScheduleStatus.SCHEDULED).size();
    }

    static boolean isBeyondAssignmentHorizon(CareSchedule schedule, LocalDateTime horizon) {
        if (schedule.getCareType() == CareType.URGENT || schedule.getCareType() == CareType.INITIAL) return false;
        return schedule.getRequestedAt() != null && !schedule.getRequestedAt().isBefore(horizon);
    }

    private void assignInitialAdmissionVet(CareSchedule schedule, Long veterinarianId) {
        if (schedule.getCareType() != CareType.INITIAL || schedule.getAdmissionId() == null) return;

        admissionRepository.findByIdForUpdate(schedule.getAdmissionId()).ifPresent(admission -> {
            if (!Objects.equals(admission.getVeterinarianId(), veterinarianId)) {
                admission.setVeterinarianId(veterinarianId);
                admissionRepository.save(admission);
            }
        });
    }

    private User assignInitialExamToLeastLoadedVet(List<User> activeVets,
            Map<Long, Integer> overdueAppointmentsByVet,
            Map<Long, Integer> scheduledMinutesByVet) {
        // Lọc vet đã đạt quota INITIAL (2/ngày) trước khi xếp hạng.
        List<User> ranked = activeVets.stream()
                .filter(v -> careScheduleRepository
                        .findByVeterinarianIdAndCareTypeAndStatusIn(v.getId(), CareType.INITIAL, CareScheduleStatus.BOOKED_OR_ACTIVE).size()
                        < ADMISSION_DAILY_QUOTA)
                .sorted(Comparator
                        .comparingInt((User vet) -> overdueAppointmentsByVet.getOrDefault(vet.getId(), 0))
                        .thenComparingInt(vet -> scheduledMinutesByVet.getOrDefault(vet.getId(), 0))
                        .thenComparing(User::getId))
                .toList();

        for (User candidate : ranked) {
            User lockedVet = userRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (isStillEligible(lockedVet)) return lockedVet;
        }
        return null;
    }

    private record SlotAssignment(LocalDateTime slot, List<User> rankedVets) {}

    private static final class VetScheduleCache {
        final List<User> candidateVets;
        final Long creatorVetId;
        final Map<Long, Boolean> hasInProgressByVet = new HashMap<>();
        final Map<Long, List<CareSchedule>> inProgressByVet = new HashMap<>();
        final Map<Long, List<CareSchedule>> scheduledByVet = new HashMap<>();
        final Map<Long, Boolean> historyByVet = new HashMap<>();

        VetScheduleCache(List<User> candidateVets, Long creatorVetId) {
            this.candidateVets = candidateVets;
            this.creatorVetId = creatorVetId;
        }
    }

    private SlotAssignment findAssignmentSlot(CareSchedule schedule,
            VetScheduleCache vetCache,
            Map<Long, Integer> overdueAppointmentsByVet, Map<Long, Integer> scheduledMinutesByVet) {
        if (schedule.getCareType() == CareType.URGENT) {
            LocalDateTime urgentSlot = LocalDateTime.now();
            List<User> ranked = rankVeterinarians(schedule, urgentSlot, vetCache, overdueAppointmentsByVet, scheduledMinutesByVet, null);
            return ranked.isEmpty() ? null : new SlotAssignment(urgentSlot, ranked);
        }

        // 1. Nếu ngựa đang có ca khám dở dang, không thể xếp thêm lịch mới
        if (careScheduleRepository.existsByHorseIdAndStatusIn(
                schedule.getHorseId(), CareScheduleStatus.ACTIVE_EXAM)) {
            return null;
        }

        // 2. Tải trước danh sách ca khám đã lên lịch của ngựa 1 lần duy nhất cho toàn bộ 14 ngày
        List<CareSchedule> scheduledForHorse = careScheduleRepository.findByHorseIdAndStatusIn(
                schedule.getHorseId(), CareScheduleStatus.BOOKED_OR_ACTIVE);

        LocalDateTime requested = schedule.getRequestedAt();
        LocalDate startDate = (requested != null && requested.isAfter(LocalDateTime.now())
                ? requested : LocalDateTime.now()).toLocalDate();
        for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
            LocalDate day = startDate.plusDays(dayOffset);
            List<CareSchedule> assignedInDay = null;

            List<LocalTime> times;
            if (schedule.getCareType() == CareType.ROUTINE) {
                // Routine care must strictly remain inside the veterinary window (13:30 - 15:30)
                times = List.of(
                        FarmSchedulePolicy.VET_WINDOW_START,
                        LocalTime.of(14, 0),
                        LocalTime.of(14, 30),
                        LocalTime.of(15, 0));
            } else {
                // Admission exams prefer the 08:00-17:00 grid OUTSIDE the routine window so
                // they never steal a routine slot. Only when every outside-window hour of the
                // day is taken can one fall back into the window's free slots (it is a routine
                // preference, not an exclusive lock; urgent cases may also land inside it).
                times = new ArrayList<>();
                for (int hour = 8; hour < 17; hour++) {
                    for (int minute : List.of(0, 30)) {
                        LocalTime time = LocalTime.of(hour, minute);
                        if (!time.isBefore(FarmSchedulePolicy.VET_WINDOW_START)
                                && time.isBefore(FarmSchedulePolicy.VET_WINDOW_END)) {
                            continue;
                        }
                        times.add(time);
                    }
                }
                times.add(FarmSchedulePolicy.VET_WINDOW_START);
                times.add(LocalTime.of(14, 0));
                times.add(LocalTime.of(14, 30));
                times.add(LocalTime.of(15, 0));
            }
            for (LocalTime time : times) {
                LocalDateTime slot = day.atTime(time);
                if (requested != null && slot.isBefore(requested)) continue;
                if (slot.isBefore(LocalDateTime.now())) continue;
                if (horseHasConflict(schedule, day, slot, scheduledForHorse)) continue;

                if (assignedInDay == null) {
                    LocalDateTime dayStart = day.atStartOfDay();
                    LocalDateTime dayEnd = dayStart.plusDays(1);
                    assignedInDay = careScheduleRepository.findAssignedInDay(dayStart, dayEnd,
                            CareScheduleStatus.BOOKED_OR_ACTIVE);
                }
                List<User> ranked = rankVeterinarians(schedule, slot, vetCache, overdueAppointmentsByVet, scheduledMinutesByVet, assignedInDay);
                if (!ranked.isEmpty()) {
                    return new SlotAssignment(slot, ranked);
                }
            }
        }
        return null;
    }

    private boolean horseHasConflict(CareSchedule schedule, LocalDate day, LocalDateTime slot, List<CareSchedule> scheduledForHorse) {
        return scheduledForHorse.stream().anyMatch(other -> {
            if (Objects.equals(other.getId(), schedule.getId())) return false;
            if (schedule.getCareType() != CareType.URGENT && other.getCareType() != CareType.URGENT) {
                LocalDate otherDate = other.getScheduledDate() != null ? other.getScheduledDate()
                        : (other.getScheduledAt() != null ? other.getScheduledAt().toLocalDate() : null);
                if (day.equals(otherDate)) return true;
            }
            return overlaps(schedule, slot, other);
        });
    }

    private List<User> rankVeterinarians(CareSchedule schedule, LocalDateTime slot,
            VetScheduleCache vetCache,
            Map<Long, Integer> overdueAppointmentsByVet, Map<Long, Integer> scheduledMinutesByVet,
            List<CareSchedule> assignedInDay) {
        List<User> vets = vetCache.candidateVets;
        if (vets == null || vets.isEmpty()) {
            return Collections.emptyList();
        }

        List<CareSchedule> assigned = assignedInDay != null ? assignedInDay : careScheduleRepository.findAssignedInDay(
                slot.toLocalDate().atStartOfDay(), slot.toLocalDate().atStartOfDay().plusDays(1),
                CareScheduleStatus.BOOKED_OR_ACTIVE);

        Map<Long, Set<Long>> horsesByVet = new HashMap<>();
        Map<Long, Integer> minutesByVet = new HashMap<>();
        for (CareSchedule item : assigned) {
            horsesByVet.computeIfAbsent(item.getVeterinarianId(), ignored -> new HashSet<>()).add(item.getHorseId());
            minutesByVet.merge(item.getVeterinarianId(),
                    item.getDurationMinutes() > 0 ? item.getDurationMinutes() : DEFAULT_DURATION_MINUTES,
                    Integer::sum);
        }

        List<User> eligibleVets = vets.stream()
                .filter(v -> {
                    if (schedule.getCareType() == CareType.INITIAL && schedule.getVeterinarianId() != null) {
                        if (!Objects.equals(v.getId(), schedule.getVeterinarianId())) return false;
                    }
                    // V2 Daily bucket quota: Routine max 3/vet/day, Preventive max 3/vet/day, Initial/Admission max 2/vet/day
                    if (schedule.getCareType() == CareType.ROUTINE) {
                        boolean isPreventive = schedule.getPurpose() == CareSchedulePurpose.PREVENTIVE;
                        long count = assigned.stream()
                                .filter(cs -> Objects.equals(cs.getVeterinarianId(), v.getId())
                                        && cs.getCareType() == CareType.ROUTINE
                                        && (isPreventive
                                                ? cs.getPurpose() == CareSchedulePurpose.PREVENTIVE
                                                : cs.getPurpose() != CareSchedulePurpose.PREVENTIVE)
                                        && !Objects.equals(cs.getId(), schedule.getId()))
                                .count();
                        int quota = isPreventive ? PREVENTIVE_DAILY_QUOTA : ROUTINE_DAILY_QUOTA;
                        if (count >= quota) return false;
                    } else if (schedule.getCareType() == CareType.INITIAL) {
                        long count = assigned.stream()
                                .filter(cs -> Objects.equals(cs.getVeterinarianId(), v.getId())
                                        && cs.getCareType() == CareType.INITIAL
                                        && !Objects.equals(cs.getId(), schedule.getId()))
                                .count();
                        if (count >= ADMISSION_DAILY_QUOTA) return false;
                    }
                    boolean activeExamBlocks = schedule.getCareType() != CareType.URGENT
                            && slot.toLocalDate().isEqual(LocalDate.now())
                            && vetCache.hasInProgressByVet.computeIfAbsent(v.getId(),
                                    id -> careScheduleRepository.existsByVeterinarianIdAndStatus(id, CareScheduleStatus.IN_PROGRESS));
                    return !activeExamBlocks;
                })
                .filter(v -> vetCache.inProgressByVet.computeIfAbsent(v.getId(),
                                id -> careScheduleRepository.findByVeterinarianIdAndStatus(id, CareScheduleStatus.IN_PROGRESS))
                        .stream()
                        .noneMatch(other -> inProgressConflictsWithSlot(schedule, slot, other)))
                .filter(v -> vetCache.scheduledByVet.computeIfAbsent(v.getId(),
                                id -> careScheduleRepository.findScheduledForVet(id, CareScheduleStatus.SCHEDULED))
                        .stream()
                        .noneMatch(other -> blocksSlot(schedule, slot, other)))
                .toList();

        if (eligibleVets.size() <= 1) {
            return eligibleVets;
        }

        if (schedule.getCareType() != CareType.URGENT) {
            for (User v : eligibleVets) {
                vetCache.historyByVet.computeIfAbsent(v.getId(),
                        id -> careScheduleRepository.existsByVeterinarianIdAndHorseIdAndStatusIn(
                                id, schedule.getHorseId(),
                                List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS, CareScheduleStatus.COMPLETED)));
            }
        }

        Map<Long, Integer> dailyCountByVet = new HashMap<>();
        for (User v : eligibleVets) {
            dailyCountByVet.put(v.getId(), countAssignedSchedulesToday(v.getId(), slot.toLocalDate()));
        }

        return eligibleVets.stream()
                .sorted(Comparator
                        // 1. Cân bằng tải: Ưu tiên bác sĩ có ít ca hơn trong ngày hôm nay
                        .comparingInt((User v) -> dailyCountByVet.getOrDefault(v.getId(), 0))
                        // 2. Ưu tiên bác sĩ đã yêu cầu/tạo lịch khám này (Continuity of care)
                        .thenComparing((User v) -> vetCache.creatorVetId != null && !Objects.equals(v.getId(), vetCache.creatorVetId))
                        // 3. Deterministic tie-breaker
                        .thenComparing(User::getId))
                .toList();
    }

    private boolean inProgressConflictsWithSlot(CareSchedule schedule, LocalDateTime slot, CareSchedule inProgress) {
        // An urgent case is an emergency: it must reach a vet whose in-progress visit
        // is stale (past its scheduled end) — otherwise day-old exams stuck in
        // IN_PROGRESS would block every urgent assignment forever. A vet still
        // examining within the booked duration is genuinely busy and is not interrupted.
        if (schedule.getCareType() == CareType.URGENT) {
            LocalDateTime startedAt = inProgress.getScheduledAt();
            if (startedAt == null) return true;
            int duration = inProgress.getDurationMinutes() > 0
                    ? inProgress.getDurationMinutes() : DEFAULT_DURATION_MINUTES;
            return startedAt.plusMinutes(duration).isAfter(LocalDateTime.now());
        }
        LocalDateTime scheduledAt = inProgress.getScheduledAt();
        if (scheduledAt == null) return slot.toLocalDate().isEqual(LocalDate.now());
        int duration = inProgress.getDurationMinutes() > 0
                ? inProgress.getDurationMinutes() : DEFAULT_DURATION_MINUTES;
        // An overdue in-progress visit remains active; retain the Vet assignment but
        // do not book a conflicting appointment until that visit is completed.
        if (!scheduledAt.plusMinutes(duration).isAfter(LocalDateTime.now())) {
            return slot.toLocalDate().isEqual(LocalDate.now());
        }
        return overlaps(schedule, slot, inProgress);
    }

    private boolean isStillEligible(User vet) {
        return vet != null && vet.isActive() && vet.getRole() != null
                && "VETERINARIAN".equals(vet.getRole().getName())
                && veterinarianProfileRepository.findById(vet.getId())
                        .map(profile -> profile.getLicenseNumber() != null && !profile.getLicenseNumber().isBlank())
                        .orElse(false);
    }

    private void recordAssignmentAudit(CareSchedule schedule, User veterinarian) {
        AuditLog audit = new AuditLog();
        audit.setAction("Care Schedule Auto Assigned");
        audit.setEntityName("CareSchedule");
        audit.setEntityId(schedule.getId());
        audit.setUser(veterinarian);
        audit.setDetails("Care schedule #" + schedule.getId() + " (" + schedule.getCareType() + ") was automatically assigned to veterinarian " + veterinarian.getFullName());
        auditLogRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<UrgentAssignmentAlert> getPendingUrgentAlerts(Long veterinarianId) {
        return getPendingUrgentAlerts(veterinarianId, false);
    }

    @Transactional(readOnly = true)
    public List<UrgentAssignmentAlert> getPendingUrgentAlerts(Long veterinarianId, boolean includeCompleted) {
        List<CareScheduleStatus> statuses = includeCompleted
                ? List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS, CareScheduleStatus.COMPLETED)
                : CareScheduleStatus.BOOKED_OR_ACTIVE;

        return careScheduleRepository.findByVeterinarianIdAndCareTypeAndStatusIn(
                        veterinarianId, CareType.URGENT,
                        statuses).stream()
                .map(schedule -> {
                    HorseIncidentReport incident = schedule.getSourceIncidentId() != null
                            ? incidentReportRepository.findById(schedule.getSourceIncidentId()).orElse(null)
                            : null;
                    Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
                    return horse == null ? null
                            : buildUrgentAlert(schedule, incident, horse, veterinarianId, schedule.getUpdatedAt());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        (UrgentAssignmentAlert a) -> a.reportedAt() != null ? a.reportedAt() : (a.scheduledAt() != null ? a.scheduledAt() : LocalDateTime.MIN)
                ).reversed().thenComparing(Comparator.comparing(UrgentAssignmentAlert::scheduleId).reversed()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UrgentAssignmentAlert getUrgentCase(Long scheduleId, Long veterinarianId) {
        CareSchedule schedule = requireAssignedUrgentCase(scheduleId, veterinarianId);
        HorseIncidentReport incident = schedule.getSourceIncidentId() != null
                ? incidentReportRepository.findById(schedule.getSourceIncidentId()).orElse(null)
                : null;
        Horse horse = horseRepository.findById(schedule.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        return buildUrgentAlert(schedule, incident, horse, veterinarianId, schedule.getUpdatedAt());
    }

    /**
     * Mọi báo cáo sự cố đã gộp vào ca sự cố, kể cả báo cáo nguồn.
     * Báo cáo gộp trước đây không hiển thị ở đâu nên Thú y chỉ thấy báo cáo đầu tiên.
     */
    @Transactional(readOnly = true)
    public List<UrgentCaseReportResponse> getIncidentCaseReports(Long scheduleId, Long veterinarianId) {
        CareSchedule schedule = requireAssignedIncidentCase(scheduleId, veterinarianId);
        List<HorseIncidentReport> reports = new ArrayList<>(
                incidentReportRepository.findByCareScheduleIdOrderByReportedAtAsc(scheduleId));
        // Báo cáo cũ có thể chưa có care_schedule_id; ca luôn lưu báo cáo nguồn.
        if (schedule.getSourceIncidentId() != null
                && reports.stream().noneMatch(report -> Objects.equals(report.getId(), schedule.getSourceIncidentId()))) {
            incidentReportRepository.findById(schedule.getSourceIncidentId()).ifPresent(reports::add);
        }
        reports.sort(Comparator.comparing(
                (HorseIncidentReport report) -> report.getReportedAt() == null ? LocalDateTime.MIN : report.getReportedAt()));
        return reports.stream().map(this::toUrgentCaseReport).toList();
    }

    private CareSchedule requireAssignedUrgentCase(Long scheduleId, Long veterinarianId) {
        CareSchedule schedule = requireAssignedIncidentCase(scheduleId, veterinarianId);
        if (schedule.getCareType() != CareType.URGENT) {
            throw new ApiException(HttpStatus.NOT_FOUND, "URGENT_CASE_NOT_FOUND", "Urgent case not found");
        }
        return schedule;
    }

    private CareSchedule requireAssignedIncidentCase(Long scheduleId, Long veterinarianId) {
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        if (!isIncidentCase(schedule)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "URGENT_CASE_NOT_FOUND", "Incident case not found");
        }
        if (!Objects.equals(schedule.getVeterinarianId(), veterinarianId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can view this case");
        }
        return schedule;
    }

    private UrgentCaseReportResponse toUrgentCaseReport(HorseIncidentReport report) {
        User reporter = report.getGroomId() != null
                ? userRepository.findById(report.getGroomId()).orElse(null) : null;
        String label = roleLabel(reporter);
        return new UrgentCaseReportResponse(
                report.getId(),
                report.getGroomId(),
                reporter != null ? reporter.getFullName() : null,
                label.isEmpty() ? null : label,
                report.getReportedAt(),
                report.getIncidentType(),
                report.getTitle(),
                report.getDescription(),
                report.getImageUrl() != null
                        ? "/api/incidents/" + report.getId() + "/image" : null
        );
    }

    private static String roleLabel(User reporter) {
        String roleName = (reporter != null && reporter.getRole() != null) ? reporter.getRole().getName() : null;
        return "GROOM".equalsIgnoreCase(roleName) ? "Groom"
                : ("HEAD_TRAINER".equalsIgnoreCase(roleName) || "TRAINER".equalsIgnoreCase(roleName)) ? "Trainer"
                : (roleName != null ? roleName : "");
    }

    /** What the reporting Groom is told when the urgent case closes: diagnosis, care to give, the vet's assessment note and whether the horse may train. */
    private static String incidentOutcomeNote(CompleteCareScheduleRequest request,
            TrainingDecision effectiveDecision, String effectiveReason) {
        StringBuilder note = new StringBuilder("Diagnosis: ").append(request.getDiagnosis().trim());
        if (request.getTreatment() != null && !request.getTreatment().isBlank()) {
            note.append("\nTreatment: ").append(request.getTreatment().trim());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            note.append("\nAssessment: ").append(request.getNotes().trim());
        }
        if (effectiveDecision == TrainingDecision.BLOCKED) {
            note.append("\nTraining: blocked");
            if (effectiveReason != null && !effectiveReason.isBlank()) {
                note.append(" - ").append(effectiveReason.trim());
            }
        } else {
            note.append("\nTraining: allowed");
        }
        // handler_note is a TEXT column, but keep the note a readable size.
        return note.length() > 2000 ? note.substring(0, 2000) : note.toString();
    }

    /** Kết luận của lần khám lâm sàng gần nhất trước sự cố này — trạng thái trước khi ngựa bị chặn khẩn cấp. */
    private Optional<HealthRecord> previousTrainingRecord(Long horseId, LocalDateTime cutoff) {
        if (cutoff != null) {
            Optional<HealthRecord> beforeCutoff = healthRecordRepository
                    .findTopByHorseIdAndTrainingDecisionIsNotNullAndExaminedAtBeforeOrderByExaminedAtDesc(horseId, cutoff);
            if (beforeCutoff.isPresent()) return beforeCutoff;
        }
        Optional<HealthRecord> notUrgent = healthRecordRepository
                .findTopByHorseIdAndRecordTypeNotAndTrainingDecisionIsNotNullOrderByExaminedAtDesc(horseId, CareType.URGENT.name());
        if (notUrgent.isPresent()) return notUrgent;
        return healthRecordRepository.findTopByHorseIdAndTrainingDecisionIsNotNullOrderByExaminedAtDesc(horseId);
    }

    private UrgentAssignmentAlert buildUrgentAlert(CareSchedule schedule, HorseIncidentReport incident,
            Horse horse, Long veterinarianId, LocalDateTime assignedAt) {
        User reporter = incident != null && incident.getGroomId() != null
                ? userRepository.findById(incident.getGroomId()).orElse(null) : null;
        String label = roleLabel(reporter);
        String formattedName = reporter != null
                ? (!label.isEmpty() ? (label + " " + reporter.getFullName()) : reporter.getFullName())
                : null;
        StableStall stall = horse.getCurrentStallId() == null ? null
                : stallRepository.findById(horse.getCurrentStallId()).orElse(null);
        long eventId = schedule.getId();
        LocalDateTime cutoff = schedule.getCreatedAt() != null ? schedule.getCreatedAt() : LocalDateTime.now();
        HealthRecord previousTraining = previousTrainingRecord(horse.getId(), cutoff).orElse(null);
        return new UrgentAssignmentAlert(
                eventId,
                schedule.getId(),
                incident != null ? incident.getId() : null,
                veterinarianId,
                horse.getId(),
                horse.getName(),
                horse.getStableLocation(),
                stall != null ? stall.getStallCode() : null,
                incident != null ? incident.getGroomId() : null,
                formattedName,
                label.isEmpty() ? null : label,
                incident != null ? incident.getReportedAt() : schedule.getCreatedAt(),
                incident != null ? incident.getIncidentType() : null,
                incident != null ? incident.getTitle() : "Urgent Care",
                incident != null ? incident.getDescription() : schedule.getDescription(),
                // The image endpoint, never the storage key: the client loads it with its access token.
                incident != null && incident.getImageUrl() != null
                        ? "/api/incidents/" + incident.getId() + "/image" : null,
                horse.getTrainingDecision(),
                schedule.getStatus(),
                schedule.getScheduledAt(),
                assignedAt,
                // What the horse was doing before the urgent block: the vet must see it before deciding.
                previousTraining != null ? previousTraining.getTrainingDecision() : null,
                previousTraining != null ? previousTraining.getRestrictionDetails() : null
        );
    }
}
