package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
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
import org.springframework.http.HttpStatus;

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

    private AdmissionTrainerReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdmissionTrainerReviewService(admissionRepository, assessmentRepository);
    }

    @Test
    @DisplayName("Trainer được phân công nộp đánh giá: lưu assessment, đơn chuyển MANAGER_REVIEW")
    void completeAssessment_success() {
        AdmissionApplication admission = admission(AdmissionStatus.TRAINER_REVIEW, 2L, 20L);
        when(admissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(admission));
        when(admissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TrainerAdmissionReviewRequest req = request();
        req.setConformationScore(new BigDecimal("8.0"));
        req.setEstimatedMonthsToRace(6);
        req.setRemarks("Good candidate");

        AdmissionApplication result = service.completeAssessment(10L, req, 2L);

        assertEquals(AdmissionStatus.MANAGER_REVIEW, result.getStatus());
        assertEquals("Good candidate", result.getTrainerFeedback());
        assertNotNull(result.getTrainerReviewedAt());

        ArgumentCaptor<RacingReadinessAssessment> captor = ArgumentCaptor.forClass(RacingReadinessAssessment.class);
        verify(assessmentRepository).save(captor.capture());
        RacingReadinessAssessment saved = captor.getValue();
        assertEquals(20L, saved.getHorseId());
        assertEquals(10L, saved.getAdmissionId());
        assertEquals(2L, saved.getTrainerId());
        assertEquals(RacingReadinessStatus.READY, saved.getReadinessStatus());
        assertEquals(new BigDecimal("8.0"), saved.getConformationScore());
    }

    @Test
    @DisplayName("Trainer khác nộp đánh giá đơn không giao cho mình -> 403, không ghi gì")
    void completeAssessment_otherTrainer_forbidden() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 5L, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, request(), 9L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verifyNoInteractions(assessmentRepository);
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đơn chưa được gán Trainer (chưa có ai đủ điều kiện) -> 409 TRAINER_NOT_ASSIGNED")
    void completeAssessment_unassigned_conflict() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, null, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, request(), 2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("TRAINER_NOT_ASSIGNED", ex.getErrorCode());
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    @DisplayName("Đơn không ở bước TRAINER_REVIEW (vd. đã nộp rồi) -> 409, không ghi đè")
    void completeAssessment_wrongStatus_conflict() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.MANAGER_REVIEW, 2L, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, request(), 2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INVALID_REVIEW_STATE", ex.getErrorCode());
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    @DisplayName("Đơn chưa gắn Horse -> 409 HORSE_NOT_ASSIGNED")
    void completeAssessment_noHorse_conflict() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 2L, null)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, request(), 2L));

        assertEquals("HORSE_NOT_ASSIGNED", ex.getErrorCode());
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    @DisplayName("Không tìm thấy đơn -> 404")
    void completeAssessment_notFound() {
        when(admissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, request(), 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Thiếu mức độ sẵn sàng -> 400")
    void completeAssessment_missingReadiness_badRequest() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 2L, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, new TrainerAdmissionReviewRequest(), 2L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    @DisplayName("Điểm ngoài 0..10 hoặc số tháng ngoài 0..60 -> 400")
    void completeAssessment_outOfRange_badRequest() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 2L, 7L)));

        TrainerAdmissionReviewRequest badScore = request();
        badScore.setTemperamentScore(new BigDecimal("11"));
        ApiException scoreEx = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, badScore, 2L));
        assertTrue(scoreEx.getMessage().contains("Điểm tính nết"));

        TrainerAdmissionReviewRequest badMonths = request();
        badMonths.setEstimatedMonthsToRace(61);
        ApiException monthsEx = assertThrows(ApiException.class,
                () -> service.completeAssessment(10L, badMonths, 2L));
        assertEquals(HttpStatus.BAD_REQUEST, monthsEx.getStatus());

        verifyNoInteractions(assessmentRepository);
    }

    private AdmissionApplication admission(AdmissionStatus status, Long trainerId, Long horseId) {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(status);
        admission.setTrainerId(trainerId);
        admission.setHorseId(horseId);
        return admission;
    }

    private TrainerAdmissionReviewRequest request() {
        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);
        return req;
    }
}
