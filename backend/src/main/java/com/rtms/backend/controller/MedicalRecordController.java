package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreateMedicalRecordRequest;
import com.rtms.backend.entity.MedicalRecord;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.MedicalRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PreAuthorize("hasAuthority('MEDICAL_RECORD_CREATE')")
    @PostMapping
    public ApiResponse<MedicalRecord> create(@Valid @RequestBody CreateMedicalRecordRequest request) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ApiResponse.success(medicalRecordService.createMedicalRecord(request, currentUser));
    }
}
