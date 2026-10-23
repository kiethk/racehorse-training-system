package com.rtms.backend.medical.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Các mũi tiêm / tẩy giun ghi nhận cho một ngựa, ngoài ca khám. */
public class HorseVaccinationRequest {

    /** Bỏ trống = hôm nay. Không được ở tương lai. */
    private LocalDate administeredOn;

    private String instructions;

    @NotEmpty(message = "At least one dose is required")
    private List<@NotNull @Valid Dose> doses;

    public static class Dose {
        @NotNull(message = "protocolId is required")
        private Long protocolId;

        @NotBlank(message = "Dosage is required")
        @Size(max = 100)
        private String dosage;

        @Size(max = 100)
        private String route;

        public Long getProtocolId() { return protocolId; }
        public void setProtocolId(Long protocolId) { this.protocolId = protocolId; }

        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }

        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }
    }

    public LocalDate getAdministeredOn() { return administeredOn; }
    public void setAdministeredOn(LocalDate administeredOn) { this.administeredOn = administeredOn; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public List<Dose> getDoses() { return doses; }
    public void setDoses(List<Dose> doses) { this.doses = doses; }
}
