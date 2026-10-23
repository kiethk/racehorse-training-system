package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.event.ClinicalRecordCreatedEvent;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CreateHealthRecordRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final HorseRepository horseRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public HealthRecordService(HealthRecordRepository healthRecordRepository,
                               HorseRepository horseRepository,
                               CareScheduleRepository careScheduleRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.healthRecordRepository = healthRecordRepository;
        this.horseRepository = horseRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public HealthRecord createHealthRecord(CreateHealthRecordRequest request, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found with id: " + request.getHorseId()));

        if (request.getCareScheduleId() != null) {
            CareSchedule schedule = careScheduleRepository.findById(request.getCareScheduleId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found with id: " + request.getCareScheduleId()));
            if (!Objects.equals(schedule.getHorseId(), request.getHorseId())) {
                throw new ApiException(HttpStatus.CONFLICT, "SCHEDULE_HORSE_MISMATCH", "Care schedule does not belong to horse " + request.getHorseId());
            }
            if ("VETERINARIAN".equals(currentUser.getRole()) && schedule.getVeterinarianId() != null && !Objects.equals(schedule.getVeterinarianId(), currentUser.getUserId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the assigned veterinarian can create a health record for this schedule");
            }
        }

        HealthRecord record = new HealthRecord();
        record.setHorseId(request.getHorseId());
        record.setVeterinarianId(currentUser.getUserId());
        record.setExaminedAt(request.getExaminedAt());
        record.setSymptoms(request.getSymptoms());
        record.setFindings(request.getFindings());
        record.setDiagnosis(request.getDiagnosis());
        record.setNotes(request.getNotes());
        record.setSourceTrainingWorkoutId(request.getSourceTrainingWorkoutId());
        record.setRecordType(request.getRecordType() != null ? request.getRecordType() : "ILLNESS");
        record.setCareScheduleId(request.getCareScheduleId());
        record.setProductOrService(request.getProductOrService());

        HealthRecord savedRecord = healthRecordRepository.save(record);
        eventPublisher.publishEvent(new ClinicalRecordCreatedEvent(
                savedRecord.getId(),
                horse.getId(),
                horse.getRegistrationNumber(),
                savedRecord.getSymptoms(),
                savedRecord.getDiagnosis(),
                List.of("VETERINARIAN", "HEAD_TRAINER", "CLUB_MANAGER")
        ));
        return savedRecord;
    }
}
