package com.rtms.backend.admission.controller;
import com.rtms.backend.admission.dto.AdmissionDetailResponse;
import com.rtms.backend.admission.dto.TrainerAdmissionQueueResponse;
import com.rtms.backend.admission.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.admission.dto.TrainerAdmissionViewResponse;
import com.rtms.backend.common.dto.ApiResponse;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.admission.service.AdmissionQueryService;
import com.rtms.backend.admission.service.AdmissionTrainerReviewService;
import com.rtms.backend.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionTrainerReviewController {

    private final AdmissionTrainerReviewService trainerReviewService;
    private final AdmissionQueryService queryService;
    private final HorseRepository horseRepository;
    private final RacingReadinessAssessmentRepository assessmentRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HorseHealthMetricRepository horseHealthMetricRepository;

    public AdmissionTrainerReviewController(
            AdmissionTrainerReviewService trainerReviewService,
            AdmissionQueryService queryService,
            HorseRepository horseRepository,
            RacingReadinessAssessmentRepository assessmentRepository,
            HealthRecordRepository healthRecordRepository,
            HorseHealthMetricRepository horseHealthMetricRepository) {
        this.trainerReviewService = trainerReviewService;
        this.queryService = queryService;
        this.horseRepository = horseRepository;
        this.assessmentRepository = assessmentRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.horseHealthMetricRepository = horseHealthMetricRepository;
    }

    /**
     * Hàng chờ của chính Trainer đang đăng nhập — hai nhóm trong một lời gọi.
     *
     * Endpoint riêng, KHÔNG tái dùng GET /api/admissions: cái đó chung cho cả
     * 5 vai trò (xem ghi chú ở AdmissionQueryService.getTrainerQueue).
     *
     * Đường dẫn "/trainer/queue" có 3 đoạn nên không đụng "/{id}" (2 đoạn);
     * với "/{id}/documents" thì Spring ưu tiên đoạn chữ "trainer" trước biến
     * "{id}", đúng như "/horses/{horseId}/readiness-history" bên dưới vẫn
     * sống chung với "/{id}/trainer-review" từ trước tới nay.
     */
    @GetMapping("/trainer/queue")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<TrainerAdmissionQueueResponse> getTrainerQueue(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(queryService.getTrainerQueue(currentUser.getUserId()));
    }

    /**
     * Màn hình Trainer xem hồ sơ candidate — gom mọi thứ vào một lời gọi.
     *
     * Danh sách đơn chờ duyệt: GET /api/admissions/trainer/queue
     */
    @GetMapping("/{id}/trainer-view")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<TrainerAdmissionViewResponse> getTrainerView(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        AdmissionDetailResponse detail = queryService.getAdmissionDetail(id);

        // Lọc hàng chờ chỉ làm giao diện gọn; chặn thật nằm ở đây, vì ai cũng gõ
        // thẳng /api/admissions/45/trainer-view được.
        if ("HEAD_TRAINER".equals(currentUser.getRole())
                && !Objects.equals(detail.getTrainerId(), currentUser.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Hồ sơ này không được phân công cho bạn!");
        }

        Horse horse = detail.getHorseId() == null
                ? null
                : horseRepository.findById(detail.getHorseId()).orElse(null);

        List<HealthRecord> healthRecords = horse == null
                ? List.of()
                : healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());

        List<HorseHealthMetric> healthMetrics = horse == null
                ? List.of()
                : horseHealthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId());

        return ApiResponse.success(new TrainerAdmissionViewResponse(
                detail,
                horse,
                healthRecords,
                healthMetrics,
                assessmentRepository.findByAdmissionId(id).orElse(null)));
    }

    /** Hoàn thành đánh giá -> đơn chuyển MANAGER_REVIEW. */
    @PostMapping("/{id}/trainer-review")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_TRAINER_REVIEW') "
                + "and hasAuthority('RACING_READINESS_ASSESSMENT_CREATE')")
    public ApiResponse<AdmissionApplication> trainerReview(
            @PathVariable Long id,
            @RequestBody TrainerAdmissionReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                trainerReviewService.completeAssessment(id, request, currentUser.getUserId()));
    }

    /** Lịch sử đánh giá của một chiến mã — dùng cho biểu đồ tiến bộ sau này. */
    @GetMapping("/horses/{horseId}/readiness-history")
    @PreAuthorize("hasAuthority('RACING_READINESS_ASSESSMENT_VIEW')")
    public ApiResponse<List<RacingReadinessAssessment>> getHistory(
            @PathVariable Long horseId,
            @RequestParam(required = false, defaultValue = "false") boolean includeAdmission) {
        return ApiResponse.success(includeAdmission
                ? assessmentRepository.findByHorseIdOrderByAssessmentDateDesc(horseId)
                : assessmentRepository
                        .findByHorseIdAndAdmissionIdIsNullOrderByAssessmentDateAsc(horseId));
    }
}
