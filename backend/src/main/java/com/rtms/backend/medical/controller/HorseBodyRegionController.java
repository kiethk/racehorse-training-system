package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.entity.HorseBodyRegion;
import com.rtms.backend.medical.repository.HorseBodyRegionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horse-body-regions")
public class HorseBodyRegionController {

    private final HorseBodyRegionRepository horseBodyRegionRepository;

    public HorseBodyRegionController(HorseBodyRegionRepository horseBodyRegionRepository) {
        this.horseBodyRegionRepository = horseBodyRegionRepository;
    }

    @PreAuthorize("hasAuthority('HORSE_BODY_REGION_VIEW')")
    @GetMapping
    public ApiResponse<List<HorseBodyRegion>> getAll() {
        return ApiResponse.success(horseBodyRegionRepository.findByIsActiveTrueOrderByIdAsc());
    }
}
