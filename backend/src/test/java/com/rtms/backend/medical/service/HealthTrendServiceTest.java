package com.rtms.backend.medical.service;

import com.rtms.backend.medical.dto.HealthDayResponse;
import com.rtms.backend.medical.dto.HealthReadingRow;
import com.rtms.backend.medical.dto.HealthTrendResponse;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.HorseScopeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthTrendServiceTest {

    @Mock private HorseHealthMetricRepository metricRepository;
    @Mock private HorseScopeService scopeService;
    @InjectMocks private HealthTrendService service;

    private final AuthenticatedUser owner = new AuthenticatedUser(5L, "owner@example.com", "HORSE_OWNER");
    private final LocalDate today = LocalDate.now();

    private HealthReadingRow reading(LocalDate date, long horseId, String name, String temperature, String heartRate,
                                     String bodyCondition) {
        return new HealthReadingRow(date, horseId, name, new BigDecimal(temperature), new BigDecimal(heartRate),
                new BigDecimal(bodyCondition));
    }

    @Test
    @DisplayName("The last 30 days up to today, for the horses in the scope of the role")
    void trendCoversThirtyDays() {
        when(scopeService.horseIdsOf(owner)).thenReturn(List.of(60L, 61L));
        when(metricRepository.findHealthReadings(List.of(60L, 61L), today.minusDays(29).atStartOfDay(),
                today.plusDays(1).atStartOfDay())).thenReturn(List.of());

        HealthTrendResponse trend = service.getTrend(owner);

        assertEquals(today.minusDays(29), trend.fromDate());
        assertEquals(today, trend.toDate());
        assertTrue(trend.days().isEmpty());
    }

    @Test
    @DisplayName("A day counts its readings and lists each abnormal horse once, with all its issues merged")
    void abnormalHorsesAreListedOncePerDay() {
        LocalDate day = today.minusDays(2);
        when(scopeService.horseIdsOf(owner)).thenReturn(List.of(60L, 61L, 62L));
        when(metricRepository.findHealthReadings(any(), any(), any())).thenReturn(List.of(
                reading(day, 61L, "Zephyr", "38.0", "40", "6.0"),    // normal
                reading(day, 60L, "Atlas", "38.9", "40", "6.0"),     // fever
                reading(day, 60L, "Atlas", "38.0", "50", "6.0"),     // same horse, fast heart rate
                reading(day, 60L, "Atlas", "38.9", "40", "6.0"),     // the fever again: not repeated
                reading(today, 62L, "Comet", "38.0", "40", "6.0")));

        List<HealthDayResponse> days = service.getTrend(owner).days();

        assertEquals(2, days.size());
        HealthDayResponse first = days.get(0);
        assertEquals(day, first.date());
        assertEquals(4, first.readings());
        assertEquals(1, first.abnormalHorses().size(), "The normal horse is not listed");
        assertEquals("Atlas", first.abnormalHorses().get(0).horseName());
        assertEquals(List.of("Temperature 38.9 °C", "Heart rate 50 bpm"), first.abnormalHorses().get(0).issues());

        HealthDayResponse second = days.get(1);
        assertEquals(today, second.date());
        assertEquals(1, second.readings());
        assertTrue(second.abnormalHorses().isEmpty(), "A day with only normal readings has no abnormal horse");
    }

    @Test
    @DisplayName("Abnormal horses of one day are listed by name")
    void abnormalHorsesAreSortedByName() {
        when(scopeService.horseIdsOf(owner)).thenReturn(List.of(60L, 61L));
        when(metricRepository.findHealthReadings(any(), any(), any())).thenReturn(List.of(
                reading(today, 61L, "Zephyr", "39.5", "40", "6.0"),
                reading(today, 60L, "Atlas", "38.0", "40", "3.0")));

        List<HealthDayResponse.AbnormalHorse> horses = service.getTrend(owner).days().get(0).abnormalHorses();

        assertEquals(List.of("Atlas", "Zephyr"), horses.stream().map(HealthDayResponse.AbnormalHorse::horseName).toList());
    }

    @Test
    @DisplayName("A role with no horses gets an empty chart and no query")
    void noHorsesNoQuery() {
        when(scopeService.horseIdsOf(owner)).thenReturn(List.of());

        assertTrue(service.getTrend(owner).days().isEmpty());
        verify(metricRepository, never()).findHealthReadings(any(), any(), any());
    }
}
