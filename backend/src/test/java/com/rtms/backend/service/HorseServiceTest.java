package com.rtms.backend.service;

import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.StableStallRepository;
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
class HorseServiceTest {

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private HorseTrainingPlanService trainingPlanService;

    @Mock
    private com.rtms.backend.repository.AreaRepository areaRepository;

    private HorseService horseService;

    @BeforeEach
    void setUp() {
        horseService = new HorseService(horseRepository, stableStallRepository, trainingPlanService, areaRepository);
    }

    @Test
    @DisplayName("Gán chuồng thành công: Chuồng mới chuyển sang OCCUPIED, ngựa nhận currentStallId")
    void testAssignStall_Success() {
        Horse horse = new Horse();
        horse.setId(1L);
        horse.setName("Lightning Bolt");
        horse.setCurrentStallId(null);

        StableStall stall = new StableStall();
        stall.setId(6L);
        stall.setStatus(StallStatus.AVAILABLE);

        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(6L)).thenReturn(Optional.of(stall));
        when(horseRepository.save(any(Horse.class))).thenAnswer(i -> i.getArgument(0));

        Horse updated = horseService.assignStall(1L, 6L);

        assertNotNull(updated);
        assertEquals(6L, updated.getCurrentStallId());
        assertEquals(StallStatus.OCCUPIED, stall.getStatus());
        verify(stableStallRepository, times(1)).save(stall);
        verify(horseRepository, times(1)).save(horse);
    }

    @Test
    @DisplayName("Đổi chuồng: Chuồng cũ về AVAILABLE, chuồng mới thành OCCUPIED")
    void testAssignStall_ChangeStall() {
        Horse horse = new Horse();
        horse.setId(1L);
        horse.setCurrentStallId(5L); // Chuồng cũ

        StableStall oldStall = new StableStall();
        oldStall.setId(5L);
        oldStall.setStatus(StallStatus.OCCUPIED);

        StableStall newStall = new StableStall();
        newStall.setId(6L);
        newStall.setStatus(StallStatus.AVAILABLE);

        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(6L)).thenReturn(Optional.of(newStall));
        when(stableStallRepository.findById(5L)).thenReturn(Optional.of(oldStall));
        when(horseRepository.save(any(Horse.class))).thenAnswer(i -> i.getArgument(0));

        Horse updated = horseService.assignStall(1L, 6L);

        assertEquals(6L, updated.getCurrentStallId());
        assertEquals(StallStatus.AVAILABLE, oldStall.getStatus());
        assertEquals(StallStatus.OCCUPIED, newStall.getStatus());
        verify(stableStallRepository, times(1)).save(oldStall);
        verify(stableStallRepository, times(1)).save(newStall);
    }

    @Test
    @DisplayName("Lỗi khi chuồng không tồn tại")
    void testAssignStall_StallNotFound() {
        Horse horse = new Horse();
        horse.setId(1L);

        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> horseService.assignStall(1L, 999L));
        verify(horseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateHorseStatus sang INJURED: kích hoạt cascade cancelFutureTrainingForHorse")
    void testUpdateHorseStatus_Injured_TriggersCascade() {
        Horse horse = new Horse();
        horse.setId(1L);
        horse.setCurrentStatus(com.rtms.backend.enums.HorseStatus.ELIGIBLE);

        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));
        when(horseRepository.save(any(Horse.class))).thenAnswer(i -> i.getArgument(0));

        com.rtms.backend.dto.UpdateHorseStatusRequest req = new com.rtms.backend.dto.UpdateHorseStatusRequest();
        req.setStatus("INJURED");

        Horse updated = horseService.updateHorseStatus(1L, req);

        assertEquals(com.rtms.backend.enums.HorseStatus.INJURED, updated.getCurrentStatus());
        verify(trainingPlanService, times(1)).cancelFutureTrainingForHorse(eq(1L), anyString());
    }

    @Test
    @DisplayName("updateHorseStatus sang ELIGIBLE: KHÔNG kích hoạt cascade huỷ huấn luyện")
    void testUpdateHorseStatus_Eligible_NoCascade() {
        Horse horse = new Horse();
        horse.setId(1L);
        horse.setCurrentStatus(com.rtms.backend.enums.HorseStatus.INJURED);

        when(horseRepository.findById(1L)).thenReturn(Optional.of(horse));
        when(horseRepository.save(any(Horse.class))).thenAnswer(i -> i.getArgument(0));

        com.rtms.backend.dto.UpdateHorseStatusRequest req = new com.rtms.backend.dto.UpdateHorseStatusRequest();
        req.setStatus("ELIGIBLE");

        Horse updated = horseService.updateHorseStatus(1L, req);

        assertEquals(com.rtms.backend.enums.HorseStatus.ELIGIBLE, updated.getCurrentStatus());
        verify(trainingPlanService, never()).cancelFutureTrainingForHorse(anyLong(), anyString());
    }

    @Test
    @DisplayName("getAllHorses với mine=true và vai trò HEAD_TRAINER: chỉ trả ngựa ở khu Trainer phụ trách")
    void testGetAllHorses_HeadTrainer_Mine() {
        com.rtms.backend.security.AuthenticatedUser trainer =
                new com.rtms.backend.security.AuthenticatedUser(10L, "trainer@test.com", "HEAD_TRAINER");

        com.rtms.backend.entity.Area myArea = new com.rtms.backend.entity.Area();
        myArea.setId(1L);
        myArea.setTrainerId(10L);

        com.rtms.backend.entity.Area otherArea = new com.rtms.backend.entity.Area();
        otherArea.setId(2L);
        otherArea.setTrainerId(99L);

        when(areaRepository.findAll()).thenReturn(List.of(myArea, otherArea));

        StableStall stall1 = new StableStall();
        stall1.setId(101L);
        stall1.setAreaId(1L);

        StableStall stall2 = new StableStall();
        stall2.setId(201L);
        stall2.setAreaId(2L);

        when(stableStallRepository.findAll()).thenReturn(List.of(stall1, stall2));

        Horse h1 = new Horse();
        h1.setId(1L);
        h1.setCurrentStallId(101L);
        h1.setCurrentStatus(com.rtms.backend.enums.HorseStatus.ELIGIBLE);

        Horse h2 = new Horse();
        h2.setId(2L);
        h2.setCurrentStallId(201L);
        h2.setCurrentStatus(com.rtms.backend.enums.HorseStatus.ELIGIBLE);

        Horse h3 = new Horse();
        h3.setId(3L);
        h3.setCurrentStallId(null);
        h3.setCurrentStatus(com.rtms.backend.enums.HorseStatus.CANDIDATE);

        when(horseRepository.findAll()).thenReturn(List.of(h1, h2, h3));

        List<Horse> result = horseService.getAllHorses(trainer, true, com.rtms.backend.enums.HorseStatus.ELIGIBLE);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }
}