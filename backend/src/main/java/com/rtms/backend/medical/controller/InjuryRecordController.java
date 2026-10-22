package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.medical.dto.HorseHealthSummaryResponse;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.medical.service.InjuryRecordService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horses/{horseId}")
public class InjuryRecordController {

    private final InjuryRecordService injuryRecordService;

    public InjuryRecordController(InjuryRecordService injuryRecordService) {
        this.injuryRecordService = injuryRecordService;
    }

    @GetMapping("/injury-records")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'GROOM')")
    public ApiResponse<List<InjuryRecord>> getHorseInjuries(@PathVariable Long horseId) {
        return ApiResponse.success(injuryRecordService.getInjuryRecordsByHorse(horseId));
    }

    /** Lịch sử khám sức khỏe của một ngựa, dùng cho màn Health của bác sĩ thú y. */
    @GetMapping("/health-records")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'GROOM')")
    public ApiResponse<List<HealthRecord>> getHorseHealthRecords(@PathVariable Long horseId) {
        return ApiResponse.success(injuryRecordService.getHealthRecordsByHorse(horseId));
    }

    @GetMapping("/health-summary")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'HORSE_OWNER', 'GROOM')")
    public ApiResponse<HorseHealthSummaryResponse> getHorseHealthSummary(@PathVariable Long horseId) {
        return ApiResponse.success(injuryRecordService.getHealthSummary(horseId));
    }

    @PostMapping("/injury-records/{injuryId}/corrections")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<InjuryRecord> correctInjury(
            @PathVariable Long horseId,
            @PathVariable Long injuryId,
            @Valid @RequestBody CorrectInjuryRecordRequest request) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ApiResponse.success(injuryRecordService.correctInjuryRecord(injuryId, request, currentUser.getUserId()));
    }
}
