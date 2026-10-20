package com.rtms.backend.horse.service;

import com.rtms.backend.horse.dto.CreateHorsePedigreeRequest;
import com.rtms.backend.horse.dto.HorsePedigreeResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HorsePedigreeServiceTest {

    private HorsePedigreeRepository pedigreeRepository;
    private HorseRepository horseRepository;
    private HorsePedigreeService pedigreeService;

    @BeforeEach
    void setUp() {
        pedigreeRepository = mock(HorsePedigreeRepository.class);
        horseRepository = mock(HorseRepository.class);
        pedigreeService = new HorsePedigreeService(pedigreeRepository, horseRepository,
                new PedigreeBirthDateValidator(horseRepository));
    }

    private Horse createMockHorse(Long id, String name, String ueln) {
        Horse horse = new Horse();
        horse.setId(id);
        horse.setName(name);
        horse.setBreed("Thoroughbred");
        horse.setDateOfBirth(LocalDate.of(2022, 1, 1));
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        horse.setRegistrationNumber(ueln);
        return horse;
    }

    @ParameterizedTest
    @CsvSource({"sire,id,2022-01-01", "sire,ueln,2022-01-02", "dam,id,2022-01-01", "dam,ueln,2022-01-02"})
    void rejectsInternalParentBirthDateByIdOrUeln(String relation, String selection, String parentBirthDate) {
        Horse child = createMockHorse(1L, "Child", "999999BIR000001");
        Horse parent = createMockHorse(2L, "Parent", "999999PAR000001");
        parent.setDateOfBirth(LocalDate.parse(parentBirthDate));
        when(horseRepository.findById(1L)).thenReturn(Optional.of(child));
        var request = new CreateHorsePedigreeRequest();
        if (selection.equals("id")) {
            when(horseRepository.findById(2L)).thenReturn(Optional.of(parent));
            if (relation.equals("sire")) request.setSireId(2L);
            else request.setDamId(2L);
        } else {
            when(horseRepository.findByRegistrationNumber("999999PAR000001")).thenReturn(List.of(parent));
            if (relation.equals("sire")) request.setSireRegistrationNumber("999999PAR000001");
            else request.setDamRegistrationNumber("999999PAR000001");
        }

        var error = assertThrows(ResponseStatusException.class, () -> pedigreeService.createOrUpdate(1L, request));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(error.getReason().contains(relation));
        verify(horseRepository, never()).save(any());
        verify(pedigreeRepository, never()).save(any());
    }

    @Test
    @DisplayName("UELN hợp lệ: chuẩn hóa trim và uppercase, lưu thành công")
    void createOrUpdate_normalizesValidUeln() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", null);
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(horseRepository.findByRegistrationNumber("250001AB1234567")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedigreeRepository.findByHorseId(horseId)).thenReturn(Optional.empty());
        when(pedigreeRepository.save(any(HorsePedigree.class))).thenAnswer(inv -> {
            HorsePedigree p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setRegistrationNumber("  250001ab1234567  ");
        request.setRegistryName("Jockey Club");

        HorsePedigreeResponse response = pedigreeService.createOrUpdate(horseId, request);

        assertNotNull(response);
        assertEquals("250001AB1234567", response.getRegistrationNumber());
        assertEquals("250001AB1234567", horse.getRegistrationNumber());
    }

    @Test
    @DisplayName("UELN sai định dạng (ví dụ VNM22SC00000001): ném 400 Bad Request")
    void createOrUpdate_rejectsInvalidUelnFormat() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", null);
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setRegistrationNumber("VNM22SC00000001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("UELN"));
    }

    @Test
    @DisplayName("Chặn child UELN = sire UELN")
    void createOrUpdate_rejectsChildSameAsSireUeln() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", null);
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setRegistrationNumber("250001000000001");
        request.setSireRegistrationNumber("250001000000001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Child UELN and sire UELN cannot be the same"));
    }

    @Test
    @DisplayName("Chặn child UELN = dam UELN")
    void createOrUpdate_rejectsChildSameAsDamUeln() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", null);
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setRegistrationNumber("250001000000001");
        request.setDamRegistrationNumber("250001000000001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Child UELN and dam UELN cannot be the same"));
    }

    @Test
    @DisplayName("Chặn sire UELN = dam UELN")
    void createOrUpdate_rejectsSireSameAsDamUeln() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", null);
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setRegistrationNumber("250001000000001");
        request.setSireRegistrationNumber("250001999999999");
        request.setDamRegistrationNumber("250001999999999");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Sire UELN and dam UELN cannot be the same"));
    }

    @Test
    @DisplayName("Chặn ngựa tự làm bố (sireId = horseId)")
    void createOrUpdate_rejectsSelfSire() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", "250001000000001");
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setSireId(horseId);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Horse cannot be its own sire"));
    }

    @Test
    @DisplayName("Chặn ngựa tự làm mẹ (damId = horseId)")
    void createOrUpdate_rejectsSelfDam() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", "250001000000001");
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setDamId(horseId);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Horse cannot be its own dam"));
    }

    @Test
    @DisplayName("Chặn bố và mẹ là cùng một con ngựa (sireId = damId)")
    void createOrUpdate_rejectsSameSireAndDam() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", "250001000000001");
        Horse parent = createMockHorse(2L, "Parent", "250001000000002");
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(horseRepository.findById(2L)).thenReturn(Optional.of(parent));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setSireId(2L);
        request.setDamId(2L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pedigreeService.createOrUpdate(horseId, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Sire and dam cannot be the same horse"));
    }

    @Test
    @DisplayName("Nhiều ngựa con có thể chung bố/mẹ (cho phép shared parents)")
    void createOrUpdate_allowsMultipleChildrenSharingSameSire() {
        Long horse1Id = 1L;
        Long horse2Id = 2L;
        Long sireId = 99L;

        Horse horse1 = createMockHorse(horse1Id, "Child1", "250001000000001");
        Horse horse2 = createMockHorse(horse2Id, "Child2", "250001000000002");
        Horse sire = createMockHorse(sireId, "SharedSire", "250001999999999");
        sire.setDateOfBirth(LocalDate.of(2015, 1, 1));

        when(horseRepository.findById(horse1Id)).thenReturn(Optional.of(horse1));
        when(horseRepository.findById(horse2Id)).thenReturn(Optional.of(horse2));
        when(horseRepository.findById(sireId)).thenReturn(Optional.of(sire));
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));

        when(pedigreeRepository.findByHorseId(horse1Id)).thenReturn(Optional.empty());
        when(pedigreeRepository.findByHorseId(horse2Id)).thenReturn(Optional.empty());
        when(pedigreeRepository.save(any(HorsePedigree.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateHorsePedigreeRequest req1 = new CreateHorsePedigreeRequest();
        req1.setSireId(sireId);
        HorsePedigreeResponse resp1 = pedigreeService.createOrUpdate(horse1Id, req1);

        CreateHorsePedigreeRequest req2 = new CreateHorsePedigreeRequest();
        req2.setSireId(sireId);
        HorsePedigreeResponse resp2 = pedigreeService.createOrUpdate(horse2Id, req2);

        assertNotNull(resp1);
        assertNotNull(resp2);
        assertEquals("SharedSire", resp1.getSireName());
        assertEquals("SharedSire", resp2.getSireName());
        assertEquals("250001999999999", resp1.getSireRegistrationNumber());
        assertEquals("250001999999999", resp2.getSireRegistrationNumber());
    }

    @Test
    @DisplayName("Parent ngoài hệ thống (sireId / damId null): lưu và trả đúng tên + UELN")
    void createOrUpdate_preservesExternalParents() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", "250001000000001");
        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(horseRepository.findByRegistrationNumber("250001EXTERNAL1")).thenReturn(List.of());
        when(horseRepository.findByRegistrationNumber("250001EXTERNAL2")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedigreeRepository.findByHorseId(horseId)).thenReturn(Optional.empty());

        ArgumentCaptor<HorsePedigree> captor = ArgumentCaptor.forClass(HorsePedigree.class);
        when(pedigreeRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setSireName("Stormy Sire");
        request.setSireRegistrationNumber("250001external1");
        request.setDamName("Breezy Dam");
        request.setDamRegistrationNumber("250001external2");

        HorsePedigreeResponse response = pedigreeService.createOrUpdate(horseId, request);

        assertNotNull(response);
        assertNull(response.getSire(), "Sire summary should be null for external horse");
        assertNull(response.getDam(), "Dam summary should be null for external horse");
        assertEquals("Stormy Sire", response.getSireName());
        assertEquals("250001EXTERNAL1", response.getSireRegistrationNumber());
        assertEquals("Breezy Dam", response.getDamName());
        assertEquals("250001EXTERNAL2", response.getDamRegistrationNumber());

        HorsePedigree savedPedigree = captor.getValue();
        assertNull(savedPedigree.getSireId());
        assertNull(savedPedigree.getDamId());
        assertEquals("Stormy Sire", savedPedigree.getSireName());
        assertEquals("250001EXTERNAL1", savedPedigree.getSireRegistrationNumber());
    }

    @Test
    @DisplayName("getByHorseId trả đủ thông tin bố mẹ kể cả khi sireId/damId null")
    void getByHorseId_returnsExternalParentInfoWhenIdsNull() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", "250001000000001");
        HorsePedigree pedigree = new HorsePedigree();
        pedigree.setId(5L);
        pedigree.setHorseId(horseId);
        pedigree.setSireId(null);
        pedigree.setSireName("External Stallion");
        pedigree.setSireRegistrationNumber("250001EXTSTALL1");
        pedigree.setDamId(null);
        pedigree.setDamName("External Mare");
        pedigree.setDamRegistrationNumber("250001EXTMARE01");

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(pedigreeRepository.findByHorseId(horseId)).thenReturn(Optional.of(pedigree));

        HorsePedigreeResponse response = pedigreeService.getByHorseId(horseId);

        assertNotNull(response);
        assertEquals("External Stallion", response.getSireName());
        assertEquals("250001EXTSTALL1", response.getSireRegistrationNumber());
        assertEquals("External Mare", response.getDamName());
        assertEquals("250001EXTMARE01", response.getDamRegistrationNumber());
        assertNull(response.getSire());
        assertNull(response.getDam());
    }

    @Test
    @DisplayName("Cập nhật snapshot không giữ ID cũ mâu thuẫn khi đổi sang bố mẹ mới")
    void createOrUpdate_clearsOldMismatchedParentIdWhenNewParentIsExternal() {
        Long horseId = 1L;
        Horse horse = createMockHorse(horseId, "Thunderbolt", "250001000000001");
        Horse oldSire = createMockHorse(88L, "Old Sire", "250001000000088");

        HorsePedigree existing = new HorsePedigree();
        existing.setId(5L);
        existing.setHorseId(horseId);
        existing.setSireId(88L);
        existing.setSireName("Old Sire");
        existing.setSireRegistrationNumber("250001000000088");

        when(horseRepository.findById(horseId)).thenReturn(Optional.of(horse));
        when(pedigreeRepository.findByHorseId(horseId)).thenReturn(Optional.of(existing));
        when(horseRepository.findByRegistrationNumber("250001NEWEXT001")).thenReturn(List.of());
        when(horseRepository.save(any(Horse.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<HorsePedigree> captor = ArgumentCaptor.forClass(HorsePedigree.class);
        when(pedigreeRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CreateHorsePedigreeRequest request = new CreateHorsePedigreeRequest();
        request.setSireId(null); // Không truyền sireId
        request.setSireName("New External Sire");
        request.setSireRegistrationNumber("250001newext001");

        HorsePedigreeResponse response = pedigreeService.createOrUpdate(horseId, request);

        assertNotNull(response);
        assertEquals("New External Sire", response.getSireName());
        assertEquals("250001NEWEXT001", response.getSireRegistrationNumber());

        HorsePedigree saved = captor.getValue();
        assertNull(saved.getSireId(), "Old sireId must not be preserved when changing to a new external sire");
        assertEquals("New External Sire", saved.getSireName());
    }
}
