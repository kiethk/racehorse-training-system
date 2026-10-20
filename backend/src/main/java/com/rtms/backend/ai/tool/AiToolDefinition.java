package com.rtms.backend.ai.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Định nghĩa một công cụ theo chuẩn OpenAI / Groq Function Calling Schema.
 */
public record AiToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {
    public Map<String, Object> toFunctionSpec() {
        Map<String, Object> func = new LinkedHashMap<>();
        func.put("name", name);
        func.put("description", description);
        func.put("parameters", parameters);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "function");
        root.put("function", func);
        return root;
    }
}
