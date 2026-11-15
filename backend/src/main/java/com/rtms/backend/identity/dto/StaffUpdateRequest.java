package com.rtms.backend.identity.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StaffUpdateRequest {

    @Size(max = 255, message = "Full name cannot exceed 255 characters")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+()\\s-]{8,20}$", message = "Phone number must be between 8 and 20 valid characters")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    // Veterinarian
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    private String licenseNumber;

    @PastOrPresent(message = "License issued date cannot be in the future")
    private LocalDate licenseIssuedDate;

    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    private String specialization;

    // Head Trainer
    @Size(max = 50, message = "Certification number cannot exceed 50 characters")
    private String certificationNumber;

    @PastOrPresent(message = "Certification issued date cannot be in the future")
    private LocalDate certificationIssuedDate;

    // Groom
    private Long trainerId;
    private boolean trainerIdProvided;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public LocalDate getLicenseIssuedDate() { return licenseIssuedDate; }
    public void setLicenseIssuedDate(LocalDate licenseIssuedDate) { this.licenseIssuedDate = licenseIssuedDate; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getCertificationNumber() { return certificationNumber; }
    public void setCertificationNumber(String certificationNumber) { this.certificationNumber = certificationNumber; }

    public LocalDate getCertificationIssuedDate() { return certificationIssuedDate; }
    public void setCertificationIssuedDate(LocalDate certificationIssuedDate) { this.certificationIssuedDate = certificationIssuedDate; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public boolean isTrainerIdProvided() { return trainerIdProvided; }
    public void setTrainerIdProvided(boolean trainerIdProvided) { this.trainerIdProvided = trainerIdProvided; }
}
