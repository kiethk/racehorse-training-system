package com.rtms.backend.analytics.controller;

import com.rtms.backend.analytics.dto.medical.BioSpcSeriesResponse;
import com.rtms.backend.analytics.dto.medical.SurvivalEdaResponse;
import com.rtms.backend.analytics.service.BioSpcAnalyticsService;
import com.rtms.backend.analytics.service.SurvivalEdaAnalyticsService;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalEdaAnalyticsControllerTest {

    @Mock
    private BioSpcAnalyticsService bioSpcAnalyticsService;

    @Mock
    private SurvivalEdaAnalyticsService survivalEdaAnalyticsService;

    @InjectMocks
    private MedicalEdaAnalyticsController controller;

    @Test
    @DisplayName("getBioSpc delegates to BioSpcAnalyticsService and returns ApiResponse")
    void testGetBioSpc() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "vet@rtms.com", "VETERINARIAN");
        BioSpcSeriesResponse mockRes = new BioSpcSeriesResponse(
                100L, "Thunder Bolt", "TEMPERATURE",
                37.5, 0.1, 5, true,
                38.5, 37.2, 38.0, 37.3,
                37.2, 38.5, Collections.emptyList()
        );

        when(bioSpcAnalyticsService.getBioSpcSeries(100L, "TEMPERATURE", user)).thenReturn(mockRes);

        ApiResponse<BioSpcSeriesResponse> res = controller.getBioSpc(100L, "TEMPERATURE", user);

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals(mockRes, res.getData());
        verify(bioSpcAnalyticsService).getBioSpcSeries(100L, "TEMPERATURE", user);
    }

    @Test
    @DisplayName("getSurvival delegates to SurvivalEdaAnalyticsService with default stratifyBy SEVERITY")
    void testGetSurvival() {
        SurvivalEdaResponse mockRes = new SurvivalEdaResponse("SEVERITY", Collections.emptyList());

        when(survivalEdaAnalyticsService.getSurvivalAnalytics("SEVERITY")).thenReturn(mockRes);

        ApiResponse<SurvivalEdaResponse> res = controller.getSurvival("SEVERITY");

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals(mockRes, res.getData());
        verify(survivalEdaAnalyticsService).getSurvivalAnalytics("SEVERITY");
    }

    @Test
    @DisplayName("getSurvival delegates to SurvivalEdaAnalyticsService with BODY_ZONE parameter")
    void testGetSurvivalBodyZone() {
        SurvivalEdaResponse mockRes = new SurvivalEdaResponse("BODY_ZONE", Collections.emptyList());

        when(survivalEdaAnalyticsService.getSurvivalAnalytics("BODY_ZONE")).thenReturn(mockRes);

        ApiResponse<SurvivalEdaResponse> res = controller.getSurvival("BODY_ZONE");

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals(mockRes, res.getData());
        verify(survivalEdaAnalyticsService).getSurvivalAnalytics("BODY_ZONE");
    }
}
