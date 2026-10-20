package com.rtms.backend.horse.controller;

import com.rtms.backend.horse.dto.HorseResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.service.HorsePhotoService;
import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorsePhotoEndpointTest {

    @Mock
    private HorseService horseService;
    @Mock
    private HorsePerformanceService performanceService;
    @Mock
    private HorseSpecialtyService specialtyService;
    @Mock
    private HorsePhotoService photoService;
    @Mock
    private com.rtms.backend.horse.service.HorseAdmissionService admissionService;

    private HorseController controller;

    @BeforeEach
    void setUp() {
        controller = new HorseController(horseService, performanceService, specialtyService, photoService, admissionService);
    }

    private Horse createHorse(Long id, Long ownerId) {
        Horse h = new Horse();
        h.setId(id);
        h.setName("Champion");
        h.setOwnerId(ownerId);
        h.setCurrentStatus(HorseStatus.ELIGIBLE);
        return h;
    }

    @Test
    @DisplayName("GET /api/horses/{id}/photo: Owner not owning the horse is rejected before photoService/storage")
    void getPhoto_ownerNotOwningHorse_rejectedBeforePhotoService() {
        Long horseId = 100L;
        AuthenticatedUser unauthOwner = new AuthenticatedUser(2L, "other@test.com", "HORSE_OWNER");

        // horseService.getHorseById enforces ownership check and throws AccessDeniedException
        when(horseService.getHorseById(eq(horseId), eq(unauthOwner)))
                .thenThrow(new AccessDeniedException("Access denied: You do not own this horse."));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.getPhoto(horseId, unauthOwner));

        assertTrue(ex.getMessage().contains("Access denied"));

        // Crucial verification: photoService is never called when ownership check fails
        verifyNoInteractions(photoService);
    }

    @Test
    @DisplayName("GET /api/horses/{id}/photo: Valid owner receives photo with correct Content-Type")
    void getPhoto_validOwner_returnsPhotoWithCorrectContentType() {
        Long horseId = 100L;
        AuthenticatedUser validOwner = new AuthenticatedUser(1L, "owner@test.com", "HORSE_OWNER");
        Horse horse = createHorse(horseId, 1L);

        when(horseService.getHorseById(eq(horseId), eq(validOwner))).thenReturn(horse);

        Resource mockResource = new ByteArrayResource("fake-image-bytes".getBytes());
        MediaType expectedMediaType = MediaType.IMAGE_JPEG;
        when(photoService.resolvePhoto(horseId))
                .thenReturn(new HorsePhotoService.PhotoResult(mockResource, expectedMediaType));

        ResponseEntity<Resource> response = controller.getPhoto(horseId, validOwner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedMediaType, response.getHeaders().getContentType());
        assertSame(mockResource, response.getBody());
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));

        verify(horseService, times(1)).getHorseById(horseId, validOwner);
        verify(photoService, times(1)).resolvePhoto(horseId);
    }

    @Test
    @DisplayName("GET /api/horses/{id}/photo: PNG photo returns image/png Content-Type")
    void getPhoto_pngPhoto_returnsPngContentType() {
        Long horseId = 200L;
        AuthenticatedUser manager = new AuthenticatedUser(9L, "manager@test.com", "CLUB_MANAGER");
        Horse horse = createHorse(horseId, 1L);

        when(horseService.getHorseById(eq(horseId), eq(manager))).thenReturn(horse);

        Resource mockResource = new ByteArrayResource("fake-png-bytes".getBytes());
        MediaType expectedMediaType = MediaType.IMAGE_PNG;
        when(photoService.resolvePhoto(horseId))
                .thenReturn(new HorsePhotoService.PhotoResult(mockResource, expectedMediaType));

        ResponseEntity<Resource> response = controller.getPhoto(horseId, manager);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedMediaType, response.getHeaders().getContentType());
        assertSame(mockResource, response.getBody());
    }

    @Test
    @DisplayName("GET /api/horses: includeSpecialty=false preserves legacy response (specialty is null, no specialty query)")
    void getAllHorses_default_specialtyIsNullAndNoSpecialtyLookup() {
        AuthenticatedUser manager = new AuthenticatedUser(9L, "manager@test.com", "CLUB_MANAGER");
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(manager);
        org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        Horse h1 = createHorse(1L, 10L);
        Horse h2 = createHorse(2L, 11L);
        when(horseService.getAllHorses(eq(manager), isNull(), isNull(), isNull())).thenReturn(List.of(h1, h2));
        when(photoService.resolveImageUrls(eq(List.of(1L, 2L)), any())).thenReturn(Map.of(1L, "/api/horses/1/photo"));

        com.rtms.backend.common.dto.ApiResponse<List<HorseResponse>> response =
                controller.getAllHorses(null, null, null, false);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        assertEquals("/api/horses/1/photo", response.getData().get(0).getImageUrl());
        assertNull(response.getData().get(0).getSpecialty());
        assertNull(response.getData().get(1).getSpecialty());

        // Batch specialty lookup is never called when includeSpecialty is false
        verifyNoInteractions(specialtyService);
    }

    @Test
    @DisplayName("GET /api/horses: includeSpecialty=true batches lookup with exact filtered IDs once")
    void getAllHorses_includeSpecialtyTrue_batchesLookupOnce() {
        AuthenticatedUser manager = new AuthenticatedUser(9L, "manager@test.com", "CLUB_MANAGER");
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(manager);
        org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        Horse h1 = createHorse(10L, 100L);
        Horse h2 = createHorse(20L, 101L);
        when(horseService.getAllHorses(eq(manager), isNull(), isNull(), isNull())).thenReturn(List.of(h1, h2));
        when(photoService.resolveImageUrls(eq(List.of(10L, 20L)), any())).thenReturn(Map.of());

        com.rtms.backend.training.dto.HorseSpecialtyResponse spec1 =
                new com.rtms.backend.training.dto.HorseSpecialtyResponse(3, List.of(), null, List.of());
        when(specialtyService.getSpecialties(eq(List.of(10L, 20L)))).thenReturn(Map.of(10L, spec1));

        com.rtms.backend.common.dto.ApiResponse<List<HorseResponse>> response =
                controller.getAllHorses(null, null, null, true);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        assertNotNull(response.getData().get(0).getSpecialty());
        assertEquals(3, response.getData().get(0).getSpecialty().minWorkoutsPerGroup());
        assertNull(response.getData().get(1).getSpecialty());

        // Batch resolved exactly once with the filtered horse IDs
        verify(specialtyService, times(1)).getSpecialties(List.of(10L, 20L));
    }
}
