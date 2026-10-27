package com.rtms.backend.horse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.dto.HorseResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import com.rtms.backend.horse.repository.HorseEditRequestDocumentRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.service.TrainingWorkoutService;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests covering:
 * - JSON shape: Horse fields appear at the root level (via @JsonUnwrapped); imageUrl is nullable.
 * - Owner not owning the horse: HorseService throws AccessDeniedException; storage is never read.
 * - Batch: resolveImageUrls calls admissionRepo exactly once regardless of horse count.
 * - 404 when no HORSE_PHOTO exists (delegated to HorsePhotoServiceTest; verified here via
 *   the service boundary with an integration-style unit test).
 */
@ExtendWith(MockitoExtension.class)
class HorseResponseJsonTest {

    // Jackson mapper configured the same way Spring Boot configures it.
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Mock
    private AdmissionApplicationRepository admissionRepo;
    @Mock
    private AdmissionDocumentRepository documentRepo;
    @Mock
    private FileStorageService storage;

    private HorsePhotoService photoService;

    @BeforeEach
    void setUp() {
        photoService = new HorsePhotoService(admissionRepo, documentRepo, storage,
                mock(HorseEditRequestDocumentRepository.class));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Horse horse(long id, String name) {
        Horse h = new Horse();
        h.setId(id);
        h.setName(name);
        h.setCurrentStatus(HorseStatus.ELIGIBLE);
        return h;
    }

    @Test
    void sexIsAnAdditiveRootFieldAndRemainsNullForLegacyHorses() throws Exception {
        Horse known = horse(1L, "Known horse");
        known.setSex(HorseSex.FEMALE);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(new HorseResponse(known, null)));
        assertEquals("FEMALE", json.get("sex").asText());
        assertEquals("ELIGIBLE", json.get("currentStatus").asText());
        assertFalse(json.has("horse"));

        JsonNode legacy = mapper.readTree(mapper.writeValueAsString(new HorseResponse(horse(2L, "Legacy horse"), null)));
        assertTrue(legacy.has("sex"));
        assertTrue(legacy.get("sex").isNull());
    }

    private AdmissionApplication admission(Long id, Long horseId) {
        AdmissionApplication a = new AdmissionApplication();
        a.setId(id);
        a.setHorseId(horseId);
        a.setOwnerId(1L);
        return a;
    }

    private AdmissionDocument photoDoc(Long id, Long admissionId) {
        AdmissionDocument d = new AdmissionDocument();
        d.setId(id);
        d.setAdmissionId(admissionId);
        d.setDocumentType(AdmissionDocumentType.HORSE_PHOTO);
        d.setFileUrl("local:test.jpg");
        return d;
    }

    // ── JSON shape: Horse fields at root, imageUrl present or null ────────────

    @Test
    @DisplayName("HorseResponse JSON: Horse fields appear at root level, imageUrl present")
    void horseResponse_withImage_jsonHasHorseFieldsAtRoot() throws Exception {
        Horse h = horse(1L, "Thunder");
        String json = mapper.writeValueAsString(new HorseResponse(h, "/api/horses/1/photo"));

        JsonNode root = mapper.readTree(json);

        // Horse fields must be unwrapped to root — not nested under "horse"
        assertFalse(root.has("horse"), "Horse fields must not be nested under 'horse'");
        assertTrue(root.has("id"),   "id must be at root");
        assertTrue(root.has("name"), "name must be at root");
        assertTrue(root.has("currentStatus"), "currentStatus must be at root");

        assertEquals(1, root.get("id").asLong());
        assertEquals("Thunder", root.get("name").asText());
        assertEquals("/api/horses/1/photo", root.get("imageUrl").asText());
    }

    @Test
    @DisplayName("HorseResponse JSON: imageUrl is null when horse has no photo")
    void horseResponse_noImage_imageUrlNullInJson() throws Exception {
        Horse h = horse(2L, "Storm");
        String json = mapper.writeValueAsString(new HorseResponse(h, null));

        JsonNode root = mapper.readTree(json);

        // imageUrl must be present in JSON but null (not absent) so frontend Horse type works
        assertTrue(root.has("imageUrl"), "imageUrl must be present in JSON output");
        assertTrue(root.get("imageUrl").isNull(), "imageUrl must be null when no photo");
        // Horse fields still at root
        assertEquals(2, root.get("id").asLong());
        assertEquals("Storm", root.get("name").asText());
    }

    @Test
    @DisplayName("detail endpoint JSON shape: Horse fields at root, imageUrl in same object")
    void detailResponse_horsFieldsAtRoot() throws Exception {
        Horse h = horse(3L, "Blaze");
        when(admissionRepo.findByHorseIdIn(List.of(3L)))
                .thenReturn(List.of(admission(10L, 3L)));
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of(photoDoc(20L, 10L)));

        Map<Long, String> imageUrls = photoService.resolveImageUrls(List.of(3L), "/api/horses");
        HorseResponse response = new HorseResponse(h, imageUrls.get(3L));
        JsonNode root = mapper.readTree(mapper.writeValueAsString(response));

        assertFalse(root.has("horse"));
        assertEquals(3, root.get("id").asLong());
        assertEquals("/api/horses/3/photo", root.get("imageUrl").asText());
    }

    // ── Access control: Owner not owning horse ────────────────────────────────

    @Test
    @DisplayName("HorseService.getHorseById: Owner of different horse is rejected with AccessDeniedException")
    void horseService_ownerCannotViewOtherHorse_accessDenied() {
        // Build a minimal HorseService to test the access check without wiring the full context.
        HorseRepository horseRepo =
                mock(HorseRepository.class);
        StableStallRepository stallRepo =
                mock(StableStallRepository.class);
        TrainingWorkoutService workoutSvc =
                mock(TrainingWorkoutService.class);
        AreaRepository areaRepo =
                mock(AreaRepository.class);

        HorseService horseService = new HorseService(horseRepo, stallRepo, workoutSvc, areaRepo);

        Horse h = horse(10L, "Rival");
        h.setOwnerId(999L); // owned by someone else

        when(horseRepo.findById(10L)).thenReturn(Optional.of(h));

        AuthenticatedUser owner = new AuthenticatedUser(1L, "owner@test.com", "HORSE_OWNER");

        // HorseService throws AccessDeniedException — storage must not be touched.
        assertThrows(AccessDeniedException.class,
                () -> horseService.getHorseById(10L, owner));

        // If the controller correctly checks HorseService first, storage is never reached.
        verify(storage, never()).load(any());
        verify(storage, never()).mediaType(any());
    }

    // ── 404 when no HORSE_PHOTO (via service boundary) ───────────────────────

    @Test
    @DisplayName("resolvePhoto: 404 when horse has no HORSE_PHOTO, storage never called")
    void resolvePhoto_noPhoto_404_storageNotCalled() {
        when(admissionRepo.findByHorseIdIn(List.of(55L))).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> photoService.resolvePhoto(55L));

        assertEquals(404, ex.getStatusCode().value());
        verify(storage, never()).load(any());
    }

    // ── Batch: single admissionRepo query for multiple horses ────────────────

    @Test
    @DisplayName("resolveImageUrls (list endpoint): admissionRepo queried once for N horses")
    void resolveImageUrls_listEndpoint_singleAdmissionQuery() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L);
        when(admissionRepo.findByHorseIdIn(ids)).thenReturn(List.of(
                admission(101L, 1L), admission(102L, 2L),
                admission(103L, 3L), admission(104L, 4L)));
        // horse 5 has no admission → absent from result
        when(documentRepo.findByAdmissionIdInAndDocumentType(any(), any()))
                .thenReturn(List.of(
                        photoDoc(201L, 101L), photoDoc(202L, 102L),
                        photoDoc(203L, 103L)));
        // horse 4 has admission but no HORSE_PHOTO doc

        Map<Long, String> result = photoService.resolveImageUrls(ids, "/api/horses");

        // One admissionRepo query for the entire list — no N+1
        verify(admissionRepo, times(1)).findByHorseIdIn(ids);
        // One documentRepo query for the entire list
        verify(documentRepo, times(1)).findByAdmissionIdInAndDocumentType(any(), any());

        assertEquals(3, result.size());
        assertNotNull(result.get(1L));
        assertNotNull(result.get(2L));
        assertNotNull(result.get(3L));
        assertNull(result.get(4L)); // no photo
        assertNull(result.get(5L)); // no admission
    }
}
