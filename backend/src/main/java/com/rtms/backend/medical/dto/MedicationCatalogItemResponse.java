package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.entity.ClinicalMedication;

/** Mục danh mục thuốc trả cho form kê đơn (Track 2a/3a). */
public class MedicationCatalogItemResponse {
    private Long id;
    private String name;
    private String therapeuticClass;
    private String defaultRoute;
    private Integer standardWithdrawalDays;
    private String defaultDosage;
    private String defaultFrequency;

    public static MedicationCatalogItemResponse from(ClinicalMedication m) {
        MedicationCatalogItemResponse r = new MedicationCatalogItemResponse();
        r.id = m.getId();
        r.name = m.getName();
        r.therapeuticClass = m.getTherapeuticClass();
        r.defaultRoute = m.getDefaultRoute();
        r.standardWithdrawalDays = m.getStandardWithdrawalDays();
        r.defaultDosage = m.getDefaultDosage();
        r.defaultFrequency = m.getDefaultFrequency();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTherapeuticClass() { return therapeuticClass; }
    public String getDefaultRoute() { return defaultRoute; }
    public Integer getStandardWithdrawalDays() { return standardWithdrawalDays; }
    public String getDefaultDosage() { return defaultDosage; }
    public String getDefaultFrequency() { return defaultFrequency; }
}
