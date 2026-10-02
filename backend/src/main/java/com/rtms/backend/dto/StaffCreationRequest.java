package com.rtms.backend.dto;

import java.time.LocalDate;

public class StaffCreationRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String role; // Allowed: GROOM, VETERINARIAN, HEAD_TRAINER

    // Veterinarian specific
    private String licenseNumber;
    private LocalDate licenseIssuedDate;
    private String specialization;

    // Head Trainer specific
    private String certificationNumber;
    private LocalDate certificationIssuedDate;

    // Groom specific
    private Long trainerId;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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
}
