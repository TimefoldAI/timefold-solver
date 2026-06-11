package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.service.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;
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

import io.quarkus.test.QuarkusUnitTest;

class ModelDescriptorResourceTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataModelConfigOverrides.class, ObjectParameter.class, TestdataEntity.class,
                            TestdataSolution.class, TestdataConstraintProvider.class, TestdataModelConvertor.class,
                            TestdataRest.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void resourceTypeDerivedFromModelRestPath() throws IOException {
        File descriptor = new File("target/timefold/timefold-model-descriptor.json");

        ModelDescriptor modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        assertThat(modelDescriptor.getResourceType().value()).isEqualTo("testdata");
    }
}
