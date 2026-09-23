package com.rtms.backend.service;

import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StableStallServiceTest {

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private UserRepository userRepository;

    private StableStallService stableStallService;

    @BeforeEach
    void setUp() {
        stableStallService = new StableStallService(stableStallRepository, userRepository);
    }

    @Test
    @DisplayName("BR-06: Gán chuồng cho Groom thành công khi chưa vượt quá 3 chuồng")
    void testAssignGroom_Success() {
        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setGroomId(null);

        Role groomRole = new Role();
        groomRole.setName("GROOM");

        User groom = new User();
        groom.setId(5L);
        groom.setFullName("Nguyen Van Groom");
        groom.setRole(groomRole);

        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(stall));
        when(userRepository.findById(5L)).thenReturn(Optional.of(groom));
        when(stableStallRepository.countByGroomId(5L)).thenReturn(2L); // currently 2 < 3
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StableStall result = stableStallService.assignGroom(10L, 5L);

        assertNotNull(result);
        assertEquals(5L, result.getGroomId());
        verify(stableStallRepository).save(stall);
    }

    @Test
    @DisplayName("BR-06: Chặn khi Groom đã phụ trách đủ 3 chuồng")
    void testAssignGroom_ExceedsCapacity_ThrowsException() {
        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setGroomId(null);

        Role groomRole = new Role();
        groomRole.setName("GROOM");

        User groom = new User();
        groom.setId(5L);
        groom.setFullName("Nguyen Van Groom");
        groom.setRole(groomRole);

        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(stall));
        when(userRepository.findById(5L)).thenReturn(Optional.of(groom));
        when(stableStallRepository.countByGroomId(5L)).thenReturn((long) FarmSchedulePolicy.MAX_STALLS_PER_GROOM);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> stableStallService.assignGroom(10L, 5L));

        assertTrue(ex.getMessage().contains("đã phụ trách 3 chuồng"));
        verify(stableStallRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chặn khi gán người dùng không có role GROOM")
    void testAssignGroom_NotGroom_ThrowsException() {
        StableStall stall = new StableStall();
        stall.setId(10L);

        Role trainerRole = new Role();
        trainerRole.setName("TRAINER");

        User trainer = new User();
        trainer.setId(8L);
        trainer.setFullName("Tran Van Trainer");
        trainer.setRole(trainerRole);

        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(stall));
        when(userRepository.findById(8L)).thenReturn(Optional.of(trainer));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> stableStallService.assignGroom(10L, 8L));

        assertTrue(ex.getMessage().contains("không phải GROOM"));
        verify(stableStallRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gỡ Groom khỏi chuồng (groomId = null) thành công")
    void testAssignGroom_Unassign_Success() {
        StableStall stall = new StableStall();
        stall.setId(10L);
        stall.setGroomId(5L);

        when(stableStallRepository.findById(10L)).thenReturn(Optional.of(stall));
        when(stableStallRepository.save(any(StableStall.class))).thenAnswer(inv -> inv.getArgument(0));

        StableStall result = stableStallService.assignGroom(10L, null);

        assertNotNull(result);
        assertNull(result.getGroomId());
        verify(stableStallRepository).save(stall);
    }
}