package com.rtms.backend.service;

import com.rtms.backend.dto.CreateInjuryRecordRequest;
import com.rtms.backend.dto.TrainingLockStatusResponse;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.HorseBodyRegion;
import com.rtms.backend.entity.InjuryRecord;
import com.rtms.backend.entity.MedicalRecord;
import com.rtms.backend.repository.HorseBodyRegionRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.InjuryRecordRepository;
import com.rtms.backend.repository.MedicalRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InjuryRecordService {

    private static final String STATUS_INJURED = "INJURED";
    private static final String STATUS_ELIGIBLE = "ELIGIBLE";

    private final InjuryRecordRepository injuryRecordRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final HorseRepository horseRepository;
    private final HorseBodyRegionRepository horseBodyRegionRepository;

    public InjuryRecordService(InjuryRecordRepository injuryRecordRepository,
            MedicalRecordRepository medicalRecordRepository,
            HorseRepository horseRepository,
            HorseBodyRegionRepository horseBodyRegionRepository) {
        this.injuryRecordRepository = injuryRecordRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.horseRepository = horseRepository;
        this.horseBodyRegionRepository = horseBodyRegionRepository;
    }

    @Transactional
    public InjuryRecord createInjuryRecord(CreateInjuryRecordRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getMedicalRecordId())
                .orElseThrow(() -> new RuntimeException("Medical record not found with id: "
                        + request.getMedicalRecordId()));

        Horse horse = horseRepository.findById(medicalRecord.getHorseId())
                .orElseThrow(() -> new RuntimeException("Horse not found with id: "
                        + medicalRecord.getHorseId()));

        HorseBodyRegion bodyRegion = horseBodyRegionRepository.findById(request.getBodyRegionId())
                .orElseThrow(() -> new RuntimeException("Body region not found with id: "
                        + request.getBodyRegionId()));

        if (!bodyRegion.isActive()) {
            throw new RuntimeException("Body region not found with id: " + request.getBodyRegionId());
        }

        InjuryRecord injury = new InjuryRecord();
        injury.setHorseId(medicalRecord.getHorseId());
        injury.setMedicalRecordId(medicalRecord.getId());
        injury.setBodyRegionId(bodyRegion.getId());
        injury.setInjuryType(request.getInjuryType());
        injury.setSeverity(request.getSeverity());
        injury.setDescription(request.getDescription());
        injury.setDiagnosedAt(request.getDiagnosedAt());
        injury.setExpectedRecoveryDate(request.getExpectedRecoveryDate());
        injury.setPositionX(request.getPositionX());
        injury.setPositionY(request.getPositionY());
        injury.setPositionZ(request.getPositionZ());

        InjuryRecord savedInjury = injuryRecordRepository.save(injury);

        // (@Transactional đảm bảo lưu InjuryRecord và cập nhật Horse luôn thực hiện cùng nhau)
        horse.setCurrentStatus(STATUS_INJURED);
        horseRepository.save(horse);

        return savedInjury;
    }

    public TrainingLockStatusResponse getTrainingLockStatus(Long horseId) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

        boolean locked = !STATUS_ELIGIBLE.equals(horse.getCurrentStatus());
        return new TrainingLockStatusResponse(horseId, horse.getCurrentStatus(), locked);
    }
}
