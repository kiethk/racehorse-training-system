package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.enums.MedicationRoute;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

/** Prescription details recorded when an examination is completed. */
public class PrescriptionItemRequest {

    @NotBlank(message = "Medication name is required")
    @Size(max = 150, message = "Medication name must not exceed 150 characters")
    private String medicationName;

    @NotBlank(message = "Dosage is required")
    @Size(max = 100, message = "Dosage must not exceed 100 characters")
    private String dosage;

    @NotNull(message = "Route is required")
    private MedicationRoute route;

    @NotBlank(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Size(max = 1000, message = "Instructions must not exceed 1000 characters")
    private String instructions;

    @JsonIgnore
    @AssertTrue(message = "End date must be on or after start date")
    public boolean isDateRangeValid() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public MedicationRoute getRoute() { return route; }
    public void setRoute(MedicationRoute route) { this.route = route; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
