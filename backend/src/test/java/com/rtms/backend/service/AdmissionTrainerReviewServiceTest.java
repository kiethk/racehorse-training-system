package com.rtms.backend.service;

import com.rtms.backend.dto.TrainerAdmissionReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.RacingReadinessStatus;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionTrainerReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;

    @Mock
    private TrainerScheduleService trainerScheduleService;

    private AdmissionTrainerReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdmissionTrainerReviewService(
                admissionRepository,
                trainerScheduleRepository,
                trainerScheduleService);
    }

    @Test
    @DisplayName("Trainer hoàn thành đánh giá qua Admission delegation thành công")
    void testCompleteAssessment_Success() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(100L);
        schedule.setAdmissionId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.IN_PROGRESS);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(20L);

        AdmissionApplication updatedAdmission = new AdmissionApplication();
        updatedAdmission.setId(10L);
        updatedAdmission.setStatus(AdmissionStatus.MANAGER_REVIEW);
        updatedAdmission.setTrainerFeedback("Good candidate");

        when(admissionRepository.findById(10L))
                .thenReturn(Optional.of(admission))
                .thenReturn(Optional.of(updatedAdmission));
        when(trainerScheduleRepository.findByAdmissionId(10L))
                .thenReturn(Optional.of(schedule));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);
        req.setConformationScore(new BigDecimal("8.0"));
        req.setRemarks("Good candidate");

        AdmissionApplication result = service.completeAssessment(10L, req, 2L);

        verify(trainerScheduleService).completeSchedule(100L, req, 2L);
        assertEquals(AdmissionStatus.MANAGER_REVIEW, result.getStatus());
        assertEquals("Good candidate", result.getTrainerFeedback());
    }

    @Test
    @DisplayName("Trainer hoàn thành đánh giá khi schedule đang ở trạng thái SCHEDULED sẽ tự kích hoạt startSchedule")
    void testCompleteAssessment_ScheduledStatus_StartsSchedule() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(100L);
        schedule.setAdmissionId(10L);
        schedule.setTrainerId(2L);
        schedule.setStatus(TrainerScheduleStatus.SCHEDULED);

        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(20L);

        when(admissionRepository.findById(10L))
                .thenReturn(Optional.of(admission))
                .thenReturn(Optional.of(admission));
        when(trainerScheduleRepository.findByAdmissionId(10L))
                .thenReturn(Optional.of(schedule));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);

        service.completeAssessment(10L, req, 2L);

        verify(trainerScheduleService).startSchedule(100L, 2L);
        verify(trainerScheduleService).completeSchedule(100L, req, 2L);
    }

    @Test
    @DisplayName("Chặn Trainer đánh giá đơn đã phân công cho Trainer khác trong TrainerSchedule (403)")
    void testCompleteAssessment_AssignedToAnotherTrainer_ThrowsForbidden() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(7L);

        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(100L);
        schedule.setAdmissionId(10L);
        schedule.setTrainerId(5L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));
        when(trainerScheduleRepository.findByAdmissionId(10L)).thenReturn(Optional.of(schedule));

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.completeAssessment(10L, req, 9L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Chỉ Trainer được phân công"));
        verifyNoInteractions(trainerScheduleService);
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
                () -> service.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("không phải TRAINER_REVIEW"));
        verifyNoInteractions(trainerScheduleService);
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
                () -> service.completeAssessment(10L, req, 2L));

        assertTrue(ex.getMessage().contains("Đơn chưa gắn hồ sơ chiến mã"));
        verifyNoInteractions(trainerScheduleService);
    }

    @Test
    @DisplayName("Chặn đánh giá nếu không tìm thấy TrainerSchedule cho đơn (404)")
    void testCompleteAssessment_NoSchedule_ThrowsNotFound() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(10L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);
        admission.setHorseId(7L);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));
        when(trainerScheduleRepository.findByAdmissionId(10L))
                .thenReturn(Optional.empty());

        TrainerAdmissionReviewRequest req = new TrainerAdmissionReviewRequest();
        req.setReadinessStatus(RacingReadinessStatus.READY);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.completeAssessment(10L, req, 2L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(trainerScheduleService);
    }
}