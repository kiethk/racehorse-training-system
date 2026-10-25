package com.rtms.backend.medical.service;

import com.rtms.backend.medical.dto.CorrectInjuryRecordRequest;
import com.rtms.backend.medical.dto.HorseHealthSummaryResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InjuryRecordServiceTest {

    @Mock private InjuryRecordRepository injuryRecordRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private com.rtms.backend.identity.repository.UserRepository userRepository;

    private InjuryRecordService injuryRecordService;
    private Horse horse;
    private InjuryRecord injury;

    @BeforeEach
    void setUp() {
        injuryRecordService = new InjuryRecordService(
                injuryRecordRepository, horseRepository, healthRecordRepository,
                careScheduleRepository, auditLogRepository, userRepository);

        horse = new Horse();
        horse.setId(10L);
        horse.setName("Thunder");
        horse.setTrainingDecision(TrainingDecision.ALLOWED);

        injury = new InjuryRecord();
        injury.setId(100L);
        injury.setHorseId(10L);
        injury.setHealthRecordId(50L);
        injury.setBodyRegionId(2L);
        injury.setInjuryType("Sprain");
        injury.setSeverity("HIGH");
        injury.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("getHealthSummary: Trả về trạng thái huấn luyện, số ca chấn thương và ca khẩn cấp")
    void getHealthSummary_returnsHorseStatusAndActiveInjuries() {
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(injuryRecordRepository.findByHorseIdOrderByDiagnosedAtDesc(10L)).thenReturn(List.of(injury));
        when(healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(10L)).thenReturn(Collections.emptyList());
        when(careScheduleRepository.existsByHorseIdAndCareTypeAndStatusIn(eq(10L), any(), any())).thenReturn(false);

        HorseHealthSummaryResponse res = injuryRecordService.getHealthSummary(10L);

        assertNotNull(res);
        assertEquals("Thunder", res.horseName());
        assertEquals(TrainingDecision.ALLOWED, res.trainingDecision());
        assertEquals(1, res.activeInjuryCount());
        assertFalse(res.hasPendingUrgent());
    }

    @Test
    @DisplayName("correctInjuryRecord: Tạo bản mới, đánh dấu bản cũ SUPERSEDED và ghi Audit Log")
    void correctInjuryRecord_createsNewAndSupersedesOld() {
        when(injuryRecordRepository.findById(100L)).thenReturn(Optional.of(injury));
        when(injuryRecordRepository.save(any(InjuryRecord.class))).thenAnswer(i -> i.getArgument(0));

        CorrectInjuryRecordRequest req = new CorrectInjuryRecordRequest(
                "Đính chính mức độ nghiêm trọng", "Sprain", "CRITICAL", "Đau nặng hơn dự kiến", "ACTIVE");

        InjuryRecord corrected = injuryRecordService.correctInjuryRecord(10L, 100L, req, 20L);

        assertNotNull(corrected);
        assertEquals("CRITICAL", corrected.getSeverity());
        assertEquals("SUPERSEDED", injury.getStatus());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("correctInjuryRecord: marker không null ghi đè tọa độ 3D")
    void correctInjuryRecord_appliesMarkerOverrideWhenPresent() {
        injury.setPositionX(java.math.BigDecimal.valueOf(1.1));
        injury.setZoneId("foreleg");
        when(injuryRecordRepository.findById(100L)).thenReturn(Optional.of(injury));
        when(injuryRecordRepository.save(any(InjuryRecord.class))).thenAnswer(i -> i.getArgument(0));

        CorrectInjuryRecordRequest req = new CorrectInjuryRecordRequest(
                "Repositioned marker after re-examination", null, null, null, null,
                new com.rtms.backend.medical.dto.MarkerPlacementRequest(
                        java.math.BigDecimal.valueOf(2.5), java.math.BigDecimal.valueOf(1.2), java.math.BigDecimal.valueOf(-0.4),
                        "hindquarters", null, null, null, null, null, null, null));

        InjuryRecord corrected = injuryRecordService.correctInjuryRecord(10L, 100L, req, 20L);

        assertEquals(java.math.BigDecimal.valueOf(2.5), corrected.getPositionX());
        assertEquals("hindquarters", corrected.getZoneId());
        assertEquals("HIGH", corrected.getSeverity());
    }

    @Test
    @DisplayName("correctInjuryRecord: không có marker thì giữ nguyên tọa độ đã lưu")
    void correctInjuryRecord_withoutMarkerKeepsStoredCoordinates() {
        injury.setPositionX(java.math.BigDecimal.valueOf(1.1));
        injury.setZoneId("foreleg");
        when(injuryRecordRepository.findById(100L)).thenReturn(Optional.of(injury));
        when(injuryRecordRepository.save(any(InjuryRecord.class))).thenAnswer(i -> i.getArgument(0));

        CorrectInjuryRecordRequest req = new CorrectInjuryRecordRequest(
                "Severity correction only", "Sprain", "MEDIUM", null, "ACTIVE");

        InjuryRecord corrected = injuryRecordService.correctInjuryRecord(10L, 100L, req, 20L);

        assertEquals(java.math.BigDecimal.valueOf(1.1), corrected.getPositionX());
        assertEquals("foreleg", corrected.getZoneId());
        assertEquals("MEDIUM", corrected.getSeverity());
    }
}
