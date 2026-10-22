package com.rtms.backend.admission.controller;

import com.rtms.backend.admission.dto.OcrExtractionResponse;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.service.AdmissionOcrGatewayService;
import com.rtms.backend.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/owner/admissions")
public class AdmissionOcrController {

    private final AdmissionOcrGatewayService ocrGatewayService;

    public AdmissionOcrController(AdmissionOcrGatewayService ocrGatewayService) {
        this.ocrGatewayService = ocrGatewayService;
    }

    @PostMapping(value = "/ocr-scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMISSION_APPLICATION_CREATE') and principal.role == 'HORSE_OWNER'")
    public ResponseEntity<ApiResponse<OcrExtractionResponse>> scanDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "documentType", required = false) AdmissionDocumentType documentType) {
        OcrExtractionResponse response = ocrGatewayService.scanAndExtract(file, documentType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
