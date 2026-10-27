package com.rtms.backend.stable.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.StableStallService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.rtms.backend.stable.dto.BlockGroomAssignmentDto;

@RestController
@RequestMapping("/api/stalls")
public class StableStallController {

    private final StableStallRepository stableStallRepository;
    private final AreaRepository areaRepository;
    private final StableStallService stableStallService;

    public StableStallController(
            StableStallRepository stableStallRepository,
            AreaRepository areaRepository,
            StableStallService stableStallService) {
        this.stableStallRepository = stableStallRepository;
        this.areaRepository = areaRepository;
        this.stableStallService = stableStallService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STABLE_STALL_VIEW')")
    public ApiResponse<List<StableStall>> getStalls(
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) AreaType areaType,
            @RequestParam(required = false) StallStatus status,
            @RequestParam(required = false) Long groomId) {

        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<StableStall> stalls = findStalls(areaCode, areaType, status, groomId);
        // Groom chỉ thấy khu của mình, chuồng của người khác không kèm groomId.
        // (A groom sees only their own areas, and other grooms' stalls without groomId.)
        if ("GROOM".equals(currentUser.getRole())) {
            stalls = StableStallService.scopeForGroom(
                    stalls, stableStallRepository.findByGroomId(currentUser.getUserId()), currentUser.getUserId());
        }
        return ApiResponse.success(stalls);
    }

    private List<StableStall> findStalls(String areaCode, AreaType areaType, StallStatus status, Long groomId) {
        if (groomId != null) {
            return stableStallRepository.findByGroomId(groomId);
        }

        if (areaCode != null) {
            Area area = areaRepository.findByCode(areaCode)
                    .orElseThrow(() -> new RuntimeException("Area not found with code: " + areaCode));

            if (status != null) {
                return stableStallRepository.findByAreaIdAndStatus(area.getId(), status);
            }

            return stableStallRepository.findByAreaId(area.getId());
        }

        if (areaType != null) {
            List<Long> areaIds = areaRepository.findByType(areaType)
                    .stream()
                    .map(Area::getId)
                    .toList();

            List<StableStall> stalls = stableStallRepository.findAll()
                    .stream()
                    .filter(stall -> areaIds.contains(stall.getAreaId()))
                    .filter(stall -> status == null || stall.getStatus() == status)
                    .toList();

            return stalls;
        }

        if (status != null) {
            return stableStallRepository.findByStatus(status);
        }

        return stableStallRepository.findAll();
    }

    /**
     * Phân công Groom cho chuồng — thuộc Quản lý câu lạc bộ (V63).
     *
     * Quyền riêng STALL_GROOM_ASSIGN, KHÔNG dùng STABLE_STALL_UPDATE: quyền
     * đó cũng gác PUT /api/horses/{id}/assign-stall, mà Huấn luyện viên vẫn
     * phải giữ để đổi chuồng cho ngựa. Dùng chung một quyền thì không tách
     * được hai việc.
     */
    @PutMapping("/{id}/assign-groom")
    @PreAuthorize("hasAuthority('STALL_GROOM_ASSIGN')")
    public ApiResponse<StableStall> assignGroom(
            @PathVariable Long id,
            @RequestBody BlockGroomAssignmentDto request) {
        return ApiResponse.success(stableStallService.assignGroom(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STABLE_STALL_VIEW')")
    public ApiResponse<StableStall> getStallById(@PathVariable Long id) {
        StableStall stall = stableStallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stable stall not found with id: " + id));

        // Cùng luật với danh sách: Groom chỉ xem chuồng trong khu của mình.
        // (Same rule as the list: a groom only sees stalls in their own areas.)
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if ("GROOM".equals(currentUser.getRole())) {
            List<StableStall> scoped = StableStallService.scopeForGroom(
                    List.of(stall), stableStallRepository.findByGroomId(currentUser.getUserId()), currentUser.getUserId());
            if (scoped.isEmpty()) {
                throw new AccessDeniedException("You can only view stalls in the areas you look after");
            }
            return ApiResponse.success(scoped.getFirst());
        }
        return ApiResponse.success(stall);
    }
}
