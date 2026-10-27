package com.rtms.backend.stable.service;

import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
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
        verify(stalls, never()).save(ArgumentMatchers.any());
    }
    @Test void noLegacyFallbackAfterPolicyRejection() {
        var stalls = mock(StableStallRepository.class);
        var assignments = mock(StableAssignmentService.class);
        var stall = new StableStall(); stall.setId(9L); stall.setAreaId(2L); stall.setStallNumber(5);
        when(stalls.findById(9L)).thenReturn(Optional.of(stall));
        when(assignments.applyAssignments(ArgumentMatchers.any())).thenThrow(
                new ResponseStatusException(HttpStatus.CONFLICT, "occupied"));
        assertThrows(ResponseStatusException.class, () -> new StableStallService(stalls, assignments).assignGroom(9L, new BlockGroomAssignmentDto()));
        verify(stalls, never()).save(ArgumentMatchers.any());
    }
    private static StableStall stall(long id, long areaId, Long groomId) {
        var s = new StableStall(); s.setId(id); s.setAreaId(areaId); s.setGroomId(groomId);
        s.setStallNumber((int) id); s.setStallCode("S" + id); return s;
    }
    @Test void groomSeesOnlyTheirAreasAndOtherStallsWithoutGroom() {
        var own = stall(1, 10, 44L);
        var neighbour = stall(2, 10, 45L);
        var otherArea = stall(3, 20, 45L);
        var scoped = StableStallService.scopeForGroom(List.of(own, neighbour, otherArea), List.of(own), 44L);
        assertEquals(List.of(1L, 2L), scoped.stream().map(StableStall::getId).toList());
        assertSame(own, scoped.get(0));
        assertNull(scoped.get(1).getGroomId());
        assertEquals(45L, neighbour.getGroomId(), "the managed entity must not be modified");
    }
    @Test void groomAreasComeFromTheirOwnStallsNotTheFilteredList() {
        var neighbour = stall(2, 10, 45L);
        var scoped = StableStallService.scopeForGroom(List.of(neighbour), List.of(stall(1, 10, 44L)), 44L);
        assertEquals(1, scoped.size());
        assertNull(scoped.get(0).getGroomId());
    }
    @Test void groomWithoutStallsSeesNothing() {
        assertTrue(StableStallService.scopeForGroom(List.of(stall(1, 10, 45L)), List.of(), 44L).isEmpty());
    }
}
