package com.rtms.backend.controller;

import com.rtms.backend.dto.*;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.RacingReadinessAssessment;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AdmissionQueryService;
import com.rtms.backend.service.AdmissionTrainerReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionTrainerReviewController {

    private final AdmissionTrainerReviewService trainerReviewService;
    private final AdmissionQueryService queryService;
    private final HorseRepository horseRepository;
    private final RacingReadinessAssessmentRepository assessmentRepository;

    public AdmissionTrainerReviewController(
            AdmissionTrainerReviewService trainerReviewService,
            AdmissionQueryService queryService,
            HorseRepository horseRepository,
            RacingReadinessAssessmentRepository assessmentRepository) {
        this.trainerReviewService = trainerReviewService;
        this.queryService = queryService;
        this.horseRepository = horseRepository;
        this.assessmentRepository = assessmentRepository;
    }

    /**
     * Màn hình Trainer xem hồ sơ candidate — gom mọi thứ vào một lời gọi.
     *
     * Danh sách đơn chờ duyệt dùng endpoint có sẵn:
     *   GET /api/admissions?status=TRAINER_REVIEW
     */
    @GetMapping("/{id}/trainer-view")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<TrainerAdmissionViewResponse> getTrainerView(@PathVariable Long id) {
        AdmissionDetailResponse detail = queryService.getAdmissionDetail(id);

        Horse horse = detail.getHorseId() == null
                ? null
                : horseRepository.findById(detail.getHorseId()).orElse(null);

        // Module Thú y chưa ghi HealthRecord / HorseHealthMetric.
        // Trả rỗng thay vì lỗi — FE hiển thị "chưa có dữ liệu".
        // Khi họ xong thì thay 2 dòng này bằng query thật.
        List<Object> healthRecords = List.of();
        List<Object> healthMetrics = List.of();

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