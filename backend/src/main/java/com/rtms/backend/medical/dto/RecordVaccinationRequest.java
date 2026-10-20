package com.rtms.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** One dose of one protocol given to one horse outside a care visit. */
public class RecordVaccinationRequest {

    @NotNull(message = "protocolId is required")
    private Long protocolId;

    @NotBlank(message = "Dosage is required")
    @Size(max = 255)
    private String dosage;

    @Size(max = 100)
    private String route;

    /** Empty = today. Must not be in the future. */
    private LocalDate administeredOn;

    private String instructions;

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
