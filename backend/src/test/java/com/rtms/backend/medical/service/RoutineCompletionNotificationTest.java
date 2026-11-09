package com.rtms.backend.medical.service;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.medical.dto.CompleteCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingGroupService;
import com.rtms.backend.training.service.TrainingDecisionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import com.rtms.backend.medical.dto.HorseHealthMetricRequest;
import com.rtms.backend.medical.enums.HydrationStatus;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutineCompletionNotificationTest {

    private CareScheduleRepository schedules;
    private HorseRepository horses;
    private NotificationService notifications;
    private InjuryRecordRepository injuryRecordRepository;
    private TreatmentPlanRepository treatmentPlanRepository;
    private HealthRecordRepository healthRecords;
    private CareScheduleService service;

    private Horse horse;
    private CareSchedule routineSchedule;

    @BeforeEach
    void setUp() {
        schedules = mock(CareScheduleRepository.class);
        horses = mock(HorseRepository.class);
        notifications = mock(NotificationService.class);
        injuryRecordRepository = mock(InjuryRecordRepository.class);
        treatmentPlanRepository = mock(TreatmentPlanRepository.class);
        healthRecords = mock(HealthRecordRepository.class);
        when(healthRecords.save(any(HealthRecord.class))).thenAnswer(inv -> {
            HealthRecord hr = inv.getArgument(0);
            hr.setId(99L);
            return hr;
        });
        TrainingGroupService groupSync = mock(TrainingGroupService.class);

        TrainingDecisionService trainingDecisionService = new TrainingDecisionService(horses, schedules, groupSync);

        service = new CareScheduleService(
                schedules,
                horses,
                healthRecords,
                mock(HorseHealthMetricRepository.class),
                mock(AdmissionApplicationRepository.class),
                mock(StableStallRepository.class),
                mock(UserRepository.class),
                mock(HorseIncidentReportRepository.class),
                mock(VeterinarianProfileRepository.class),
                mock(AuditLogRepository.class),
                mock(ApplicationEventPublisher.class),
                mock(EntityManager.class),
                notifications,
                trainingDecisionService,
                mock(PreventiveVaccinationService.class),
                injuryRecordRepository,
                treatmentPlanRepository,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );

        horse = new Horse();
        horse.setId(50L);
        horse.setName("Wild Cherry");
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        routineSchedule = new CareSchedule();
        routineSchedule.setId(200L);
        routineSchedule.setHorseId(50L);
        routineSchedule.setCareType(CareType.ROUTINE);
        routineSchedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        routineSchedule.setVeterinarianId(10L);

        when(schedules.findById(200L)).thenReturn(Optional.of(routineSchedule));
        when(schedules.findByIdForUpdate(200L)).thenReturn(Optional.of(routineSchedule));
        when(schedules.save(any(CareSchedule.class))).thenAnswer(inv -> {
            CareSchedule s = inv.getArgument(0);
            if (s.getId() == null) s.setId(301L);
            return s;
        });
        when(schedules.saveAndFlush(any(CareSchedule.class))).thenAnswer(inv -> {
            CareSchedule s = inv.getArgument(0);
            if (s.getId() == null) s.setId(300L);
            return s;
        });
        when(horses.findById(50L)).thenReturn(Optional.of(horse));
        when(horses.findByIdForUpdate(50L)).thenReturn(Optional.of(horse));
        when(horses.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Completing a routine examination notifies horse's assigned trainer")
    void notifiesTrainerOnRoutineCompletion() {
        Long trainerId = 77L;
        when(horses.findTrainerIdByHorseId(50L)).thenReturn(Optional.of(trainerId));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
                req.setTrainingDecision(TrainingDecision.ALLOWED);
        req.setDiagnosis("Routine check healthy");
        req.setFindings("All clear");
        HorseHealthMetricRequest metric = new HorseHealthMetricRequest();
        metric.setTemperature(new BigDecimal("37.8"));
        metric.setHeartRate(new BigDecimal("36"));
        metric.setRespiratoryRate(new BigDecimal("12"));
        metric.setWeight(new BigDecimal("480.0"));
        metric.setBodyConditionScore(new BigDecimal("5"));
        metric.setHydrationStatus(HydrationStatus.NORMAL);
        req.setMetrics(List.of(metric));

        service.completeCareSchedule(200L, req, 10L);

        verify(notifications).sendAssignmentNotification(
                eq(trainerId),
                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE),
                eq(200L),
                eq(50L),
                eq(NotificationTypes.CARE_SCHEDULE_ROUTINE_COMPLETED),
                eq("Routine Examination Completed: Wild Cherry"),
                org.mockito.ArgumentMatchers.contains("Cleared for training (ALLOWED)")
        );
    }

    @Test
    @DisplayName("Completing routine with BLOCKED decision recovers targeted condition and notifies trainer")
    void resolvesTargetedConditionOnRoutineCompletion() {
        Long trainerId = 77L;
        when(horses.findTrainerIdByHorseId(50L)).thenReturn(Optional.of(trainerId));

        InjuryRecord oldInjury = new InjuryRecord();
        oldInjury.setId(101L);
        oldInjury.setHorseId(50L);
        oldInjury.setStatus("ACTIVE");
        oldInjury.setHealthRecordId(999L);
        when(injuryRecordRepository.findById(101L)).thenReturn(Optional.of(oldInjury));

        TreatmentPlan plan = new TreatmentPlan();
        plan.setId(500L);
        plan.setStatus("ACTIVE");
        when(treatmentPlanRepository.findByHealthRecordIdAndStatusIgnoreCase(999L, "ACTIVE"))
                .thenReturn(List.of(plan));

        CompleteCareScheduleRequest req = new CompleteCareScheduleRequest();
                req.setTrainingDecision(TrainingDecision.ALLOWED);
        req.setDiagnosis("Injury healed");
        req.setFindings("No swelling");
        HorseHealthMetricRequest metric = new HorseHealthMetricRequest();
        metric.setTemperature(new BigDecimal("37.8"));
        metric.setHeartRate(new BigDecimal("36"));
        metric.setRespiratoryRate(new BigDecimal("12"));
        metric.setWeight(new BigDecimal("480.0"));
        metric.setBodyConditionScore(new BigDecimal("5"));
        metric.setHydrationStatus(HydrationStatus.NORMAL);
        req.setMetrics(List.of(metric));
        req.setResolvedConditionIds(List.of(101L));

        service.completeCareSchedule(200L, req, 10L);

        verify(injuryRecordRepository).save(oldInjury);
        org.junit.jupiter.api.Assertions.assertEquals("RECOVERED", oldInjury.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", plan.getStatus());

        verify(notifications).sendAssignmentNotification(
                eq(trainerId),
                eq(NotificationTypes.REFERENCE_CARE_SCHEDULE),
                eq(200L),
                eq(50L),
                eq(NotificationTypes.CARE_SCHEDULE_ROUTINE_COMPLETED),
                eq("Routine Examination Completed: Wild Cherry"),
                org.mockito.ArgumentMatchers.contains("Cleared for training (ALLOWED)")
        );
    }
}
