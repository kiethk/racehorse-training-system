package com.rtms.backend.medical.service;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.service.TrainingDecisionService;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.ClinicalMedicationRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CareScheduleDateFilterTest {

    @Mock
    private CareScheduleRepository careScheduleRepository;

    private CareScheduleService careScheduleService;

    @Captor
    private ArgumentCaptor<LocalDateTime> dayStartCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> dayEndCaptor;

    @BeforeEach
    void setUp() {
        careScheduleService = new CareScheduleService(
                careScheduleRepository, mock(HorseRepository.class), mock(HealthRecordRepository.class), mock(HorseHealthMetricRepository.class),
                mock(AdmissionApplicationRepository.class), mock(StableStallRepository.class), mock(UserRepository.class),
                mock(HorseIncidentReportRepository.class), mock(VeterinarianProfileRepository.class), mock(AuditLogRepository.class),
                mock(ApplicationEventPublisher.class), mock(EntityManager.class), mock(NotificationService.class),
                mock(TrainingDecisionService.class), mock(PreventiveVaccinationService.class), mock(InjuryRecordRepository.class),
                mock(TreatmentPlanRepository.class), new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    void listSchedules_withDate_computesDayBoundariesAndPassesToRepository() {
        LocalDate filterDate = LocalDate.of(2026, 10, 18);
        Pageable pageable = PageRequest.of(0, 10);
        Page<CareSchedule> emptyPage = new PageImpl<>(List.of());

        when(careScheduleRepository.findFiltered(
                eq(CareScheduleStatus.SCHEDULED),
                eq(CareType.ROUTINE),
                eq(1L),
                eq(2L),
                eq((Long) null),
                eq(3L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(pageable)
        )).thenReturn(emptyPage);

        careScheduleService.listSchedules(
                CareScheduleStatus.SCHEDULED,
                CareType.ROUTINE,
                1L,
                2L,
                3L,
                filterDate,
                pageable
        );

        verify(careScheduleRepository).findFiltered(
                eq(CareScheduleStatus.SCHEDULED),
                eq(CareType.ROUTINE),
                eq(1L),
                eq(2L),
                eq((Long) null),
                eq(3L),
                dayStartCaptor.capture(),
                dayEndCaptor.capture(),
                eq(pageable)
        );

        LocalDateTime expectedStart = LocalDateTime.of(2026, 10, 18, 0, 0, 0);
        LocalDateTime expectedEnd = LocalDateTime.of(2026, 10, 19, 0, 0, 0);

        assertThat(dayStartCaptor.getValue()).isEqualTo(expectedStart);
        assertThat(dayEndCaptor.getValue()).isEqualTo(expectedEnd);
    }

    @Test
    void listSchedules_withoutDate_passesNullBoundariesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CareSchedule> emptyPage = new PageImpl<>(List.of());

        when(careScheduleRepository.findFiltered(
                isNull(CareScheduleStatus.class),
                isNull(CareType.class),
                isNull(Long.class),
                isNull(Long.class),
                isNull(Long.class),
                isNull(Long.class),
                isNull(LocalDateTime.class),
                isNull(LocalDateTime.class),
                eq(pageable)
        )).thenReturn(emptyPage);

        careScheduleService.listSchedules(
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        verify(careScheduleRepository).findFiltered(
                isNull(CareScheduleStatus.class),
                isNull(CareType.class),
                isNull(Long.class),
                isNull(Long.class),
                isNull(Long.class),
                isNull(Long.class),
                dayStartCaptor.capture(),
                dayEndCaptor.capture(),
                eq(pageable)
        );

        assertThat(dayStartCaptor.getValue()).isNull();
        assertThat(dayEndCaptor.getValue()).isNull();
    }
}
