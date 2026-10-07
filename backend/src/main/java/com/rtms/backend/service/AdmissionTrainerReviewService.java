package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.RacingReadinessAssessment;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.RacingReadinessAssessmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bước Trainer trong luồng nhập học.
 *
 * Trainer được hệ thống gán sẵn (admission.trainerId) ngay sau khi Thú y khám
 * xong — xem TrainerAssignmentService. Chỉ đúng Trainer đó mới nộp được.
 *
 * Trainer KHÔNG approve/reject: hoàn thành đánh giá là đơn tự chuyển
 * MANAGER_REVIEW, Manager mới quyết định cuối cùng.
 *
 * Bước này KHÔNG đụng tới Horse.currentStatus (vẫn CANDIDATE), chuồng cách ly,
 * hay Course / HorseTrainingPlan / TrainingWorkout.
 */
@Service
public class AdmissionTrainerReviewService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;
    private static final int MAX_MONTHS = 60;

    private final AdmissionApplicationRepository admissionRepository;
    private final RacingReadinessAssessmentRepository assessmentRepository;

    public AdmissionTrainerReviewService(AdmissionApplicationRepository admissionRepository,
                                         RacingReadinessAssessmentRepository assessmentRepository) {
        this.admissionRepository = admissionRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional
    public AdmissionApplication completeAssessment(Long admissionId,
                                                    TrainerAdmissionReviewRequest request,
                                                    Long trainerId) {

        // Khóa đơn: hai lần bấm nộp gần như cùng lúc thì lần sau thấy MANAGER_REVIEW và dừng.
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

        validate(request);

        RacingReadinessAssessment assessment = new RacingReadinessAssessment();
        assessment.setHorseId(admission.getHorseId());
        assessment.setAdmissionId(admission.getId());   // NOT NULL -> loại "nhập học"
        assessment.setTrainerId(trainerId);
        assessment.setReadinessStatus(request.getReadinessStatus());
        assessment.setConformationScore(request.getConformationScore());
        assessment.setTemperamentScore(request.getTemperamentScore());
        assessment.setGaitQualityScore(request.getGaitQualityScore());
        assessment.setEstimatedMonthsToRace(request.getEstimatedMonthsToRace());
        assessment.setAssessmentDate(LocalDate.now());
        assessment.setRemarks(request.getRemarks());
        // fitnessScore, validUntil: để NULL — ngựa đang cách ly, chỉ đánh giá định kỳ mới có
        assessmentRepository.save(assessment);

        admission.setTrainerFeedback(request.getRemarks());
        admission.setTrainerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.MANAGER_REVIEW);
        return admissionRepository.save(admission);
    }

    private void validate(TrainerAdmissionReviewRequest request) {
        if (request == null || request.getReadinessStatus() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Phải chọn mức độ sẵn sàng thi đấu (READY / NEEDS_MORE_TRAINING / UNSUITABLE)!");
        }
        validateScore(request.getConformationScore(), "Điểm dáng vóc");
        validateScore(request.getTemperamentScore(), "Điểm tính nết");
        validateScore(request.getGaitQualityScore(), "Điểm bước đi");
        if (request.getEstimatedMonthsToRace() != null
                && (request.getEstimatedMonthsToRace() < 0 || request.getEstimatedMonthsToRace() > MAX_MONTHS)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Ước tính thời gian phải từ 0 đến " + MAX_MONTHS + " tháng!");
        }
    }

    private void validateScore(BigDecimal score, String label) {
        if (score == null) {
            return;
        }
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", label + " phải từ 0 đến 10!");
        }
    }
}
