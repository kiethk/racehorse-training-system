package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingDecisionServiceTest {

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private CareScheduleRepository careScheduleRepository;

    @Mock
    private HorseTrainingPlanService trainingPlanService;

    private TrainingDecisionService service;
    private Horse horse;

    @BeforeEach
    void setUp() {
        service = new TrainingDecisionService(horseRepository, careScheduleRepository, trainingPlanService);
        horse = new Horse();
        horse.setId(7L);
        horse.setName("Thunder");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
    }

    @Test
    @DisplayName("block: lưu BLOCKED + lý do và hủy buổi tập tương lai")
    void block_setsDecisionAndCancelsFutureTraining() {
        service.block(horse, "Tendon strain");

        assertEquals(TrainingDecision.BLOCKED, horse.getTrainingDecision());
        assertEquals("Tendon strain", horse.getTrainingDecisionReason());
        verify(horseRepository).save(horse);
        verify(trainingPlanService).cancelFutureTrainingForHorse(7L, "Ngựa bị chặn tập: Tendon strain");
    }

    @Test
    @DisplayName("allow: về ALLOWED, xóa lý do, không đụng tới kế hoạch")
    void allow_clearsReason() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason("Tendon strain");

        service.allow(horse);

        assertEquals(TrainingDecision.ALLOWED, horse.getTrainingDecision());
        assertNull(horse.getTrainingDecisionReason());
        verify(horseRepository).save(horse);
        verifyNoInteractions(trainingPlanService);
    }

    @Test
    @DisplayName("assertCanTrain: ngựa ELIGIBLE + ALLOWED thì qua")
    void assertCanTrain_eligibleAndAllowed_passes() {
        assertDoesNotThrow(() -> service.assertCanTrain(horse));
    }

    @Test
    @DisplayName("assertCanTrain: ngựa chưa được nhận (CANDIDATE) -> 409 HORSE_NOT_ELIGIBLE")
    void assertCanTrain_candidate_rejected() {
        horse.setCurrentStatus(HorseStatus.CANDIDATE);

        ApiException ex = assertThrows(ApiException.class, () -> service.assertCanTrain(horse));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("HORSE_NOT_ELIGIBLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("assertCanTrain: bị chặn -> 409 TRAINING_BLOCKED, nêu lý do và giờ khám lại")
    void assertCanTrain_blocked_showsReasonAndNextExam() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason("Tendon strain");
        when(careScheduleRepository.findNextExamTime(eq(7L), anyList()))
                .thenReturn(LocalDateTime.of(2026, 10, 20, 14, 0));

        ApiException ex = assertThrows(ApiException.class, () -> service.assertCanTrain(horse));

        assertEquals("TRAINING_BLOCKED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Tendon strain"));
        assertTrue(ex.getMessage().contains("khám lại lúc 20/10/2026 14:00"));
    }

    @Test
    @DisplayName("assertCanTrain: bị chặn mà không còn lịch khám -> báo 'chưa có lịch khám lại'")
    void assertCanTrain_blockedWithoutNextExam() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        when(careScheduleRepository.findNextExamTime(eq(7L), anyList())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.assertCanTrain(horse));

        assertTrue(ex.getMessage().contains("chưa có lịch khám lại"));
        assertTrue(ex.getMessage().contains("theo chỉ định của Thú y"));
    }
}
