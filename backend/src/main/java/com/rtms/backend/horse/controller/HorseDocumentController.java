package com.rtms.backend.horse.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.dto.HorseDocumentSlotResponse;
import com.rtms.backend.horse.entity.HorseEditRequestDocument;
import com.rtms.backend.horse.service.HorseEditRequestService;
import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** A horse's current documents (one slot per type) and the files uploaded with its edit requests. */
@RestController
@RequestMapping("/api/horses/{horseId}")
public class HorseDocumentController {

    private final HorseService horseService;
    private final HorseEditRequestService editRequests;
    private final FileStorageService storage;

    public HorseDocumentController(HorseService horseService, HorseEditRequestService editRequests,
                                   FileStorageService storage) {
        this.horseService = horseService;
        this.editRequests = editRequests;
        this.storage = storage;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    public ApiResponse<List<HorseDocumentSlotResponse>> documents(
            @PathVariable Long horseId,
            @AuthenticationPrincipal AuthenticatedUser viewer) {
        // Same access rule as the horse itself: owners see their horses, grooms the horses in their stalls.
        horseService.getHorseById(horseId, viewer);
        return ApiResponse.success(editRequests.documentSlots(horseId));
    }

    @GetMapping("/edit-documents/{documentId}/file")
    @PreAuthorize("hasAuthority('HORSE_VIEW')")
    public ResponseEntity<Resource> editDocumentFile(
            @PathVariable Long horseId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal AuthenticatedUser viewer) {
        horseService.getHorseById(horseId, viewer);
        HorseEditRequestDocument document = editRequests.editDocument(horseId, documentId);
        String filename = document.getOriginalFileName() == null
                ? "horse-document-" + documentId
                : document.getOriginalFileName();
        return ResponseEntity.ok()
                .contentType(storage.mediaType(document.getFileUrl()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(storage.load(document.getFileUrl()));
    }
}
