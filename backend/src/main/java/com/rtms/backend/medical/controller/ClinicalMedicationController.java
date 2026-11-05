package com.rtms.backend.medical.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.medical.dto.MedicationCatalogItemResponse;
import com.rtms.backend.medical.entity.ClinicalMedication;
import com.rtms.backend.medical.repository.ClinicalMedicationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh mục thuốc chuẩn cho form kê đơn (chỉ producer Vet — Track 2a/3a). */
@RestController
@RequestMapping("/api/clinical-medications")
public class ClinicalMedicationController {

    private final ClinicalMedicationRepository medicationRepository;

    public ClinicalMedicationController(ClinicalMedicationRepository medicationRepository) {
        this.medicationRepository = medicationRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<List<MedicationCatalogItemResponse>> listActive() {
        List<ClinicalMedication> items = medicationRepository.findByActiveTrueOrderByNameAsc();
        return ApiResponse.success(items.stream().map(MedicationCatalogItemResponse::from).toList());
    }
}
