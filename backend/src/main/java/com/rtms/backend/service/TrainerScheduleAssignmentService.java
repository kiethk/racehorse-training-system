package com.rtms.backend.service;

import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TrainerScheduleAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TrainerScheduleAssignmentService.class);
    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final TrainerScheduleRepository trainerScheduleRepository;
    private final AdmissionApplicationRepository admissionRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final HorseRepository horseRepository;
    private final HeadTrainerWorkloadService headTrainerWorkloadService;
    private final NotificationService notificationService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public TrainerScheduleAssignmentService(
            TrainerScheduleRepository trainerScheduleRepository,
            AdmissionApplicationRepository admissionRepository,
            CareScheduleRepository careScheduleRepository,
            HorseRepository horseRepository,
            HeadTrainerWorkloadService headTrainerWorkloadService,
            NotificationService notificationService,
            org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.trainerScheduleRepository = trainerScheduleRepository;
        this.admissionRepository = admissionRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.horseRepository = horseRepository;
        this.headTrainerWorkloadService = headTrainerWorkloadService;
        this.notificationService = notificationService;
        this.transactionTemplate = transactionManager != null
                ? new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                : null;
    }

    public TrainerScheduleAssignmentService(
            TrainerScheduleRepository trainerScheduleRepository,
            AdmissionApplicationRepository admissionRepository,
            CareScheduleRepository careScheduleRepository,
            HorseRepository horseRepository,
            HeadTrainerWorkloadService headTrainerWorkloadService,
            NotificationService notificationService) {
        this(trainerScheduleRepository, admissionRepository, careScheduleRepository, horseRepository,
                headTrainerWorkloadService, notificationService, null);
    }

    /**
     * Gán trực tiếp một Head Trainer và tạo TrainerSchedule sau khi Vet hoàn thành CareSchedule INITIAL.
     * Hoạt động an toàn concurrency và idempotent.
     */
    @Transactional
    public Optional<TrainerSchedule> assignForCompletedInitialCare(Long admissionId, Long sourceCareScheduleId) {
        // Kiểm tra idempotent trước
        Optional<TrainerSchedule> existing = trainerScheduleRepository.findByAdmissionId(admissionId);
        if (existing.isPresent()) {
            return existing;
        }

        AdmissionApplication admission = admissionRepository.findByIdForUpdate(admissionId)
                .orElse(null);
        if (admission == null) {
            log.warn("Cannot assign trainer schedule: Admission #{} not found", admissionId);
            return Optional.empty();
        }

        if (admission.getStatus() != AdmissionStatus.TRAINER_REVIEW) {
            log.warn("Cannot assign trainer schedule: Admission #{} is in status {}, expected TRAINER_REVIEW",
                    admissionId, admission.getStatus());
            return Optional.empty();
        }

        CareSchedule careSchedule = careScheduleRepository.findById(sourceCareScheduleId)
                .orElse(null);
        if (careSchedule == null || careSchedule.getCareType() != CareType.INITIAL
                || careSchedule.getStatus() != CareScheduleStatus.COMPLETED) {
            log.warn("Cannot assign trainer schedule: Source care schedule #{} is not a completed INITIAL schedule",
                    sourceCareScheduleId);
            return Optional.empty();
        }

        // Kiểm tra lại sau khi lock
        existing = trainerScheduleRepository.findByAdmissionId(admissionId);
        if (existing.isPresent()) {
            return existing;
        }

        LocalDateTime scheduledAt = LocalDateTime.now();
        Optional<User> selectedTrainerOpt = headTrainerWorkloadService.selectLeastLoadedHeadTrainer(
                scheduledAt, DEFAULT_DURATION_MINUTES);

        if (selectedTrainerOpt.isEmpty()) {
            log.info("No available eligible Head Trainer found for admission #{}. Remaining in TRAINER_REVIEW for retry.",
                    admissionId);
            return Optional.empty();
        }

        User selectedTrainer = selectedTrainerOpt.get();

        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setHorseId(admission.getHorseId());
        schedule.setAdmissionId(admission.getId());
        schedule.setSourceCareScheduleId(sourceCareScheduleId);
        schedule.setTrainerId(selectedTrainer.getId());
        schedule.setStatus(TrainerScheduleStatus.SCHEDULED);
        schedule.setScheduledAt(scheduledAt);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);

        TrainerSchedule saved = trainerScheduleRepository.save(schedule);

        // Gửi thông báo tới Trainer được phân công
        String horseName = "Horse";
        if (admission.getHorseId() != null) {
            Horse horse = horseRepository.findById(admission.getHorseId()).orElse(null);
            if (horse != null && horse.getName() != null) {
                horseName = horse.getName();
            }
        }

        notificationService.sendAssignmentNotification(
                selectedTrainer.getId(),
                NotificationTypes.REFERENCE_TRAINER_SCHEDULE,
                saved.getId(),
                admission.getHorseId(),
                NotificationTypes.TRAINER_SCHEDULE_ASSIGNED,
                "New horse assignment",
                "You have been assigned to candidate horse " + horseName + " for racing readiness assessment."
        );

        return Optional.of(saved);
    }

    /**
     * Retry gán TrainerSchedule cho các đơn đang ở TRAINER_REVIEW chưa có TrainerSchedule.
     * Gọi định kỳ bởi CareScheduleScheduler.
     * Mỗi đơn được thực hiện trong transaction độc lập để tránh cascading rollback khi có xung đột.
     */
    public void retryPendingAssignments() {
        List<AdmissionApplication> pendingAdmissions = admissionRepository.findPendingTrainerScheduleAdmissions();
        for (AdmissionApplication admission : pendingAdmissions) {
            CareSchedule initialSchedule = careScheduleRepository
                    .findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admission.getId(), CareType.INITIAL)
                    .orElse(null);

            if (initialSchedule != null && initialSchedule.getStatus() == CareScheduleStatus.COMPLETED) {
                try {
                    if (transactionTemplate != null) {
                        transactionTemplate.execute(status -> assignForCompletedInitialCare(admission.getId(), initialSchedule.getId()));
                    } else {
                        assignForCompletedInitialCare(admission.getId(), initialSchedule.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to assign trainer schedule for admission #{}", admission.getId(), e);
                }
            }
        }
    }
}
