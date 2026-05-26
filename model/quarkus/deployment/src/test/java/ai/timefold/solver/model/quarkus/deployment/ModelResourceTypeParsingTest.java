package ai.timefold.solver.model.quarkus.deployment;

import static ai.timefold.solver.model.quarkus.deployment.TimefoldModelDescriptorProcessor.getResourceTypeFromPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.timefold.solver.model.definition.api.ResourceType;

import org.junit.jupiter.api.Test;

class ModelResourceTypeParsingTest {

    @Test
    void versionedPath() {
        assertEquals(new ResourceType("schedules"), getResourceTypeFromPath("/v1/schedules"));
    }

    @Test
    void nonVersionedPath() {
        assertEquals(new ResourceType("schedules"), getResourceTypeFromPath("/schedules"));
    }

    @Test
    void emptyPath() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> getResourceTypeFromPath(""));
        assertEquals("Could not derive model resource type: ModelRest @Path value is empty.", exception.getMessage());
    }

    @Test
    void missingPathSegment() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> getResourceTypeFromPath("/"));
        assertEquals("Could not derive model resource type: ModelRest @Path does not contain path segments.",
                exception.getMessage());
    }

    @Test
    void versionOnlyPath() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> getResourceTypeFromPath("/v1"));
        assertEquals("Could not derive model resource type: ModelRest @Path only contains API version but no resource segment.",
                exception.getMessage());
    }
}
