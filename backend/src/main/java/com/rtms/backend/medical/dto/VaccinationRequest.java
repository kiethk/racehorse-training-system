package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.enums.VaccinationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Một mũi tiêm phòng / tẩy giun theo phác đồ, gửi kèm khi hoàn tất ca khám. */
public class VaccinationRequest {

    @NotNull(message = "protocolId is required")
    private Long protocolId;

    @NotBlank(message = "Dosage is required")
    @Size(max = 255)
    private String dosage;

    /** Bỏ trống thì dùng default_route của phác đồ. */
    @Size(max = 100)
    private String route;

    private String instructions;

    /** Bỏ trống = hôm nay. Bắt buộc ở quá khứ khi source = HISTORICAL. */
    private LocalDate administeredOn;

    /** Bỏ trống = ADMINISTERED. */
    private VaccinationSource source;

    public Long getProtocolId() { return protocolId; }
    public void setProtocolId(Long protocolId) { this.protocolId = protocolId; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public LocalDate getAdministeredOn() { return administeredOn; }
    public void setAdministeredOn(LocalDate administeredOn) { this.administeredOn = administeredOn; }

    public VaccinationSource getSource() { return source; }
    public void setSource(VaccinationSource source) { this.source = source; }
}
