package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreateHorseRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.repository.HorseRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horses")
public class HorseController {

    private final HorseRepository horseRepository;

    public HorseController(HorseRepository horseRepository) {
        this.horseRepository = horseRepository;
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping
    public ApiResponse<List<Horse>> getAllHorses() {
        return ApiResponse.success(horseRepository.findAll());
    }

    @PreAuthorize("hasAuthority('HORSE_CREATE')")
    @PostMapping
    public ApiResponse<Horse> createHorse(@RequestBody CreateHorseRequest request) {
        Horse horse = new Horse();
        horse.setName(request.getName());
        horse.setBreed(request.getBreed());
        horse.setDateOfBirth(request.getDateOfBirth());
        horse.setStableLocation(request.getStableLocation());
        horse.setOwnerId(request.getOwnerId());
        // currentStatus KHÔNG set ở đây - để mặc định "ELIGIBLE" theo giá trị default
        // trong Entity/DB

        Horse saved = horseRepository.save(horse);
        return ApiResponse.success(saved);
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<Horse> getHorseById(@PathVariable Long id) {
        Horse horse = horseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + id));
        return ApiResponse.success(horse);
    }
}