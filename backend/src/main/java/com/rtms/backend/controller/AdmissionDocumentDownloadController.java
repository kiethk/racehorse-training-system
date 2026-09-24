package com.rtms.backend.controller;

import com.rtms.backend.entity.AdmissionDocument;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AdmissionFileStorage;
import com.rtms.backend.service.OwnerAdmissionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionDocumentDownloadController {
    private final OwnerAdmissionService service;
    private final AdmissionFileStorage storage;

    public AdmissionDocumentDownloadController(OwnerAdmissionService service, AdmissionFileStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping("/{id}/documents/{documentId}/file")
    @PreAuthorize("hasAuthority('ADMISSION_DOCUMENT_VIEW')")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long documentId,
            @AuthenticationPrincipal AuthenticatedUser viewer) {
        AdmissionDocument document = service.getDocumentForViewer(id, documentId, viewer);
        Resource file = storage.load(document.getFileUrl());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admission-document-" + documentId)
                .header("X-Content-Type-Options", "nosniff")
                .body(file);
    }
}
