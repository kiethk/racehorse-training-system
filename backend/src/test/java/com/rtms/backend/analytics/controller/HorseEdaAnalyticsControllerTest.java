package com.rtms.backend.analytics.controller;

import com.rtms.backend.analytics.dto.*;
import com.rtms.backend.analytics.service.HorseEdaAnalyticsService;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorseEdaAnalyticsControllerTest {

    @Mock
    private HorseEdaAnalyticsService analyticsService;

    @InjectMocks
    private HorseEdaAnalyticsController controller;

    @Test
    @DisplayName("getFleetEdaAnalytics delegates to service and returns ApiResponse")
    void testGetFleetEdaAnalytics() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "trainer@rtms.com", "HEAD_TRAINER");
        EdaDistributionStats stats = new EdaDistributionStats(40, 60, 50, 50, 45, 55, 10, 5, 10, 0);
        EdaCorrelationSummary corr = new EdaCorrelationSummary("speed", "hr", 0.75, "STRONG_POSITIVE", 1.2, 30.0, 10);
        FleetEdaResponse mockRes = new FleetEdaResponse(stats, stats, corr, Collections.emptyList());

        when(analyticsService.getFleetEdaAnalytics(user, 180)).thenReturn(mockRes);

        var apiResponse = controller.getFleetEdaAnalytics(180, user);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals(mockRes, apiResponse.getData());
        verify(analyticsService).getFleetEdaAnalytics(user, 180);
    }

    @Test
    @DisplayName("getIndividualEdaAnalytics delegates to service and returns ApiResponse")
    void testGetIndividualEdaAnalytics() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "trainer@rtms.com", "HEAD_TRAINER");
        EdaDistributionStats stats = new EdaDistributionStats(40, 60, 50, 50, 45, 55, 10, 5, 10, 0);
        EdaCorrelationSummary corr = new EdaCorrelationSummary("speed", "hr", -0.45, "MODERATE_NEGATIVE", -0.8, 120.0, 10);
        IndividualHorseEdaResponse mockRes = new IndividualHorseEdaResponse(15L, "Storm Wind", "Thoroughbred", stats, stats, corr, Collections.emptyList());

        when(analyticsService.getIndividualEdaAnalytics(15L, user, 365)).thenReturn(mockRes);

        var apiResponse = controller.getIndividualEdaAnalytics(15L, 365, user);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals(mockRes, apiResponse.getData());
        verify(analyticsService).getIndividualEdaAnalytics(15L, user, 365);
    }
}
