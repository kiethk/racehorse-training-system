package com.rtms.backend.controller;

import com.rtms.backend.dto.*;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorseHealthMetric;
import com.rtms.backend.entity.RacingReadinessAssessment;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.HorseHealthMetricRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
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
    private final HealthRecordRepository healthRecordRepository;
    private final HorseHealthMetricRepository horseHealthMetricRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;

    public AdmissionTrainerReviewController(
            AdmissionTrainerReviewService trainerReviewService,
            AdmissionQueryService queryService,
            HorseRepository horseRepository,
            RacingReadinessAssessmentRepository assessmentRepository,
            HealthRecordRepository healthRecordRepository,
            HorseHealthMetricRepository horseHealthMetricRepository,
            TrainerScheduleRepository trainerScheduleRepository) {
        this.trainerReviewService = trainerReviewService;
        this.queryService = queryService;
        this.horseRepository = horseRepository;
        this.assessmentRepository = assessmentRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.horseHealthMetricRepository = horseHealthMetricRepository;
        this.trainerScheduleRepository = trainerScheduleRepository;
    }

    /**
     * Màn hình Trainer xem hồ sơ candidate — gom mọi thứ vào một lời gọi.
     *
     * Danh sách đơn chờ duyệt dùng endpoint có sẵn:
     *   GET /api/admissions?status=TRAINER_REVIEW
     */
    @GetMapping("/{id}/trainer-view")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<TrainerAdmissionViewResponse> getTrainerView(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        AdmissionDetailResponse detail = queryService.getAdmissionDetail(id);
        if ("HEAD_TRAINER".equals(currentUser.getRole())
                && !java.util.Objects.equals(detail.getTrainerId(), currentUser.getUserId())) {
            throw new com.rtms.backend.config.ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the assigned trainer can view this admission");
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

        com.rtms.backend.entity.TrainerSchedule schedule = trainerScheduleRepository.findByAdmissionId(id).orElse(null);

        return ApiResponse.success(new TrainerAdmissionViewResponse(
                detail,
                horse,
                healthRecords,
                healthMetrics,
                assessmentRepository.findByAdmissionId(id).orElse(null),
                schedule == null ? null : TrainerScheduleResponse.from(schedule)));
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
