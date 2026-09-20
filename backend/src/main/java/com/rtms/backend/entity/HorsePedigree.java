package com.rtms.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "horse_pedigrees")
public class HorsePedigree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "horse_id", nullable = false, unique = true)
    private Long horseId;

    @Column(name = "sire_id")
    private Long sireId;

    @Column(name = "sire_name")
    private String sireName;

    @Column(name = "sire_registration_number", length = 100)
    private String sireRegistrationNumber;

    @Column(name = "dam_id")
    private Long damId;

    @Column(name = "dam_name")
    private String damName;

    @Column(name = "dam_registration_number", length = 100)
    private String damRegistrationNumber;

    @Column(name = "registry_name", length = 255)
    private String registryName;

    @Column(name = "pedigree_notes", length = 2000)
    private String pedigreeNotes;

    @Column(name = "registration_number", length = 255)
    private String registrationNumber;

    public HorsePedigree(Long id, Long sireId, Long horseId, Long damId, String registryName, String pedigreeNotes,
            String registrationNumber) {
        this.id = id;
        this.sireId = sireId;
        this.horseId = horseId;
        this.damId = damId;
        this.registryName = registryName;
        this.pedigreeNotes = pedigreeNotes;
        this.registrationNumber = registrationNumber;
    }

    public HorsePedigree() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

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

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
