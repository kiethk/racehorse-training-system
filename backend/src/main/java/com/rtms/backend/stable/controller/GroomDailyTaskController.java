package com.rtms.backend.stable.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.stable.dto.CreateGroomDailyTaskRequest;
import com.rtms.backend.stable.dto.TodayTaskItemResponse;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/groom-daily-tasks")
public class GroomDailyTaskController {

    private final GroomDailyTaskService taskService;

    public GroomDailyTaskController(GroomDailyTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_CREATE')")
    public ApiResponse<GroomDailyTask> createTask(
            @RequestBody CreateGroomDailyTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        GroomDailyTask created = taskService.createTask(request, currentUser);
        return ApiResponse.success(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_VIEW')")
    public ApiResponse<List<GroomDailyTask>> getDailyTasks(
            @RequestParam(required = false) Long groomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<GroomDailyTask> tasks = taskService.getDailyTasks(groomId, date, currentUser);
        return ApiResponse.success(tasks);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_UPDATE')")
    public ApiResponse<GroomDailyTask> completeTask(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        GroomDailyTask completed = taskService.completeTask(id, currentUser);
        return ApiResponse.success(completed);
    }

    /**
     * Completes a task with what the Groom recorded: an optional note and an optional photo.
     * The PATCH above stays for callers that only tick the task.
     */
    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_UPDATE')")
    public ApiResponse<GroomDailyTask> completeTaskWithEvidence(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(taskService.completeTask(id, note, file, currentUser));
    }

    /** The photo taken on completion. Served here so the viewer's permission is checked first. */
    @GetMapping("/{id}/completion-image")
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_VIEW')")
    public ResponseEntity<Resource> completionImage(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Resource file = taskService.loadCompletionImage(id, currentUser);
        return ResponseEntity.ok()
                .contentType(taskService.completionImageMediaType(id, currentUser))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=task-" + id)
                .header("X-Content-Type-Options", "nosniff")
                .body(file);
    }

    @PostMapping("/generate-routine")
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_CREATE')")
    public ApiResponse<List<GroomDailyTask>> generateRoutine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<GroomDailyTask> tasks = taskService.generateDailyRoutineTasks(date);
        return ApiResponse.success(tasks);
    }

    /**
     * Màn hình "Today Checklist" — gom SOP + buổi tập + lịch thú y trong một ngày.
     */
    @GetMapping("/today")
    @PreAuthorize("hasAuthority('GROOM_DAILY_TASK_VIEW')")
    public ApiResponse<List<TodayTaskItemResponse>> getTodayTasks(
            @RequestParam(required = false) Long groomId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(
                taskService.getTodayAggregatedTasks(groomId, date, currentUser));
    }
}
