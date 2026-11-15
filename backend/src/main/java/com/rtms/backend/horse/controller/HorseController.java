package com.rtms.backend.horse.controller;

import com.rtms.backend.common.storage.FileVariant;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.horse.dto.CreateHorseRequest;
import jakarta.validation.Valid;
import com.rtms.backend.horse.dto.HorseResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.service.HorsePhotoService;
import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/horses")
public class HorseController {

    private static final String BASE_URL = "/api/horses";

    private final HorseService horseService;
    private final HorseSpecialtyService specialtyService;
    private final HorsePhotoService photoService;
    private final com.rtms.backend.horse.service.HorseAdmissionService admissionService;
    private final UserRepository userRepository;

    public HorseController(HorseService horseService,
                           HorseSpecialtyService specialtyService, HorsePhotoService photoService,
                           com.rtms.backend.horse.service.HorseAdmissionService admissionService) {
        this(horseService, specialtyService, photoService, admissionService, null);
    }

    @Autowired
    public HorseController(HorseService horseService,
                           HorseSpecialtyService specialtyService, HorsePhotoService photoService,
                           com.rtms.backend.horse.service.HorseAdmissionService admissionService,
                           @Autowired(required = false) UserRepository userRepository) {
        this.horseService = horseService;
        this.specialtyService = specialtyService;
        this.photoService = photoService;
        this.admissionService = admissionService;
        this.userRepository = userRepository;
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

        Map<Long, String> ownerNames;
        if (userRepository != null) {
            List<Long> ownerIds = horses.stream()
                    .map(Horse::getOwnerId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!ownerIds.isEmpty()) {
                ownerNames = userRepository.findAllById(ownerIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing));
            } else {
                ownerNames = Collections.emptyMap();
            }
        } else {
            ownerNames = Collections.emptyMap();
        }

        List<HorseResponse> response = horses.stream()
                .map(h -> new HorseResponse(
                        h,
                        imageUrls.get(h.getId()),
                        Boolean.TRUE.equals(includeSpecialty) ? specialties.get(h.getId()) : null,
                        h.getOwnerId() != null ? ownerNames.get(h.getOwnerId()) : null))
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    @PreAuthorize("hasRole('CLUB_MANAGER') and hasAuthority('HORSE_CREATE')")
    @PostMapping
    public ApiResponse<Horse> createHorse(@Valid @RequestBody CreateHorseRequest request) {
        return ApiResponse.success(horseService.createHorse(request));
    }

    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    @GetMapping("/{id}")
    public ApiResponse<HorseResponse> getHorseById(@PathVariable Long id) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Horse horse = horseService.getHorseById(id, currentUser);
        Map<Long, String> imageUrls = photoService.resolveImageUrls(List.of(id), BASE_URL);
        String ownerName = null;
        if (userRepository != null && horse.getOwnerId() != null) {
            ownerName = userRepository.findById(horse.getOwnerId())
                    .map(User::getFullName)
                    .orElse(null);
        }
        return ApiResponse.success(new HorseResponse(horse, imageUrls.get(id), null, ownerName));
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
            @RequestParam(required = false) FileVariant size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        // Re-use getHorseById access check (owner sees only their horse).
        horseService.getHorseById(id, currentUser);

        // Resolve document once — both Resource and MediaType from the same lookup.
        HorsePhotoService.PhotoResult photo = photoService.resolvePhoto(id, size);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename("horse-" + id + "-photo", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(photo.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.resource());
    }

    // Phong độ của một chiến mã (fitness-trend, alerts, specialty) nằm ở training HorsePerformanceController.
    // (A horse's performance (fitness-trend, alerts, specialty) lives in training HorsePerformanceController.)

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

    @PreAuthorize("hasAuthority('HORSE_STATUS_UPDATE') or hasRole('CLUB_MANAGER') or hasRole('GROOM')")
    @PutMapping("/{id}/status")
    public ApiResponse<HorseResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.rtms.backend.horse.dto.HorseStatusChangeRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Horse horse = horseService.changeHorseStatus(id, request, currentUser);
        Map<Long, String> imageUrls = photoService.resolveImageUrls(List.of(id), BASE_URL);
        return ApiResponse.success(new HorseResponse(horse, imageUrls.get(id)));
    }

}
