package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextScheduleIdempotencyTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CareScheduleService careScheduleService;

    private User vet;
    private Horse horse;
    private CareSchedule sourceSchedule;

    @BeforeEach
    void setUp() {
        vet = new User();
        vet.setId(5L);

        horse = new Horse();
        horse.setId(50L);

        sourceSchedule = new CareSchedule();
        sourceSchedule.setId(500L);
        sourceSchedule.setHorseId(horse.getId());
        sourceSchedule.setVeterinarianId(vet.getId());
        sourceSchedule.setStatus(CareScheduleStatus.COMPLETED);
    }

    @Test
    @DisplayName("createNextSchedule standalone with same idempotency key returns existing schedule")
    void testCreateNextSchedule_idempotent() {
        LocalDateTime future = LocalDateTime.now().plusDays(2);
        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(horse.getId());
        req.setSourceScheduleId(sourceSchedule.getId());
        req.setCareType(CareType.ROUTINE);
        req.setDescription("Follow-up check");
        req.setScheduledAt(future);
        req.setIdempotencyKey("idemp-unique-123");

        CareSchedule existingSchedule = new CareSchedule();
        existingSchedule.setId(777L);
        existingSchedule.setHorseId(horse.getId());
        existingSchedule.setRequestedById(vet.getId());
        existingSchedule.setIdempotencyKey("idemp-unique-123");
        // Compute matching fingerprint
        String fp = horse.getId() + "|null|ROUTINE|" + future + "|Follow-up check";
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(fp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            existingSchedule.setRequestFingerprint(java.util.HexFormat.of().formatHex(digest));
        } catch (Exception ignored) {}

        when(userRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(careScheduleRepository.findByIdForUpdate(sourceSchedule.getId())).thenReturn(Optional.of(sourceSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByRequestedByIdAndIdempotencyKey(vet.getId(), "idemp-unique-123"))
                .thenReturn(Optional.of(existingSchedule));

        var response = careScheduleService.createNextSchedule(req, vet.getId());

        assertEquals(777L, response.id());
        verify(careScheduleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createNextSchedule with same key but different payload throws 409 IDEMPOTENCY_CONFLICT")
    void testCreateNextSchedule_payloadConflict() {
        LocalDateTime future = LocalDateTime.now().plusDays(2);
        CreateNextScheduleRequest req = new CreateNextScheduleRequest();
        req.setHorseId(horse.getId());
        req.setSourceScheduleId(sourceSchedule.getId());
        req.setCareType(CareType.ROUTINE);
        req.setDescription("Different description");
        req.setScheduledAt(future);
        req.setIdempotencyKey("idemp-unique-123");

        CareSchedule existingSchedule = new CareSchedule();
        existingSchedule.setId(777L);
        existingSchedule.setRequestedById(vet.getId());
        existingSchedule.setIdempotencyKey("idemp-unique-123");
        existingSchedule.setRequestFingerprint("different_sha256_hash");

        when(userRepository.findById(vet.getId())).thenReturn(Optional.of(vet));
        when(careScheduleRepository.findByIdForUpdate(sourceSchedule.getId())).thenReturn(Optional.of(sourceSchedule));
        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByRequestedByIdAndIdempotencyKey(vet.getId(), "idemp-unique-123"))
                .thenReturn(Optional.of(existingSchedule));

        ApiException ex = assertThrows(ApiException.class, () ->
                careScheduleService.createNextSchedule(req, vet.getId()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("IDEMPOTENCY_CONFLICT", ex.getErrorCode());
    }
}
