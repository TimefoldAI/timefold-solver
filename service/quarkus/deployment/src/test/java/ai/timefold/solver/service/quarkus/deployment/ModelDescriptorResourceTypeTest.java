package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelDescriptor;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.ObjectParameter;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataModelConvertor;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusExtensionTest;

class ModelDescriptorResourceTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config =
            ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataModelConfigOverrides.class,
                    ObjectParameter.class, TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class,
                    TestdataModelConvertor.class, TestdataRest.class);

    @Inject
    ObjectMapper objectMapper;

    @Test
    void resourceTypeDerivedFromModelRestPath() throws IOException {
        File descriptor = new File("target/timefold/timefold-model-descriptor.json");

        ModelDescriptor modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        assertThat(modelDescriptor.getResourceType().value()).isEqualTo("testdata");
    }
}
