package com.rtms.backend.stable.service;

import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StableStallServiceTest {
    @Test void legacyStallAddressAlwaysDelegatesWholeBlockAndBaseline() {
        var stalls = mock(StableStallRepository.class);
        var assignments = mock(StableAssignmentService.class);
        var stall = new StableStall(); stall.setId(9L); stall.setAreaId(2L); stall.setStallNumber(5);
        when(stalls.findById(9L)).thenReturn(Optional.of(stall));
        var edit = new BlockGroomAssignmentDto(); edit.setGroomId(200L);
        edit.setExpectedGroomIds(Arrays.asList(null, null, null));
        new StableStallService(stalls, assignments).assignGroom(9L, edit);
        var captor = ArgumentCaptor.forClass(BatchStableAssignmentRequest.class);
        verify(assignments).applyAssignments(captor.capture());
        var actual = captor.getValue().getGroomAssignments().getFirst();
        assertEquals(2L, actual.getAreaId()); assertEquals(2, actual.getBlockNumber());
        assertEquals(edit.getExpectedGroomIds(), actual.getExpectedGroomIds());
        verify(stalls, never()).save(org.mockito.ArgumentMatchers.any());
    }
    @Test void noLegacyFallbackAfterPolicyRejection() {
        var stalls = mock(StableStallRepository.class);
        var assignments = mock(StableAssignmentService.class);
        var stall = new StableStall(); stall.setId(9L); stall.setAreaId(2L); stall.setStallNumber(5);
        when(stalls.findById(9L)).thenReturn(Optional.of(stall));
        when(assignments.applyAssignments(org.mockito.ArgumentMatchers.any())).thenThrow(
                new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "occupied"));
        assertThrows(ResponseStatusException.class, () -> new StableStallService(stalls, assignments).assignGroom(9L, new BlockGroomAssignmentDto()));
        verify(stalls, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
