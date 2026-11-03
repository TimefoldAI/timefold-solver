package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.usage.UniqueResource;
import ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource.TestdataSolution;
import ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource.TestdataVehicle;
import ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource.TestdataVisit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusExtensionTest;

class ModelDescriptorUniqueResourcesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataVehicle.class,
            TestdataVisit.class, TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class);

    @Inject
    ObjectMapper objectMapper;

    @Test
    void uniqueResourcesDerivedFromResourceIdAnnotations() throws IOException {
        var descriptor = new File("target/timefold/timefold-model-descriptor.json");

        var modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        assertThat(modelDescriptor.getUniqueResources()).containsExactlyInAnyOrder(
                new UniqueResource("vehicle", "$.vehicles[*].id"),
                new UniqueResource("visit", "$.visits[*].id"),
                new UniqueResource("visit", "$.vehicles[*].visits[*].id"));
    }
}
