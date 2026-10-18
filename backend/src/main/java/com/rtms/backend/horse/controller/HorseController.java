package com.rtms.backend.horse.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.CreateHorseRequest;
import com.rtms.backend.horse.dto.HorseResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.service.HorsePhotoService;
import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/horses")
public class HorseController {

    private static final String BASE_URL = "/api/horses";

    private final HorseService horseService;
    private final HorsePerformanceService performanceService;
    private final HorseSpecialtyService specialtyService;
    private final HorsePhotoService photoService;
    private final com.rtms.backend.horse.service.HorseAdmissionService admissionService;

    public HorseController(HorseService horseService, HorsePerformanceService performanceService,
                           HorseSpecialtyService specialtyService, HorsePhotoService photoService,
                           com.rtms.backend.horse.service.HorseAdmissionService admissionService) {
        this.horseService = horseService;
        this.performanceService = performanceService;
        this.specialtyService = specialtyService;
        this.photoService = photoService;
        this.admissionService = admissionService;
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping
    public ApiResponse<List<HorseResponse>> getAllHorses(
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) HorseStatus status,
            @RequestParam(required = false, defaultValue = "false") Boolean includeSpecialty) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<Horse> horses = horseService.getAllHorses(currentUser, mine, unassigned, status);

        // Batch resolve imageUrls — single pair of queries for the whole list.
        List<Long> ids = horses.stream().map(Horse::getId).collect(Collectors.toList());
        Map<Long, String> imageUrls = photoService.resolveImageUrls(ids, BASE_URL);

        Map<Long, HorseSpecialtyResponse> specialties = Boolean.TRUE.equals(includeSpecialty) && !ids.isEmpty()
                ? specialtyService.getSpecialties(ids)
                : Collections.emptyMap();

        List<HorseResponse> response = horses.stream()
                .map(h -> new HorseResponse(
                        h,
                        imageUrls.get(h.getId()),
                        Boolean.TRUE.equals(includeSpecialty) ? specialties.get(h.getId()) : null))
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    @PreAuthorize("hasAuthority('HORSE_CREATE')")
    @PostMapping
    public ApiResponse<Horse> createHorse(@RequestBody CreateHorseRequest request) {
        return ApiResponse.success(horseService.createHorse(request));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<HorseResponse> getHorseById(@PathVariable Long id) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Horse horse = horseService.getHorseById(id, currentUser);
        Map<Long, String> imageUrls = photoService.resolveImageUrls(List.of(id), BASE_URL);
        return ApiResponse.success(new HorseResponse(horse, imageUrls.get(id)));
    }

    /**
     * Bỏ trống stallId = gỡ ngựa khỏi chuồng, giống assign-groom bỏ trống groomId.
     * Vô hiệu hóa endpoint đổi chuồng của Trainer theo chỉ đạo quản trị; giữ hàm private để tái sử dụng sau này cho Manager nếu cần.
     */
    // @PreAuthorize("hasAuthority('STABLE_STALL_UPDATE')")
    // @PutMapping("/{id}/assign-stall")
    private ApiResponse<Horse> assignStall(
            @PathVariable Long id,
            @RequestParam(required = false) Long stallId) {
        return ApiResponse.success(horseService.assignStall(id, stallId));
    }

    /**
     * Streams the HORSE_PHOTO for a horse. Requires HORSE_VIEW — the same
     * permission as GET /api/horses/{id}. Never exposes admission documents
     * or raw storage paths to the caller.
     */
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getPhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        // Re-use getHorseById access check (owner sees only their horse).
        horseService.getHorseById(id, currentUser);

        // Resolve document once — both Resource and MediaType from the same lookup.
        HorsePhotoService.PhotoResult photo = photoService.resolvePhoto(id);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename("horse-" + id + "-photo", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(photo.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.resource());
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/fitness-trend")
    public ApiResponse<List<HorseFitnessTrendItemResponse>> getFitnessTrend(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getFitnessTrend(id, from, to, currentUser));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/alerts")
    public ApiResponse<List<HorseAlertResponse>> getAlerts(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(performanceService.getHorseAlerts(id, currentUser));
    }

    /** Sở trường thi đấu suy ra từ các buổi tập; vai nào xem được ngựa thì xem được. (Racing specialty inferred from workouts.) */
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/specialty")
    public ApiResponse<HorseSpecialtyResponse> getSpecialty(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(specialtyService.getSpecialty(id, currentUser));
    }

    /**
     * Retrieves the admission history linked to this horse by horseId.
     * Contains actual stored milestone timestamps and review decisions.
     */
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}/admissions")
    public ApiResponse<List<com.rtms.backend.horse.dto.HorseAdmissionHistoryResponse>> getAdmissions(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(admissionService.getAdmissionsForHorse(id, currentUser));
    }

}
