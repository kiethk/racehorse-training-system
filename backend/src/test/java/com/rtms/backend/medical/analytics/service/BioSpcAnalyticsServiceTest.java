package com.rtms.backend.medical.analytics.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.analytics.dto.BioSpcSeriesResponse;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.TrainingAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BioSpcAnalyticsServiceTest {

    @Mock
    private HorseHealthMetricRepository metricRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private TrainingAccessPolicy accessPolicy;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private BioSpcAnalyticsService service;
    private AuthenticatedUser mockVet;
    private Horse mockHorse;

    @BeforeEach
    void setUp() {
        service = new BioSpcAnalyticsService(
                metricRepository,
                horseRepository,
                accessPolicy,
                userRepository,
                notificationService
        );

        mockVet = new AuthenticatedUser(10L, "vet@rtms.com", "VETERINARIAN");

        mockHorse = new Horse();
        mockHorse.setId(100L);
        mockHorse.setName("Thunder Bolt");
    }

    @Test
    @DisplayName("When K < 5 samples, baseline is inactive and uses species safe limits without notification if normal")
    void testBioSpcAccumulatingBaseline() {
        when(horseRepository.findById(100L)).thenReturn(Optional.of(mockHorse));

        List<HorseHealthMetric> metrics = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(3);
        for (int i = 0; i < 3; i++) {
            HorseHealthMetric m = new HorseHealthMetric();
            m.setHorseId(100L);
            m.setRecordedAt(baseTime.plusHours(i * 12));
            m.setTemperature(BigDecimal.valueOf(37.5 + i * 0.1));
            metrics.add(m);
        }
        when(metricRepository.findByHorseIdOrderByRecordedAtDesc(100L)).thenReturn(metrics);

        BioSpcSeriesResponse response = service.getBioSpcSeries(100L, "TEMPERATURE", mockVet);

        assertNotNull(response);
        assertEquals(100L, response.horseId());
        assertEquals("Thunder Bolt", response.horseName());
        assertEquals("TEMPERATURE", response.metricType());
        assertFalse(response.isBaselineActive());
        assertEquals(3, response.sampleCount());
        assertEquals(3, response.points().size());

        verify(accessPolicy).assertCanViewHorse(100L, mockVet);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("When K >= 5 samples and latest reading violates UCL, triggers BIO_SPC_ANOMALY_DETECTED notification")
    void testBioSpcActiveBaselineWithAnomalyNotification() {
        when(horseRepository.findById(100L)).thenReturn(Optional.of(mockHorse));

        User vetUser = new User();
        vetUser.setId(20L);
        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vetUser));

        List<HorseHealthMetric> metrics = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);
        // 5 baseline readings around 37.4
        for (long i = 1; i <= 5; i++) {
            HorseHealthMetric m = new HorseHealthMetric();
            m.setHorseId(100L);
            m.setRecordedAt(baseTime.plusDays(i));
            m.setTemperature(BigDecimal.valueOf(37.4));
            metrics.add(m);
        }
        // 6th reading: fever spike 39.2 > UCL
        HorseHealthMetric spike = new HorseHealthMetric();
        spike.setHorseId(100L);
        spike.setRecordedAt(baseTime.plusDays(6));
        spike.setTemperature(BigDecimal.valueOf(39.2));
        metrics.add(spike);

        when(metricRepository.findByHorseIdOrderByRecordedAtDesc(100L)).thenReturn(metrics);

        BioSpcSeriesResponse response = service.getBioSpcSeries(100L, "TEMPERATURE", mockVet);

        assertNotNull(response);
        assertTrue(response.isBaselineActive());
        assertEquals(6, response.sampleCount());
        assertTrue(response.upperControlLimit() < 39.2);

        // Verification of notification dispatching
        verify(notificationService).createNotification(
                eq(20L),
                contains("Thunder Bolt"),
                contains("outside 3-sigma limits"),
                eq(NotificationTypes.BIO_SPC_ANOMALY_DETECTED),
                eq(NotificationTypes.REFERENCE_HORSE),
                eq(100L),
                anyString()
        );
    }

    @Test
    @DisplayName("Horse not found throws 404 ApiException")
    void testHorseNotFound() {
        when(horseRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                service.getBioSpcSeries(999L, "TEMPERATURE", mockVet)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("HORSE_NOT_FOUND", ex.getErrorCode());
    }
}
