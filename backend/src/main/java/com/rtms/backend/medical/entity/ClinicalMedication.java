package com.rtms.backend.medical.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Danh mục thuốc chuẩn (A3): Vet kê từ catalog, withdrawal không được thấp hơn standard. */
@Entity
@Table(name = "clinical_medications")
public class ClinicalMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "therapeutic_class", nullable = false, length = 80)
    private String therapeuticClass;

    @Column(name = "default_route", nullable = false, length = 30)
    private String defaultRoute;

    @Column(name = "standard_withdrawal_days", nullable = false)
    private Integer standardWithdrawalDays;

    @Column(name = "default_dosage", length = 100)
    private String defaultDosage;

    @Column(name = "default_frequency", length = 20)
    private String defaultFrequency;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTherapeuticClass() { return therapeuticClass; }
    public void setTherapeuticClass(String therapeuticClass) { this.therapeuticClass = therapeuticClass; }

    public String getDefaultRoute() { return defaultRoute; }
    public void setDefaultRoute(String defaultRoute) { this.defaultRoute = defaultRoute; }

    public Integer getStandardWithdrawalDays() { return standardWithdrawalDays; }
    public void setStandardWithdrawalDays(Integer standardWithdrawalDays) { this.standardWithdrawalDays = standardWithdrawalDays; }

    public String getDefaultDosage() { return defaultDosage; }
    public void setDefaultDosage(String defaultDosage) { this.defaultDosage = defaultDosage; }

    public String getDefaultFrequency() { return defaultFrequency; }
    public void setDefaultFrequency(String defaultFrequency) { this.defaultFrequency = defaultFrequency; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
