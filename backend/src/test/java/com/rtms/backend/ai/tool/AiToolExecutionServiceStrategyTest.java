package com.rtms.backend.ai.tool;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiToolExecutionServiceStrategyTest {

    @Test
    @DisplayName("The registry loads handlers successfully and dispatches by toolName")
    void shouldDispatchToCorrectHandler() {
        DynamicToolRegistryService registryService = Mockito.mock(DynamicToolRegistryService.class);
        AiToolHandler mockHandler = Mockito.mock(AiToolHandler.class);
        when(mockHandler.getToolName()).thenReturn("customMockTool");
        when(mockHandler.execute(anyMap(), any())).thenReturn(Map.of("status", "success"));

        when(registryService.isToolAllowedForRole("customMockTool", "CLUB_MANAGER")).thenReturn(true);

        AiToolExecutionService service = new AiToolExecutionService(registryService, List.of(mockHandler));
        assertEquals(1, service.getHandlerMap().size());
        assertTrue(service.getHandlerMap().containsKey("customMockTool"));

        AuthenticatedUser user = new AuthenticatedUser(1L, "mgr@rtms.com", "CLUB_MANAGER");
        Object result = service.executeTool("customMockTool", Map.of("key", "val"), user);

        assertNotNull(result);
        assertEquals(Map.of("status", "success"), result);
        verify(mockHandler, times(1)).execute(eq(Map.of("key", "val")), eq(user));
    }

    @Test
    @DisplayName("Throws 404 when toolName is not present in handlerMap")
    void shouldThrowNotFoundWhenToolMissing() {
        DynamicToolRegistryService registryService = Mockito.mock(DynamicToolRegistryService.class);
        when(registryService.isToolAllowedForRole("unknownTool", "CLUB_MANAGER")).thenReturn(true);

        AiToolExecutionService service = new AiToolExecutionService(registryService, Collections.emptyList());

        AuthenticatedUser user = new AuthenticatedUser(1L, "mgr@rtms.com", "CLUB_MANAGER");
        ApiException ex = assertThrows(ApiException.class, () ->
                service.executeTool("unknownTool", Collections.emptyMap(), user));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("TOOL_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Throws 403 when the role is not authorized to call the tool")
    void shouldThrowForbiddenWhenRoleNotAllowed() {
        DynamicToolRegistryService registryService = Mockito.mock(DynamicToolRegistryService.class);
        AiToolHandler mockHandler = Mockito.mock(AiToolHandler.class);
        when(mockHandler.getToolName()).thenReturn("adminOnlyTool");
        when(registryService.isToolAllowedForRole("adminOnlyTool", "GROOM")).thenReturn(false);

        AiToolExecutionService service = new AiToolExecutionService(registryService, List.of(mockHandler));

        AuthenticatedUser user = new AuthenticatedUser(2L, "groom@rtms.com", "GROOM");
        ApiException ex = assertThrows(ApiException.class, () ->
                service.executeTool("adminOnlyTool", Collections.emptyMap(), user));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("TOOL_FORBIDDEN", ex.getErrorCode());
    }

    @Test
    @DisplayName("Throws 401 when the user or userId is null")
    void shouldThrowUnauthorizedWhenUserNull() {
        DynamicToolRegistryService registryService = Mockito.mock(DynamicToolRegistryService.class);
        AiToolExecutionService service = new AiToolExecutionService(registryService, Collections.emptyList());

        ApiException ex1 = assertThrows(ApiException.class, () ->
                service.executeTool("anyTool", Collections.emptyMap(), null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex1.getStatus());

        AuthenticatedUser invalidUser = new AuthenticatedUser(null, "anon@rtms.com", "GROOM");
        ApiException ex2 = assertThrows(ApiException.class, () ->
                service.executeTool("anyTool", Collections.emptyMap(), invalidUser));
        assertEquals(HttpStatus.UNAUTHORIZED, ex2.getStatus());
    }
}
