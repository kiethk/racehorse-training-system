package com.rtms.backend.controller;

import com.rtms.backend.dto.*;
import com.rtms.backend.enums.VetExamStatus;
import com.rtms.backend.enums.VetExamType;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.VetExamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vet-exams")
public class VetExamController {
    private final VetExamService service;

    public VetExamController(VetExamService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('VET_EXAM_CREATE')")
    public ApiResponse<VetExamResponse> create(@Valid @RequestBody CreateVetExamRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ApiResponse.success(service.createExam(request, actor.getUserId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VET_EXAM_VIEW')")
    public ApiResponse<Page<VetExamResponse>> list(
            @RequestParam(required = false) VetExamStatus status,
            @RequestParam(required = false) VetExamType type, Pageable pageable) {
        return ApiResponse.success(service.list(status, type, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VET_EXAM_VIEW')")
    public ApiResponse<VetExamResponse> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('VET_EXAM_MANAGE') and principal.role == 'VETERINARIAN'")
    public ApiResponse<VetExamResponse> start(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser vet) {
        return ApiResponse.success(service.start(id, vet.getUserId()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('VET_EXAM_MANAGE') and principal.role == 'VETERINARIAN'")
    public ApiResponse<VetExamResponse> complete(@PathVariable Long id,
            @Valid @RequestBody CompleteVetExamRequest request,
            @AuthenticationPrincipal AuthenticatedUser vet) {
        return ApiResponse.success(service.complete(id, request, vet.getUserId()));
    }
}
