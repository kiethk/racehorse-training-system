package com.rtms.backend.service;

import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.RacingReadinessAssessment;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.RacingReadinessStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.RacingReadinessAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionTrainerReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    @Mock
    private RacingReadinessAssessmentRepository assessmentRepository;

    private AdmissionTrainerReviewService trainerReviewService;

    @BeforeEach
    void setUp() {
        trainerReviewService = new AdmissionTrainerReviewService(admissionRepository, assessmentRepository);
    }

    @Test
    @DisplayName("Trainer hoàn thành đánh giá thành công: lưu assessment và chuyển đơn sang MANAGER_REVIEW")
    void testCompleteAssessment_Success() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(7L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));
        when(assessmentRepository.save(any(RacingReadinessAssessment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(admissionRepository.save(any(AdmissionApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.NEEDS_MORE_TRAINING);
        req.setConformationScore(new BigDecimal("7.5"));
        req.setTemperamentScore(new BigDecimal("8.0"));
        req.setGaitQualityScore(new BigDecimal("6.5"));
        req.setEstimatedMonthsToRace(4);
        req.setRemarks("Dáng vóc cân đối, bước đi đều.");

        AdmissionApplication result = trainerReviewService.completeAssessment(10L, req, 2L);

        assertEquals(AdmissionStatus.MANAGER_REVIEW, result.getStatus());
        assertEquals(2L, result.getTrainerId());
        assertEquals("Dáng vóc cân đối, bước đi đều.", result.getTrainerFeedback());
        assertNotNull(result.getTrainerReviewedAt());

        ArgumentCaptor<RacingReadinessAssessment> captor = ArgumentCaptor.forClass(RacingReadinessAssessment.class);
        verify(assessmentRepository).save(captor.capture());
        RacingReadinessAssessment savedAssessment = captor.getValue();

        assertEquals(7L, savedAssessment.getHorseId());
        assertEquals(10L, savedAssessment.getAdmissionId());
        assertEquals(2L, savedAssessment.getTrainerId());
        assertEquals(RacingReadinessStatus.NEEDS_MORE_TRAINING, savedAssessment.getReadinessStatus());
        assertEquals(new BigDecimal("7.5"), savedAssessment.getConformationScore());
        assertEquals(new BigDecimal("8.0"), savedAssessment.getTemperamentScore());
        assertEquals(new BigDecimal("6.5"), savedAssessment.getGaitQualityScore());
        assertEquals(4, savedAssessment.getEstimatedMonthsToRace());
        assertNull(savedAssessment.getFitnessScore());
        assertNull(savedAssessment.getValidUntil());
    }

    @Test
    @DisplayName("Chặn đánh giá nếu đơn không ở bước TRAINER_REVIEW")
    void testCompleteAssessment_WrongStatus_ThrowsException() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admission.setHorseId(7L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> trainerReviewService.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("không phải TRAINER_REVIEW"));
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chặn đánh giá nếu đơn chưa được tạo Horse (horseId == null)")
    void testCompleteAssessment_NoHorseId_ThrowsException() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(null);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> trainerReviewService.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("Đơn chưa gắn hồ sơ chiến mã"));
        verify(assessmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chặn đánh giá nếu thiếu readinessStatus")
    void testCompleteAssessment_MissingReadinessStatus_ThrowsException() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(7L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trainerReviewService.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("Phải chọn mức độ sẵn sàng"));
    }

    @Test
    @DisplayName("Chặn đánh giá nếu điểm số vượt quá 10 hoặc nhỏ hơn 0")
    void testCompleteAssessment_InvalidScore_ThrowsException() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(7L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);
        req.setConformationScore(new BigDecimal("11.0")); // > 10

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trainerReviewService.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("Điểm dáng vóc phải từ 0 đến 10"));
    }

    @Test
    @DisplayName("Chặn đánh giá nếu estimatedMonthsToRace vượt quá 60")
    void testCompleteAssessment_InvalidEstimatedMonths_ThrowsException() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(7L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);
        req.setEstimatedMonthsToRace(65); // > 60

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trainerReviewService.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("Ước tính thời gian phải từ 0 đến 60 tháng"));
    }
}