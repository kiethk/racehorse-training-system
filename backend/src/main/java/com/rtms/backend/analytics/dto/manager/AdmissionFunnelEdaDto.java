package com.rtms.backend.analytics.dto.manager;

import java.util.List;

public record AdmissionFunnelEdaDto(
        long totalApplications,
        long approvedCount,
        long rejectedCount,
        long expiredCount,
        long inProgressCount,
        double approvalRate,
        double rejectionRate,
        String bottleneckStage,
        AdmissionStageLatenciesDto medianLatenciesHours,
        List<StageFunnelPointDto> stageFunnel
) {}
