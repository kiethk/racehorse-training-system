package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupCandidateResponse;
import com.rtms.backend.training.dto.GroupHorseRequest;
import com.rtms.backend.training.dto.RenameTrainingGroupRequest;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.service.TrainingGroupService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller chỉ nhận request và gọi service; mọi luật thành viên và phân quyền theo chủ nhóm nằm ở TrainingGroupService.
 * (The controller only receives requests and calls the service; every membership rule and the owner check live in
 * TrainingGroupService.)
 */
@RestController
@RequestMapping("/api/training-groups")
public class TrainingGroupController {

    private final TrainingGroupService groupService;

    public TrainingGroupController(TrainingGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TRAINING_GROUP_VIEW')")
    public ApiResponse<List<TrainingGroupResponse>> getGroups(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.getGroups(currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<TrainingGroupResponse> createGroup(
            @Valid @RequestBody CreateTrainingGroupRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.createGroup(request, currentUser));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<TrainingGroupResponse> renameGroup(
            @PathVariable Long id,
            @Valid @RequestBody RenameTrainingGroupRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.renameGroup(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        groupService.deleteGroup(id, currentUser);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/candidates")
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<List<GroupCandidateResponse>> getCandidates(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.getCandidates(id, currentUser));
    }

    @PostMapping("/{id}/horses")
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<TrainingGroupResponse> addHorse(
            @PathVariable Long id,
            @Valid @RequestBody GroupHorseRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.addHorse(id, request.horseId(), currentUser));
    }

    /** Đưa ngựa từ nhóm hiện tại của nó sang nhóm {id}. (Moves the horse from its current group into group {id}.) */
    @PostMapping("/{id}/horses/move")
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<TrainingGroupResponse> moveHorse(
            @PathVariable Long id,
            @Valid @RequestBody GroupHorseRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.moveHorse(id, request.horseId(), currentUser));
    }

    @DeleteMapping("/{id}/horses/{horseId}")
    @PreAuthorize("hasAuthority('TRAINING_GROUP_MANAGE')")
    public ApiResponse<TrainingGroupResponse> removeHorse(
            @PathVariable Long id,
            @PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(groupService.removeHorse(id, horseId, currentUser));
    }
}
