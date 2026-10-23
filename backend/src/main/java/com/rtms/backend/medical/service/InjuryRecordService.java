package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.medical.dto.HorseHealthSummaryResponse;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InjuryRecordService {

    private final InjuryRecordRepository injuryRecordRepository;
    private final HorseRepository horseRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public InjuryRecordService(
            InjuryRecordRepository injuryRecordRepository,
            HorseRepository horseRepository,
            HealthRecordRepository healthRecordRepository,
            CareScheduleRepository careScheduleRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.injuryRecordRepository = injuryRecordRepository;
        this.horseRepository = horseRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<InjuryRecord> getInjuryRecordsByHorse(Long horseId) {
        return injuryRecordRepository.findByHorseIdOrderByDiagnosedAtDesc(horseId);
    }

    /** Lịch sử khám của một ngựa, mới nhất trước (health history trong Health tab). */
    @Transactional(readOnly = true)
    public List<HealthRecord> getHealthRecordsByHorse(Long horseId) {
        return healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horseId);
    }

    @Transactional(readOnly = true)
    public HorseHealthSummaryResponse getHealthSummary(Long horseId) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found with id: " + horseId));

        boolean hasUrgent = careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(
                horseId, CareType.URGENT, List.of(CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS));

        List<InjuryRecord> injuries = injuryRecordRepository.findByHorseIdOrderByDiagnosedAtDesc(horseId);
        int activeInjuries = (int) injuries.stream().filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus())).count();

        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horseId);
        LocalDateTime lastExam = records.isEmpty() ? null : records.get(0).getExaminedAt();
        String lastDiag = records.isEmpty() ? null : records.get(0).getDiagnosis();

        return new HorseHealthSummaryResponse(
                horse.getId(),
                horse.getName(),
                horse.getTrainingDecision(),
                horse.getTrainingDecisionReason(),
                hasUrgent,
                lastExam,
                lastDiag,
                activeInjuries,
                null
        );
    }

    @Transactional
    public InjuryRecord correctInjuryRecord(Long horseId, Long id, CorrectInjuryRecordRequest req, Long userId) {
        InjuryRecord existing = injuryRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INJURY_NOT_FOUND", "Injury record not found"));

        if (existing.getHorseId() == null || !existing.getHorseId().equals(horseId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INJURY_NOT_FOUND", "Injury record not found for this horse");
        }

        if (existing.getStatus() != null && "SUPERSEDED".equalsIgnoreCase(existing.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "INJURY_ALREADY_SUPERSEDED", "Cannot correct an injury record that has already been superseded");
        }

        // Tạo bản mới hiệu chỉnh
        InjuryRecord corrected = new InjuryRecord();
        corrected.setHorseId(existing.getHorseId());
        corrected.setHealthRecordId(existing.getHealthRecordId());
        corrected.setBodyRegionId(existing.getBodyRegionId());
        corrected.setInjuryType(req.injuryType() != null ? req.injuryType() : existing.getInjuryType());
        corrected.setSeverity(req.severity() != null ? req.severity() : existing.getSeverity());
        corrected.setDescription(req.description() != null ? req.description() : existing.getDescription());
        corrected.setStatus(req.status() != null ? req.status() : existing.getStatus());
        corrected.setDiagnosedAt(existing.getDiagnosedAt());
        corrected.setExpectedRecoveryDate(existing.getExpectedRecoveryDate());
        corrected.setPositionX(existing.getPositionX());
        corrected.setPositionY(existing.getPositionY());
        corrected.setPositionZ(existing.getPositionZ());

        InjuryRecord saved = injuryRecordRepository.save(corrected);

        // Đánh dấu bản cũ
        existing.setStatus("SUPERSEDED");
        injuryRecordRepository.save(existing);

        // Audit Log
        AuditLog audit = new AuditLog();
        audit.setAction("INJURY_RECORD_CORRECTION: " + req.correctionReason());
        audit.setEntityName("InjuryRecord");
        audit.setEntityId(saved.getId());
        User u = userRepository.getReferenceById(userId);
        audit.setUser(u);
        auditLogRepository.save(audit);

        return saved;
    }
}
