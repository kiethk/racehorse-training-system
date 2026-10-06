package com.rtms.backend.controller;
import com.rtms.backend.dto.HorseHealthMetricRequest;
import com.rtms.backend.dto.HorseHealthMetricResponse;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.HorseHealthMetric;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.HorseHealthMetricRepository;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/horses/{horseId}/health-metrics")
public class HorseHealthMetricController {
    private final HorseHealthMetricRepository metrics;
    private final HealthRecordRepository records;
    private final HorseRepository horses;

    public HorseHealthMetricController(HorseHealthMetricRepository metrics, HealthRecordRepository records,
            HorseRepository horses) {
        this.metrics = metrics;
        this.records = records;
        this.horses = horses;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HORSE_HEALTH_METRIC_VIEW')")
    public ApiResponse<List<HorseHealthMetricResponse>> list(@PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        Horse horse = horses.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Horse not found"));
        if ("HORSE_OWNER".equals(actor.getRole()) && !Objects.equals(horse.getOwnerId(), actor.getUserId())) {
            throw new AccessDeniedException("You can only view metrics for horses you own");
        }
        return ApiResponse.success(metrics.findByHorseIdOrderByRecordedAtDesc(horseId).stream()
                .map(HorseHealthMetricResponse::from).toList());
    }

    @PostMapping("/health-records/{recordId}")
    @PreAuthorize("hasAuthority('HORSE_HEALTH_METRIC_CREATE')")
    public ApiResponse<HorseHealthMetricResponse> create(@PathVariable Long horseId, @PathVariable Long recordId,
            @Valid @RequestBody HorseHealthMetricRequest input,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        HealthRecord record = records.findById(recordId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Health record not found"));
        if (!Objects.equals(record.getHorseId(), horseId) || !Objects.equals(record.getVeterinarianId(), actor.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Health record does not belong to this horse and veterinarian");
        }
        HorseHealthMetric metric = new HorseHealthMetric();
        metric.setHorseId(horseId);
        metric.setHealthRecordId(recordId);
        metric.setRecordedAt(LocalDateTime.now());
        metric.setHeartRate(input.getHeartRate());
        metric.setTemperature(input.getTemperature());
        metric.setWeight(input.getWeight());
        metric.setRespiratoryRate(input.getRespiratoryRate());
        metric.setHydrationStatus(input.getHydrationStatus());
        metric.setBodyConditionScore(input.getBodyConditionScore());
        metric.setNotes(input.getNotes());
        return ApiResponse.success(HorseHealthMetricResponse.from(metrics.save(metric)));
    }
}
