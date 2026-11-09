package com.rtms.backend.medical.service;

import java.time.LocalDate;
import java.time.LocalDateTime;



import com.rtms.backend.admission.repository.AdmissionApplicationRepository;

import com.rtms.backend.audit.repository.AuditLogRepository;

import com.rtms.backend.horse.entity.Horse;

import com.rtms.backend.horse.repository.HorseRepository;

import com.rtms.backend.identity.repository.UserRepository;

import com.rtms.backend.identity.repository.VeterinarianProfileRepository;

import com.rtms.backend.incident.repository.HorseIncidentReportRepository;

import com.rtms.backend.medical.entity.CareSchedule;

import com.rtms.backend.medical.enums.CareSchedulePurpose;

import com.rtms.backend.medical.enums.CareScheduleStatus;

import com.rtms.backend.medical.enums.CareType;

import com.rtms.backend.medical.enums.RescheduleReason;

import com.rtms.backend.medical.repository.CareScheduleRepository;

import com.rtms.backend.medical.repository.HealthRecordRepository;

import com.rtms.backend.medical.repository.HorseHealthMetricRepository;

import com.rtms.backend.medical.repository.InjuryRecordRepository;

import com.rtms.backend.medical.repository.PrescriptionRepository;

import com.rtms.backend.medical.repository.TreatmentPlanRepository;

import com.rtms.backend.notification.service.NotificationService;

import com.rtms.backend.notification.service.NotificationTypes;

import com.rtms.backend.stable.repository.StableStallRepository;

import com.rtms.backend.training.service.TrainingDecisionService;

import com.rtms.backend.training.service.TrainingGroupService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;

import org.mockito.quality.Strictness;

import org.springframework.context.ApplicationEventPublisher;



import java.time.LocalDateTime;

import java.util.List;

import java.util.Optional;



import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.*;



/** D1 — job lifecycle sweep (ROADMAP v2.2 §4.5) với clock cố định. */

@ExtendWith(MockitoExtension.class)

@MockitoSettings(strictness = Strictness.LENIENT)

class CareScheduleLifecycleSweepTest {



    @Mock private CareScheduleRepository careScheduleRepository;

    @Mock private HorseRepository horseRepository;

    @Mock private HealthRecordRepository healthRecordRepository;

    @Mock private HorseHealthMetricRepository metricRepository;

    @Mock private AdmissionApplicationRepository admissionRepository;

    @Mock private StableStallRepository stallRepository;

    @Mock private UserRepository userRepository;

    @Mock private HorseIncidentReportRepository incidentReportRepository;

    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;

    @Mock private AuditLogRepository auditLogRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private EntityManager entityManager;

    @Mock private NotificationService notificationService;

    @Mock private TrainingGroupService groupSync;

    @Mock private PreventiveVaccinationService preventiveVaccinationService;

    @Mock private InjuryRecordRepository injuryRecordRepository;

    @Mock private TreatmentPlanRepository treatmentPlanRepository;



    private CareScheduleService service;



    private static final LocalDateTime TODAY_0900 = LocalDateTime.of(2026, 11, 1, 9, 0);



    @BeforeEach

    void setUp() {

        service = new CareScheduleService(

                careScheduleRepository, horseRepository, healthRecordRepository, metricRepository,

                admissionRepository, stallRepository, userRepository, incidentReportRepository,

                veterinarianProfileRepository, auditLogRepository, eventPublisher, entityManager,

                notificationService,

                new TrainingDecisionService(horseRepository, careScheduleRepository, groupSync),

                preventiveVaccinationService, injuryRecordRepository,

                treatmentPlanRepository, new com.fasterxml.jackson.databind.ObjectMapper());

        Horse horse = new Horse();

        horse.setId(10L);

        horse.setName("Test Horse");

        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

    }



    private CareSchedule schedule(Long id, CareType type, CareScheduleStatus status, LocalDateTime scheduledAt) {

        CareSchedule cs = new CareSchedule();

        cs.setId(id);

        cs.setHorseId(10L);

        cs.setVeterinarianId(20L);

        cs.setCareType(type);

        cs.setPurpose(CareSchedulePurpose.FOLLOW_UP);

        cs.setStatus(status);

        cs.setScheduledAt(scheduledAt);

        cs.setDurationMinutes(30);

        return cs;

    }



    @Test
    @DisplayName("Trong giờ làm việc trước 15:30: không rollover EOD hay escalate")
    void workHours_beforeWindowEnd_doesNotRollOverEod() {
        service.runLifecycleSweep(TODAY_0900);

        verify(careScheduleRepository, never()).findRollableEodCandidates(any(), any());
        verify(careScheduleRepository, never()).findStaleUrgentForEscalation(any(), any());
    }



    @Test

    @DisplayName("07:00 ngoài giờ làm: không sweep, không rollover")

    void beforeWorkday_doesNothing() {

        service.runLifecycleSweep(LocalDateTime.of(2026, 11, 1, 7, 0));

        verify(careScheduleRepository, never()).findRollableEodCandidates(any(), any());

    }



    @Test

    @DisplayName("17:01: INITIAL IN_PROGRESS hôm nay → SCHEDULED T+1 08:00, reason EOD_ROLLOVER, notify vet")

    void eod_rollsInitialIntoNextWorkingDay() {

        CareSchedule initial = schedule(100L, CareType.INITIAL, CareScheduleStatus.IN_PROGRESS,

                LocalDateTime.of(2026, 11, 1, 9, 0));

        when(careScheduleRepository.findRollableEodCandidates(any(), any()))

                .thenReturn(List.of(initial));

        when(careScheduleRepository.findScheduledForHorse(10L, CareScheduleStatus.SCHEDULED))

                .thenReturn(List.of());

        when(careScheduleRepository.findAssignedInDay(any(), any(), any())).thenReturn(List.of());

        when(careScheduleRepository.findStaleUrgentForEscalation(any(), any())).thenReturn(List.of());



        service.runLifecycleSweep(LocalDateTime.of(2026, 11, 1, 17, 1));



        assertEquals(CareScheduleStatus.SCHEDULED, initial.getStatus());

        assertEquals(LocalDate.of(2026, 11, 2), initial.getScheduledDate());
        assertNull(initial.getScheduledAt());

        assertEquals(RescheduleReason.TIME_EXCEEDED, initial.getRescheduleReason());

        assertNotNull(initial.getRescheduleNote());

        assertTrue(initial.getRescheduleNote().contains("(was in progress)"));

        verify(notificationService).sendAssignmentNotification(eq(20L),

                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE), eq(100L), eq(10L),

                eq(NotificationTypes.CARE_SCHEDULE_EOD_ROLLED), anyString(), anyString());

    }



    @Test

    @DisplayName("17:01: ROUTINE chưa xong → khe 13:30 T+1 (khung routine là ưu tiên, không đổi)")

    void eod_keepsRoutineInsideTheWindow() {

        CareSchedule routine = schedule(101L, CareType.ROUTINE, CareScheduleStatus.SCHEDULED,

                LocalDateTime.of(2026, 11, 1, 14, 0));

        when(careScheduleRepository.findRollableEodCandidates(any(), any()))

                .thenReturn(List.of(routine));

        when(careScheduleRepository.findScheduledForHorse(10L, CareScheduleStatus.SCHEDULED))

                .thenReturn(List.of());

        when(careScheduleRepository.findAssignedInDay(any(), any(), any())).thenReturn(List.of());

        when(careScheduleRepository.findStaleUrgentForEscalation(any(), any())).thenReturn(List.of());



        service.runLifecycleSweep(LocalDateTime.of(2026, 11, 1, 17, 1));



        assertEquals(LocalDate.of(2026, 11, 2), routine.getScheduledDate());
        assertNull(routine.getScheduledAt());

        assertEquals(CareScheduleStatus.SCHEDULED, routine.getStatus());

    }



    @Test

    @DisplayName("17:01: URGENT quá hạn KHÔNG dời — chỉ escalate notification, trạng thái giữ nguyên")

    void eod_urgentEscalatesWithoutMoving() {

        when(careScheduleRepository.findRollableEodCandidates(any(), any())).thenReturn(List.of());

        CareSchedule urgent = schedule(102L, CareType.URGENT, CareScheduleStatus.IN_PROGRESS,

                LocalDateTime.of(2026, 10, 31, 20, 0));

        when(careScheduleRepository.findStaleUrgentForEscalation(any(), any())).thenReturn(List.of(urgent));



        service.runLifecycleSweep(LocalDateTime.of(2026, 11, 1, 17, 1));



        assertEquals(CareScheduleStatus.IN_PROGRESS, urgent.getStatus());

        assertEquals(LocalDateTime.of(2026, 10, 31, 20, 0), urgent.getScheduledAt());

        verify(careScheduleRepository, never()).save(urgent);

        verify(notificationService).sendAssignmentNotification(eq(20L),

                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE), eq(102L), eq(10L),

                eq(NotificationTypes.CARE_SCHEDULE_URGENT_ESCALATED), anyString(), anyString());

    }



    @Test

    @DisplayName("nextEodSlot: vet đã kín 08:00 T+1 → rơi xuống 08:30; ngựa đã hẹn 08:00–08:30 cũng né")

    void nextSlot_skipsBusyVetAndHorse() {

        CareSchedule initial = schedule(103L, CareType.INITIAL, CareScheduleStatus.SCHEDULED,

                LocalDateTime.of(2026, 11, 1, 8, 0));

        CareSchedule vetBusy = schedule(7L, CareType.ROUTINE, CareScheduleStatus.SCHEDULED,

                LocalDateTime.of(2026, 11, 2, 8, 0));

        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(10L), any()))

                .thenReturn(List.of());

        when(careScheduleRepository.findAssignedInDay(any(), any(), any()))

                .thenReturn(List.of(vetBusy));



        LocalDateTime slot = service.nextEodSlot(initial, LocalDateTime.of(2026, 11, 1, 17, 1));



        assertEquals(LocalDateTime.of(2026, 11, 2, 8, 30), slot);



        CareSchedule horseBusy = schedule(8L, CareType.ROUTINE, CareScheduleStatus.SCHEDULED,

                LocalDateTime.of(2026, 11, 2, 8, 30));

        when(careScheduleRepository.findByHorseIdAndStatusIn(eq(10L), any()))

                .thenReturn(List.of(horseBusy));

        slot = service.nextEodSlot(initial, LocalDateTime.of(2026, 11, 1, 17, 1));

        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 0), slot);

    }



    @Test

    @DisplayName("Cờ dẫn xuất overdue: IN_PROGRESS quá 30' → true; SCHEDULED/under-30' → false")

    void overdueFlag_isDerivedNotStored() {

        CareSchedule late = schedule(105L, CareType.ROUTINE, CareScheduleStatus.IN_PROGRESS,

                LocalDateTime.now().minusMinutes(40));

        assertTrue(com.rtms.backend.medical.dto.CareScheduleResponse.from(late).overdue());



        CareSchedule fresh = schedule(106L, CareType.ROUTINE, CareScheduleStatus.IN_PROGRESS,

                LocalDateTime.now().minusMinutes(10));

        assertFalse(com.rtms.backend.medical.dto.CareScheduleResponse.from(fresh).overdue());



        CareSchedule waiting = schedule(107L, CareType.ROUTINE, CareScheduleStatus.SCHEDULED,

                LocalDateTime.now().minusMinutes(90));

        assertFalse(com.rtms.backend.medical.dto.CareScheduleResponse.from(waiting).overdue());

    }



    @Test

    @DisplayName("EOD_ROLLOVER ghi vào entity → cột DB nhận giá trị mới (enum mapping)")

    void rolloverReason_persistsViaSave() {

        CareSchedule initial = schedule(104L, CareType.INITIAL, CareScheduleStatus.SCHEDULED,

                LocalDateTime.of(2026, 11, 1, 9, 0));

        when(careScheduleRepository.findRollableEodCandidates(any(), any())).thenReturn(List.of(initial));

        when(careScheduleRepository.findScheduledForHorse(anyLong(), any())).thenReturn(List.of());

        when(careScheduleRepository.findAssignedInDay(any(), any(), any())).thenReturn(List.of());

        when(careScheduleRepository.findStaleUrgentForEscalation(any(), any())).thenReturn(List.of());



        service.runLifecycleSweep(LocalDateTime.of(2026, 11, 1, 18, 0));



        ArgumentCaptor<CareSchedule> saved = ArgumentCaptor.forClass(CareSchedule.class);

        verify(careScheduleRepository).save(saved.capture());

        assertEquals(RescheduleReason.TIME_EXCEEDED, saved.getValue().getRescheduleReason());

    }

}

