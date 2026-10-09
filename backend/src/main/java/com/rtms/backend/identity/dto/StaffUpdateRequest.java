package com.rtms.backend.identity.dto;

import java.time.LocalDate;

public class StaffUpdateRequest {

    private String fullName;
    private String phone;
    private String address;

    // Veterinarian
    private String licenseNumber;
    private LocalDate licenseIssuedDate;
    private String specialization;

    // Head Trainer
    private String certificationNumber;
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
