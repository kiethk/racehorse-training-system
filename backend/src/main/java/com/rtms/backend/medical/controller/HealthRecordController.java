package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.CreateHealthRecordRequest;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.medical.service.HealthRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PreAuthorize("hasAuthority('HEALTH_RECORD_CREATE')")
    @PostMapping
    public ApiResponse<HealthRecord> create(@Valid @RequestBody CreateHealthRecordRequest request) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ApiResponse.success(healthRecordService.createHealthRecord(request, currentUser));
    }
}
