package com.rtms.backend.stable.service;

import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private EntityManager entityManager;

    @Captor
    private ArgumentCaptor<Area> areaCaptor;

    @Captor
    private ArgumentCaptor<List<StableStall>> stallsCaptor;

    private AreaService areaService;

    @BeforeEach
    void setUp() {
        areaService = new AreaService(areaRepository, stableStallRepository, entityManager);
        jakarta.persistence.Query lock = mock(jakarta.persistence.Query.class);
        lenient().when(entityManager.createNativeQuery(any(String.class))).thenReturn(lock);
        lenient().when(lock.setParameter(any(String.class), any())).thenReturn(lock);
    }

    @Test
    void lockFailureMustAbortCreation() {
        when(entityManager.createNativeQuery(any(String.class))).thenThrow(new IllegalStateException("lock failed"));
        assertThrows(IllegalStateException.class, () -> areaService.createArea(AreaType.REGULAR));
        verifyNoInteractions(areaRepository, stableStallRepository);
    }

    @Test
    @DisplayName("createArea(REGULAR) generates next code E, creates 12 stalls E01-E12 with AVAILABLE status")
    void testCreateRegularArea() {
        Area a = new Area(); a.setCode("A"); a.setType(AreaType.REGULAR);
        Area b = new Area(); b.setCode("B"); b.setType(AreaType.REGULAR);
        Area c = new Area(); c.setCode("C"); c.setType(AreaType.REGULAR);
        Area d = new Area(); d.setCode("D"); d.setType(AreaType.REGULAR);

        when(areaRepository.findByType(AreaType.REGULAR)).thenReturn(List.of(a, b, c, d));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> {
            Area saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Area result = areaService.createArea(AreaType.REGULAR);

        assertNotNull(result);
        assertEquals("E", result.getCode());
        assertEquals("Area E", result.getName());
        assertEquals(AreaType.REGULAR, result.getType());
        assertNull(result.getTrainerId());

        verify(stableStallRepository).saveAll(stallsCaptor.capture());
        List<StableStall> stalls = stallsCaptor.getValue();
        assertEquals(12, stalls.size());

        for (int i = 0; i < 12; i++) {
            StableStall stall = stalls.get(i);
            assertEquals(10L, stall.getAreaId());
            assertEquals(i + 1, stall.getStallNumber());
            assertEquals(i < 9 ? "E0" + (i + 1) : "E" + (i + 1), stall.getStallCode());
            assertEquals(StallStatus.AVAILABLE, stall.getStatus());
            assertNull(stall.getGroomId());
        }
    }

    @Test
    @DisplayName("createArea(QUARANTINE) generates next code Q-B, creates 12 stalls Q-B01-Q-B12")
    void testCreateQuarantineArea() {
        Area qa = new Area(); qa.setCode("Q-A"); qa.setType(AreaType.QUARANTINE);

        when(areaRepository.findByType(AreaType.QUARANTINE)).thenReturn(List.of(qa));
        when(areaRepository.save(any(Area.class))).thenAnswer(inv -> {
            Area saved = inv.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        Area result = areaService.createArea(AreaType.QUARANTINE);

        assertNotNull(result);
        assertEquals("Q-B", result.getCode());
        assertEquals("Quarantine Area B", result.getName());
        assertEquals(AreaType.QUARANTINE, result.getType());

        verify(stableStallRepository).saveAll(stallsCaptor.capture());
        List<StableStall> stalls = stallsCaptor.getValue();
        assertEquals(12, stalls.size());

        for (int i = 0; i < 12; i++) {
            assertEquals(i < 9 ? "Q-B0" + (i + 1) : "Q-B" + (i + 1), stalls.get(i).getStallCode());
        }
        assertEquals(StallStatus.AVAILABLE, stalls.get(0).getStatus());
    }

    @Test
    @DisplayName("createArea throws NullPointerException on null type")
    void testNullTypeThrows() {
        assertThrows(NullPointerException.class, () -> areaService.createArea(null));
    }
}
