package com.rtms.backend.horse.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.common.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorsePhotoServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionRepo;
    @Mock
    private AdmissionDocumentRepository documentRepo;
    @Mock
    private FileStorageService storage;
    @Mock
    private com.rtms.backend.horse.repository.HorseEditRequestDocumentRepository editDocuments;

    private HorsePhotoService service;

    @BeforeEach
    void setUp() {
        service = new HorsePhotoService(admissionRepo, documentRepo, storage, editDocuments);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AdmissionApplication admission(Long id, Long horseId) {
        AdmissionApplication a = new AdmissionApplication();
        a.setId(id);
        a.setHorseId(horseId);
        a.setOwnerId(1L);
        return a;
    }

    private AdmissionDocument photo(Long id, Long admissionId, String fileUrl) {
        AdmissionDocument d = new AdmissionDocument();
        d.setId(id);
        d.setAdmissionId(admissionId);
        d.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        d.setFileUrl(fileUrl);
        return d;
    }

    // ── resolveImageUrls ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveImageUrls: horse with photo → imageUrl present in map")
    void resolveImageUrls_withPhoto_returnsUrl() {
        when(admissionRepo.findByHorseIdIn(List.of(10L)))
                .thenReturn(List.of(admission(5L, 10L)));
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of(photo(99L, 5L, "local:abc.jpg")));

        Map<Long, String> result = service.resolveImageUrls(List.of(10L), "/api/horses");

        assertEquals(1, result.size());
        assertEquals("/api/horses/10/photo", result.get(10L));
    }

    @Test
    @DisplayName("resolveImageUrls: horse without photo → absent from map, imageUrl null")
    void resolveImageUrls_noPhoto_absentFromMap() {
        when(admissionRepo.findByHorseIdIn(List.of(10L)))
                .thenReturn(List.of(admission(5L, 10L)));
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of()); // no HORSE_PHOTO documents

        Map<Long, String> result = service.resolveImageUrls(List.of(10L), "/api/horses");

        assertTrue(result.isEmpty());
        assertNull(result.get(10L)); // absent → null, as frontend Horse.imageUrl expects
    }

    @Test
    @DisplayName("resolveImageUrls: no admissions → empty map, storage never called")
    void resolveImageUrls_noAdmissions_emptyMap() {
        when(admissionRepo.findByHorseIdIn(any())).thenReturn(List.of());

        Map<Long, String> result = service.resolveImageUrls(List.of(1L, 2L), "/api/horses");

        assertTrue(result.isEmpty());
        verify(documentRepo, never()).findByAdmissionIdInAndDocumentType(any(), any());
        verify(storage, never()).load(any());
    }

    @Test
    @DisplayName("resolveImageUrls: empty horseIds → empty map, no queries fired")
    void resolveImageUrls_emptyInput_noQueries() {
        Map<Long, String> result = service.resolveImageUrls(List.of(), "/api/horses");

        assertTrue(result.isEmpty());
        verify(admissionRepo, never()).findByHorseIdIn(any());
        verify(documentRepo, never()).findByAdmissionIdInAndDocumentType(any(), any());
    }

    @Test
    @DisplayName("resolveImageUrls (batch): admissionRepo called ONCE for whole list — no N+1")
    void resolveImageUrls_batch_singleAdmissionQuery() {
        List<Long> horseIds = List.of(1L, 2L, 3L);
        when(admissionRepo.findByHorseIdIn(horseIds)).thenReturn(
                List.of(admission(10L, 1L), admission(20L, 2L), admission(30L, 3L)));
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of(
                        photo(100L, 10L, "local:a.jpg"),
                        photo(200L, 20L, "local:b.jpg")));
        // horse 3 has no photo

        Map<Long, String> result = service.resolveImageUrls(horseIds, "/api/horses");

        // admissionRepo called exactly once, not once per horse
        verify(admissionRepo, times(1)).findByHorseIdIn(horseIds);
        // documentRepo called exactly once, not once per horse
        verify(documentRepo, times(1)).findByAdmissionIdInAndDocumentType(any(), any());

        assertEquals(2, result.size());
        assertNotNull(result.get(1L));
        assertNotNull(result.get(2L));
        assertNull(result.get(3L)); // no photo for horse 3
    }

    @Test
    @DisplayName("resolveImageUrls: latest admission wins when horse has multiple")
    void resolveImageUrls_multipleAdmissions_latestWins() {
        // horse 10 has two admissions; id=20 is newer
        when(admissionRepo.findByHorseIdIn(List.of(10L)))
                .thenReturn(List.of(admission(10L, 10L), admission(20L, 10L)));
        // only admission 20 has a photo
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of(photo(99L, 20L, "local:new.jpg")));

        Map<Long, String> result = service.resolveImageUrls(List.of(10L), "/api/horses");

        assertEquals("/api/horses/10/photo", result.get(10L));
    }

    // ── resolvePhoto ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolvePhoto: returns Resource and MediaType from single document lookup")
    void resolvePhoto_success_singleLookup() {
        when(admissionRepo.findByHorseIdIn(List.of(5L)))
                .thenReturn(List.of(admission(100L, 5L)));
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of(photo(200L, 100L, "local:horse.jpg")));

        Resource fakeResource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(storage.load("local:horse.jpg")).thenReturn(fakeResource);
        when(storage.mediaType("local:horse.jpg")).thenReturn(MediaType.IMAGE_JPEG);

        HorsePhotoService.PhotoResult result = service.resolvePhoto(5L);

        assertSame(fakeResource, result.resource());
        assertEquals(MediaType.IMAGE_JPEG, result.mediaType());

        // storage.load and storage.mediaType each called exactly once with the same key
        verify(storage, times(1)).load("local:horse.jpg");
        verify(storage, times(1)).mediaType("local:horse.jpg");
        // document lookup is not duplicated
        verify(admissionRepo, times(1)).findByHorseIdIn(any());
        verify(documentRepo, times(1)).findByAdmissionIdInAndDocumentType(any(), any());
    }

    @Test
    @DisplayName("resolvePhoto: no admission for horse → 404, storage never called")
    void resolvePhoto_noAdmission_404() {
        when(admissionRepo.findByHorseIdIn(List.of(99L))).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolvePhoto(99L));

        assertEquals(404, ex.getStatusCode().value());
        verify(storage, never()).load(any());
        verify(storage, never()).mediaType(any());
    }

    @Test
    @DisplayName("resolvePhoto: admission exists but no HORSE_PHOTO document → 404, storage never called")
    void resolvePhoto_admissionButNoDocument_404() {
        when(admissionRepo.findByHorseIdIn(List.of(7L)))
                .thenReturn(List.of(admission(50L, 7L)));
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolvePhoto(7L));

        assertEquals(404, ex.getStatusCode().value());
        verify(storage, never()).load(any());
    }

    @Test
    @DisplayName("BR-05: a photo approved through an edit request wins over the admission photo")
    void approvedEditPhotoWinsOverAdmissionPhoto() {
        var edited = new com.rtms.backend.horse.entity.HorseEditRequestDocument();
        edited.setHorseId(10L);
        edited.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        edited.setFileUrl("local:edited.png");
        edited.setCurrent(true);
        when(editDocuments.findFirstByHorseIdAndDocumentTypeAndCurrentTrue(10L, AdmissionDocumentType.HORSE_PHOTO))
                .thenReturn(java.util.Optional.of(edited));
        Resource bytes = new ByteArrayResource(new byte[]{1});
        when(storage.load("local:edited.png")).thenReturn(bytes);
        when(storage.mediaType("local:edited.png")).thenReturn(MediaType.IMAGE_PNG);

        HorsePhotoService.PhotoResult result = service.resolvePhoto(10L);

        assertSame(bytes, result.resource());
        verifyNoInteractions(admissionRepo, documentRepo);
    }

    @Test
    @DisplayName("BR-05: a horse whose only photo came from an edit request still gets an image URL")
    void editedPhotoAloneProducesImageUrl() {
        var edited = new com.rtms.backend.horse.entity.HorseEditRequestDocument();
        edited.setHorseId(10L);
        when(editDocuments.findByHorseIdInAndDocumentTypeAndCurrentTrue(List.of(10L), AdmissionDocumentType.HORSE_PHOTO))
                .thenReturn(List.of(edited));
        when(admissionRepo.findByHorseIdIn(List.of(10L))).thenReturn(List.of());

        assertEquals(Map.of(10L, "/api/horses/10/photo"), service.resolveImageUrls(List.of(10L), "/api/horses"));
    }
}
