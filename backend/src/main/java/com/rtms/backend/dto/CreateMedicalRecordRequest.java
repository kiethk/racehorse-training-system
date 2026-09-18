package com.rtms.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateMedicalRecordRequest {
    @NotNull
    private Long horseId;

    @NotNull
    private LocalDateTime examinedAt;

    private String symptoms;
    private String clinicalFindings;

    @NotBlank
    private String diagnosis;

    private String notes;
    private LocalDate followUpDate;
    private Long sourceTrainingWorkoutId;

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public LocalDateTime getExaminedAt() {
        return examinedAt;
    }

    public void setExaminedAt(LocalDateTime examinedAt) {
        this.examinedAt = examinedAt;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getClinicalFindings() {
        return clinicalFindings;
    }

    public void setClinicalFindings(String clinicalFindings) {
        this.clinicalFindings = clinicalFindings;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public Long getSourceTrainingWorkoutId() {
        return sourceTrainingWorkoutId;
    }

    public void setSourceTrainingWorkoutId(Long sourceTrainingWorkoutId) {
        this.sourceTrainingWorkoutId = sourceTrainingWorkoutId;
    }
}
