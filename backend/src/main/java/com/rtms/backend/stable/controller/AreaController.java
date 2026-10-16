package com.rtms.backend.stable.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.service.AreaService;
import com.rtms.backend.stable.service.AreaLifecycleService;
import com.rtms.backend.stable.service.StableAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaRepository areaRepository;
    private final AreaService areaService;
    private final StableAssignmentService stableAssignmentService;
    private final AreaLifecycleService lifecycle;

    public AreaController(AreaRepository areaRepository,
                          AreaService areaService,
                          StableAssignmentService stableAssignmentService, AreaLifecycleService lifecycle) {
        this.areaRepository = areaRepository;
        this.areaService = areaService;
        this.stableAssignmentService = stableAssignmentService;
        this.lifecycle = lifecycle;
    }

    @GetMapping("/{id}/deletion-check")
    @PreAuthorize("hasAuthority('AREA_DELETE')")
    public ApiResponse<AreaDeletionCheck> checkDeletion(@PathVariable Long id) {
        return ApiResponse.success(lifecycle.check(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AREA_DELETE')")
    public ApiResponse<AreaDeletionResult> deleteArea(@PathVariable Long id) {
        return ApiResponse.success(lifecycle.delete(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AREA_CREATE')")
    public ApiResponse<Area> createArea(@Valid @RequestBody CreateAreaRequest request) {
        Area area = areaService.createArea(request.getType());
        return ApiResponse.success(area);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AREA_VIEW')")
    public ApiResponse<List<Area>> getAreas(
            @RequestParam(required = false) AreaType type
    ) {
        List<Area> areas;

        if (type != null) {
            areas = areaRepository.findByType(type);
        } else {
            areas = areaRepository.findAll();
        }

        return ApiResponse.success(areas);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AREA_VIEW')")
    public ApiResponse<Area> getAreaById(@PathVariable Long id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Area not found with id: " + id));

        return ApiResponse.success(area);
    }

    /**
     * Batch assignment endpoint for Manager Stable: assigns/unassigns/replaces
     * Trainers for Areas and Grooms for entire 3-stall blocks in a single transaction.
     */
    @RequestMapping(value = "/assignments", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAuthority('AREA_UPDATE') and hasAuthority('STALL_GROOM_ASSIGN')")
    public ApiResponse<StableAssignmentResponse> applyAssignments(
            @Valid @RequestBody BatchStableAssignmentRequest request) {
        return ApiResponse.success(stableAssignmentService.applyAssignments(request));
    }

    /**
     * Assign / change / unassign Trainer for an Area.
     */
    @PutMapping("/{id}/trainer")
    @PreAuthorize("hasAuthority('AREA_UPDATE')")
    public ApiResponse<StableAssignmentResponse> assignTrainer(
            @PathVariable Long id,
            @RequestBody AreaTrainerAssignmentDto dto) {
        dto.setAreaId(id);
        BatchStableAssignmentRequest req = new BatchStableAssignmentRequest();
        req.setTrainerAssignments(List.of(dto));
        return ApiResponse.success(stableAssignmentService.applyAssignments(req));
    }

    /**
     * Assign / change / unassign Groom for an entire 3-stall block.
     */
    @PutMapping("/{id}/blocks/{blockNumber}/groom")
    @PreAuthorize("hasAuthority('STALL_GROOM_ASSIGN')")
    public ApiResponse<StableAssignmentResponse> assignBlockGroom(
            @PathVariable Long id,
            @PathVariable Integer blockNumber,
            @RequestBody BlockGroomAssignmentDto dto) {
        dto.setAreaId(id);
        dto.setBlockNumber(blockNumber);
        BatchStableAssignmentRequest req = new BatchStableAssignmentRequest();
        req.setGroomAssignments(List.of(dto));
        return ApiResponse.success(stableAssignmentService.applyAssignments(req));
    }
}
