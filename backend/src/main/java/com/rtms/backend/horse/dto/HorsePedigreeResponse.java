package com.rtms.backend.horse.dto;

public class HorsePedigreeResponse {
    private Long id;
    private HorseSummaryResponse horse;
    private HorseSummaryResponse sire;
    private HorseSummaryResponse dam;

    private String registrationNumber;
    private String registryName;
    private String pedigreeNotes;

    // External or resolved parent names & UELNs
    private String sireName;
    private String sireRegistrationNumber;
    private String damName;
    private String damRegistrationNumber;

    public HorsePedigreeResponse() {
    }

    public HorsePedigreeResponse(Long id, HorseSummaryResponse horse, HorseSummaryResponse sire, HorseSummaryResponse dam, String registrationNumber, String registryName, String pedigreeNotes) {
        this(id, horse, sire, dam, registrationNumber, registryName, pedigreeNotes, null, null, null, null);
    }

    public HorsePedigreeResponse(
            Long id,
            HorseSummaryResponse horse,
            HorseSummaryResponse sire,
            HorseSummaryResponse dam,
            String registrationNumber,
            String registryName,
            String pedigreeNotes,
            String sireName,
            String sireRegistrationNumber,
            String damName,
            String damRegistrationNumber) {
        this.id = id;
        this.horse = horse;
        this.sire = sire;
        this.dam = dam;
        this.registrationNumber = registrationNumber;
        this.registryName = registryName;
        this.pedigreeNotes = pedigreeNotes;
        this.sireName = sireName;
        this.sireRegistrationNumber = sireRegistrationNumber;
        this.damName = damName;
        this.damRegistrationNumber = damRegistrationNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HorseSummaryResponse getHorse() {
        return horse;
    }

    public void setHorse(HorseSummaryResponse horse) {
        this.horse = horse;
    }

    public HorseSummaryResponse getSire() {
        return sire;
    }

    public void setSire(HorseSummaryResponse sire) {
        this.sire = sire;
    }

    public HorseSummaryResponse getDam() {
        return dam;
    }

    public void setDam(HorseSummaryResponse dam) {
        this.dam = dam;
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
}
