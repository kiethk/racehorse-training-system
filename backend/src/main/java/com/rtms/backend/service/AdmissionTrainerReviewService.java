package com.rtms.backend.service;

import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.RacingReadinessAssessment;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.RacingReadinessAssessmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bước Trainer trong luồng nhập học.
 *
 * Trainer KHÔNG approve/reject — V37 đã xoá cột trainer_decision.
 * Trainer hoàn thành đánh giá -> đơn tự chuyển MANAGER_REVIEW.
 * Manager mới là người quyết định cuối cùng.
 *
 * Bước này KHÔNG đụng tới:
 *   - Horse.currentStatus  (vẫn CANDIDATE)
 *   - chuồng cách ly       (vẫn OCCUPIED)
 *   - Course / HorseTrainingPlan / TrainingWorkout
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

        AdmissionApplication admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đơn nhập học #" + admissionId));

        // ---- Một lần duy nhất ----
        // Nộp xong đơn chuyển MANAGER_REVIEW, nên gọi lại sẽ dừng ở đây.
        if (admission.getStatus() != AdmissionStatus.TRAINER_REVIEW) {
            throw new IllegalStateException(String.format(
                    "Đơn đang ở bước %s, không phải TRAINER_REVIEW — không thể đánh giá!",
                    admission.getStatus()));
        }

        // ---- Đúng người được phân công ----
        //
        // Guard này PHẢI nằm ở đây, không chỉ ở câu truy vấn hàng chờ. Ẩn đơn
        // khỏi danh sách chỉ là tiện lợi cho giao diện; ai cũng gọi thẳng
        // POST /api/admissions/45/trainer-review được.
        //
        // Trước đây dòng cuối hàm này ghi setTrainerId(trainerId) vô điều
        // kiện. Khi bước Thú y bắt đầu gán sẵn, dòng đó biến thành GHI ĐÈ âm
        // thầm: Trainer B nộp đánh giá cho đơn của Trainer A thì trainer_id bị
        // sửa thành B, kèm theo racing_readiness_assessments.trainer_id = B —
        // không còn dấu vết nào cho thấy thuật toán đã chọn A.
        if (admission.getTrainerId() != null
                && !admission.getTrainerId().equals(trainerId)) {
            throw new AccessDeniedException(
                    "Hồ sơ này đã được phân công cho Huấn luyện viên khác đánh giá!");
        }

        // ---- Bước Groom phải tạo Horse trước ----
        if (admission.getHorseId() == null) {
            throw new IllegalStateException(
                    "Đơn chưa gắn hồ sơ chiến mã. Bước Groom phải tạo Horse (CANDIDATE) "
                  + "và xếp chuồng cách ly trước khi Trainer đánh giá!");
        }

        if (request.getReadinessStatus() == null) {
            throw new IllegalArgumentException(
                    "Phải chọn mức độ sẵn sàng thi đấu (READY / NEEDS_MORE_TRAINING / UNSUITABLE)!");
        }

        validateScore(request.getConformationScore(), "Điểm dáng vóc");
        validateScore(request.getTemperamentScore(), "Điểm tính nết");
        validateScore(request.getGaitQualityScore(), "Điểm bước đi");

        if (request.getEstimatedMonthsToRace() != null
                && (request.getEstimatedMonthsToRace() < 0
                 || request.getEstimatedMonthsToRace() > MAX_MONTHS)) {
            throw new IllegalArgumentException(
                    "Ước tính thời gian phải từ 0 đến " + MAX_MONTHS + " tháng!");
        }

        // ---- Tạo bản đánh giá ----
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

        // fitnessScore : để NULL — ngựa đang cách ly, không đo được thể lực
        // validUntil   : để NULL — chỉ đánh giá định kỳ mới có hạn dùng
        //                (CHECK chk_rra_valid_until ràng buộc điều này)

        assessmentRepository.save(assessment);

        // ---- Ghi dấu bước Trainer lên đơn ----
        //
        // Chỉ gán khi còn trống. Đơn đã có người phụ trách thì giữ nguyên —
        // guard ở đầu hàm đã bảo đảm người đó chính là người đang gọi, nên
        // gán lại cũng không đổi gì, mà bỏ gán thì không bao giờ ghi đè nhầm.
        //
        // TODO(sau khi Thú y gán trainer_id tự động): đổi nhánh null thành
        // lỗi "Đơn chưa được phân công Huấn luyện viên". Chưa làm ngay vì mọi
        // đơn hiện có trong cơ sở dữ liệu đều mang trainer_id = NULL, siết
        // bây giờ là không đánh giá được đơn nào nữa.
        if (admission.getTrainerId() == null) {
            admission.setTrainerId(trainerId);
        }
        admission.setTrainerFeedback(request.getRemarks());
        admission.setTrainerReviewedAt(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.MANAGER_REVIEW);

        return admissionRepository.save(admission);
    }

    private void validateScore(BigDecimal score, String label) {
        if (score == null) {
            return;
        }
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException(label + " phải từ 0 đến 10!");
        }
    }
}