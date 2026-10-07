package com.rtms.backend.service;

import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.HorseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Gán Head Trainer cho đơn nhập học — kết quả ghi thẳng vào
 * admission_applications.trainer_id, nơi duy nhất lưu "đơn này của Trainer nào".
 *
 * Không có lịch hẹn: Trainer nhận đơn rồi xem lúc nào cũng được.
 */
@Service
public class TrainerAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TrainerAssignmentService.class);

    private final AdmissionApplicationRepository admissionRepository;
    private final HorseRepository horseRepository;
    private final HeadTrainerWorkloadService workloadService;
    private final NotificationService notificationService;

    public TrainerAssignmentService(AdmissionApplicationRepository admissionRepository,
                                    HorseRepository horseRepository,
                                    HeadTrainerWorkloadService workloadService,
                                    NotificationService notificationService) {
        this.admissionRepository = admissionRepository;
        this.horseRepository = horseRepository;
        this.workloadService = workloadService;
        this.notificationService = notificationService;
    }

    /**
     * Gán nếu đơn đang ở TRAINER_REVIEW mà chưa có Trainer. Chạy lặp lại vô hại.
     *
     * REQUIRES_NEW: được gọi sau khi transaction của Vet đã commit (và từ job
     * định kỳ), nên luôn cần transaction riêng để khóa hàng có hiệu lực. Khóa
     * đơn trước rồi kiểm lại trainerId, nên hai lượt chạy song song chỉ một
     * lượt gán được.
     *
     * @return Trainer đã được gán, hoặc rỗng nếu chưa có ai phù hợp (job sẽ thử lại)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> assignIfNeeded(Long admissionId) {
        AdmissionApplication admission = admissionRepository.findByIdForUpdate(admissionId).orElse(null);
        if (admission == null || admission.getStatus() != AdmissionStatus.TRAINER_REVIEW) {
            return Optional.empty();
        }
        if (admission.getTrainerId() != null) {
            return Optional.of(admission.getTrainerId());
        }

        Optional<User> selected = workloadService.selectLeastLoadedHeadTrainer();
        if (selected.isEmpty()) {
            log.info("No eligible Head Trainer for admission #{} yet; the scheduled job will retry.", admissionId);
            return Optional.empty();
        }

        User trainer = selected.get();
        admission.setTrainerId(trainer.getId());
        admissionRepository.save(admission);

        String horseName = admission.getHorseId() == null ? "candidate horse"
                : horseRepository.findById(admission.getHorseId()).map(Horse::getName).orElse("candidate horse");
        notificationService.sendAssignmentNotification(
                trainer.getId(),
                admission.getId(),
                admission.getHorseId(),
                NotificationTypes.ADMISSION_TRAINER_ASSIGNED,
                "New horse assignment",
                "You have been assigned to candidate horse " + horseName + " for racing readiness assessment.");

        return Optional.of(trainer.getId());
    }

    /** Đơn đang chờ được gán — đơn chờ lâu nhất trước. */
    @Transactional(readOnly = true)
    public List<Long> findUnassignedAdmissionIds() {
        return admissionRepository
                .findByStatusAndTrainerIdIsNullOrderBySubmittedAtAscIdAsc(AdmissionStatus.TRAINER_REVIEW)
                .stream()
                .map(AdmissionApplication::getId)
                .toList();
    }
}
