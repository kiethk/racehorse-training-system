package com.rtms.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientTimeFilterTest {

    private final ClientTimeFilter filter = new ClientTimeFilter();

    @AfterEach
    void clearContext() {
        ClientTimeContext.clear();
    }

    @Test
    void convertsUtcHeaderToFarmZone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Time", "2026-11-27T07:00:00Z");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LocalDateTime> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seen.set(ClientTimeContext.now()));

        assertEquals(LocalDateTime.of(2026, 11, 27, 14, 0), seen.get());
    }

    @Test
    void acceptsOffsetHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Time", "2026-11-27T14:00:00+07:00");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LocalDateTime> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seen.set(ClientTimeContext.now()));

        assertEquals(LocalDateTime.of(2026, 11, 27, 14, 0), seen.get());
    }

    @Test
    void dynamicClockUsesClientTimeForTheCurrentRequest() {
        ClientTimeContext.set(LocalDateTime.of(2026, 11, 27, 14, 0));

        Clock clock = new ClockConfig().clock();

        assertEquals(Instant.parse("2026-11-27T07:00:00Z"), clock.instant());
    }

    @Test
    void invalidHeaderFallsBackToFarmNow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Time", "not-a-date");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LocalDateTime> seen = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seen.set(ClientTimeContext.now()));

        assertNotNull(seen.get());
        assertNotEquals(LocalDateTime.of(2026, 11, 27, 14, 0), seen.get());
    }

    @Test
    void clearsClientTimeAfterTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Time", "2026-11-27T07:00:00Z");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertNotEquals(LocalDateTime.of(2026, 11, 27, 14, 0), ClientTimeContext.now());
    }
}
