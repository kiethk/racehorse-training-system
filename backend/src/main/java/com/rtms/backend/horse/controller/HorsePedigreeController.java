package com.rtms.backend.horse.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.CreateHorsePedigreeRequest;
import com.rtms.backend.horse.dto.HorsePedigreeResponse;
import com.rtms.backend.horse.service.HorsePedigreeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/horses")
public class HorsePedigreeController {
    private final HorsePedigreeService horsePedigreeService;

    public HorsePedigreeController(HorsePedigreeService horsePedigreeService) {
        this.horsePedigreeService = horsePedigreeService;
    }

    @GetMapping("/{horseId}/pedigree")
    @PreAuthorize("hasAnyAuthority('HORSE_PEDIGREE_VIEW', 'HORSE_VIEW')")
    public ApiResponse<HorsePedigreeResponse> getPedigree(@PathVariable Long horseId) {
        return ApiResponse.success(horsePedigreeService.getByHorseId(horseId));
    }

    @PostMapping("/{horseId}/pedigree")
    @PreAuthorize("hasAnyAuthority('HORSE_PEDIGREE_MANAGE', 'HORSE_CREATE') or hasRole('CLUB_MANAGER')")
    public ApiResponse<HorsePedigreeResponse> createOrUpdatePedigree(
            @PathVariable Long horseId,
            @Valid @RequestBody CreateHorsePedigreeRequest request
    ) {
        return ApiResponse.success(
                horsePedigreeService.createOrUpdate(
                        horseId,
                        request
                )
        );
    }
}
