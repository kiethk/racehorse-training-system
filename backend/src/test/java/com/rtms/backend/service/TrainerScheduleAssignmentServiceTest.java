package com.rtms.backend.service;

import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerScheduleAssignmentServiceTest {

    @Mock
    private TrainerScheduleRepository trainerScheduleRepository;

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    @Mock
    private CareScheduleRepository careScheduleRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HeadTrainerWorkloadService headTrainerWorkloadService;

    @Mock
    private NotificationService notificationService;

    private TrainerScheduleAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new TrainerScheduleAssignmentService(
                trainerScheduleRepository,
                admissionRepository,
                careScheduleRepository,
                horseRepository,
                headTrainerWorkloadService,
                notificationService
        );
    }

    private CareSchedule initialCompletedCare() {
        CareSchedule cs = new CareSchedule();
        cs.setId(100L);
        cs.setCareType(CareType.INITIAL);
        cs.setStatus(CareScheduleStatus.COMPLETED);
        return cs;
    }

    @Test
    @DisplayName("Idempotent: Nếu đơn đã có TrainerSchedule thì không tạo lại")
    void assignForCompletedInitialCare_idempotent_returnsExisting() {
        TrainerSchedule existing = new TrainerSchedule();
        existing.setId(50L);
        existing.setAdmissionId(1L);
        existing.setTrainerId(8L);

        when(trainerScheduleRepository.findByAdmissionId(1L)).thenReturn(Optional.of(existing));

        Optional<TrainerSchedule> result = service.assignForCompletedInitialCare(1L, 100L);

        assertTrue(result.isPresent());
        assertEquals(50L, result.get().getId());
        verify(trainerScheduleRepository, never()).save(any());
        verifyNoInteractions(headTrainerWorkloadService, notificationService);
    }

    @Test
    @DisplayName("Không gán nếu không tìm thấy đơn nhập học")
    void assignForCompletedInitialCare_admissionNotFound_returnsEmpty() {
        when(trainerScheduleRepository.findByAdmissionId(1L)).thenReturn(Optional.empty());
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        Optional<TrainerSchedule> result = service.assignForCompletedInitialCare(1L, 100L);

        assertTrue(result.isEmpty());
        verify(trainerScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không gán nếu đơn chưa ở trạng thái TRAINER_REVIEW")
    void assignForCompletedInitialCare_wrongStatus_returnsEmpty() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setStatus(AdmissionStatus.VET_REVIEW);

        when(trainerScheduleRepository.findByAdmissionId(1L)).thenReturn(Optional.empty());
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        Optional<TrainerSchedule> result = service.assignForCompletedInitialCare(1L, 100L);

        assertTrue(result.isEmpty());
        verify(trainerScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không có Trainer khả dụng: không tạo row, giữ nguyên đơn để scheduler retry sau")
    void assignForCompletedInitialCare_noTrainerAvailable_returnsEmpty() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(20L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);

        when(trainerScheduleRepository.findByAdmissionId(1L)).thenReturn(Optional.empty());
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(initialCompletedCare()));
        when(headTrainerWorkloadService.selectLeastLoadedHeadTrainer(any(), anyInt())).thenReturn(Optional.empty());

        Optional<TrainerSchedule> result = service.assignForCompletedInitialCare(1L, 100L);

        assertTrue(result.isEmpty());
        verify(trainerScheduleRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Gán Trainer thành công: tạo TrainerSchedule SCHEDULED và gửi notification")
    void assignForCompletedInitialCare_success() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(20L);
        admission.setStatus(AdmissionStatus.TRAINER_REVIEW);

        Horse horse = new Horse();
        horse.setId(20L);
        horse.setName("Speedy");

        User trainer = new User();
        trainer.setId(8L);

        when(trainerScheduleRepository.findByAdmissionId(1L)).thenReturn(Optional.empty());
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(initialCompletedCare()));
        when(headTrainerWorkloadService.selectLeastLoadedHeadTrainer(any(), anyInt())).thenReturn(Optional.of(trainer));
        when(horseRepository.findById(20L)).thenReturn(Optional.of(horse));
        when(trainerScheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> {
            TrainerSchedule ts = inv.getArgument(0);
            ts.setId(55L);
            return ts;
        });

        Optional<TrainerSchedule> result = service.assignForCompletedInitialCare(1L, 100L);

        assertTrue(result.isPresent());
        ArgumentCaptor<TrainerSchedule> captor = ArgumentCaptor.forClass(TrainerSchedule.class);
        verify(trainerScheduleRepository).save(captor.capture());

        TrainerSchedule saved = captor.getValue();
        assertEquals(8L, saved.getTrainerId());
        assertEquals(20L, saved.getHorseId());
        assertEquals(1L, saved.getAdmissionId());
        assertEquals(100L, saved.getSourceCareScheduleId());
        assertEquals(TrainerScheduleStatus.SCHEDULED, saved.getStatus());
        assertEquals(30, saved.getDurationMinutes());
        assertNotNull(saved.getScheduledAt());

        verify(notificationService).sendAssignmentNotification(
                eq(8L),
                eq(NotificationTypes.REFERENCE_TRAINER_SCHEDULE),
                eq(55L),
                eq(20L),
                eq(NotificationTypes.TRAINER_SCHEDULE_ASSIGNED),
                anyString(),
                contains("Speedy")
        );
    }

    @Test
    @DisplayName("retryPendingAssignments: quét các đơn pending và thử gán")
    void retryPendingAssignments_processesPendingAdmissions() {
        AdmissionApplication admission1 = new AdmissionApplication();
        admission1.setId(1L);
        admission1.setHorseId(10L);
        admission1.setStatus(AdmissionStatus.TRAINER_REVIEW);

        User trainer = new User();
        trainer.setId(5L);

        when(admissionRepository.findPendingTrainerScheduleAdmissions()).thenReturn(List.of(admission1));
        when(trainerScheduleRepository.findByAdmissionId(1L)).thenReturn(Optional.empty());
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission1));
        when(careScheduleRepository.findById(100L)).thenReturn(Optional.of(initialCompletedCare()));
        when(headTrainerWorkloadService.selectLeastLoadedHeadTrainer(any(), anyInt())).thenReturn(Optional.of(trainer));
        when(horseRepository.findById(10L)).thenReturn(Optional.empty());
        when(careScheduleRepository.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(1L, CareType.INITIAL))
                .thenReturn(Optional.of(initialCompletedCare()));
        when(trainerScheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> {
            TrainerSchedule ts = inv.getArgument(0);
            ts.setId(60L);
            return ts;
        });

        service.retryPendingAssignments();

        verify(trainerScheduleRepository).save(any(TrainerSchedule.class));
    }
}
