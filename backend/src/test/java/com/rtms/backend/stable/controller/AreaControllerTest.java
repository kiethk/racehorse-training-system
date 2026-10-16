package com.rtms.backend.stable.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.service.AreaService;
import com.rtms.backend.stable.service.AreaLifecycleService;
import com.rtms.backend.stable.service.StableAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaControllerTest {

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private AreaService areaService;

    @Mock
    private StableAssignmentService stableAssignmentService;

    @Mock private AreaLifecycleService lifecycle;

    private AreaController controller;

    @BeforeEach
    void setUp() {
        controller = new AreaController(areaRepository, areaService, stableAssignmentService, lifecycle);
    }

    @Test
    @DisplayName("createArea delegates to AreaService and returns success response")
    void testCreateArea() {
        Area created = new Area();
        created.setId(1L);
        created.setCode("E");
        created.setName("Area E");
        created.setType(AreaType.REGULAR);

        when(areaService.createArea(AreaType.REGULAR)).thenReturn(created);

        ApiResponse<Area> response = controller.createArea(new CreateAreaRequest(AreaType.REGULAR));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("E", response.getData().getCode());
        verify(areaService).createArea(AreaType.REGULAR);
    }

    @Test
    @DisplayName("getAreas without type returns all areas")
    void testGetAreasAll() {
        Area a = new Area(); a.setCode("A");
        Area b = new Area(); b.setCode("B");
        when(areaRepository.findAll()).thenReturn(List.of(a, b));

        ApiResponse<List<Area>> response = controller.getAreas(null);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
    }

    @Test
    @DisplayName("getAreas with type returns filtered areas")
    void testGetAreasByType() {
        Area qa = new Area(); qa.setCode("Q-A"); qa.setType(AreaType.QUARANTINE);
        when(areaRepository.findByType(AreaType.QUARANTINE)).thenReturn(List.of(qa));

        ApiResponse<List<Area>> response = controller.getAreas(AreaType.QUARANTINE);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("Q-A", response.getData().get(0).getCode());
    }

    @Test
    @DisplayName("getAreaById returns area if found")
    void testGetAreaById() {
        Area a = new Area(); a.setId(5L); a.setCode("A");
        when(areaRepository.findById(5L)).thenReturn(Optional.of(a));

        ApiResponse<Area> response = controller.getAreaById(5L);
        assertTrue(response.isSuccess());
        assertEquals(5L, response.getData().getId());
    }

    @Test
    @DisplayName("applyAssignments delegates to StableAssignmentService")
    void testApplyAssignments() {
        BatchStableAssignmentRequest req = new BatchStableAssignmentRequest();
        StableAssignmentResponse res = new StableAssignmentResponse(List.of(), List.of(), 0, "OK");
        when(stableAssignmentService.applyAssignments(req)).thenReturn(res);

        ApiResponse<StableAssignmentResponse> response = controller.applyAssignments(req);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("OK", response.getData().getMessage());
        verify(stableAssignmentService).applyAssignments(req);
    }

    @Test
    @DisplayName("assignTrainer convenience endpoint delegates to StableAssignmentService")
    void testAssignTrainer() {
        StableAssignmentResponse res = new StableAssignmentResponse(List.of(), List.of(), 0, "OK");
        when(stableAssignmentService.applyAssignments(any(BatchStableAssignmentRequest.class))).thenReturn(res);

        ApiResponse<StableAssignmentResponse> response = controller.assignTrainer(1L, new AreaTrainerAssignmentDto(1L, 100L, 50L));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(stableAssignmentService).applyAssignments(any(BatchStableAssignmentRequest.class));
    }

    @Test
    @DisplayName("assignBlockGroom convenience endpoint delegates to StableAssignmentService")
    void testAssignBlockGroom() {
        StableAssignmentResponse res = new StableAssignmentResponse(List.of(), List.of(), 0, "OK");
        when(stableAssignmentService.applyAssignments(any(BatchStableAssignmentRequest.class))).thenReturn(res);

        ApiResponse<StableAssignmentResponse> response = controller.assignBlockGroom(1L, 2, new BlockGroomAssignmentDto(1L, 2, 200L, 150L));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(stableAssignmentService).applyAssignments(any(BatchStableAssignmentRequest.class));
    }
}
