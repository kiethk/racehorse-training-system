package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.RescheduleLotRequest;
import com.rtms.backend.dto.TrainingLotResponse;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.Subject;
import com.rtms.backend.entity.TrainingLot;
import com.rtms.backend.entity.TrainingWorkout;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.SubjectRepository;
import com.rtms.backend.repository.TrainingWorkoutRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.TrainingLotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/lots")
public class TrainingLotController {

    private final TrainingLotService lotService;
    private final TrainingWorkoutRepository workoutRepository;
    private final SubjectRepository subjectRepository;
    private final HorseRepository horseRepository;

    public TrainingLotController(TrainingLotService lotService,
                                 TrainingWorkoutRepository workoutRepository,
                                 SubjectRepository subjectRepository,
                                 HorseRepository horseRepository) {
        this.lotService = lotService;
        this.workoutRepository = workoutRepository;
        this.subjectRepository = subjectRepository;
        this.horseRepository = horseRepository;
    }

    /**
     * Xem lịch lot theo khoảng ngày.
     * Giúp Trainer biết ngày nào còn khe trống trước khi chọn ngày bắt đầu —
     * biến quyết định mò mẫm thành quyết định có căn cứ.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('TRAINING_LOT_VIEW')")
    public ApiResponse<List<TrainingLotResponse>> getLots(
            @RequestParam(required = false) Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        Long target = "HEAD_TRAINER".equalsIgnoreCase(currentUser.getRole())
                ? currentUser.getUserId()
                : (trainerId != null ? trainerId : currentUser.getUserId());

        List<TrainingLotResponse> result = new ArrayList<>();
        for (TrainingLot lot : lotService.getLots(target, from, to)) {
            result.add(toResponse(lot));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TRAINING_LOT_VIEW')")
    public ApiResponse<TrainingLotResponse> getLotById(@PathVariable Long id) {
        TrainingLot lot = lotService.findLotById(id);
        return ApiResponse.success(toResponse(lot));
    }

    /** Dời giờ lot — cả hàng ngựa trong lot dời theo. */
    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('TRAINING_LOT_UPDATE')")
    public ApiResponse<TrainingLotResponse> reschedule(
            @PathVariable Long id,
            @RequestBody RescheduleLotRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        TrainingLot lot = lotService.rescheduleLot(id, request.getNewStartTime(), currentUser.getUserId());
        return ApiResponse.success(toResponse(lot));
    }

    /** Huỷ lot và mọi buổi tập SCHEDULED bên trong. */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('TRAINING_LOT_UPDATE')")
    public ApiResponse<TrainingLotResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        TrainingLot lot = lotService.cancelLot(id, currentUser.getUserId());
        return ApiResponse.success(toResponse(lot));
    }

    private TrainingLotResponse toResponse(TrainingLot lot) {
        Subject subject = subjectRepository.findById(lot.getSubjectId()).orElse(null);
        List<TrainingWorkout> workouts = workoutRepository.findByLotId(lot.getId());

        List<String> horseNames = new ArrayList<>();
        for (TrainingWorkout w : workouts) {
            horseNames.add(horseRepository.findById(w.getHorseId())
                    .map(Horse::getName).orElse("#" + w.getHorseId()));
        }

        return new TrainingLotResponse(
                lot,
                subject != null ? subject.getName() : "#" + lot.getSubjectId(),
                subject != null ? subject.getDurationMinutes() : null,
                lotService.countOccupied(lot.getId()),
                horseNames);
    }
}
