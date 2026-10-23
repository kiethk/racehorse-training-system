package com.rtms.backend.horse.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateHorsePedigreeRequest {
    private Long sireId;
    private Long damId;

    @Size(max = 255)
    private String sireName;

    @Pattern(regexp = "^\\s*$|^([0-9]{6}[a-zA-Z0-9]{9}|[a-zA-Z]{3}[a-zA-Z0-9]{12})$", message = "Sire UELN must be 15 alphanumeric characters (e.g. 704001RT0000001 or VNM18RT00000001)")
    @Size(max = 15)
    private String sireRegistrationNumber;

    @Size(max = 255)
    private String damName;

    @Pattern(regexp = "^\\s*$|^([0-9]{6}[a-zA-Z0-9]{9}|[a-zA-Z]{3}[a-zA-Z0-9]{12})$", message = "Dam UELN must be 15 alphanumeric characters (e.g. 704001GB0000001 or VNM19GB00000001)")
    @Size(max = 15)
    private String damRegistrationNumber;

    @Pattern(regexp = "^\\s*$|^([0-9]{6}[a-zA-Z0-9]{9}|[a-zA-Z]{3}[a-zA-Z0-9]{12})$", message = "UELN must be 15 alphanumeric characters (e.g. 704001SC0000001 or VNM22SC00000001)")
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
