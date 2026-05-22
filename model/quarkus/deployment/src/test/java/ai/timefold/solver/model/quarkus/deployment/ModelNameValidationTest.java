package ai.timefold.solver.model.quarkus.deployment;

import static ai.timefold.solver.model.quarkus.deployment.TimefoldModelDescriptorProcessor.validateModelId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ModelNameValidationTest {

    public static final String EXPECTED = "Model name can only contain letters (a-z, A-Z) and numbers (0-9), and hyphens (-).";

    @Test
    void testValidModelId() {
        assertDoesNotThrow(() -> validateModelId("employee-scheduling_v1"));
    }

    @Test
    void testEmptyModelId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId(""));
        assertEquals("Model name cannot be null or empty.", exception.getMessage());
    }

    @Test
    void testModelIdWithMultipleUnderscores() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("invalid_model_name"));
        assertEquals("Model name cannot contain underscore (_).", exception.getMessage());
    }

    @Test
    void testWhitespaceModelId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("model id with space"));
        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    void testModelIdWithEmoji() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("\uD83D\uDE80Rocket_123"));
        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    void testSystemModelId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("system.users"));
        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    void testModelIdWithDotSystem() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("example.system.model"));
        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    void testModelIdWithNullCharacter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("invalid\0id"));
        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    void testModelIdWithDollarNotAllowed() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("oplog.$main"));
        assertEquals(EXPECTED, exception.getMessage());
    }

    @Test
    void testTooLongModelId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateModelId("a".repeat(255) + "_"));
        assertEquals("Model name and version is too long.",
                exception.getMessage());
    }

}
