package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.medical.dto.CreateInjuryRecordRequest;
import com.rtms.backend.medical.dto.HorseHealthSummaryResponse;
import com.rtms.backend.medical.dto.MarkerPlacementRequest;
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

    @Transactional(readOnly = true)
    public List<InjuryRecord> getActiveInjuryRecordsByHorse(Long horseId) {
        return injuryRecordRepository.findByHorseIdAndStatusIgnoreCaseOrderByDiagnosedAtDesc(horseId, "ACTIVE");
    }

    @Transactional
    public InjuryRecord createInjuryRecord(Long horseId, CreateInjuryRecordRequest req) {
        InjuryRecord r = new InjuryRecord();
        r.setHorseId(horseId);
        r.setHealthRecordId(req.healthRecordId());
        r.setInjuryType(req.injuryType());
        r.setBodyRegionId(req.bodyRegionId());
        r.setSeverity(req.severity());
        r.setDescription(req.description());
        r.setDiagnosedAt(req.diagnosedAt());
        r.setStatus("ACTIVE");
        r.setPositionX(req.positionX());
        r.setPositionY(req.positionY());
        r.setPositionZ(req.positionZ());
        r.setZoneId(req.zoneId());
        r.setLocalX(req.localX());
        r.setLocalY(req.localY());
        r.setLocalZ(req.localZ());
        r.setNormalX(req.normalX());
        r.setNormalY(req.normalY());
        r.setNormalZ(req.normalZ());
        r.setMeshName(req.meshName());
        return injuryRecordRepository.save(r);
    }

    @Transactional
    public InjuryRecord recoverInjuryRecord(Long horseId, Long id, Long userId) {
        InjuryRecord existing = injuryRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INJURY_NOT_FOUND", "Injury record not found"));
        if (existing.getHorseId() == null || !existing.getHorseId().equals(horseId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INJURY_NOT_FOUND", "Injury record not found for this horse");
        }
        if ("RECOVERED".equalsIgnoreCase(existing.getStatus())) {
            return existing;
        }
        existing.setStatus("RECOVERED");
        existing.setRecoveredAt(LocalDateTime.now());
        InjuryRecord saved = injuryRecordRepository.save(existing);

        AuditLog audit = new AuditLog();
        audit.setAction("INJURY_RECORD_RECOVERY");
        audit.setEntityName("InjuryRecord");
        audit.setEntityId(saved.getId());
        User u = userRepository.getReferenceById(userId);
        audit.setUser(u);
        auditLogRepository.save(audit);

        return saved;
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
        // Preserve durable marker data when correcting (T4).
        corrected.setZoneId(existing.getZoneId());
        corrected.setLocalX(existing.getLocalX());
        corrected.setLocalY(existing.getLocalY());
        corrected.setLocalZ(existing.getLocalZ());
        corrected.setNormalX(existing.getNormalX());
        corrected.setNormalY(existing.getNormalY());
        corrected.setNormalZ(existing.getNormalZ());
        corrected.setMeshName(existing.getMeshName());
        // Vet repositioned the marker on the model — override the captured fields.
        if (req.marker() != null) {
            applyMarkerOverride(corrected, req.marker());
        }

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

    private static void applyMarkerOverride(InjuryRecord target, MarkerPlacementRequest marker) {
        if (marker.positionX() != null) target.setPositionX(marker.positionX());
        if (marker.positionY() != null) target.setPositionY(marker.positionY());
        if (marker.positionZ() != null) target.setPositionZ(marker.positionZ());
        if (marker.zoneId() != null) target.setZoneId(marker.zoneId());
        if (marker.localX() != null) target.setLocalX(marker.localX());
        if (marker.localY() != null) target.setLocalY(marker.localY());
        if (marker.localZ() != null) target.setLocalZ(marker.localZ());
        if (marker.normalX() != null) target.setNormalX(marker.normalX());
        if (marker.normalY() != null) target.setNormalY(marker.normalY());
        if (marker.normalZ() != null) target.setNormalZ(marker.normalZ());
        if (marker.meshName() != null) target.setMeshName(marker.meshName());
    }
}
