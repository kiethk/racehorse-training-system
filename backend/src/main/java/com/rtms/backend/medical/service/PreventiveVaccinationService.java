package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.VaccinationRequest;
import com.rtms.backend.medical.entity.Prescription;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.VaccinationSource;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ghi mũi tiêm phòng thành TreatmentPlan "Tiêm phòng" + Prescription gắn phác đồ, rồi dời mốc PREVENTIVE.
 * Không có bảng vaccination_records: lịch sử tiêm nằm trong prescriptions (docs/preventive-care-flow.md mục 4).
 */
@Service
public class PreventiveVaccinationService {

    static final String PLAN_NAME = "Tiêm phòng";
    static final String HISTORICAL_TAG = "[HISTORICAL]";

    private final PreventiveProtocolRepository protocols;
    private final TreatmentPlanRepository treatmentPlans;
    private final PrescriptionRepository prescriptions;
    private final PreventiveStatusService preventiveStatus;
    private final Clock clock;

    @Autowired
    public PreventiveVaccinationService(PreventiveProtocolRepository protocols, TreatmentPlanRepository treatmentPlans,
            PrescriptionRepository prescriptions, PreventiveStatusService preventiveStatus) {
        this(protocols, treatmentPlans, prescriptions, preventiveStatus, Clock.systemDefaultZone());
    }

    PreventiveVaccinationService(PreventiveProtocolRepository protocols, TreatmentPlanRepository treatmentPlans,
            PrescriptionRepository prescriptions, PreventiveStatusService preventiveStatus, Clock clock) {
        this.protocols = protocols;
        this.treatmentPlans = treatmentPlans;
        this.prescriptions = prescriptions;
        this.preventiveStatus = preventiveStatus;
        this.clock = clock;
    }

    /** Một mũi đã kiểm tra hợp lệ, sẵn sàng ghi. */
    public record ValidatedVaccination(PreventiveProtocol protocol, VaccinationRequest request,
            VaccinationSource source, LocalDate administeredOn) {
    }

    /**
     * Kiểm tra toàn bộ danh sách trước khi đổi bất kỳ trạng thái nào, để lỗi nhập liệu không
     * đi kèm một nửa ca khám đã ghi. Rỗng hoặc null thì trả danh sách rỗng.
     */
    @Transactional(readOnly = true)
    public List<ValidatedVaccination> validate(List<VaccinationRequest> requests, CareType careType) {
        if (requests == null || requests.isEmpty()) return List.of();
        LocalDate today = LocalDate.now(clock);
        Set<Long> administeredProtocols = new HashSet<>();
        List<ValidatedVaccination> validated = new ArrayList<>(requests.size());
        for (VaccinationRequest request : requests) {
            if (request == null || request.getProtocolId() == null) {
                throw invalid("protocolId is required for every vaccination");
            }
            if (request.getDosage() == null || request.getDosage().isBlank()) {
                throw invalid("Dosage is required for every vaccination");
            }
            PreventiveProtocol protocol = protocols.findById(request.getProtocolId())
                    .filter(PreventiveProtocol::isActive)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROTOCOL_NOT_FOUND",
                            "Preventive protocol " + request.getProtocolId() + " does not exist or is inactive"));

            VaccinationSource source = request.getSource() == null ? VaccinationSource.ADMINISTERED : request.getSource();
            LocalDate on = request.getAdministeredOn() == null ? today : request.getAdministeredOn();
            if (source == VaccinationSource.HISTORICAL) {
                if (careType != CareType.INITIAL) {
                    throw invalid("Historical vaccinations can only be entered during the admission (INITIAL) examination");
                }
                if (!on.isBefore(today)) {
                    throw invalid("Historical vaccination date for " + protocol.getCode() + " must be in the past");
                }
            } else {
                if (!on.equals(today)) {
                    throw invalid("A vaccination given in this examination must be dated today");
                }
                if (!administeredProtocols.add(protocol.getId())) {
                    throw invalid(protocol.getCode() + " is listed twice in this examination");
                }
            }
            validated.add(new ValidatedVaccination(protocol, request, source, on));
        }
        return validated;
    }

    /** Ghi các mũi đã kiểm tra vào một TreatmentPlan của health record. Không làm gì khi danh sách rỗng. */
    @Transactional
    public void record(Long healthRecordId, List<ValidatedVaccination> vaccinations) {
        if (vaccinations.isEmpty()) return;
        LocalDate today = LocalDate.now(clock);

        TreatmentPlan plan = new TreatmentPlan();
        plan.setHealthRecordId(healthRecordId);
        plan.setTreatmentName(PLAN_NAME);
        plan.setInstructions("Tiêm phòng / tẩy giun theo phác đồ");
        plan.setStartDate(vaccinations.stream().map(ValidatedVaccination::administeredOn)
                .min(Comparator.naturalOrder()).orElse(today));
        plan.setEndDate(today);
        plan.setStatus("COMPLETED");
        TreatmentPlan savedPlan = treatmentPlans.save(plan);

        for (ValidatedVaccination vaccination : vaccinations) {
            VaccinationRequest request = vaccination.request();
            PreventiveProtocol protocol = vaccination.protocol();
            Prescription prescription = new Prescription();
            prescription.setTreatmentPlanId(savedPlan.getId());
            prescription.setPreventiveProtocolId(protocol.getId());
            prescription.setMedicationName(protocol.getName());
            prescription.setDosage(request.getDosage().trim());
            prescription.setRoute(request.getRoute() != null && !request.getRoute().isBlank()
                    ? request.getRoute().trim() : protocol.getDefaultRoute());
            prescription.setFrequency("ONCE");
            prescription.setStartDate(vaccination.administeredOn());
            prescription.setEndDate(vaccination.administeredOn());
            prescription.setInstructions(instructions(vaccination));
            prescriptions.save(prescription);
        }
    }

    /**
     * Dời mốc PREVENTIVE sau một ca khám. Khám nhập trại bỏ qua phác đồ chưa có dữ liệu
     * (Manager duyệt admission mới đặt hạn cho chúng); các ca khác coi chúng là quá hạn.
     */
    @Transactional
    public void refreshMilestone(Long horseId, Long healthRecordId, Long actorId, CareType careType) {
        LocalDate missingDueOn = careType == CareType.INITIAL ? null : LocalDate.now(clock);
        preventiveStatus.upsertMilestone(horseId, "preventive:" + healthRecordId, actorId, missingDueOn);
    }

    /** Hoãn tiêm: mốc PREVENTIVE mới đặt sau {@code days} ngày, lý do UNDER_TREATMENT. */
    @Transactional
    public void deferMilestone(Long horseId, Long healthRecordId, Long actorId, int days) {
        preventiveStatus.deferMilestone(horseId, "preventive:" + healthRecordId, actorId,
                LocalDate.now(clock).plusDays(days));
    }

    private static String instructions(ValidatedVaccination vaccination) {
        String note = vaccination.request().getInstructions() == null ? "" : vaccination.request().getInstructions().trim();
        if (vaccination.source() != VaccinationSource.HISTORICAL) return note.isEmpty() ? null : note;
        return note.isEmpty() ? HISTORICAL_TAG : HISTORICAL_TAG + " " + note;
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VACCINATION", message);
    }
}
