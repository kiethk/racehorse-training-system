package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunPreviewResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.CourseRunSummaryResponse;
import com.rtms.backend.training.dto.CourseRunHorseResponse;
import com.rtms.backend.training.dto.SessionHorseOutcomeResponse;
import com.rtms.backend.training.service.CourseRunService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller chỉ nhận request và gọi service; mọi luật lập lịch và phân quyền theo chủ kế hoạch nằm ở CourseRunService.
 * (The controller only receives requests and calls the service; every scheduling rule and the course run owner check live in
 * CourseRunService.)
 */
@RestController
@RequestMapping("/api/course-runs")
public class CourseRunController {

    private final CourseRunService courseRunService;

    public CourseRunController(CourseRunService courseRunService) {
        this.courseRunService = courseRunService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COURSE_RUN_LIST')")
    public ApiResponse<List<CourseRunSummaryResponse>> getCourseRuns(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long horseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.getCourseRuns(groupId, horseId, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_RUN_VIEW')")
    public ApiResponse<CourseRunDetailResponse> getCourseRun(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.getCourseRun(id, currentUser));
    }

    @GetMapping("/{id}/horses")
    @PreAuthorize("hasAuthority('COURSE_RUN_VIEW')")
    public ApiResponse<List<CourseRunHorseResponse>> getCourseRunHorses(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.getCourseRunHorses(id, currentUser));
    }

    @GetMapping("/{id}/sessions/{sessionId}/horses")
    @PreAuthorize("hasAuthority('COURSE_RUN_VIEW')")
    public ApiResponse<List<SessionHorseOutcomeResponse>> getSessionHorses(
            @PathVariable Long id, @PathVariable Long sessionId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.getSessionHorses(id, sessionId, currentUser));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('COURSE_RUN_MANAGE')")
    public ApiResponse<CourseRunPreviewResponse> preview(
            @Valid @RequestBody CourseRunRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.previewCourseRun(request, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_RUN_MANAGE')")
    public ApiResponse<CourseRunDetailResponse> createCourseRun(
            @Valid @RequestBody CourseRunRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.createCourseRun(request, currentUser));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('COURSE_RUN_MANAGE')")
    public ApiResponse<CourseRunDetailResponse> cancelCourseRun(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(courseRunService.cancelCourseRun(id, currentUser));
    }
}
