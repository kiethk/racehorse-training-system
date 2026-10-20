package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.BatchVaccinationRequest;
import com.rtms.backend.medical.dto.BatchVaccinationResult;
import com.rtms.backend.medical.dto.VaccinationRequest;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.VaccinationSource;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Ghi nhận tiêm hàng loạt (docs/preventive-care-flow.md mục 5.7). Tiêm không phải khám:
 * mỗi ngựa có một health record VACCINATION không gắn lịch, không có kết luận tập luyện,
 * nên trạng thái tập của ngựa giữ nguyên. Một ngựa lỗi thì rollback cả lô.
 */
@Service
public class BatchVaccinationService {

    static final String RECORD_TYPE = "VACCINATION";

    private final PreventiveProtocolRepository protocols;
    private final HorseRepository horses;
    private final HealthRecordRepository healthRecords;
    private final PreventiveVaccinationService vaccinations;
    private final PreventiveStatusService preventiveStatus;
    private final Clock clock;

    @Autowired
    public BatchVaccinationService(PreventiveProtocolRepository protocols, HorseRepository horses,
            HealthRecordRepository healthRecords, PreventiveVaccinationService vaccinations,
            PreventiveStatusService preventiveStatus) {
        this(protocols, horses, healthRecords, vaccinations, preventiveStatus, Clock.systemDefaultZone());
    }

    BatchVaccinationService(PreventiveProtocolRepository protocols, HorseRepository horses,
            HealthRecordRepository healthRecords, PreventiveVaccinationService vaccinations,
            PreventiveStatusService preventiveStatus, Clock clock) {
        this.protocols = protocols;
        this.horses = horses;
        this.healthRecords = healthRecords;
        this.vaccinations = vaccinations;
        this.preventiveStatus = preventiveStatus;
        this.clock = clock;
    }

    @Transactional
    public List<BatchVaccinationResult> record(BatchVaccinationRequest request, Long vetId) {
        if (request.getHorseIds() == null || request.getHorseIds().isEmpty()) {
            throw invalid("horseIds is required");
        }
        if (request.getHorseIds().stream().anyMatch(Objects::isNull)) {
            throw invalid("horseIds must not contain null");
        }
        if (request.getDosage() == null || request.getDosage().isBlank()) {
            throw invalid("Dosage is required");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate administeredOn = request.getAdministeredOn() == null ? today : request.getAdministeredOn();
        if (administeredOn.isAfter(today)) {
            throw invalid("Vaccination date cannot be in the future");
        }
        PreventiveProtocol protocol = protocols.findById(request.getProtocolId())
                .filter(PreventiveProtocol::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROTOCOL_NOT_FOUND",
                        "Preventive protocol " + request.getProtocolId() + " does not exist or is inactive"));

        VaccinationRequest dose = new VaccinationRequest();
        dose.setProtocolId(protocol.getId());
        dose.setDosage(request.getDosage());
        dose.setRoute(request.getRoute());
        dose.setInstructions(request.getInstructions());
        dose.setAdministeredOn(administeredOn);
        dose.setSource(VaccinationSource.ADMINISTERED);
        List<PreventiveVaccinationService.ValidatedVaccination> doses = List.of(
                new PreventiveVaccinationService.ValidatedVaccination(protocol, dose,
                        VaccinationSource.ADMINISTERED, administeredOn));

        // Khóa ngựa theo thứ tự id tăng dần để hai lô chồng nhau không deadlock.
        List<BatchVaccinationResult> results = new ArrayList<>();
        for (Long horseId : new TreeSet<>(request.getHorseIds())) {
            Horse horse = horses.findByIdForUpdate(horseId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND",
                            "Horse " + horseId + " not found"));
            if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE && horse.getCurrentStatus() != HorseStatus.CANDIDATE) {
                throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_IN_STABLE",
                        "Horse " + horse.getName() + " is not in the stable");
            }

            HealthRecord record = new HealthRecord();
            record.setHorseId(horseId);
            record.setVeterinarianId(vetId);
            record.setRecordType(RECORD_TYPE);
            record.setExaminedAt(LocalDateTime.now(clock));
            record.setTreatment(protocol.getName());
            record.setNotes(request.getInstructions() == null || request.getInstructions().isBlank()
                    ? null : request.getInstructions().trim());
            HealthRecord saved = healthRecords.save(record);

            vaccinations.record(saved.getId(), doses);
            // Ngựa còn trong quy trình nhập trại: phác đồ chưa có dữ liệu chờ Manager duyệt mới hẹn.
            LocalDate missingDueOn = horse.getCurrentStatus() == HorseStatus.CANDIDATE ? null : today;
            preventiveStatus.upsertMilestone(horseId, "preventive:" + saved.getId(), vetId, missingDueOn);
            results.add(new BatchVaccinationResult(horseId, saved.getId()));
        }
        return results;
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VACCINATION", message);
    }
}
