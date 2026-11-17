package com.rtms.backend.analytics.service;

import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.analytics.dto.manager.AdmissionFunnelEdaDto;
import com.rtms.backend.analytics.dto.manager.TrainerEvaluationEdaDto;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerEdaAnalyticsServiceTest {

    @Mock
    private CareScheduleRepository careScheduleRepository;

    @Mock
    private TrainingWorkoutRepository trainingWorkoutRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private UserRepository userRepository;

    private ManagerEdaAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new ManagerEdaAnalyticsService(
                careScheduleRepository,
                trainingWorkoutRepository,
                stableStallRepository,
                areaRepository,
                admissionApplicationRepository,
                userRepository
        );
    }

    @Test
    void getAdmissionFunnelReturnsOnlyAdmissionFunnel() {
        when(admissionApplicationRepository.findAdmissionsSince(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        AdmissionFunnelEdaDto shortWindowResponse = service.getAdmissionFunnel(1);
        AdmissionFunnelEdaDto longWindowResponse = service.getAdmissionFunnel(500);

        assertNotNull(shortWindowResponse);
        assertEquals(0, shortWindowResponse.totalApplications());
        assertEquals("NONE", shortWindowResponse.bottleneckStage());
        assertNotNull(longWindowResponse);
        assertEquals(0, longWindowResponse.totalApplications());

        verify(admissionApplicationRepository).findAdmissionsSince(
                eq(LocalDate.now().minusDays(7).atStartOfDay())
        );
        verify(admissionApplicationRepository).findAdmissionsSince(
                eq(LocalDate.now().minusDays(365).atStartOfDay())
        );
        verifyNoMoreInteractions(admissionApplicationRepository);
        verifyNoInteractions(
                careScheduleRepository,
                trainingWorkoutRepository,
                stableStallRepository,
                areaRepository,
                userRepository
        );
    }

    @Test
    void getTrainerEvaluationReturnsOnlyTrainerEvaluation() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(trainingWorkoutRepository.findTrainerWorkoutsSince(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        TrainerEvaluationEdaDto shortWindowResponse = service.getTrainerEvaluation(1);
        TrainerEvaluationEdaDto longWindowResponse = service.getTrainerEvaluation(500);

        assertNotNull(shortWindowResponse);
        assertEquals(0, shortWindowResponse.totalWorkoutsScheduled());
        assertEquals(List.of(), shortWindowResponse.trainers());
        assertNotNull(longWindowResponse);
        assertEquals(0, longWindowResponse.totalWorkoutsScheduled());

        verify(trainingWorkoutRepository).findTrainerWorkoutsSince(
                eq(LocalDate.now().minusDays(7))
        );
        verify(trainingWorkoutRepository).findTrainerWorkoutsSince(
                eq(LocalDate.now().minusDays(365))
        );
        verifyNoMoreInteractions(trainingWorkoutRepository);
        verify(userRepository, org.mockito.Mockito.times(2)).findAll();
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(
                careScheduleRepository,
                stableStallRepository,
                areaRepository,
                admissionApplicationRepository
        );
    }
}
