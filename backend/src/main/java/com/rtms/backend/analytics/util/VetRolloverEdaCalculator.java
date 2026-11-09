package com.rtms.backend.analytics.util;

import com.rtms.backend.analytics.dto.manager.RescheduleReasonBreakdownDto;
import com.rtms.backend.analytics.dto.manager.VetDelaysEdaDto;
import com.rtms.backend.analytics.dto.manager.VeterinarianDelayDto;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.RescheduleReason;

import java.util.*;
import java.util.stream.Collectors;

public final class VetRolloverEdaCalculator {

    private VetRolloverEdaCalculator() {}

    public static VetDelaysEdaDto calculate(List<CareSchedule> schedules, Map<Long, String> vetNames) {
        if (schedules == null || schedules.isEmpty()) {
            return new VetDelaysEdaDto(
                    0.0,
                    0,
                    0,
                    false,
                    "No care schedule data available for this period.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        int totalSchedules = schedules.size();
        int totalRollovers = 0;
        Map<String, Long> reasonCounts = new LinkedHashMap<>();

        Map<Long, List<CareSchedule>> schedulesByVet = schedules.stream()
                .filter(cs -> cs.getVeterinarianId() != null)
                .collect(Collectors.groupingBy(CareSchedule::getVeterinarianId));

        for (CareSchedule cs : schedules) {
            int rollovers = Math.max(0, cs.getRolloverCount());
            totalRollovers += rollovers;
            if (rollovers > 0) {
                RescheduleReason reason = cs.getRescheduleReason();
                String reasonKey = reason != null ? reason.name() : "UNSPECIFIED";
                reasonCounts.merge(reasonKey, (long) rollovers, Long::sum);
            }
        }

        double clinicRolloverRate = totalSchedules > 0
                ? Math.round(((double) totalRollovers / totalSchedules) * 1000.0) / 1000.0
                : 0.0;

        // Compute per-vet stats
        List<TempVetStats> tempStats = new ArrayList<>();
        for (Map.Entry<Long, List<CareSchedule>> entry : schedulesByVet.entrySet()) {
            Long vetId = entry.getKey();
            List<CareSchedule> vetList = entry.getValue();
            int vetSchedules = vetList.size();
            int vetRollovers = vetList.stream().mapToInt(cs -> Math.max(0, cs.getRolloverCount())).sum();
            double rate = vetSchedules > 0 ? (double) vetRollovers / vetSchedules : 0.0;
            tempStats.add(new TempVetStats(vetId, vetNames.getOrDefault(vetId, "Vet #" + vetId), vetSchedules, vetRollovers, rate));
        }

        // Statistical distribution of vet rates (Mean and StdDev)
        double meanRate = tempStats.isEmpty() ? 0.0 : tempStats.stream().mapToDouble(s -> s.rate).average().orElse(0.0);
        double variance = tempStats.size() > 1
                ? tempStats.stream().mapToDouble(s -> Math.pow(s.rate - meanRate, 2)).sum() / tempStats.size()
                : 0.0;
        double stdDev = Math.sqrt(variance);

        List<VeterinarianDelayDto> vetDtos = new ArrayList<>();
        List<String> delinquentVets = new ArrayList<>();

        for (TempVetStats s : tempStats) {
            double zScore = stdDev > 0.001 ? (s.rate - meanRate) / stdDev : 0.0;
            double roundedZ = Math.round(zScore * 100.0) / 100.0;
            double roundedRate = Math.round(s.rate * 1000.0) / 1000.0;

            // Anomaly condition:
            // Standard: Z >= 2.0 with rate > 10%
            // Small sample adjustment (Shiffler bound: max Z for N <= 4 is < 2.0): Z >= 1.3 with rate >= 25% and > 1.5x mean
            boolean isOutlier = roundedZ >= 2.0 || (tempStats.size() <= 4 && roundedZ >= 1.3 && s.rate >= 0.25 && s.rate >= 1.5 * meanRate);
            boolean alert = isOutlier && s.schedules >= 3;

            if (alert) {
                delinquentVets.add(s.name);
            }
            vetDtos.add(new VeterinarianDelayDto(
                    s.vetId,
                    s.name,
                    s.schedules,
                    s.rollovers,
                    roundedRate,
                    roundedZ,
                    alert
            ));
        }

        vetDtos.sort(Comparator.comparingDouble(VeterinarianDelayDto::rolloverRate).reversed());

        // Reason breakdown
        List<RescheduleReasonBreakdownDto> reasonBreakdown = new ArrayList<>();
        for (Map.Entry<String, Long> entry : reasonCounts.entrySet()) {
            double pct = totalRollovers > 0
                    ? Math.round(((double) entry.getValue() / totalRollovers * 100.0) * 10.0) / 10.0
                    : 0.0;
            reasonBreakdown.add(new RescheduleReasonBreakdownDto(entry.getKey(), entry.getValue(), pct));
        }
        reasonBreakdown.sort(Comparator.comparingLong(RescheduleReasonBreakdownDto::count).reversed());

        long timeExceededCount = reasonCounts.getOrDefault("TIME_EXCEEDED", 0L);
        double timeExceededPct = totalRollovers > 0 ? (double) timeExceededCount / totalRollovers : 0.0;
        boolean systemicUnderstaffingAlert = clinicRolloverRate > 0.15 && timeExceededPct >= 0.50;

        String diagnosticSummary;
        if (systemicUnderstaffingAlert) {
            diagnosticSummary = String.format(Locale.US,
                    "Systemic Understaffing Alert: Clinic-wide rollover rate is %.1f%% with %.0f%% due to appointment overtime, indicating clinic capacity strain.",
                    clinicRolloverRate * 100.0, timeExceededPct * 100.0);
        } else if (!delinquentVets.isEmpty()) {
            diagnosticSummary = "Individual Delay Alert: Veterinarian(s) " + String.join(", ", delinquentVets)
                    + " exhibit rollover rates significantly higher than clinic peers.";
        } else {
            diagnosticSummary = "Normal Clinic Operations: Examination completion and rollover rates are within expected parameters.";
        }

        return new VetDelaysEdaDto(
                clinicRolloverRate,
                totalSchedules,
                totalRollovers,
                systemicUnderstaffingAlert,
                diagnosticSummary,
                reasonBreakdown,
                vetDtos
        );
    }

    private record TempVetStats(Long vetId, String name, int schedules, int rollovers, double rate) {}
}
