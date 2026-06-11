package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.service.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigDescriptor;
import ai.timefold.solver.service.definition.internal.descriptor.ModelConfigParameter;
import ai.timefold.solver.service.definition.internal.descriptor.ParameterKind;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigreference.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigreference.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigreference.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigreference.TestdataModelConvertor;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigreference.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigreference.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class ModelConfigDescriptorConstraintReferenceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataModelConfigOverrides.class, TestdataEntity.class,
                            TestdataSolution.class, TestdataConstraintProvider.class, TestdataModelConvertor.class,
                            TestdataRest.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void detectModelConfigOverrides() throws IOException {
        File descriptor = new File("target/timefold/timefold-model-descriptor.json");

        ModelDescriptor modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        ModelConfigDescriptor modelConfigDescriptor = modelDescriptor.getModelConfigDescriptor();

        assertThat(modelConfigDescriptor.configParameters()).hasSize(3);
        SoftAssertions.assertSoftly(softly -> {
            ModelConfigParameter constraintWeightByDefault = modelConfigDescriptor.configParameters().get(0);
            softly.assertThat(constraintWeightByDefault.id()).isEqualTo("constraintWeightByDefault");
            softly.assertThat(constraintWeightByDefault.kind()).isEqualTo(ParameterKind.WEIGHT);
            softly.assertThat(constraintWeightByDefault.constraintNameRef())
                    .isEqualTo(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME);

            ModelConfigParameter parameter = modelConfigDescriptor.configParameters().get(1);
            softly.assertThat(parameter.id()).isEqualTo("parameter");
            softly.assertThat(parameter.kind()).isEqualTo(ParameterKind.PARAMETER);
            softly.assertThat(parameter.constraintNameRef())
                    .isEqualTo(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME);

            ModelConfigParameter constraintWeightByAnnotation = modelConfigDescriptor.configParameters().get(2);
            softly.assertThat(constraintWeightByAnnotation.id()).isEqualTo("constraintWeightByAnnotation");
            softly.assertThat(constraintWeightByAnnotation.kind()).isEqualTo(ParameterKind.WEIGHT);
            softly.assertThat(constraintWeightByAnnotation.constraintNameRef())
                    .isEqualTo(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME);
        });
    }
}
