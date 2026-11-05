package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.service.HorseAttendanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lịch sử Group Plan và Attendance của một chiến mã. (The Group Plan history and Attendance of one horse.) */
@RestController
@RequestMapping("/api/horses/{horseId}/group-plan-history")
public class HorseAttendanceController {

    private final HorseAttendanceService attendanceService;

    public HorseAttendanceController(HorseAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TRAINING_WORKOUT_VIEW')")
    public ApiResponse<List<HorseAttendanceResponse>> getHistory(
            @PathVariable Long horseId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(attendanceService.getHistory(horseId, currentUser));
    }
}
