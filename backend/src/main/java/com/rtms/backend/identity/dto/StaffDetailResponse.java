package com.rtms.backend.identity.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StaffDetailResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
    private Object profile;

    // ── Nested profile DTOs ────────────────────────────────────────────────

    public static class GroomProfileDto {
        private Long trainerId;
        public Long getTrainerId() { return trainerId; }
        public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }
    }

    public static class VeterinarianProfileDto {
        private String licenseNumber;
        private LocalDate licenseIssuedDate;
        private String specialization;
        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
        public LocalDate getLicenseIssuedDate() { return licenseIssuedDate; }
        public void setLicenseIssuedDate(LocalDate licenseIssuedDate) { this.licenseIssuedDate = licenseIssuedDate; }
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
    }

    public static class TrainerProfileDto {
        private String certificationNumber;
        private LocalDate certificationIssuedDate;
        public String getCertificationNumber() { return certificationNumber; }
        public void setCertificationNumber(String certificationNumber) { this.certificationNumber = certificationNumber; }
        public LocalDate getCertificationIssuedDate() { return certificationIssuedDate; }
        public void setCertificationIssuedDate(LocalDate certificationIssuedDate) { this.certificationIssuedDate = certificationIssuedDate; }
    }

    // ── Main field accessors ────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Object getProfile() { return profile; }
    public void setProfile(Object profile) { this.profile = profile; }
}
