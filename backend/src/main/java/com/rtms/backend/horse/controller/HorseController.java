package com.rtms.backend.horse.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.CreateHorseRequest;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.service.HorseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/horses")
public class HorseController {

    private final HorseService horseService;

    public HorseController(HorseService horseService) {
        this.horseService = horseService;
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping
    public ApiResponse<List<Horse>> getAllHorses(
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) HorseStatus status) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ApiResponse.success(
                horseService.getAllHorses(currentUser, mine, unassigned, status));
    }

    @PreAuthorize("hasAuthority('HORSE_CREATE')")
    @PostMapping
    public ApiResponse<Horse> createHorse(@RequestBody CreateHorseRequest request) {
        return ApiResponse.success(horseService.createHorse(request));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<Horse> getHorseById(@PathVariable Long id) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ApiResponse.success(horseService.getHorseById(id, currentUser));
    }

    /** Bỏ trống stallId = gỡ ngựa khỏi chuồng, giống assign-groom bỏ trống groomId. */
    @PreAuthorize("hasAuthority('STABLE_STALL_UPDATE')")
    @PutMapping("/{id}/assign-stall")
    public ApiResponse<Horse> assignStall(
            @PathVariable Long id,
            @RequestParam(required = false) Long stallId) {
        return ApiResponse.success(horseService.assignStall(id, stallId));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/fitness-trend")
    public ApiResponse<List<HorseFitnessTrendItemResponse>> getFitnessTrend(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(horseService.getFitnessTrend(id, from, to));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/alerts")
    public ApiResponse<List<HorseAlertResponse>> getAlerts(@PathVariable Long id) {
        return ApiResponse.success(horseService.getHorseAlerts(id));
    }

}
