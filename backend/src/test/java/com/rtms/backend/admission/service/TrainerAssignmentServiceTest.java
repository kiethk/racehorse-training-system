package com.rtms.backend.admission.service;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;


import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.event.InitialExamCompletedEvent;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerAssignmentServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private HeadTrainerWorkloadService workloadService;

    @Mock
    private NotificationService notificationService;

    private TrainerAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new TrainerAssignmentService(admissionRepository, horseRepository, workloadService, notificationService);
    }

    @Test
    @DisplayName("Đơn TRAINER_REVIEW chưa có Trainer: gán người ít việc nhất và gửi thông báo ADMISSION_TRAINER_ASSIGNED")
    void assignIfNeeded_assignsAndNotifies() {
        AdmissionApplication admission = admission(AdmissionStatus.TRAINER_REVIEW, null);
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(workloadService.selectLeastLoadedHeadTrainer()).thenReturn(Optional.of(trainer(8L)));
        Horse horse = new Horse();
        horse.setName("Desert Star");
        when(horseRepository.findById(20L)).thenReturn(Optional.of(horse));

        Optional<Long> result = service.assignIfNeeded(1L);

        assertEquals(Optional.of(8L), result);
        assertEquals(8L, admission.getTrainerId());
        verify(admissionRepository).save(admission);
        verify(notificationService).sendAssignmentNotification(eq(8L), eq(1L), eq(20L),
                eq(NotificationTypes.ADMISSION_TRAINER_ASSIGNED), anyString(), contains("Desert Star"));
    }

    @Test
    @DisplayName("Đơn đã có Trainer: không chọn lại, không gửi thông báo lần hai")
    void assignIfNeeded_alreadyAssigned_isNoOp() {
        when(admissionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(admission(AdmissionStatus.TRAINER_REVIEW, 8L)));

        assertEquals(Optional.of(8L), service.assignIfNeeded(1L));

        verifyNoInteractions(workloadService, notificationService);
        verify(admissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đơn không còn ở TRAINER_REVIEW (đã nộp đánh giá...) thì bỏ qua")
    void assignIfNeeded_wrongStatus_skips() {
        when(admissionRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(admission(AdmissionStatus.MANAGER_REVIEW, null)));

        assertTrue(service.assignIfNeeded(1L).isEmpty());

        verifyNoInteractions(workloadService, notificationService);
    }

    @Test
    @DisplayName("Chưa có Trainer đủ điều kiện: để trống trainerId cho job thử lại, không ghi gì")
    void assignIfNeeded_noEligibleTrainer_leavesUnassigned() {
        AdmissionApplication admission = admission(AdmissionStatus.TRAINER_REVIEW, null);
        when(admissionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(workloadService.selectLeastLoadedHeadTrainer()).thenReturn(Optional.empty());

        assertTrue(service.assignIfNeeded(1L).isEmpty());

        assertNull(admission.getTrainerId());
        verify(admissionRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("findUnassignedAdmissionIds lấy đơn TRAINER_REVIEW chưa có Trainer, đơn cũ trước")
    void findUnassignedAdmissionIds_returnsIdsInOrder() {
        when(admissionRepository.findByStatusAndTrainerIdIsNullOrderBySubmittedAtAscIdAsc(AdmissionStatus.TRAINER_REVIEW))
                .thenReturn(List.of(withId(3L), withId(1L)));

        assertEquals(List.of(3L, 1L), service.findUnassignedAdmissionIds());
    }

    @Test
    @DisplayName("Triggers: lỗi khi gán sau commit chỉ ghi log, không ném ra ngoài")
    void triggers_swallowAssignmentFailures() {
        TrainerAssignmentService failing = mock(TrainerAssignmentService.class);
        when(failing.assignIfNeeded(anyLong())).thenThrow(new RuntimeException("DB down"));
        when(failing.findUnassignedAdmissionIds()).thenReturn(List.of(1L, 2L));
        TrainerAssignmentTriggers triggers = new TrainerAssignmentTriggers(failing);

        assertDoesNotThrow(() -> triggers.onInitialExamCompleted(new InitialExamCompletedEvent(1L)));
        assertDoesNotThrow(triggers::retryPendingAssignments);

        // Đơn đầu lỗi không làm dừng việc gán các đơn còn lại.
        verify(failing).assignIfNeeded(2L);
    }

    private AdmissionApplication admission(AdmissionStatus status, Long trainerId) {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setHorseId(20L);
        admission.setStatus(status);
        admission.setTrainerId(trainerId);
        return admission;
    }

    private AdmissionApplication withId(Long id) {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(id);
        return admission;
    }

    private User trainer(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
