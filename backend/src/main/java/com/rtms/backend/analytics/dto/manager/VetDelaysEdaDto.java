package com.rtms.backend.analytics.dto.manager;

import java.util.List;

public record VetDelaysEdaDto(
        double clinicRolloverRate,
        int totalCareSchedules,
        int totalRolloverEvents,
        boolean systemicUnderstaffingAlert,
        String diagnosticSummary,
        List<RescheduleReasonBreakdownDto> reasonBreakdown,
        List<VeterinarianDelayDto> veterinarians
) {}
