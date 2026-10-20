package com.rtms.backend.medical.service;
import com.rtms.backend.horse.entity.Horse;


import com.rtms.backend.medical.dto.CreateHealthRecordRequest;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final HorseRepository horseRepository;

    public HealthRecordService(HealthRecordRepository healthRecordRepository,
            HorseRepository horseRepository) {
        this.healthRecordRepository = healthRecordRepository;
        this.horseRepository = horseRepository;
    }

    public HealthRecord createHealthRecord(CreateHealthRecordRequest request, AuthenticatedUser currentUser) {
        horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + request.getHorseId()));

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

        return healthRecordRepository.save(record);
    }
}
