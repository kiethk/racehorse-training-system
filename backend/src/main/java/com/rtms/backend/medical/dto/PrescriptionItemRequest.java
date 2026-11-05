package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.enums.MedicationRoute;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Đơn thuốc Vet kê tự do (Track 0 — ROADMAP v3.0).
 * Bỏ medicationId/withdrawalDays/startDate/endDate; thêm medicationName/duration/notes.
 * BE tự tính startDate = today, endDate = today + duration.
 * Serialize thành JSON snake_case lưu vào treatment_plans.instructions.
 */
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
    @Size(max = 50, message = "Frequency must not exceed 50 characters")
    private String frequency;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Max(value = 365, message = "Duration must not exceed 365 days")
    private Integer duration;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public MedicationRoute getRoute() { return route; }
    public void setRoute(MedicationRoute route) { this.route = route; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
