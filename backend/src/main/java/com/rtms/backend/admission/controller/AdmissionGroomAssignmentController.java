package com.rtms.backend.admission.controller;

import com.rtms.backend.admission.dto.AdmissionDetailResponse;
import com.rtms.backend.admission.dto.GroomAssignmentHistoryItem;
import com.rtms.backend.admission.dto.GroomAssignmentRequest;
import com.rtms.backend.admission.dto.QuarantineGroomOption;
import com.rtms.backend.admission.enums.AssignmentEndReason;
import com.rtms.backend.admission.service.AdmissionQueryService;
import com.rtms.backend.admission.service.GroomAssignmentService;
import com.rtms.backend.admission.service.GroomWorkloadService;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionGroomAssignmentController {
    private final GroomAssignmentService assignments;
    private final GroomWorkloadService workload;
    private final AdmissionQueryService query;

    public AdmissionGroomAssignmentController(GroomAssignmentService assignments,
            GroomWorkloadService workload, AdmissionQueryService query) {
        this.assignments = assignments;
        this.workload = workload;
        this.query = query;
    }

    @PostMapping("/{id}/groom-assignment")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_MANAGER_REVIEW')")
    public ApiResponse<AdmissionDetailResponse> reassign(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody GroomAssignmentRequest request) {
        assignments.reassign(id, AssignmentEndReason.MANUAL, user.getUserId(), request.groomId());
        return ApiResponse.success(query.getAdmissionDetail(id));
    }

    @GetMapping("/{id}/groom-assignments")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_VIEW')")
    public ApiResponse<List<GroomAssignmentHistoryItem>> history(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(assignments.history(id, user));
    }

    @GetMapping("/quarantine-grooms")
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_MANAGER_REVIEW')")
    public ApiResponse<List<QuarantineGroomOption>> quarantineGrooms() {
        return ApiResponse.success(workload.describeQuarantineGrooms().stream()
                .map(g -> new QuarantineGroomOption(g.id(), g.name(), g.workload(), g.full())).toList());
    }
}
