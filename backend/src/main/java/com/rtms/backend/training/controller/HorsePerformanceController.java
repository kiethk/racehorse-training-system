package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.DashboardHorseResponse;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseScheduleItemResponse;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.dto.PerformanceTrendResponse;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Phong độ và thể lực của chiến mã, chỉ đọc: biểu đồ thể lực, cảnh báo nguy cơ, sở trường thi đấu của từng con và
 * dashboard theo vai trò. Tất cả tính khi đọc từ các buổi tập đã hoàn thành.
 * (Read-only horse performance and fitness: fitness chart, risk alerts and racing specialty of one horse, and the
 * role dashboard. Everything is computed on read from completed workouts.)
 *
 * Các URL giữ nguyên như trước khi tách khỏi HorseController và HorseTrainingPlanController, nên frontend không đổi.
 * (The URLs are the same as before they were split out of HorseController and HorseTrainingPlanController, so the
 * frontend is unchanged.)
 */
@RestController
public class HorsePerformanceController {

    private final HorsePerformanceService performanceService;
    private final HorseSpecialtyService specialtyService;

    public HorsePerformanceController(HorsePerformanceService performanceService,
                                      HorseSpecialtyService specialtyService) {
        this.performanceService = performanceService;
        this.specialtyService = specialtyService;
    }

    // =================================================================
    // MỘT CHIẾN MÃ (One horse)
    // =================================================================

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/api/horses/{id}/fitness-trend")
    public ApiResponse<List<HorseFitnessTrendItemResponse>> getFitnessTrend(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getFitnessTrend(id, from, to, currentUser));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/api/horses/{id}/alerts")
    public ApiResponse<List<HorseAlertResponse>> getAlerts(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getHorseAlerts(id, currentUser));
    }

    /** Lịch tập của con ngựa trong khoảng ngày, kể cả buổi đã hủy. (The horse training schedule over a date range, cancelled sessions included.) */
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/api/horses/{id}/schedule")
    public ApiResponse<List<HorseScheduleItemResponse>> getSchedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getHorseSchedule(id, from, to, currentUser));
    }

    /** Sở trường thi đấu suy ra từ các buổi tập; vai nào xem được ngựa thì xem được. (Racing specialty inferred from workouts.) */
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/api/horses/{id}/specialty")
    public ApiResponse<HorseSpecialtyResponse> getSpecialty(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(specialtyService.getSpecialty(id, currentUser));
    }

    // =================================================================
    // DASHBOARD THEO VAI TRÒ (Role dashboard)
    // =================================================================

    /**
     * Dashboard tiến độ và thể lực của các chiến mã trong phạm vi vai trò (FE-7.1).
     * (Progress and fitness dashboard of the horses in the role's scope (FE-7.1).)
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/api/training-plans/dashboard")
    public ApiResponse<List<DashboardHorseResponse>> getDashboard(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getDashboard(currentUser));
    }

    /**
     * Biểu đồ phong độ chung trên dashboard: trung bình theo tuần, 12 tuần gần nhất.
     * (Performance chart on the dashboard: weekly averages over the last 12 weeks.)
     */
    @PreAuthorize("hasAuthority('TRAINING_PLAN_VIEW')")
    @GetMapping("/api/training-plans/dashboard/performance-trend")
    public ApiResponse<PerformanceTrendResponse> getPerformanceTrend(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getPerformanceTrend(currentUser));
    }
}
