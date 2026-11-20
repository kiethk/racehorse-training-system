package com.rtms.backend.ai.tool.definition;

import com.rtms.backend.ai.tool.AiToolDefinition;
import com.rtms.backend.ai.tool.ToolDefinitionSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI Tool Definitions Regression Test Suite")
class ToolDefinitionsRegressionTest {

    @Test
    @DisplayName("Verify ToolDefinitionSchema creates valid JSON Schema structure")
    void testBuildParamSchema() {
        Map<String, Object> props = Map.of(
                "horseId", Map.of("type", "integer", "description", "Horse ID"),
                "date", Map.of("type", "string", "description", "Date YYYY-MM-DD")
        );

        Map<String, Object> schema = ToolDefinitionSchema.buildParamSchema(props, "horseId");

        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        assertEquals(props, schema.get("properties"));
        assertTrue(schema.containsKey("required"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertEquals(List.of("horseId"), required);
    }

    @Test
    @DisplayName("Verify ToolDefinitionSchema with no required fields")
    void testBuildParamSchemaNoRequired() {
        Map<String, Object> props = Map.of(
                "notes", Map.of("type", "string")
        );

        Map<String, Object> schema = ToolDefinitionSchema.buildParamSchema(props);
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        assertFalse(schema.containsKey("required"));
    }

    @Test
    @DisplayName("All role tool definition factories construct non-empty valid schemas in definition package")
    void testAllRoleToolDefinitionsStructure() {
        List<List<AiToolDefinition>> allRoleDefinitions = List.of(
                GroomToolDefinitions.create(),
                ManagerToolDefinitions.create(),
                OwnerToolDefinitions.create(),
                TrainerToolDefinitions.create(),
                VetToolDefinitions.create()
        );

        for (List<AiToolDefinition> definitions : allRoleDefinitions) {
            assertNotNull(definitions);
            assertFalse(definitions.isEmpty(), "Tool definitions list must not be empty");

            for (AiToolDefinition tool : definitions) {
                assertNotNull(tool.name(), "Tool name must not be null");
                assertFalse(tool.name().isBlank(), "Tool name must not be blank");
                assertNotNull(tool.description(), "Tool description must not be null for " + tool.name());
                assertFalse(tool.description().isBlank(), "Tool description must not be blank for " + tool.name());

                Map<String, Object> spec = tool.toFunctionSpec();
                assertNotNull(spec, "Function spec must not be null for " + tool.name());
                assertEquals("function", spec.get("type"));

                @SuppressWarnings("unchecked")
                Map<String, Object> func = (Map<String, Object>) spec.get("function");
                assertNotNull(func);
                assertEquals(tool.name(), func.get("name"));
                assertEquals(tool.description(), func.get("description"));
                assertNotNull(func.get("parameters"));
            }
        }
    }
}
