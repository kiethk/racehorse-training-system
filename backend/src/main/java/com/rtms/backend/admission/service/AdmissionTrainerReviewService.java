package com.rtms.backend.admission.service;

import com.rtms.backend.admission.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.TrainingLimits;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Bước Trainer trong luồng nhập học.
 *
 * Trainer được hệ thống gán sẵn (admission.trainerId) ngay sau khi Thú y khám
 * xong — xem TrainerAssignmentService. Chỉ đúng Trainer đó mới nộp được.
 *
 * Trainer KHÔNG approve/reject: nộp nhận xét là đơn tự chuyển MANAGER_REVIEW,
 * Manager mới quyết định cuối cùng.
 *
 * Bước này KHÔNG đụng tới Horse.currentStatus (vẫn CANDIDATE), chuồng cách ly,
 * hay Course / CourseRun / TrainingWorkout.
 * (The Trainer step of an admission: only the assigned Trainer can hand in the written feedback, which moves the admission to MANAGER_REVIEW. It never touches the horse status, the quarantine stall, or any course.)
 */
@Service
public class AdmissionTrainerReviewService {

    private final AdmissionApplicationRepository admissionRepository;

    public AdmissionTrainerReviewService(AdmissionApplicationRepository admissionRepository) {
        this.admissionRepository = admissionRepository;
    }

    @Transactional
    public AdmissionApplication completeReview(Long admissionId,
            TrainerAdmissionReviewRequest request,
            Long trainerId) {

        // Khóa đơn: hai lần bấm nộp gần như cùng lúc thì lần sau thấy MANAGER_REVIEW và
        // dừng. (Lock the admission: of two near-simultaneous submits, the second sees MANAGER_REVIEW and stops.)
        AdmissionApplication admission = admissionRepository.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                        "Không tìm thấy đơn nhập học #" + admissionId));

        if (admission.getStatus() != AdmissionStatus.TRAINER_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", String.format(
                    "Đơn đang ở bước %s, không phải TRAINER_REVIEW — không thể đánh giá!",
                    admission.getStatus()));
        }
        if (admission.getTrainerId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "TRAINER_NOT_ASSIGNED",
                    "Đơn chưa được phân công Huấn luyện viên. Hệ thống sẽ tự phân công, vui lòng thử lại sau!");
        }
        if (!admission.getTrainerId().equals(trainerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Hồ sơ này đã được phân công cho Huấn luyện viên khác đánh giá!");
        }
        if (admission.getHorseId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_ASSIGNED",
                    "Đơn chưa gắn hồ sơ chiến mã — bước Groom phải tạo Horse trước khi Trainer đánh giá!");
        }

        admission.setTrainerFeedback(validFeedback(request));
        admission.setTrainerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.MANAGER_REVIEW);
        return admissionRepository.save(admission);
    }

    /** Nhận xét bắt buộc, không toàn khoảng trắng, không quá giới hạn. (Feedback is required, not all blanks, and within the length limit.) */
    private String validFeedback(TrainerAdmissionReviewRequest request) {
        String feedback = request == null || request.trainerFeedback() == null
                ? ""
                : request.trainerFeedback().trim();
        if (feedback.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Nhận xét của Huấn luyện viên là bắt buộc!");
        }
        if (feedback.length() > TrainingLimits.MAX_ADMISSION_TRAINER_FEEDBACK_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Nhận xét của Huấn luyện viên không được vượt quá "
                            + TrainingLimits.MAX_ADMISSION_TRAINER_FEEDBACK_LENGTH + " ký tự!");
        }
        return feedback;
    }
}
