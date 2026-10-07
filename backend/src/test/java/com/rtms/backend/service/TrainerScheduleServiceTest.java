package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.dto.TrainerScheduleDetailResponse;
import com.rtms.backend.dto.TrainerScheduleResponse;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.RacingReadinessStatus;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerScheduleServiceTest {

    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    @Mock
    private RacingReadinessAssessmentRepository assessmentRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private HorseHealthMetricRepository healthMetricRepository;

    @Mock
    private CandidateHorseProfileRepository candidateProfileRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private AdmissionQueryService admissionQueryService;

    private TrainerScheduleService service;

    @BeforeEach
    void setUp() {
        service = new TrainerScheduleService(
                trainerScheduleRepository,
                admissionRepository,
                assessmentRepository,
                horseRepository,
                healthRecordRepository,
                healthMetricRepository,
                candidateProfileRepository,
                stableStallRepository,
                admissionQueryService
        );
    }

    @Test
    @DisplayName("listSchedules: Trainer lọc theo trainerId thành công")
    void listSchedules_asTrainer_returnsFiltered() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.SCHEDULED);
        when(trainerScheduleRepository.findFiltered(2L, null)).thenReturn(List.of(schedule));

        List<TrainerScheduleResponse> result = service.listSchedules(2L, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    @DisplayName("getScheduleDetail: Trainer khác cố tình xem lịch nhận 403 Forbidden")
    void getScheduleDetail_otherTrainer_throwsForbidden() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        when(trainerScheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getScheduleDetail(10L, 99L, "HEAD_TRAINER"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("getScheduleDetail: Manager hoặc Assigned Trainer xem được chi tiết")
    void getScheduleDetail_assignedTrainer_success() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setAdmissionId(1L);
        schedule.setHorseId(20L);

        when(trainerScheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(horseRepository.findById(20L)).thenReturn(Optional.of(new Horse()));
        when(assessmentRepository.findByTrainerScheduleId(10L)).thenReturn(Optional.empty());
        when(assessmentRepository.findByAdmissionId(1L)).thenReturn(Optional.empty());

        TrainerScheduleDetailResponse detail = service.getScheduleDetail(10L, 2L, "HEAD_TRAINER");

        assertNotNull(detail);
        assertEquals(10L, detail.schedule().id());
    }

    @Test
    @DisplayName("startSchedule: SCHEDULED chuyển sang IN_PROGRESS thành công")
    void startSchedule_success() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.SCHEDULED);

        when(trainerScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));
        when(trainerScheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerScheduleResponse result = service.startSchedule(10L, 2L);

        assertEquals(TrainerScheduleStatus.IN_PROGRESS, result.status());
        assertEquals(TrainerScheduleStatus.IN_PROGRESS, schedule.getStatus());
    }

    @Test
    @DisplayName("startSchedule: Không thể start nếu lịch đã COMPLETED")
    void startSchedule_alreadyCompleted_throwsConflict() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.COMPLETED);

        when(trainerScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.startSchedule(10L, 2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("completeSchedule: Chặn đánh giá nếu thiếu readinessStatus")
    void completeSchedule_missingReadiness_throwsBadRequest() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.IN_PROGRESS);

        when(trainerScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeSchedule(10L, req, 2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("completeSchedule: Chặn đánh giá nếu điểm số vượt quá 10")
    void completeSchedule_scoreOver10_throwsBadRequest() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.IN_PROGRESS);

        when(trainerScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);
        req.setConformationScore(new BigDecimal("11.0"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeSchedule(10L, req, 2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("completeSchedule: Hoàn tất đánh giá thành công, tạo assessment và chuyển Admission sang MANAGER_REVIEW atomically")
    void completeSchedule_success() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(10L);
        schedule.setTrainerId(2L);
        schedule.setHorseId(20L);
        schedule.setAdmissionId(1L);
        schedule.setStatus(TrainerScheduleStatus.IN_PROGRESS);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);

        when(trainerScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(schedule));
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(assessmentRepository.findByTrainerScheduleId(10L)).thenReturn(Optional.empty());
        when(assessmentRepository.save(any(RacingReadinessAssessment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerScheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(admissionRepository.save(any(AdmissionApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);
        req.setConformationScore(new BigDecimal("8.5"));
        req.setTemperamentScore(new BigDecimal("9.0"));
        req.setGaitQualityScore(new BigDecimal("8.0"));
        req.setEstimatedMonthsToRace(2);
        req.setRemarks("Rất tiềm năng");

        TrainerScheduleResponse result = service.completeSchedule(10L, req, 2L);

        assertEquals(TrainerScheduleStatus.COMPLETED, result.status());
        assertEquals(TrainerScheduleStatus.COMPLETED, schedule.getStatus());
        assertNotNull(schedule.getCompletedAt());

        // Assessment verification
        ArgumentCaptor<RacingReadinessAssessment> assessCaptor = ArgumentCaptor.forClass(RacingReadinessAssessment.class);
        verify(assessmentRepository).save(assessCaptor.capture());
        RacingReadinessAssessment savedAssessment = assessCaptor.getValue();
        assertEquals(10L, savedAssessment.getTrainerScheduleId());
        assertEquals(2L, savedAssessment.getTrainerId());
        assertEquals(20L, savedAssessment.getHorseId());
        assertEquals(1L, savedAssessment.getAdmissionId());
        assertEquals(RacingReadinessStatus.READY, savedAssessment.getReadinessStatus());
        assertEquals(new BigDecimal("8.5"), savedAssessment.getConformationScore());

        // Admission verification
        assertEquals(AdmissionStatus.MANAGER_REVIEW, admission.getStatus());
        assertEquals("Rất tiềm năng", admission.getTrainerFeedback());
        assertNotNull(admission.getTrainerReviewedAt());
    }
}
