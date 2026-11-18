package com.rtms.backend.ai.tool;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiToolExecutionServiceTest extends AiToolExecutionServiceTestSupport {

    // =================================================================
    // Security & Authorization Tests
    // =================================================================

    @Test
    @DisplayName("Unauthenticated users receive HTTP 401 UNAUTHORIZED")
    void testExecuteTool_Unauthenticated() {
        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("getMyHorses", Map.of(), null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Users are denied with 403 TOOL_FORBIDDEN when their role cannot call a tool")
    void testExecuteTool_DisallowedToolForbidden() {
        AuthenticatedUser groom = new AuthenticatedUser(3L, "groom@test.com", "GROOM");

        ApiException ex = assertThrows(ApiException.class, () ->
                toolExecutionService.executeTool("searchClinicalKnowledge", Map.of("query", "test"), groom));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("TOOL_FORBIDDEN", ex.getErrorCode());
    }
}
