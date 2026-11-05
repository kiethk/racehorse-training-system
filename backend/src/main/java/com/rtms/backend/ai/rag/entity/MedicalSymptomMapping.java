package com.rtms.backend.ai.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_symptom_mappings")
public class MedicalSymptomMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_symptom", nullable = false, length = 100)
    private String rawSymptom;

    @Column(name = "medical_term", nullable = false, length = 255)
    private String medicalTerm;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public MedicalSymptomMapping() {
    }

    public MedicalSymptomMapping(String rawSymptom, String medicalTerm, String category) {
        this.rawSymptom = rawSymptom;
        this.medicalTerm = medicalTerm;
        this.category = category;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRawSymptom() {
        return rawSymptom;
    }

    public void setRawSymptom(String rawSymptom) {
        this.rawSymptom = rawSymptom;
    }

    public String getMedicalTerm() {
        return medicalTerm;
    }

    public void setMedicalTerm(String medicalTerm) {
        this.medicalTerm = medicalTerm;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
