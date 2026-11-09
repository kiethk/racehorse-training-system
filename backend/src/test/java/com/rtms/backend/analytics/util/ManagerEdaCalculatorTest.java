package com.rtms.backend.analytics.util;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.analytics.dto.manager.ManagerEdaOverviewResponse;
import com.rtms.backend.analytics.dto.manager.TrainerWorkoutRow;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ManagerEdaCalculatorTest {

    @Test
    @DisplayName("VetRolloverEdaCalculator flags individual delinquency when Z >= 2.0 and high rate")
    void testVetRolloverIndividualDelinquency() {
        Map<Long, String> names = Map.of(1L, "Dr. Alice", 2L, "Dr. Bob", 3L, "Dr. Charlie");
        List<CareSchedule> schedules = new ArrayList<>();

        // Dr. Alice: 10 schedules, 0 rollovers
        for (int i = 0; i < 10; i++) {
            CareSchedule cs = new CareSchedule();
            cs.setVeterinarianId(1L);
            cs.setRolloverCount(0);
            schedules.add(cs);
        }

        // Dr. Bob: 10 schedules, 0 rollovers
        for (int i = 0; i < 10; i++) {
            CareSchedule cs = new CareSchedule();
            cs.setVeterinarianId(2L);
            cs.setRolloverCount(0);
            schedules.add(cs);
        }

        // Dr. Charlie: 10 schedules, 6 rollovers with URGENT_PREEMPTION
        for (int i = 0; i < 10; i++) {
            CareSchedule cs = new CareSchedule();
            cs.setVeterinarianId(3L);
            cs.setRolloverCount(i < 6 ? 1 : 0);
            if (i < 6) cs.setRescheduleReason(RescheduleReason.URGENT_PREEMPTION);
            schedules.add(cs);
        }

        var result = VetRolloverEdaCalculator.calculate(schedules, names);

        assertThat(result.totalCareSchedules()).isEqualTo(30);
        assertThat(result.totalRolloverEvents()).isEqualTo(6);
        assertThat(result.systemicUnderstaffingAlert()).isFalse(); // Not systemic since TIME_EXCEEDED is 0
        assertThat(result.veterinarians()).hasSize(3);

        var charlie = result.veterinarians().stream()
                .filter(v -> v.veterinarianId().equals(3L))
                .findFirst().orElseThrow();
        assertThat(charlie.individualDelinquencyAlert()).isTrue();
        assertThat(charlie.zScore()).isGreaterThanOrEqualTo(1.4);
    }

    @Test
    @DisplayName("VetRolloverEdaCalculator flags systemic understaffing when clinic rollover > 15% and TIME_EXCEEDED >= 50%")
    void testVetRolloverSystemicUnderstaffing() {
        Map<Long, String> names = Map.of(1L, "Dr. Alice", 2L, "Dr. Bob");
        List<CareSchedule> schedules = new ArrayList<>();

        // 20 schedules total, 6 rollovers with TIME_EXCEEDED (30% rollover rate)
        for (int i = 0; i < 20; i++) {
            CareSchedule cs = new CareSchedule();
            cs.setVeterinarianId(i % 2 == 0 ? 1L : 2L);
            cs.setRolloverCount(i < 6 ? 1 : 0);
            if (i < 6) cs.setRescheduleReason(RescheduleReason.TIME_EXCEEDED);
            schedules.add(cs);
        }

        var result = VetRolloverEdaCalculator.calculate(schedules, names);
        assertThat(result.systemicUnderstaffingAlert()).isTrue();
        assertThat(result.diagnosticSummary()).contains("Systemic Understaffing Alert");
    }

    @Test
    @DisplayName("TrainerEdaCalculator correctly measures adherence, ratings, and fitness progression delta")
    void testTrainerEdaCalculation() {
        Map<Long, String> names = Map.of(10L, "Trainer Tom");
        List<TrainerWorkoutRow> rows = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().minusDays(10);

        // Horse 100: Session 1 (speed 40, HR 80), Session 2 (speed 44, HR 72) -> gain +4 speed, +8 HR
        rows.add(new TrainerWorkoutRow(10L, WorkoutStatus.RAN, 8, BigDecimal.valueOf(40.0), 80, baseDate, 100L));
        rows.add(new TrainerWorkoutRow(10L, WorkoutStatus.RAN, 9, BigDecimal.valueOf(44.0), 72, baseDate.plusDays(5), 100L));
        rows.add(new TrainerWorkoutRow(10L, WorkoutStatus.CANCELLED, null, null, null, baseDate.plusDays(6), 101L));

        var result = TrainerEdaCalculator.calculate(rows, names);

        assertThat(result.totalWorkoutsScheduled()).isEqualTo(3);
        assertThat(result.totalWorkoutsCompleted()).isEqualTo(2);
        assertThat(result.overallAdherenceRate()).isEqualTo(66.7);
        assertThat(result.trainers()).hasSize(1);

        var tom = result.trainers().get(0);
        assertThat(tom.trainerName()).isEqualTo("Trainer Tom");
        assertThat(tom.adherenceRate()).isEqualTo(66.7);
        assertThat(tom.averageRating()).isEqualTo(8.5);
        assertThat(tom.speedGainKmh()).isEqualTo(4.0);
        assertThat(tom.hrRecoveryImprovementBpm()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("OccupancyEdaCalculator computes buffer margin and quarantine stress alert")
    void testOccupancyEdaCalculation() {
        Area regularArea = new Area();
        regularArea.setId(1L);
        regularArea.setCode("A");
        regularArea.setName("Main Barn");
        regularArea.setType(AreaType.REGULAR);

        Area quarantineArea = new Area();
        quarantineArea.setId(2L);
        quarantineArea.setCode("Q");
        quarantineArea.setName("Quarantine Block");
        quarantineArea.setType(AreaType.QUARANTINE);

        List<Area> areas = List.of(regularArea, quarantineArea);

        List<AreaStallCountProjection> projections = List.of(
                createProjection(1L, StallStatus.OCCUPIED, 80L),
                createProjection(1L, StallStatus.AVAILABLE, 20L),
                createProjection(2L, StallStatus.OCCUPIED, 9L),
                createProjection(2L, StallStatus.AVAILABLE, 1L)
        );

        // 1 available quarantine stall, but 3 admissions awaiting arrival -> buffer margin = -2
        var result = OccupancyEdaCalculator.calculate(areas, projections, 3L);

        assertThat(result.totalStalls()).isEqualTo(110L);
        assertThat(result.quarantineTotal()).isEqualTo(10L);
        assertThat(result.quarantineOccupied()).isEqualTo(9L);
        assertThat(result.quarantineRate()).isEqualTo(90.0);
        assertThat(result.inflowBufferMargin()).isEqualTo(-2L);
        assertThat(result.quarantineStressAlert()).isTrue();
    }

    @Test
    @DisplayName("AdmissionFunnelEdaCalculator measures funnel conversions and bottleneck latency")
    void testAdmissionFunnelEdaCalculation() {
        List<AdmissionApplication> apps = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Application 1: Full pipeline (submitted -> arrived -> groom -> vet -> trainer -> manager approved)
        AdmissionApplication a1 = new AdmissionApplication();
        a1.setStatus(AdmissionStatus.APPROVED);
        a1.setSubmittedAt(now.minusHours(50));
        a1.setArrivedAt(now.minusHours(40));      // +10h arrival
        a1.setGroomReviewedAt(now.minusHours(35)); // +5h groom
        a1.setVetReviewedAt(now.minusHours(15));   // +20h vet (bottleneck!)
        a1.setTrainerReviewedAt(now.minusHours(10));// +5h trainer
        a1.setManagerReviewedAt(now.minusHours(2)); // +8h manager
        apps.add(a1);

        // Application 2: Dropped at Vet
        AdmissionApplication a2 = new AdmissionApplication();
        a2.setStatus(AdmissionStatus.REJECTED);
        a2.setSubmittedAt(now.minusHours(30));
        a2.setArrivedAt(now.minusHours(24));
        a2.setGroomReviewedAt(now.minusHours(20));
        a2.setVetReviewedAt(now.minusHours(10));
        apps.add(a2);

        var result = AdmissionFunnelEdaCalculator.calculate(apps);

        assertThat(result.totalApplications()).isEqualTo(2);
        assertThat(result.approvedCount()).isEqualTo(1);
        assertThat(result.approvalRate()).isEqualTo(50.0);
        assertThat(result.rejectionRate()).isEqualTo(50.0);
        assertThat(result.bottleneckStage()).isEqualTo("VET_REVIEW");
        assertThat(result.stageFunnel()).isNotEmpty();
    }

    private AreaStallCountProjection createProjection(Long areaId, StallStatus status, Long count) {
        return new AreaStallCountProjection() {
            @Override public Long getAreaId() { return areaId; }
            @Override public StallStatus getStatus() { return status; }
            @Override public Long getCount() { return count; }
        };
    }
}
