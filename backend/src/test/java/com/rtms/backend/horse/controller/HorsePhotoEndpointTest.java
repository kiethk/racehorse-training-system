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

    private HorseController controller;

    @BeforeEach
    void setUp() {
        controller = new HorseController(horseService, performanceService, specialtyService, photoService);
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
}
