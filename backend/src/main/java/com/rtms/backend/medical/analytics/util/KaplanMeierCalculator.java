package com.rtms.backend.medical.analytics.util;

import com.rtms.backend.medical.analytics.dto.SurvivalEdaResponse.KaplanMeierStepDto;

import java.util.*;

/**
 * Pure algorithmic utility for Kaplan-Meier product-limit survival estimator:
 * S(t) = \prod_{t_k \le t} (1 - d_k / n_k)
 * Supports right-censoring and median recovery time calculation.
 */
public final class KaplanMeierCalculator {

    private KaplanMeierCalculator() {
    }

    public record SurvivalObservation(int days, boolean recovered) {}

    public record KmAnalysisResult(
            int totalCases,
            int recoveredCount,
            int censoredCount,
            Double medianRecoveryDays,
            List<KaplanMeierStepDto> steps
    ) {}

    /**
     * Computes the Kaplan-Meier survival curve from a collection of time-to-recovery observations.
     */
    public static KmAnalysisResult calculate(List<SurvivalObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return new KmAnalysisResult(0, 0, 0, null, List.of());
        }

        int totalCases = observations.size();
        int totalRecovered = (int) observations.stream().filter(SurvivalObservation::recovered).count();
        int totalCensored = totalCases - totalRecovered;

        // Group observations by days
        // Map: day -> [eventsCount (d_k), censoredCount (c_k)]
        SortedMap<Integer, int[]> dayMap = new TreeMap<>();
        for (SurvivalObservation obs : observations) {
            int d = Math.max(obs.days(), 1); // minimum 1 day of observation
            int[] counts = dayMap.computeIfAbsent(d, k -> new int[2]);
            if (obs.recovered()) {
                counts[0]++; // event (recovered)
            } else {
                counts[1]++; // censored
            }
        }

        List<KaplanMeierStepDto> steps = new ArrayList<>();
        // Step 0: at t=0, S(0)=1.0, atRisk=totalCases, events=0
        steps.add(new KaplanMeierStepDto(0, 1.0, totalCases, 0));

        double currentSurvival = 1.0;
        int currentAtRisk = totalCases;
        Double medianDays = null;

        for (Map.Entry<Integer, int[]> entry : dayMap.entrySet()) {
            int day = entry.getKey();
            int events = entry.getValue()[0];
            int censored = entry.getValue()[1];

            if (currentAtRisk <= 0) {
                break;
            }

            double prevSurvival = currentSurvival;
            if (events > 0) {
                currentSurvival *= (1.0 - ((double) events / currentAtRisk));
            }

            // Check if median crossed (S(t) <= 0.5)
            if (medianDays == null && currentSurvival <= 0.5) {
                if (Math.abs(currentSurvival - 0.5) < 1e-9) {
                    medianDays = (double) day;
                } else if (prevSurvival > 0.5) {
                    // Linear interpolation between previous step day and current day
                    int prevDay = steps.get(steps.size() - 1).days();
                    double fraction = (prevSurvival - 0.5) / (prevSurvival - currentSurvival);
                    double interp = prevDay + fraction * (day - prevDay);
                    medianDays = Math.round(interp * 10.0) / 10.0;
                } else {
                    medianDays = (double) day;
                }
            }

            double roundedProb = Math.round(currentSurvival * 1000.0) / 1000.0;
            steps.add(new KaplanMeierStepDto(day, roundedProb, currentAtRisk, events));

            currentAtRisk -= (events + censored);
        }

        return new KmAnalysisResult(totalCases, totalRecovered, totalCensored, medianDays, steps);
    }
}
