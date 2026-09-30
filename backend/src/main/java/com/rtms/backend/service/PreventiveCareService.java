package com.rtms.backend.service;

import com.rtms.backend.dto.CompletePreventiveCareScheduleRequest;
import com.rtms.backend.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.config.ApiException;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreventiveCareService {
    private static final Set<String> VET_CARE_TYPES = Set.of("ROUTINE_EXAM", "VACCINATION", "DEWORMING");

    private final PreventiveCareScheduleRepository scheduleRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HorseRepository horseRepository;

    public PreventiveCareService(PreventiveCareScheduleRepository scheduleRepository,
            HealthRecordRepository healthRecordRepository,
            HorseRepository horseRepository) {
        this.scheduleRepository = scheduleRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.horseRepository = horseRepository;
    }

    public PreventiveCareSchedule createSchedule(CreatePreventiveCareScheduleRequest request, AuthenticatedUser currentUser) {
        if (!VET_CARE_TYPES.contains(request.getCareType() == null ? "" : request.getCareType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Only ROUTINE_EXAM, VACCINATION and DEWORMING can be created here; INITIAL_EXAM is created by the Groom/admission flow");
        }
        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setHorseId(request.getHorseId());
        schedule.setVeterinarianId(currentUser.getUserId());
        schedule.setCareType(request.getCareType());
        schedule.setScheduledDate(request.getScheduledDate());
        schedule.setDescription(request.getDescription());
        // status mặc định "SCHEDULED" đã set sẵn trong Entity

        return scheduleRepository.save(schedule);
    }

    public List<PreventiveCareSchedule> getSchedulesByHorse(Long horseId, AuthenticatedUser currentUser)
            throws AccessDeniedException {
        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            Horse horse = horseRepository.findById(horseId)
                    .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

            if (!currentUser.getUserId().equals(horse.getOwnerId())) {
                throw new AccessDeniedException("You can only view schedules for horses you own");
            }
        }
        return scheduleRepository.findByHorseIdOrderByScheduledDateAsc(horseId);
    }

    @Transactional
    public HealthRecord recordCompletion(Long scheduleId, CompletePreventiveCareScheduleRequest request, AuthenticatedUser currentUser) {
        PreventiveCareSchedule schedule = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Schedule not found"));
        if (!"PENDING".equals(schedule.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "Schedule is not pending");
        }
        if ("INITIAL_EXAM".equals(schedule.getCareType())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE",
                    "INITIAL_EXAM must be completed through the admission vet review");
        }

        HealthRecord record = new HealthRecord();
        record.setHorseId(schedule.getHorseId());
        record.setPreventiveCareScheduleId(schedule.getId());
        record.setRecordType(schedule.getCareType());
        
        if (schedule.getVeterinarianId() != null && !schedule.getVeterinarianId().equals(currentUser.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_ASSIGNED_TO_OTHER_VET", "Schedule assigned to another veterinarian");
        }
        record.setVeterinarianId(currentUser.getUserId());
        schedule.setVeterinarianId(currentUser.getUserId());
        record.setExaminedAt(LocalDateTime.now());
        record.setProductOrService(request.getProductOrService());
        record.setFindings(request.getResult());
        record.setNotes(request.getNotes());
        record.setFollowUpDate(request.getNextDueDate());

        HealthRecord savedRecord = healthRecordRepository.save(record);

        schedule.setStatus("COMPLETED");
        scheduleRepository.save(schedule);
        
        if (request.getNextDueDate() != null) {
            PreventiveCareSchedule nextSchedule = new PreventiveCareSchedule();
            nextSchedule.setHorseId(schedule.getHorseId());
            nextSchedule.setVeterinarianId(schedule.getVeterinarianId());
            nextSchedule.setCareType(schedule.getCareType());
            nextSchedule.setScheduledDate(request.getNextDueDate());
            nextSchedule.setDescription("Follow-up/Next due for: " + schedule.getCareType());
            nextSchedule.setStatus("PENDING");
            scheduleRepository.save(nextSchedule);
        }

        return savedRecord;
    }
}
