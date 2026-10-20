package com.rtms.backend.admission.service;

import com.rtms.backend.event.InitialExamCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Hai đường kích hoạt việc gán Trainer.
 *
 * Tách khỏi TrainerAssignmentService có chủ đích: gọi assignIfNeeded qua
 * proxy của bean khác thì @Transactional(REQUIRES_NEW) mới có hiệu lực. Gọi
 * nội bộ trong cùng một bean sẽ bỏ qua proxy và chạy KHÔNG có transaction.
 */
@Component
public class TrainerAssignmentTriggers {

    private static final Logger log = LoggerFactory.getLogger(TrainerAssignmentTriggers.class);

    private final TrainerAssignmentService assignmentService;

    public TrainerAssignmentTriggers(TrainerAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * Chạy sau khi transaction hoàn tất khám của Vet đã commit. Lỗi ở đây chỉ
     * được ghi log — kết quả khám đã lưu, job định kỳ sẽ gán lại.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInitialExamCompleted(InitialExamCompletedEvent event) {
        try {
            assignmentService.assignIfNeeded(event.admissionId());
        } catch (Exception e) {
            log.error("Trainer assignment failed for admission #{}; will retry", event.admissionId(), e);
        }
    }

    /** Job định kỳ: gán các đơn còn trống, mỗi đơn một transaction riêng. */
    public void retryPendingAssignments() {
        for (Long admissionId : assignmentService.findUnassignedAdmissionIds()) {
            try {
                assignmentService.assignIfNeeded(admissionId);
            } catch (Exception e) {
                log.error("Trainer assignment retry failed for admission #{}", admissionId, e);
            }
        }
    }
}
