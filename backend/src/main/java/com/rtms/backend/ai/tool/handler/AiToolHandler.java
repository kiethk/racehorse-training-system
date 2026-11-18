package com.rtms.backend.ai.tool.handler;

import com.rtms.backend.security.AuthenticatedUser;

import java.util.Map;

/**
 * Standard interface for each AI tool handler using the Strategy Pattern.
 * Each tool is an independent Spring Bean that encapsulates its logic and dependencies.
 */
public interface AiToolHandler {

    /**
     * Canonical tool name (matches the toolName called by the LLM and defined in DynamicToolRegistryService).
     */
    String getToolName();

    /**
     * Executes the tool with its arguments and the current user context.
     *
     * @param arguments   Arguments parsed from the tool call
     * @param currentUser Authenticated user (contains userId, role, etc.)
     * @return Result data (Map, List, or DTO object) to add back to the LLM context
     */
    Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser);
}
