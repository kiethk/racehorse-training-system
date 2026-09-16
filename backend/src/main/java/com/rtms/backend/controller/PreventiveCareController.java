package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.CreatePreventiveCareRecordRequest;
import com.rtms.backend.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.PreventiveCareRecord;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.PreventiveCareRecordRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preventive-care-schedules")
public class PreventiveCareController {

    private final PreventiveCareScheduleRepository scheduleRepository;
    private final PreventiveCareRecordRepository recordRepository;
    private final HorseRepository horseRepository;

    public PreventiveCareController(PreventiveCareScheduleRepository scheduleRepository,
            PreventiveCareRecordRepository recordRepository,
            HorseRepository horseRepository) {
        this.scheduleRepository = scheduleRepository;
        this.recordRepository = recordRepository;
        this.horseRepository = horseRepository;
    }

    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_CREATE')")
    @PostMapping
    public ApiResponse<PreventiveCareSchedule> createSchedule(
            @RequestBody CreatePreventiveCareScheduleRequest request) {
        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setHorseId(request.getHorseId());
        schedule.setVeterinarianId(request.getVeterinarianId());
        schedule.setCareType(request.getCareType());
        schedule.setScheduledDate(request.getScheduledDate());
        schedule.setDescription(request.getDescription());
        // status mặc định "SCHEDULED" đã set sẵn trong Entity

        PreventiveCareSchedule saved = scheduleRepository.save(schedule);
        return ApiResponse.success(saved);
    }

    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_VIEW')")
    @GetMapping
    public ApiResponse<List<PreventiveCareSchedule>> getUpcomingByHorse(@RequestParam Long horseId) {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

            if (!currentUser.getUserId().equals(horse.getOwnerId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only view preventive care schedules for horses you own");
            }
        }

        return ApiResponse.success(scheduleRepository.findByHorseIdOrderByScheduledDateAsc(horseId));
    }

    @PreAuthorize("hasAuthority('PREVENTIVE_CARE_CREATE')")
    @PostMapping("/{id}/records")
    public ApiResponse<PreventiveCareRecord> recordCompletion(@PathVariable Long id,
            @RequestBody CreatePreventiveCareRecordRequest request) {
        PreventiveCareSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));

        PreventiveCareRecord record = new PreventiveCareRecord();
        record.setHorseId(schedule.getHorseId());
        record.setScheduleId(schedule.getId());
        record.setVeterinarianId(request.getVeterinarianId());
        record.setCareType(schedule.getCareType());
        record.setPerformedAt(request.getPerformedAt());
        record.setPerformedByName(request.getPerformedByName());
        record.setProductOrService(request.getProductOrService());
        record.setResult(request.getResult());
        record.setNotes(request.getNotes());
        record.setNextDueDate(request.getNextDueDate());

        PreventiveCareRecord savedRecord = recordRepository.save(record);

        // Cập nhật trạng thái schedule gốc thành COMPLETED
        schedule.setStatus("COMPLETED");
        scheduleRepository.save(schedule);

        return ApiResponse.success(savedRecord);
    }
}