package com.rtms.backend.service;

import com.rtms.backend.dto.CreateMedicalRecordRequest;
import com.rtms.backend.entity.MedicalRecord;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.MedicalRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final HorseRepository horseRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
            HorseRepository horseRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.horseRepository = horseRepository;
    }

    public MedicalRecord createMedicalRecord(CreateMedicalRecordRequest request, AuthenticatedUser currentUser) {
        horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + request.getHorseId()));

        MedicalRecord record = new MedicalRecord();
        record.setHorseId(request.getHorseId());
        record.setVeterinarianId(currentUser.getUserId());
        record.setExaminedAt(request.getExaminedAt());
        record.setSymptoms(request.getSymptoms());
        record.setClinicalFindings(request.getClinicalFindings());
        record.setDiagnosis(request.getDiagnosis());
        record.setNotes(request.getNotes());
        record.setFollowUpDate(request.getFollowUpDate());
        record.setSourceTrainingWorkoutId(request.getSourceTrainingWorkoutId());

        return medicalRecordRepository.save(record);
    }
}
