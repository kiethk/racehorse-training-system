package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.dto.HorseHealthSummaryResponse;
import com.rtms.backend.entity.InjuryRecord;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.InjuryRecordService;
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
