package com.rtms.backend.identity.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Locale;

public class AccountCreationRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 72)
    private String password;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phone;

    @Size(max = 255)
    private String address;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "GROOM|VETERINARIAN|HEAD_TRAINER|HORSE_OWNER|CLUB_MANAGER", message = "Invalid role")
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
    @JsonIgnore
    public boolean isVetLicenseValid() {
        return !"VETERINARIAN".equals(role) || (licenseNumber != null && !licenseNumber.isBlank());
    }

    @AssertTrue(message = "Certification number is required for Head Trainer")
    @JsonIgnore
    public boolean isTrainerCertificationValid() {
        return !"HEAD_TRAINER".equals(role) || (certificationNumber != null && !certificationNumber.isBlank());
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

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

    public StaffCreationRequest toStaffCreationRequest() {
        StaffCreationRequest s = new StaffCreationRequest();
        s.setFullName(this.fullName);
        s.setEmail(this.email);
        s.setPassword(this.password);
        s.setPhone(this.phone);
        s.setAddress(this.address);
        s.setRole(this.role);
        s.setLicenseNumber(this.licenseNumber);
        s.setLicenseIssuedDate(this.licenseIssuedDate);
        s.setSpecialization(this.specialization);
        s.setCertificationNumber(this.certificationNumber);
        s.setCertificationIssuedDate(this.certificationIssuedDate);
        s.setTrainerId(this.trainerId);
        return s;
    }
}
