package com.rtms.backend.training.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ManagerDashboardSummaryResponse(
        StallSummary stallSummary,
        FleetSummary fleetSummary,
        AdmissionSummary admissionSummary,
        IncidentSummary incidentSummary,
        List<BarnOccupancyDto> barnOccupancies,
        List<PendingAdmissionActionDto> pendingAdmissions,
        List<UrgentIncidentActionDto> urgentIncidents
) {
    public record StallSummary(
            long totalStalls,
            long occupiedStalls,
            long availableStalls,
            long reservedStalls,
            double occupancyRate
    ) {}

    public record FleetSummary(
            long totalHorses,
            long activeEligible,
            long inTraining,
            long medicalBlocked,
            long inactive
    ) {}

    public record AdmissionSummary(
            long totalPending,
            long managerReviewCount,
            long waitingArrivalCount,
            long groomReviewCount,
            long arrivalDeadlineAlertCount
    ) {}

    public record IncidentSummary(
            long openIncidents,
            long criticalUrgentIncidents
    ) {}

    public record BarnOccupancyDto(
            Long areaId,
            String areaCode,
            String areaName,
            String areaType,
            long totalStalls,
            long occupiedStalls,
            long availableStalls,
            long reservedStalls,
            double occupancyRate
    ) {}

    public record PendingAdmissionActionDto(
            Long admissionId,
            String candidateHorseName,
            String breed,
            Long ownerId,
            String ownerName,
            String status,
            LocalDateTime submittedAt,
            LocalDateTime arrivalDeadlineAt,
            boolean isArrivalOverdueOrExpiring
    ) {}

    public record UrgentIncidentActionDto(
            Long incidentId,
            Long horseId,
            String horseName,
            String title,
            String description,
            String severity,
            String status,
            LocalDateTime reportedAt,
            Long reportedById,
            String reporterName
    ) {}
}
