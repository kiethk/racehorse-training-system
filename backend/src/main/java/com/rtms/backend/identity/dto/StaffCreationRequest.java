package com.rtms.backend.identity.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public class StaffCreationRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255)
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    @Pattern(regexp = "^$|^(0|\\+84)[3|5|7|8|9][0-9]{8}$|^[+]?[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits or valid Vietnamese phone number")
    private String phone;
    @Size(max = 255)
    private String address;
    @NotBlank
    @Pattern(regexp = "GROOM|VETERINARIAN|HEAD_TRAINER", message = "Invalid staff role")
    private String role;

    // Veterinarian specific
    @Size(max = 100)
    private String licenseNumber;
    @PastOrPresent(message = "License issued date cannot be in the future")
    private LocalDate licenseIssuedDate;
    @Size(max = 255)
    private String specialization;

    // Head Trainer specific
    @Size(max = 100)
    private String certificationNumber;
    @PastOrPresent(message = "Certification issued date cannot be in the future")
    private LocalDate certificationIssuedDate;

    // Groom specific
    private Long trainerId;

    @AssertTrue(message = "License number is required for Veterinarian")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isVetLicenseValid() {
        return !"VETERINARIAN".equals(role) || licenseNumber != null && !licenseNumber.isBlank();
    }

    @AssertTrue(message = "Certification number is required for Head Trainer")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isTrainerCertificationValid() {
        return !"HEAD_TRAINER".equals(role) || certificationNumber != null && !certificationNumber.isBlank();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public LocalDate getLicenseIssuedDate() {
        return licenseIssuedDate;
    }

    public void setLicenseIssuedDate(LocalDate licenseIssuedDate) {
        this.licenseIssuedDate = licenseIssuedDate;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getCertificationNumber() {
        return certificationNumber;
    }

    public void setCertificationNumber(String certificationNumber) {
        this.certificationNumber = certificationNumber;
    }

    public LocalDate getCertificationIssuedDate() {
        return certificationIssuedDate;
    }

    public void setCertificationIssuedDate(LocalDate certificationIssuedDate) {
        this.certificationIssuedDate = certificationIssuedDate;
    }

    public Long getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Long trainerId) {
        this.trainerId = trainerId;
    }
}
