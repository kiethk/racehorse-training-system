package com.rtms.backend.controller;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.HandleIncidentRequest;
import com.rtms.backend.service.GroomIncidentReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GroomIncidentReportControllerTests {

    @Test
    @DisplayName("PATCH /{id}/handle bị khóa: 409 và không đổi trạng thái báo cáo (BR-URG-21)")
    void handleEndpointIsBlockedAndDoesNotMutate() {
        GroomIncidentReportService service = mock(GroomIncidentReportService.class);
        GroomIncidentReportController controller = new GroomIncidentReportController(service);

        ApiException ex = assertThrows(ApiException.class,
                () -> controller.handleReport(1L, new HandleIncidentRequest(), null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INCIDENT_CLOSE_VIA_CARE_SCHEDULE", ex.getErrorCode());
        verifyNoInteractions(service);
    }
}
