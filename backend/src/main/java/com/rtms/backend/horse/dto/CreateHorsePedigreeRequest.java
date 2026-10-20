package com.rtms.backend.horse.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateHorsePedigreeRequest {
    private Long sireId;
    private Long damId;

    @Size(max = 255)
    private String sireName;

    @Pattern(regexp = "^\\s*$|^[0-9]{6}[a-zA-Z0-9]{9}$", message = "Sire UELN must be 15 alphanumeric characters (6 digits followed by 9 alphanumeric)")
    @Size(max = 15)
    private String sireRegistrationNumber;

    @Size(max = 255)
    private String damName;

    @Pattern(regexp = "^\\s*$|^[0-9]{6}[a-zA-Z0-9]{9}$", message = "Dam UELN must be 15 alphanumeric characters (6 digits followed by 9 alphanumeric)")
    @Size(max = 15)
    private String damRegistrationNumber;

    @Pattern(regexp = "^\\s*$|^[0-9]{6}[a-zA-Z0-9]{9}$", message = "UELN must be 15 alphanumeric characters (6 digits followed by 9 alphanumeric)")
    @Size(max = 15)
    private String registrationNumber;

    @Size(max = 255)
    private String registryName;

    @Size(max = 2000)
    private String pedigreeNotes;

    public Long getSireId() {
        return sireId;
    }

    public void setSireId(Long sireId) {
        this.sireId = sireId;
    }

    public Long getDamId() {
        return damId;
    }

    public void setDamId(Long damId) {
        this.damId = damId;
    }

    public String getSireName() {
        return sireName;
    }

    public void setSireName(String sireName) {
        this.sireName = sireName;
    }

    public String getSireRegistrationNumber() {
        return sireRegistrationNumber;
    }

    public void setSireRegistrationNumber(String sireRegistrationNumber) {
        this.sireRegistrationNumber = sireRegistrationNumber;
    }

    public String getDamName() {
        return damName;
    }

    public void setDamName(String damName) {
        this.damName = damName;
    }

    public String getDamRegistrationNumber() {
        return damRegistrationNumber;
    }

    public void setDamRegistrationNumber(String damRegistrationNumber) {
        this.damRegistrationNumber = damRegistrationNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getRegistryName() {
        return registryName;
    }

    public void setRegistryName(String registryName) {
        this.registryName = registryName;
    }

    public String getPedigreeNotes() {
        return pedigreeNotes;
    }

    public void setPedigreeNotes(String pedigreeNotes) {
        this.pedigreeNotes = pedigreeNotes;
    }
}
