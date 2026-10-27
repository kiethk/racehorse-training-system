package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.medical.dto.CreateInjuryRecordRequest;
import com.rtms.backend.medical.dto.HorseHealthSummaryResponse;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.medical.service.InjuryRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/horses/{horseId}")
public class InjuryRecordController {

    private final InjuryRecordService injuryRecordService;
    private final HorseRepository horseRepository;

    public InjuryRecordController(InjuryRecordService injuryRecordService, HorseRepository horseRepository) {
        this.injuryRecordService = injuryRecordService;
        this.horseRepository = horseRepository;
    }

    @GetMapping("/injury-records")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'GROOM')")
    public ApiResponse<List<InjuryRecord>> getHorseInjuries(@PathVariable Long horseId) {
        return ApiResponse.success(injuryRecordService.getInjuryRecordsByHorse(horseId));
    }

    @GetMapping("/injury-records/active")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'GROOM')")
    public ApiResponse<List<InjuryRecord>> getActiveHorseInjuries(@PathVariable Long horseId) {
        return ApiResponse.success(injuryRecordService.getActiveInjuryRecordsByHorse(horseId));
    }

    @PostMapping("/injury-records")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<InjuryRecord> createInjury(
            @PathVariable Long horseId,
            @Valid @RequestBody CreateInjuryRecordRequest request) {
        return ApiResponse.success(injuryRecordService.createInjuryRecord(horseId, request));
    }

    @PostMapping("/injury-records/{injuryId}/recover")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<InjuryRecord> recoverInjury(
            @PathVariable Long horseId,
            @PathVariable Long injuryId) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ApiResponse.success(injuryRecordService.recoverInjuryRecord(horseId, injuryId, currentUser.getUserId()));
    }

    /** Lịch sử khám sức khỏe của một ngựa, dùng cho màn Health của bác sĩ thú y. */
    @GetMapping("/health-records")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'GROOM')")
    public ApiResponse<List<HealthRecord>> getHorseHealthRecords(@PathVariable Long horseId) {
        return ApiResponse.success(injuryRecordService.getHealthRecordsByHorse(horseId));
    }

    @GetMapping("/health-summary")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'HEAD_TRAINER', 'CLUB_MANAGER', 'HORSE_OWNER', 'GROOM')")
    public ApiResponse<HorseHealthSummaryResponse> getHorseHealthSummary(@PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        // Owners may only read the summary of horses they own (same rule as the health-metrics endpoint).
        if (currentUser != null && "HORSE_OWNER".equals(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Horse not found"));
            if (!Objects.equals(horse.getOwnerId(), currentUser.getUserId())) {
                throw new AccessDeniedException("You can only view the health summary of horses you own");
            }
        }
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
        return ApiResponse.success(injuryRecordService.correctInjuryRecord(horseId, injuryId, request, currentUser.getUserId()));
    }
}
