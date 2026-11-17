package com.rtms.backend.admission.service;
import com.rtms.backend.horse.entity.Horse;


import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.admission.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionTrainerReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    private AdmissionTrainerReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdmissionTrainerReviewService(admissionRepository);
    }

    @Test
    @DisplayName("Trainer được phân công nộp nhận xét: lưu vào đơn, đơn chuyển MANAGER_REVIEW")
    void completeReview_success() {
        AdmissionApplication admission = admission(AdmissionStatus.TRAINER_REVIEW, 2L, 20L);
        when(admissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(admission));
        when(admissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.completeReview(10L,
                new TrainerAdmissionReviewRequest("  Good candidate  "), 2L);

        assertEquals(AdmissionStatus.MANAGER_REVIEW, result.getStatus());
        assertEquals("Good candidate", result.getTrainerFeedback());
        assertNotNull(result.getTrainerReviewedAt());
    }

    @Test
    @DisplayName("Trainer khác nộp đánh giá đơn không giao cho mình -> 403, không ghi gì")
    void completeReview_otherTrainer_forbidden() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 5L, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeReview(10L, request(), 9L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(admissionRepository, never()).save(any());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đơn chưa được gán Trainer (chưa có ai đủ điều kiện) -> 409 TRAINER_NOT_ASSIGNED")
    void completeReview_unassigned_conflict() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, null, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeReview(10L, request(), 2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("TRAINER_NOT_ASSIGNED", ex.getErrorCode());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đơn không ở bước TRAINER_REVIEW (vd. đã nộp rồi) -> 409, không ghi đè")
    void completeReview_wrongStatus_conflict() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.MANAGER_REVIEW, 2L, 7L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeReview(10L, request(), 2L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INVALID_REVIEW_STATE", ex.getErrorCode());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đơn chưa gắn Horse -> 409 HORSE_NOT_ASSIGNED")
    void completeReview_noHorse_conflict() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 2L, null)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeReview(10L, request(), 2L));

        assertEquals("HORSE_NOT_ASSIGNED", ex.getErrorCode());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không tìm thấy đơn -> 404")
    void completeReview_notFound() {
        when(admissionRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeReview(10L, request(), 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Nhận xét trống hoặc toàn khoảng trắng -> 400, không đổi đơn")
    void completeReview_blankFeedback_badRequest() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 2L, 7L)));

        for (TrainerAdmissionReviewRequest bad : new TrainerAdmissionReviewRequest[] {
                new TrainerAdmissionReviewRequest(null), new TrainerAdmissionReviewRequest("   ") }) {
            ApiException ex = assertThrows(ApiException.class, () -> service.completeReview(10L, bad, 2L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nhận xét dài quá giới hạn -> 400; đúng giới hạn thì nhận")
    void completeReview_tooLong_badRequest() {
        when(admissionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 2L, 7L)));
        when(admissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String max = "x".repeat(TrainingLimits.MAX_ADMISSION_TRAINER_FEEDBACK_LENGTH);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.completeReview(10L, new TrainerAdmissionReviewRequest(max + "x"), 2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

        assertEquals(max, service.completeReview(10L, new TrainerAdmissionReviewRequest(max), 2L).getTrainerFeedback());
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
        return new TrainerAdmissionReviewRequest("Good candidate");
    }
}
