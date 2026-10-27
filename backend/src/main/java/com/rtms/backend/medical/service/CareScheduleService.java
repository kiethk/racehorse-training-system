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
import com.rtms.backend.medical.dto.CancelCareScheduleRequest;
import com.rtms.backend.medical.dto.CareScheduleDetailResponse;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.dto.HorseHealthMetricRequest;
import com.rtms.backend.medical.dto.UrgentCaseReportResponse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
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
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.rtms.backend.event.ClinicalRecordCreatedEvent;
import com.rtms.backend.event.InitialExamCompletedEvent;
import com.rtms.backend.event.UrgentAssignmentCommittedEvent;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

@Service
public class CareScheduleService {

    private static final int DEFAULT_DURATION_MINUTES = 30;
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
    static final String ROUTINE_CADENCE_DESCRIPTION = "Routine follow-up recheck examination";

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
            PreventiveVaccinationService preventiveVaccinationService) {
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
                List.of(CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));
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
        schedule.setStatus(CareScheduleStatus.REQUESTED);
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
        Horse horse = horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horseId);
        schedule.setCareType(careType);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setRequestedAt(requestedAt);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setDescription(description != null ? description.trim() : null);
        schedule.setSourceIncidentId(sourceIncidentId);

        if (careType == CareType.URGENT) {
            // Chặn phòng ngừa ngay khi có ca khẩn cấp, kéo theo hủy buổi tập
            // tương lai: không để ngựa đang có vấn đề vẫn được dắt ra sân sáng mai.
            String reason = (description != null && !description.isBlank())
                    ? "Urgent veterinary care pending: " + description.trim()
                    : "Urgent veterinary care pending";
            trainingDecisionService.block(horse, reason);
        }

        CareSchedule saved = careScheduleRepository.save(schedule);
        assignRequestedSchedule(saved);
        return CareScheduleResponse.from(saved);
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
        boolean vetHasConflict = careScheduleRepository.findByVeterinarianIdAndStatus(
                        vetId, CareScheduleStatus.IN_PROGRESS).stream()
                .anyMatch(other -> inProgressConflictsWithSlot(schedule, start, other));
        if (vetHasConflict
                || careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Veterinarian or horse has an examination in progress");
        }
        List<CareSchedule> booked = new ArrayList<>(careScheduleRepository.findScheduledForVet(vetId, CareScheduleStatus.SCHEDULED));
        if (schedule.getCareType() != CareType.URGENT) {
            booked.addAll(careScheduleRepository.findScheduledForHorse(schedule.getHorseId(), CareScheduleStatus.SCHEDULED));
        }
        for (CareSchedule other : booked) {
            if (Objects.equals(other.getId(), schedule.getId()) || !overlaps(schedule, start, other)) continue;
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Veterinarian or horse is already scheduled in this slot");
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
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only SCHEDULED care schedules can be started");
        }

        if (!Objects.equals(schedule.getVeterinarianId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can start this schedule");
        }

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
            // Chặn tập luôn phải có điểm kết thúc: lần khám lại chính là mốc "tạm nghỉ đến".
            // Không có lịch thì ngựa bị khóa vô thời hạn cho tới khi ai đó nhớ ra mà tạo.
            if (request.getNextSchedule() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FOLLOW_UP_REQUIRED",
                        "A follow-up examination (nextSchedule) is required when training is BLOCKED");
            }
        }

        CareSchedule schedule = lockClinicalSchedule(scheduleId);

        if (schedule.getStatus() == CareScheduleStatus.COMPLETED) {
            if (Objects.equals(schedule.getVeterinarianId(), vetId)) {
                if (request.getNextSchedule() != null) {
                    createNextSchedule(request.getNextSchedule(), vetId, schedule);
                } else {
                    createRoutineCadenceSchedule(vetId, schedule);
                }
                return CareScheduleResponse.from(schedule);
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can access this completed schedule");
        }

        if (schedule.getStatus() != CareScheduleStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Care schedule must be IN_PROGRESS to complete");
        }
        if (!Objects.equals(schedule.getVeterinarianId(), vetId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can complete this schedule");
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
        if (schedule.getPurpose() == CareSchedulePurpose.PREVENTIVE && vaccinations.isEmpty() && deferDays == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PREVENTIVE_ACTION_REQUIRED",
                    "Preventive care completion requires recording vaccinations or specifying deferral days");
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
        List<CareScheduleStatus> activeStatuses = List.of(
                CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS);
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
        if (!preservedUrgentLock) {
            if (schedule.getCareType() == CareType.URGENT && decision == TrainingDecision.ALLOWED) {
                restoreTrainingDecisionBeforeUrgent(horse, schedule);
            } else if (decision == TrainingDecision.ALLOWED) {
                trainingDecisionService.allow(horse);
            } else {
                trainingDecisionService.block(horse, request.getRestrictionDetails().trim());
            }
        }
        // The horse's own state is the effective decision: it stays blocked when another urgent
        // case is open or the last non-urgent exam concluded BLOCKED, so report that, not the conclusion.
        TrainingDecision effectiveDecision = horse.getTrainingDecision();
        String effectiveReason = horse.getTrainingDecisionReason();

        LocalDateTime now = LocalDateTime.now();
        HealthRecord record = new HealthRecord();
        record.setHorseId(horse.getId());
        record.setVeterinarianId(vetId);
        record.setCareScheduleId(schedule.getId());
        record.setRecordType(schedule.getCareType().name());
        record.setExaminedAt(now);
        record.setFindings(request.getFindings().trim());
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setTreatment(request.getTreatment() != null ? request.getTreatment().trim() : null);
        record.setTrainingDecision(request.getTrainingDecision());
        record.setRestrictionDetails(request.getRestrictionDetails() != null ? request.getRestrictionDetails().trim() : null);
        record.setSymptoms(request.getSymptoms() != null ? request.getSymptoms().trim() : null);
        record.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        HealthRecord savedRecord = healthRecordRepository.save(record);
        eventPublisher.publishEvent(new ClinicalRecordCreatedEvent(
                savedRecord.getId(),
                horse.getId(),
                horse.getRegistrationNumber(),
                savedRecord.getSymptoms(),
                savedRecord.getDiagnosis(),
                List.of("VETERINARIAN", "HEAD_TRAINER", "CLUB_MANAGER")
        ));
        if (!vaccinations.isEmpty()) {
            preventiveVaccinationService.record(savedRecord.getId(), vaccinations);
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
        }

        // Gán Trainer chạy SAU khi transaction này commit (TrainerAssignmentTriggers):
        // lỗi lúc gán không thể làm rollback kết quả khám của Vet.
        if (schedule.getAdmissionId() != null && schedule.getCareType() == CareType.INITIAL) {
            eventPublisher.publishEvent(new InitialExamCompletedEvent(schedule.getAdmissionId()));
        }

        if (schedule.getCareType() == CareType.URGENT) {
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

            // Gửi thông báo cho Trainer phụ trách khu chuồng của ngựa
            horseRepository.findTrainerIdByHorseId(horse.getId()).ifPresent(recipients::add);

            // Bắn thông báo URGENT_CASE_CLOSED về cho cả Groom và Trainer liên quan
            String horseName = horse.getName() != null ? horse.getName() : ("#" + horse.getId());
            String title = "Ca khám khẩn cấp hoàn tất: " + horseName;
            String message = "Bác sĩ Thú y đã hoàn tất ca khám khẩn cấp cho ngựa " + horseName
                    + ". Quyết định tập luyện: " + effectiveDecision
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

        // Atomic next schedule creation if provided
        if (request.getNextSchedule() != null) {
            createNextSchedule(request.getNextSchedule(), vetId, completedSchedule);
        } else {
            createRoutineCadenceSchedule(vetId, completedSchedule);
        }

        return CareScheduleResponse.from(completedSchedule);
    }

    @Transactional
    public CareScheduleResponse cancelCareSchedule(Long scheduleId, CancelCareScheduleRequest request, Long userId) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cancel reason is required");
        }

        CareSchedule schedule = careScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));

        if (schedule.getStatus() != CareScheduleStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS", "Only SCHEDULED care schedules can be cancelled");
        }

        User actor = userRepository.findById(userId).orElse(null);
        boolean isManager = actor != null && actor.getRole() != null && "CLUB_MANAGER".equals(actor.getRole().getName());

        if (!isManager) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only club managers can cancel care schedules; veterinarians cannot decline assignments");
        }

        // Ngựa đang tạm nghỉ chỉ được mở khóa bởi một lần khám. Hủy nốt lần khám
        // cuối cùng thì ngựa bị khóa vô thời hạn mà không còn ai để mở.
        // Mốc tiêm phòng (có thể ở +180 ngày) không thay được lần tái khám đó.
        Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
        if (horse != null && horse.getTrainingDecision() == TrainingDecision.BLOCKED
                && schedule.getPurpose() != CareSchedulePurpose.PREVENTIVE
                && !careScheduleRepository.existsByHorseIdAndPurposeAndStatusInAndIdNot(horse.getId(),
                        CareSchedulePurpose.FOLLOW_UP,
                        List.of(CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS),
                        schedule.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_FOLLOW_UP",
                    "This horse is blocked from training and this is its last pending examination. "
                            + "Schedule a replacement examination before cancelling this one.");
        }

        schedule.setStatus(CareScheduleStatus.CANCELLED);
        schedule.setCancelReason(request.getReason().trim());
        careScheduleRepository.save(schedule);

        return CareScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public CareScheduleDetailResponse getScheduleDetail(Long scheduleId) {
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
        User vet = schedule.getVeterinarianId() != null ? userRepository.findById(schedule.getVeterinarianId()).orElse(null) : null;
        HealthRecord hr = healthRecordRepository.findByCareScheduleId(scheduleId).orElse(null);

        return CareScheduleDetailResponse.of(schedule, horse, vet, hr);
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
            Long horseId, Long vetId, Long admissionId, LocalDate date, Pageable pageable) {
        LocalDateTime dayStart = date != null ? date.atStartOfDay() : null;
        LocalDateTime dayEnd = date != null ? dayStart.plusDays(1) : null;
        return careScheduleRepository.findFiltered(status, careType, horseId, vetId, admissionId, dayStart, dayEnd, pageable)
                .map(CareScheduleResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CareScheduleResponse> listSchedules(CareScheduleStatus status, CareType careType,
            Long horseId, Long vetId, Long admissionId, Pageable pageable) {
        return listSchedules(status, careType, horseId, vetId, admissionId, null, pageable);
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
        return createNextSchedule(request, userId, source, true);
    }

    private CareScheduleResponse createNextSchedule(CreateNextScheduleRequest request, Long userId, CareSchedule source) {
        return createNextSchedule(request, userId, source, false);
    }

    private CareScheduleResponse createNextSchedule(CreateNextScheduleRequest request, Long userId,
            CareSchedule source, boolean standalone) {
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
            if (request.getAdmissionId() != null && !Objects.equals(request.getAdmissionId(), admissionId)) {
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
        String operationKey = standalone
                ? request.getIdempotencyKey() == null ? null : request.getIdempotencyKey().trim()
                : "completion:" + source.getId();
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
        String fingerprint = fingerprint(horseId, admissionId, requestedAt, description);
        Optional<CareSchedule> existing = standalone
                ? careScheduleRepository.findByRequestedByIdAndIdempotencyKey(userId, operationKey)
                : careScheduleRepository.findBySourceScheduleId(source.getId());
        if (existing.isPresent()) {
            if (!Objects.equals(existing.get().getRequestFingerprint(), fingerprint)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency key was already used with a different payload");
            }
            return CareScheduleResponse.from(existing.get());
        }

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horse.getId());
        schedule.setAdmissionId(admissionId);
        // sourceScheduleId on the entity is the one-to-one completion-operation key.
        // Standalone requests use (requestedById, idempotencyKey) so multiple distinct
        // follow-ups may be authorized by the same completed examination.
        schedule.setSourceScheduleId(standalone ? null : source.getId());
        schedule.setRequestedById(userId);
        schedule.setIdempotencyKey(operationKey);
        schedule.setRequestFingerprint(fingerprint);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
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
        LocalDateTime completedAt = completed.getCompletedAt();
        if (completedAt == null) return;

        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setCareType(CareType.ROUTINE);
        next.setScheduledAt(completedAt.plusDays(ROUTINE_CADENCE_DAYS));
        next.setDescription(ROUTINE_CADENCE_DESCRIPTION);
        createNextSchedule(next, vetId, completed);
    }

    private static String fingerprint(Long horseId, Long admissionId, LocalDateTime requestedAt, String description) {
        String value = horseId + "|" + admissionId + "|ROUTINE|" + requestedAt + "|" + description;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Transactional
    public void assignRequestedSchedules() {
        LocalDateTime horizon = LocalDateTime.now().plusDays(ASSIGNMENT_HORIZON_DAYS);
        for (CareSchedule schedule : careScheduleRepository.lockNextRequestedBatch(ASSIGNMENT_BATCH_SIZE, horizon)) {
            assignRequestedSchedule(schedule);
        }
    }

    public void assignRequestedSchedule(CareSchedule schedule) {
        assignRequestedSchedule(schedule, null);
    }

    /**
     * @param preferredVetId Vet who created this schedule. Ranked ahead of the load-balanced vets
     *                       while they still have a free slot, so a Vet keeps the visit they just
     *                       created; otherwise the normal assignment flow decides.
     */
    void assignRequestedSchedule(CareSchedule schedule, Long preferredVetId) {
        if (schedule.getStatus() != CareScheduleStatus.REQUESTED) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(ASSIGNMENT_HORIZON_DAYS);
        if (isBeyondAssignmentHorizon(schedule, horizon)) return;

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
                .findByStatusInAndVeterinarianIdIsNotNull(List.of(
                        CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS))) {
            if (assigned.getVeterinarianId() == null) continue;
            LocalDateTime when = assigned.getScheduledAt() != null ? assigned.getScheduledAt() : assigned.getRequestedAt();
            if (when != null && !when.isBefore(horizon)) continue;
            scheduledMinutesByVet.merge(assigned.getVeterinarianId(),
                    assigned.getDurationMinutes() > 0 ? assigned.getDurationMinutes() : DEFAULT_DURATION_MINUTES,
                    Integer::sum);
            if (assigned.getScheduledAt() != null && assigned.getScheduledAt().isBefore(now)
                    && (assigned.getStatus() == CareScheduleStatus.SCHEDULED
                            || assigned.getStatus() == CareScheduleStatus.IN_PROGRESS)) {
                overdueAppointmentsByVet.merge(assigned.getVeterinarianId(), 1, Integer::sum);
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

        VetScheduleCache vetCache = new VetScheduleCache(candidateVets, preferredVetId);
        SlotAssignment assignment = findAssignmentSlot(schedule, vetCache, overdueAppointmentsByVet, scheduledMinutesByVet);
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
            schedule.setScheduledAt(slot);
            schedule.setStatus(CareScheduleStatus.SCHEDULED);

            careScheduleRepository.save(schedule);
            assignInitialAdmissionVet(schedule, lockedVet.getId());

            boolean urgent = schedule.getCareType() == CareType.URGENT;
            String notificationType;
            String notificationTitle;
            String notificationMessage;
            if (urgent) {
                notificationType = NotificationTypes.CARE_SCHEDULE_URGENT_ASSIGNED;
                notificationTitle = "Urgent case assigned";
                notificationMessage = "You have been assigned to an urgent care case for horse " + horse.getName()
                        + ". Training is blocked until the examination is complete.";
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

            if (urgent) {
                HorseIncidentReport incident = incidentReportRepository.findByIdForUpdate(schedule.getSourceIncidentId())
                        .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "INCIDENT_NOT_FOUND", "Urgent incident not found"));
                LocalDateTime assignedAt = LocalDateTime.now();
                incident.setStatus(IncidentStatus.IN_REVIEW);
                incident.setHandledById(lockedVet.getId());
                incident.setHandledAt(assignedAt);
                incidentReportRepository.save(incident);
                eventPublisher.publishEvent(new UrgentAssignmentCommittedEvent(
                        buildUrgentAlert(schedule, incident, horse, lockedVet.getId(), assignedAt)));
            }

            recordAssignmentAudit(schedule, lockedVet);
            return;
        }
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
        List<User> ranked = activeVets.stream()
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
        final Long preferredVetId;
        final Map<Long, Boolean> hasInProgressByVet = new HashMap<>();
        final Map<Long, List<CareSchedule>> inProgressByVet = new HashMap<>();
        final Map<Long, List<CareSchedule>> scheduledByVet = new HashMap<>();
        final Map<Long, Boolean> historyByVet = new HashMap<>();

        VetScheduleCache(List<User> candidateVets, Long preferredVetId) {
            this.candidateVets = candidateVets;
            this.preferredVetId = preferredVetId;
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
        if (careScheduleRepository.existsByHorseIdAndStatus(schedule.getHorseId(), CareScheduleStatus.IN_PROGRESS)) {
            return null;
        }

        // 2. Tải trước danh sách ca khám đã lên lịch của ngựa 1 lần duy nhất cho toàn bộ 14 ngày
        List<CareSchedule> scheduledForHorse = careScheduleRepository.findScheduledForHorse(
                schedule.getHorseId(), CareScheduleStatus.SCHEDULED);

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
                times = new ArrayList<>(List.of(
                        FarmSchedulePolicy.VET_WINDOW_START,
                        LocalTime.of(14, 0),
                        LocalTime.of(14, 30),
                        LocalTime.of(15, 0)));
                for (int hour = 8; hour <= 17; hour++) {
                    for (int minute : List.of(0, 30)) {
                        LocalTime time = LocalTime.of(hour, minute);
                        if (!times.contains(time)) times.add(time);
                    }
                }
            }
            for (LocalTime time : times) {
                LocalDateTime slot = day.atTime(time);
                if (requested != null && slot.isBefore(requested)) continue;
                if (slot.isBefore(LocalDateTime.now())) continue;
                if (horseHasConflict(schedule, slot, scheduledForHorse)) continue;

                if (assignedInDay == null) {
                    LocalDateTime dayStart = day.atStartOfDay();
                    LocalDateTime dayEnd = dayStart.plusDays(1);
                    assignedInDay = careScheduleRepository.findAssignedInDay(dayStart, dayEnd,
                            List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));
                }
                List<User> ranked = rankVeterinarians(schedule, slot, vetCache, overdueAppointmentsByVet, scheduledMinutesByVet, assignedInDay);
                if (!ranked.isEmpty()) {
                    return new SlotAssignment(slot, ranked);
                }
            }
        }
        return null;
    }

    private boolean horseHasConflict(CareSchedule schedule, LocalDateTime slot, List<CareSchedule> scheduledForHorse) {
        return scheduledForHorse.stream().anyMatch(other -> !Objects.equals(other.getId(), schedule.getId()) && overlaps(schedule, slot, other));
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
                List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));

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
                        return Objects.equals(v.getId(), schedule.getVeterinarianId());
                    }
                    boolean activeExamBlocks = (schedule.getCareType() == CareType.URGENT || slot.toLocalDate().isEqual(LocalDate.now()))
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
                        .noneMatch(other -> overlaps(schedule, slot, other)))
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

        return eligibleVets.stream()
                .sorted(Comparator
                        .comparing((User v) -> !Objects.equals(v.getId(), vetCache.preferredVetId))
                        .thenComparingInt((User v) -> overdueAppointmentsByVet.getOrDefault(v.getId(), 0))
                        .thenComparingInt(v -> scheduledMinutesByVet.getOrDefault(v.getId(), 0))
                        .thenComparingInt(v -> horsesByVet.getOrDefault(v.getId(), Set.of()).size())
                        .thenComparingInt(v -> minutesByVet.getOrDefault(v.getId(), 0))
                        .thenComparing((User v) -> schedule.getCareType() == CareType.URGENT ? false
                                : !Boolean.TRUE.equals(vetCache.historyByVet.get(v.getId())))
                        .thenComparing(User::getId))
                .toList();
    }

    private boolean inProgressConflictsWithSlot(CareSchedule schedule, LocalDateTime slot, CareSchedule inProgress) {
        if (schedule.getCareType() == CareType.URGENT) return true;
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
        return careScheduleRepository.findByVeterinarianIdAndCareTypeAndStatusIn(
                        veterinarianId, CareType.URGENT,
                        List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)).stream()
                .map(schedule -> {
                    HorseIncidentReport incident = schedule.getSourceIncidentId() != null
                            ? incidentReportRepository.findById(schedule.getSourceIncidentId()).orElse(null)
                            : null;
                    Horse horse = horseRepository.findById(schedule.getHorseId()).orElse(null);
                    return horse == null ? null
                            : buildUrgentAlert(schedule, incident, horse, veterinarianId, schedule.getUpdatedAt());
                })
                .filter(Objects::nonNull)
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
     * Mọi báo cáo sự cố đã gộp vào ca khẩn cấp, kể cả báo cáo nguồn.
     * Báo cáo gộp trước đây không hiển thị ở đâu nên Thú y chỉ thấy báo cáo đầu tiên.
     */
    @Transactional(readOnly = true)
    public List<UrgentCaseReportResponse> getUrgentCaseReports(Long scheduleId, Long veterinarianId) {
        CareSchedule schedule = requireAssignedUrgentCase(scheduleId, veterinarianId);
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
        CareSchedule schedule = careScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        if (schedule.getCareType() != CareType.URGENT) {
            throw new ApiException(HttpStatus.NOT_FOUND, "URGENT_CASE_NOT_FOUND", "Urgent case not found");
        }
        if (!Objects.equals(schedule.getVeterinarianId(), veterinarianId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can view this urgent case");
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
                report.getSeverity(),
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

    /** What the reporting Groom is told when the urgent case closes: diagnosis, care to give, and whether the horse may train. */
    private static String incidentOutcomeNote(CompleteCareScheduleRequest request,
            TrainingDecision effectiveDecision, String effectiveReason) {
        StringBuilder note = new StringBuilder("Diagnosis: ").append(request.getDiagnosis().trim());
        if (request.getTreatment() != null && !request.getTreatment().isBlank()) {
            note.append("\nTreatment: ").append(request.getTreatment().trim());
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

    /**
     * Đóng ca khẩn cấp cuối cùng: trả lại kết luận của lần khám gần nhất không phải ca khẩn cấp thay vì luôn mở khóa.
     * Bệnh án của ca khẩn cấp đang đóng chưa được lưu ở bước này nên không lẫn vào kết quả.
     */
    private void restoreTrainingDecisionBeforeUrgent(Horse horse, CareSchedule schedule) {
        LocalDateTime cutoff = schedule.getCreatedAt() != null ? schedule.getCreatedAt() : LocalDateTime.now();
        HealthRecord previous = previousTrainingRecord(horse.getId(), cutoff).orElse(null);
        if (previous != null && previous.getTrainingDecision() == TrainingDecision.BLOCKED) {
            trainingDecisionService.block(horse, previous.getRestrictionDetails());
        } else {
            trainingDecisionService.allow(horse);
        }
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
                incident != null ? incident.getSeverity() : null,
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
