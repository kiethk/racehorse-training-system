package com.rtms.backend.analytics.controller;

import com.rtms.backend.analytics.dto.manager.*;
import com.rtms.backend.analytics.service.ManagerEdaAnalyticsService;
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
class ManagerEdaAnalyticsControllerTest {

    @Mock
    private ManagerEdaAnalyticsService service;

    @InjectMocks
    private ManagerEdaAnalyticsController controller;

    @Test
    @DisplayName("getOverview delegates to service and returns ApiResponse with ManagerEdaOverviewResponse")
    void testGetOverview() {
        VetDelaysEdaDto vetDelays = new VetDelaysEdaDto(
                0.05, 50, 2, false, "Normal",
                Collections.emptyList(), Collections.emptyList()
        );
        TrainerEvaluationEdaDto trainerEval = new TrainerEvaluationEdaDto(
                85.0, 100, 85, Collections.emptyList()
        );
        StallOccupancyEdaDto occupancy = new StallOccupancyEdaDto(
                75.0, 100, 75, 25, 10, 5, 5, 50.0,
                90, 70, 20, 77.8, 2, 3, false, Collections.emptyList()
        );
        AdmissionFunnelEdaDto admissions = new AdmissionFunnelEdaDto(
                20, 15, 3, 2, 0, 75.0, 15.0, "NONE",
                new AdmissionStageLatenciesDto(5.0, 2.0, 4.0, 3.0, 1.0, 15.0),
                Collections.emptyList()
        );
        ManagerEdaOverviewResponse mockResponse = new ManagerEdaOverviewResponse(
                90, vetDelays, trainerEval, occupancy, admissions
        );

        when(service.getOverview(90)).thenReturn(mockResponse);

        var response = controller.getOverview(90);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(mockResponse, response.getData());
        assertEquals(90, response.getData().timeWindowDays());
        verify(service).getOverview(90);
    }
}
