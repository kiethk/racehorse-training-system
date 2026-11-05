package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.HorseVaccinationRequest;
import com.rtms.backend.medical.dto.HorseVaccinationResult;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ghi nhận tiêm cho một ngựa từ tab "To do" của hub. Tiêm không phải khám: health record
 * VACCINATION không gắn lịch, không có kết luận tập luyện, nên trạng thái tập của ngựa giữ nguyên.
 */
@Service
public class HorseVaccinationService {

    static final String RECORD_TYPE = "VACCINATION";

    private final PreventiveProtocolRepository protocols;
    private final HorseRepository horses;
    private final HealthRecordRepository healthRecords;
    private final PreventiveVaccinationService vaccinations;
    private final PreventiveStatusService preventiveStatus;
    private final Clock clock;

    @Autowired
    public HorseVaccinationService(PreventiveProtocolRepository protocols, HorseRepository horses,
            HealthRecordRepository healthRecords, PreventiveVaccinationService vaccinations,
            PreventiveStatusService preventiveStatus) {
        this(protocols, horses, healthRecords, vaccinations, preventiveStatus, Clock.systemDefaultZone());
    }

    HorseVaccinationService(PreventiveProtocolRepository protocols, HorseRepository horses,
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
    public HorseVaccinationResult record(Long horseId, HorseVaccinationRequest request, Long vetId) {
        if (request.getDoses() == null || request.getDoses().isEmpty()) {
            throw invalid("At least one dose is required");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate administeredOn = request.getAdministeredOn() == null ? today : request.getAdministeredOn();
        if (administeredOn.isAfter(today)) {
            throw invalid("Vaccination date cannot be in the future");
        }
        String instructions = request.getInstructions() == null || request.getInstructions().isBlank()
                ? null : request.getInstructions().trim();

        // Kiểm tra toàn bộ mũi trước khi ghi bất cứ thứ gì.
        Set<Long> seen = new HashSet<>();
        List<PreventiveVaccinationService.ValidatedVaccination> doses = new ArrayList<>();
        for (HorseVaccinationRequest.Dose dose : request.getDoses()) {
            if (dose == null || dose.getProtocolId() == null) throw invalid("protocolId is required for every dose");
            if (dose.getDosage() == null || dose.getDosage().isBlank()) throw invalid("Dosage is required for every dose");
            if (!seen.add(dose.getProtocolId())) throw invalid("A protocol is listed twice");
            PreventiveProtocol protocol = protocols.findById(dose.getProtocolId())
                    .filter(PreventiveProtocol::isActive)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROTOCOL_NOT_FOUND",
                            "Preventive protocol " + dose.getProtocolId() + " does not exist or is inactive"));

            VaccinationRequest entry = new VaccinationRequest();
            entry.setProtocolId(protocol.getId());
            entry.setDosage(dose.getDosage());
            entry.setRoute(dose.getRoute());
            entry.setInstructions(instructions);
            entry.setAdministeredOn(administeredOn);
            entry.setSource(VaccinationSource.ADMINISTERED);
            doses.add(new PreventiveVaccinationService.ValidatedVaccination(protocol, entry,
                    VaccinationSource.ADMINISTERED, administeredOn));
        }

        Horse horse = horses.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE && horse.getCurrentStatus() != HorseStatus.CANDIDATE) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_IN_STABLE",
                    "Horse " + horse.getName() + " is not in the stable");
        }

        HealthRecord record = new HealthRecord();
        record.setHorseId(horseId);
        record.setVeterinarianId(vetId);
        record.setRecordType(RECORD_TYPE);
        record.setExaminedAt(LocalDateTime.now(clock));
        record.setTreatment(doses.stream().map(d -> d.protocol().getName()).collect(Collectors.joining(", ")));
        record.setNotes(instructions);
        HealthRecord saved = healthRecords.save(record);

        vaccinations.record(saved.getId(), doses);
        // Ngựa còn trong quy trình nhập trại: phác đồ chưa có dữ liệu chờ Manager duyệt mới hẹn.
        LocalDate missingDueOn = horse.getCurrentStatus() == HorseStatus.CANDIDATE ? null : today;
        preventiveStatus.upsertMilestone(horseId, "preventive:" + saved.getId(), vetId, missingDueOn);
        return new HorseVaccinationResult(horseId, saved.getId());
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VACCINATION", message);
    }
}
