package com.rtms.backend.admission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.admission.dto.OcrExtractionResponse;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.enums.HorseSex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AdmissionOcrGatewayServiceTest {

    private FileStorageService fileStorage;
    private ObjectMapper objectMapper;
    private AdmissionOcrGatewayService service;

    @BeforeEach
    void setUp() {
        fileStorage = mock(FileStorageService.class);
        objectMapper = new ObjectMapper();
        service = new AdmissionOcrGatewayService(
                fileStorage,
                objectMapper,
                "http://localhost:8000",
                1,
                1
        );
    }

    @Test
    @DisplayName("scanAndExtract rejects empty file with 400 Bad Request")
    void scanAndExtract_rejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);

        assertThrows(ResponseStatusException.class, () ->
                service.scanAndExtract(emptyFile, AdmissionDocumentType.REGISTRATION_DOCUMENT)
        );
    }

    @Test
    @DisplayName("scanAndExtract stores file and returns fallback response when AI service is offline")
    void scanAndExtract_offlineFallback_preservesStoredFile() {
        byte[] pdfBytes = "%PDF-1.4 mock pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "passport.pdf", "application/pdf", pdfBytes);

        when(fileStorage.store(any(), anyString())).thenReturn("local:mock-stored-key.pdf");

        OcrExtractionResponse response = service.scanAndExtract(file, AdmissionDocumentType.REGISTRATION_DOCUMENT);

        verify(fileStorage).validate(file);
        verify(fileStorage).store(eq(file), anyString());

        assertNotNull(response);
        assertFalse(response.success());
        assertNotNull(response.data());
        assertNotNull(response.data().fileReference());
        assertEquals("local:mock-stored-key.pdf", response.data().fileReference().fileUrl());
        assertEquals("passport.pdf", response.data().fileReference().originalFilename());
        assertEquals(0, response.extractedFieldsCount());
    }
}
