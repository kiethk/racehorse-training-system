package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.AdmissionDetailResponse;
import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.dto.TrainerScheduleDetailResponse;
import com.rtms.backend.dto.TrainerScheduleResponse;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorseHealthMetric;
import com.rtms.backend.entity.RacingReadinessAssessment;
import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.HorseHealthMetricRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TrainerScheduleService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;
    private static final int MAX_MONTHS = 60;

    private final TrainerScheduleRepository trainerScheduleRepository;
    private final AdmissionApplicationRepository admissionRepository;
    private final RacingReadinessAssessmentRepository assessmentRepository;
    private final HorseRepository horseRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HorseHealthMetricRepository healthMetricRepository;
    private final CandidateHorseProfileRepository candidateProfileRepository;
    private final StableStallRepository stableStallRepository;
    private final AdmissionQueryService admissionQueryService;

    public TrainerScheduleService(
            TrainerScheduleRepository trainerScheduleRepository,
            AdmissionApplicationRepository admissionRepository,
            RacingReadinessAssessmentRepository assessmentRepository,
            HorseRepository horseRepository,
            HealthRecordRepository healthRecordRepository,
            HorseHealthMetricRepository healthMetricRepository,
            CandidateHorseProfileRepository candidateProfileRepository,
            StableStallRepository stableStallRepository,
            AdmissionQueryService admissionQueryService) {
        this.trainerScheduleRepository = trainerScheduleRepository;
        this.admissionRepository = admissionRepository;
        this.assessmentRepository = assessmentRepository;
        this.horseRepository = horseRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.healthMetricRepository = healthMetricRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.stableStallRepository = stableStallRepository;
        this.admissionQueryService = admissionQueryService;
    }

    @Transactional(readOnly = true)
    public List<TrainerScheduleResponse> listSchedules(Long trainerId, TrainerScheduleStatus status) {
        List<TrainerSchedule> list = trainerScheduleRepository.findFiltered(trainerId, status);
        if (list.isEmpty()) {
            return List.of();
        }

        java.util.Set<Long> admissionIds = list.stream()
                .map(TrainerSchedule::getAdmissionId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, com.rtms.backend.entity.CandidateHorseProfile> candidateMap =
                candidateProfileRepository.findByAdmissionIdIn(admissionIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.rtms.backend.entity.CandidateHorseProfile::getAdmissionId,
                                c -> c,
                                (a, b) -> a));

        java.util.Map<Long, AdmissionApplication> admissionMap =
                admissionRepository.findAllById(admissionIds).stream()
                        .collect(java.util.stream.Collectors.toMap(AdmissionApplication::getId, a -> a));

        java.util.Set<Long> stallIds = admissionMap.values().stream()
                .map(AdmissionApplication::getQuarantineStallId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, com.rtms.backend.entity.StableStall> stallMap =
                stableStallRepository.findAllById(stallIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.rtms.backend.entity.StableStall::getId,
                                s -> s));

        return list.stream().map(ts -> {
            com.rtms.backend.entity.CandidateHorseProfile cand = ts.getAdmissionId() == null ? null : candidateMap.get(ts.getAdmissionId());
            AdmissionApplication adm = ts.getAdmissionId() == null ? null : admissionMap.get(ts.getAdmissionId());
            com.rtms.backend.entity.StableStall stall = (adm == null || adm.getQuarantineStallId() == null) ? null : stallMap.get(adm.getQuarantineStallId());
            return TrainerScheduleResponse.from(
                    ts,
                    cand == null ? null : cand.getName(),
                    cand == null ? null : cand.getBreed(),
                    stall == null ? null : stall.getStallCode()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public TrainerScheduleDetailResponse getScheduleDetail(Long scheduleId, Long userId, String role) {
        TrainerSchedule schedule = trainerScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Trainer schedule not found"));

        boolean isAssignedTrainer = "HEAD_TRAINER".equals(role) && Objects.equals(schedule.getTrainerId(), userId);
        boolean isManager = "CLUB_MANAGER".equals(role) || "ADMIN".equals(role);

        if (!isAssignedTrainer && !isManager) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the assigned head trainer can view this schedule");
        }

        AdmissionDetailResponse admission = admissionQueryService.getAdmissionDetail(schedule.getAdmissionId());
        Horse horse = schedule.getHorseId() == null ? null : horseRepository.findById(schedule.getHorseId()).orElse(null);

        List<HealthRecord> healthRecords = horse == null ? List.of()
                : healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        List<HorseHealthMetric> healthMetrics = horse == null ? List.of()
                : healthMetricRepository.findByHorseIdOrderByRecordedAtDesc(horse.getId());

        RacingReadinessAssessment assessment = assessmentRepository.findByTrainerScheduleId(schedule.getId())
                .orElseGet(() -> assessmentRepository.findByAdmissionId(schedule.getAdmissionId()).orElse(null));

        return new TrainerScheduleDetailResponse(
                TrainerScheduleResponse.from(schedule),
                admission,
                horse,
                healthRecords,
                healthMetrics,
                assessment
        );
    }

    @Transactional
    public TrainerScheduleResponse startSchedule(Long scheduleId, Long trainerId) {
        TrainerSchedule schedule = trainerScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Trainer schedule not found"));

        if (!Objects.equals(schedule.getTrainerId(), trainerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned trainer can start this task");
        }

        if (schedule.getStatus() == TrainerScheduleStatus.IN_PROGRESS) {
            return TrainerScheduleResponse.from(schedule);
        }

        if (schedule.getStatus() != TrainerScheduleStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "Only SCHEDULED trainer schedule can be started");
        }

        schedule.setStatus(TrainerScheduleStatus.IN_PROGRESS);
        return TrainerScheduleResponse.from(trainerScheduleRepository.save(schedule));
    }

    @Transactional
    public TrainerScheduleResponse completeSchedule(Long scheduleId, TrainerAdmissionReviewRequest request, Long trainerId) {
        TrainerSchedule schedule = trainerScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Trainer schedule not found"));

        if (!Objects.equals(schedule.getTrainerId(), trainerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the assigned trainer can complete this assessment");
        }

        // Idempotent: nếu đã hoàn thành bởi chính trainer này thì trả về luôn không tạo assessment trùng
        if (schedule.getStatus() == TrainerScheduleStatus.COMPLETED) {
            return TrainerScheduleResponse.from(schedule);
        }

        if (schedule.getStatus() != TrainerScheduleStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "Only IN_PROGRESS trainer schedule can be completed");
        }

        validateReviewRequest(request);

        AdmissionApplication admission = admissionRepository.findByIdForUpdate(schedule.getAdmissionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Associated admission not found"));

        if (admission.getStatus() != AdmissionStatus.TRAINER_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "Admission is not in TRAINER_REVIEW status");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Tạo bản đánh giá RacingReadinessAssessment
        RacingReadinessAssessment assessment = assessmentRepository.findByTrainerScheduleId(schedule.getId())
                .orElseGet(() -> assessmentRepository.findByAdmissionId(schedule.getAdmissionId())
                        .orElseGet(RacingReadinessAssessment::new));

        assessment.setHorseId(schedule.getHorseId());
        assessment.setAdmissionId(schedule.getAdmissionId());
        assessment.setTrainerScheduleId(schedule.getId());
        assessment.setTrainerId(trainerId);
        assessment.setReadinessStatus(request.getReadinessStatus());
        assessment.setConformationScore(request.getConformationScore());
        assessment.setTemperamentScore(request.getTemperamentScore());
        assessment.setGaitQualityScore(request.getGaitQualityScore());
        assessment.setEstimatedMonthsToRace(request.getEstimatedMonthsToRace());
        assessment.setAssessmentDate(LocalDate.now());
        assessment.setRemarks(request.getRemarks());
        assessmentRepository.save(assessment);

        // 2. Chuyển trạng thái TrainerSchedule sang COMPLETED
        schedule.setStatus(TrainerScheduleStatus.COMPLETED);
        schedule.setCompletedAt(now);
        TrainerSchedule completedSchedule = trainerScheduleRepository.save(schedule);

        // 3. Cập nhật AdmissionApplication sang MANAGER_REVIEW
        admission.setTrainerFeedback(request.getRemarks());
        admission.setTrainerReviewedAt(now);
        admission.setStatus(AdmissionStatus.MANAGER_REVIEW);
        admissionRepository.save(admission);

        return TrainerScheduleResponse.from(completedSchedule);
    }

    private void validateReviewRequest(TrainerAdmissionReviewRequest request) {
        if (request == null || request.getReadinessStatus() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Readiness status is required (READY / NEEDS_MORE_TRAINING / UNSUITABLE)");
        }

        validateScore(request.getConformationScore(), "Conformation score");
        validateScore(request.getTemperamentScore(), "Temperament score");
        validateScore(request.getGaitQualityScore(), "Gait quality score");

        if (request.getEstimatedMonthsToRace() != null
                && (request.getEstimatedMonthsToRace() < 0 || request.getEstimatedMonthsToRace() > MAX_MONTHS)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Estimated months must be between 0 and " + MAX_MONTHS);
        }
    }

    private void validateScore(BigDecimal score, String label) {
        if (score == null) return;
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", label + " must be between 0 and 10");
        }
    }
}
