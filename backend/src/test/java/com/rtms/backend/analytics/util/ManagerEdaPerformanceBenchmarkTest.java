package com.rtms.backend.analytics.util;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.analytics.dto.manager.*;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.RescheduleReason;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.StableStallRepository.AreaStallCountProjection;
import com.rtms.backend.training.enums.WorkoutStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ManagerEdaPerformanceBenchmarkTest {

    @Test
    @DisplayName("Performance Benchmark: High-volume EDA calculations execute within tight latency bounds (< 50ms)")
    void benchmarkHighVolumeCalculations() {
        // 1. Synthesize 2,000 care schedules across 8 veterinarians
        Map<Long, String> vetNames = Map.of(
                1L, "Dr. Alice", 2L, "Dr. Bob", 3L, "Dr. Charlie", 4L, "Dr. Diana",
                5L, "Dr. Evan", 6L, "Dr. Fiona", 7L, "Dr. George", 8L, "Dr. Helen"
        );
        List<CareSchedule> schedules = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            CareSchedule cs = new CareSchedule();
            cs.setVeterinarianId((long) (i % 8 + 1));
            cs.setRolloverCount(i % 7 == 0 ? 1 : 0);
            cs.setRescheduleReason(i % 14 == 0 ? RescheduleReason.TIME_EXCEEDED : RescheduleReason.URGENT_PREEMPTION);
            schedules.add(cs);
        }

        // 2. Synthesize 2,000 training workout rows across 6 trainers
        Map<Long, String> trainerNames = Map.of(
                1L, "Trainer 1", 2L, "Trainer 2", 3L, "Trainer 3",
                4L, "Trainer 4", 5L, "Trainer 5", 6L, "Trainer 6"
        );
        List<TrainerWorkoutRow> workoutRows = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().minusDays(60);
        for (int i = 0; i < 2000; i++) {
            long trainerId = (long) (i % 6 + 1);
            long horseId = (long) (i % 50 + 1);
            WorkoutStatus status = (i % 12 == 0) ? WorkoutStatus.CANCELLED : ((i % 8 == 0) ? WorkoutStatus.HELD : WorkoutStatus.RAN);
            Integer rating = (status == WorkoutStatus.RAN) ? (7 + (i % 3)) : null;
            BigDecimal speed = (status == WorkoutStatus.RAN) ? BigDecimal.valueOf(38.0 + (i % 15) * 0.5) : null;
            Integer hr = (status == WorkoutStatus.RAN) ? (85 - (i % 15)) : null;

            workoutRows.add(new TrainerWorkoutRow(trainerId, status, rating, speed, hr, baseDate.plusDays(i % 50), horseId));
        }

        // 3. Synthesize stall occupancy areas and projections across 10 areas
        List<Area> areas = new ArrayList<>();
        List<AreaStallCountProjection> projections = new ArrayList<>();
        for (long a = 1; a <= 10; a++) {
            Area area = new Area();
            area.setId(a);
            area.setCode("B" + a);
            area.setName(a == 10 ? "Quarantine Unit" : "Barn " + a);
            area.setType(a == 10 ? AreaType.QUARANTINE : AreaType.REGULAR);
            areas.add(area);

            final long areaId = a;
            projections.add(createProjection(areaId, StallStatus.OCCUPIED, 40L));
            projections.add(createProjection(areaId, StallStatus.AVAILABLE, 10L));
        }

        // 4. Synthesize 1,000 admission applications
        List<AdmissionApplication> admissions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 1000; i++) {
            AdmissionApplication app = new AdmissionApplication();
            app.setStatus(i % 5 == 0 ? AdmissionStatus.REJECTED : (i % 3 == 0 ? AdmissionStatus.MANAGER_REVIEW : AdmissionStatus.APPROVED));
            app.setSubmittedAt(now.minusHours(100 + i % 200));
            app.setArrivedAt(now.minusHours(80 + i % 150));
            app.setGroomReviewedAt(now.minusHours(60 + i % 100));
            app.setVetReviewedAt(now.minusHours(40 + i % 80));
            app.setTrainerReviewedAt(now.minusHours(20 + i % 40));
            app.setManagerReviewedAt(now.minusHours(2 + i % 10));
            admissions.add(app);
        }

        // Warm-up JIT
        for (int i = 0; i < 5; i++) {
            VetRolloverEdaCalculator.calculate(schedules, vetNames);
            TrainerEdaCalculator.calculate(workoutRows, trainerNames);
            OccupancyEdaCalculator.calculate(areas, projections, 5L);
            AdmissionFunnelEdaCalculator.calculate(admissions);
        }

        // Measure execution time
        long start = System.nanoTime();
        int iterations = 50;
        for (int i = 0; i < iterations; i++) {
            VetDelaysEdaDto vDto = VetRolloverEdaCalculator.calculate(schedules, vetNames);
            TrainerEvaluationEdaDto tDto = TrainerEdaCalculator.calculate(workoutRows, trainerNames);
            StallOccupancyEdaDto sDto = OccupancyEdaCalculator.calculate(areas, projections, 5L);
            AdmissionFunnelEdaDto aDto = AdmissionFunnelEdaCalculator.calculate(admissions);

            assertThat(vDto).isNotNull();
            assertThat(tDto).isNotNull();
            assertThat(sDto).isNotNull();
            assertThat(aDto).isNotNull();
        }
        long durationNs = System.nanoTime() - start;
        double totalMs = durationNs / 1_000_000.0;
        double avgMsPerRun = totalMs / iterations;

        System.out.printf("BENCHMARK RESULTS: %d iterations over 5,000+ domain entities completed in %.2f ms (Average: %.3f ms/run)%n",
                iterations, totalMs, avgMsPerRun);

        // Assert average per-request compute time is well under 50ms SLA
        assertThat(avgMsPerRun).isLessThan(50.0);
    }

    private AreaStallCountProjection createProjection(Long areaId, StallStatus status, Long count) {
        return new AreaStallCountProjection() {
            @Override public Long getAreaId() { return areaId; }
            @Override public StallStatus getStatus() { return status; }
            @Override public Long getCount() { return count; }
        };
    }
}
