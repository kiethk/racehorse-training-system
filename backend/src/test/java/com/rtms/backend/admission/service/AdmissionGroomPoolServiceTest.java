package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdmissionGroomPoolServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 10, 0);

    private StableStallRepository stalls;
    private AdmissionGroomPoolService pool;

    @BeforeEach
    void setUp() {
        stalls = mock(StableStallRepository.class);
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        pool = new AdmissionGroomPoolService(stalls, 24, clock);
    }

    /** Rows as the native query returns them: {groomId, load, free}. */
    private void loads(long[]... rows) {
        List<Object[]> result = new java.util.ArrayList<>();
        for (long[] row : rows) result.add(new Object[]{row[0], row[1], row[2]});
        when(stalls.findQuarantineGroomLoads()).thenReturn(result);
    }

    @Test
    @DisplayName("Least loaded among grooms with a free stall; a full groom is never eligible")
    void leastLoadedIgnoresGroomsWithoutFreeStall() {
        // A: load 1, 1 free. B: load 1, 2 free. C: load 0 but no free stall.
        loads(new long[]{1, 1, 1}, new long[]{2, 1, 2}, new long[]{3, 0, 0});

        assertEquals(List.of(1L, 2L), pool.leastLoadedGroomIds());
        assertEquals(List.of(1L, 2L), pool.eligibleGroomIds());
        assertFalse(pool.viewFor(3L).eligible());
        assertFalse(pool.viewFor(3L).leastLoaded());
    }

    @Test
    @DisplayName("A busier eligible groom is eligible but not least loaded")
    void busierGroomIsOnlyEligible() {
        loads(new long[]{1, 0, 2}, new long[]{2, 3, 1});

        var busy = pool.viewFor(2L);

        assertTrue(busy.eligible());
        assertFalse(busy.leastLoaded());
        assertEquals(List.of(1L), pool.leastLoadedGroomIds());
    }

    @Test
    @DisplayName("No eligible groom: nobody sees the pool")
    void emptyPool() {
        loads(new long[]{1, 2, 0});

        assertEquals(List.of(), pool.leastLoadedGroomIds());
        var view = pool.viewFor(1L);
        assertFalse(view.eligible());
        assertFalse(view.leastLoaded());
    }

    @Test
    @DisplayName("A groom without quarantine stalls gets an empty view")
    void unknownGroom() {
        loads(new long[]{1, 0, 1});

        var view = pool.viewFor(99L);

        assertFalse(view.eligible());
        assertFalse(view.leastLoaded());
    }

    @Test
    @DisplayName("Fallback window: an eligible groom sees an admission only after N hours")
    void canSeeAfterFallbackWindow() {
        loads(new long[]{1, 0, 1}, new long[]{2, 2, 1});
        var busy = pool.viewFor(2L);
        assertEquals(NOW.minusHours(24), busy.fallbackCutoff());

        assertFalse(busy.canSee(admission(NOW.minusHours(23))));
        assertTrue(busy.canSee(admission(NOW.minusHours(24))));
        assertTrue(busy.canSee(admission(NOW.minusHours(25))));
        assertTrue(pool.viewFor(1L).canSee(admission(NOW)));
    }

    @Test
    @DisplayName("Only GROOM_REVIEW admissions belong to the pool")
    void canSeeOnlyPoolStatus() {
        loads(new long[]{1, 0, 1});
        AdmissionApplication arriving = admission(NOW.minusDays(3));
        arriving.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);

        assertFalse(pool.viewFor(1L).canSee(arriving));
    }

    private static AdmissionApplication admission(LocalDateTime startedAt) {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admission.setGroomReviewStartedAt(startedAt);
        return admission;
    }
}
