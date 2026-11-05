package com.rtms.backend.medical.entity;

import com.rtms.backend.medical.enums.ProtocolCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Danh mục phác đồ tiêm phòng / tẩy giun. Chỉ đọc trong MVP: thêm hoặc sửa bằng Flyway migration. */
@Entity
@Table(name = "preventive_protocols")
public class PreventiveProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProtocolCategory category;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "warning_days", nullable = false)
    private int warningDays;

    @Column(nullable = false)
    private boolean mandatory;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "default_dosage", length = 100)
    private String defaultDosage;

    @Column(name = "default_route", length = 100)
    private String defaultRoute;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProtocolCategory getCategory() { return category; }
    public void setCategory(ProtocolCategory category) { this.category = category; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public int getWarningDays() { return warningDays; }
    public void setWarningDays(int warningDays) { this.warningDays = warningDays; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDefaultDosage() { return defaultDosage; }
    public void setDefaultDosage(String defaultDosage) { this.defaultDosage = defaultDosage; }

    public String getDefaultRoute() { return defaultRoute; }
    public void setDefaultRoute(String defaultRoute) { this.defaultRoute = defaultRoute; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
