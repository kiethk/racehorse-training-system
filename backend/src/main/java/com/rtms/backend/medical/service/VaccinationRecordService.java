package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.RecordVaccinationRequest;
import com.rtms.backend.medical.dto.RecordVaccinationResult;
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
import java.util.List;

/**
 * Records one dose given to one horse outside a care visit (docs/preventive-care-flow.md section 5.7).
 * A vaccination is not an examination: the horse gets a VACCINATION health record with no
 * schedule and no training decision, so its training status stays as it is.
 */
@Service
public class VaccinationRecordService {

    static final String RECORD_TYPE = "VACCINATION";

    private final PreventiveProtocolRepository protocols;
    private final HorseRepository horses;
    private final HealthRecordRepository healthRecords;
    private final PreventiveVaccinationService vaccinations;
    private final PreventiveStatusService preventiveStatus;
    private final Clock clock;

    @Autowired
    public VaccinationRecordService(PreventiveProtocolRepository protocols, HorseRepository horses,
            HealthRecordRepository healthRecords, PreventiveVaccinationService vaccinations,
            PreventiveStatusService preventiveStatus) {
        this(protocols, horses, healthRecords, vaccinations, preventiveStatus, Clock.systemDefaultZone());
    }

    VaccinationRecordService(PreventiveProtocolRepository protocols, HorseRepository horses,
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
    public RecordVaccinationResult record(Long horseId, RecordVaccinationRequest request, Long vetId) {
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

        Horse horse = horses.findByIdForUpdate(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND",
                        "Horse " + horseId + " not found"));
        if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE && horse.getCurrentStatus() != HorseStatus.CANDIDATE) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_IN_STABLE",
                    "Horse " + horse.getName() + " is not in the stable");
        }

        VaccinationRequest dose = new VaccinationRequest();
        dose.setProtocolId(protocol.getId());
        dose.setDosage(request.getDosage());
        dose.setRoute(request.getRoute());
        dose.setInstructions(request.getInstructions());
        dose.setAdministeredOn(administeredOn);
        dose.setSource(VaccinationSource.ADMINISTERED);

        HealthRecord record = new HealthRecord();
        record.setHorseId(horseId);
        record.setVeterinarianId(vetId);
        record.setRecordType(RECORD_TYPE);
        record.setExaminedAt(LocalDateTime.now(clock));
        record.setTreatment(protocol.getName());
        record.setNotes(request.getInstructions() == null || request.getInstructions().isBlank()
                ? null : request.getInstructions().trim());
        HealthRecord saved = healthRecords.save(record);

        vaccinations.record(saved.getId(), List.of(new PreventiveVaccinationService.ValidatedVaccination(
                protocol, dose, VaccinationSource.ADMINISTERED, administeredOn)));
        // A horse still in admission has protocols with no data yet: book them once the Manager approves it.
        LocalDate missingDueOn = horse.getCurrentStatus() == HorseStatus.CANDIDATE ? null : today;
        preventiveStatus.upsertMilestone(horseId, "preventive:" + saved.getId(), vetId, missingDueOn);
        return new RecordVaccinationResult(horseId, saved.getId());
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VACCINATION", message);
    }
}
