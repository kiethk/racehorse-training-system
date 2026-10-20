package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Tiêm cùng một phác đồ cho nhiều ngựa (ví dụ cả dãy chuồng) trong một lần ghi nhận. */
public class BatchVaccinationRequest {

    @NotEmpty(message = "horseIds is required")
    @Size(max = 100, message = "At most 100 horses per batch")
    private List<@NotNull Long> horseIds;

    @NotNull(message = "protocolId is required")
    private Long protocolId;

    @NotBlank(message = "Dosage is required")
    @Size(max = 255)
    private String dosage;

    @Size(max = 100)
    private String route;

    /** Bỏ trống = hôm nay. Không được ở tương lai. */
    private LocalDate administeredOn;

    private String instructions;

    public List<Long> getHorseIds() { return horseIds; }
    public void setHorseIds(List<Long> horseIds) { this.horseIds = horseIds; }

    public Long getProtocolId() { return protocolId; }
    public void setProtocolId(Long protocolId) { this.protocolId = protocolId; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public LocalDate getAdministeredOn() { return administeredOn; }
    public void setAdministeredOn(LocalDate administeredOn) { this.administeredOn = administeredOn; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
