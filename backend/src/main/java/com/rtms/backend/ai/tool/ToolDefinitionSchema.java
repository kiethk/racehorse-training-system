package com.rtms.backend.ai.tool;

import java.util.*;

/** Builds JSON Schema parameter definitions for AI tools. */
final class ToolDefinitionSchema {
    private ToolDefinitionSchema() {
    }

    static Map<String, Object> buildParamSchema(Map<String, Object> properties, String... requiredFields) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            schema.put("properties", properties);
            if (requiredFields != null && requiredFields.length > 0) {
                schema.put("required", Arrays.asList(requiredFields));
            }
            return schema;
        }
}
