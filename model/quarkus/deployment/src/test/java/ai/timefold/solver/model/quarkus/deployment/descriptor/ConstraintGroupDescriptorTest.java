package ai.timefold.solver.model.quarkus.deployment.descriptor;

import static ai.timefold.solver.model.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.model.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.ModelDescriptor;
import ai.timefold.solver.model.definition.internal.descriptor.ConstraintDescriptor;
import ai.timefold.solver.model.definition.internal.descriptor.ConstraintGroupDescriptor;
import ai.timefold.solver.model.quarkus.deployment.testdata.constraintgroup.TestdataConstraintProvider;
import ai.timefold.solver.model.quarkus.deployment.testdata.constraintgroup.TestdataEntity;
import ai.timefold.solver.model.quarkus.deployment.testdata.constraintgroup.TestdataModelConvertor;
import ai.timefold.solver.model.quarkus.deployment.testdata.constraintgroup.TestdataRest;
import ai.timefold.solver.model.quarkus.deployment.testdata.constraintgroup.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class ConstraintGroupDescriptorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class,
                            TestdataModelConvertor.class, TestdataRest.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void detectConstraintGroup() throws IOException {
        File descriptor = new File("target/timefold/timefold-model-descriptor.json");

        ModelDescriptor modelDescriptor = objectMapper.readValue(descriptor, ModelDescriptor.class);
        assertThat(modelDescriptor.getConstraintGroupDescriptors()).hasSize(2);
        ConstraintGroupDescriptor testGroupDescriptor = modelDescriptor.getConstraintGroupDescriptors().getFirst();

        assertSoftly(softly -> {
            softly.assertThat(testGroupDescriptor.id()).isEqualTo(TestdataConstraintProvider.TEST_GROUP.id());
            softly.assertThat(testGroupDescriptor.name()).isEqualTo(TestdataConstraintProvider.TEST_GROUP.name());
            softly.assertThat(testGroupDescriptor.description()).isEqualTo(TestdataConstraintProvider.TEST_GROUP.description());
            softly.assertThat(testGroupDescriptor.icon()).isEqualTo(TestdataConstraintProvider.TEST_GROUP.icon());
            softly.assertThat(testGroupDescriptor.tags()).isEqualTo(TestdataConstraintProvider.TEST_GROUP.tags());
        });

        assertThat(testGroupDescriptor.constraintDescriptors()).hasSize(1);
        ConstraintDescriptor constraintDescriptor = testGroupDescriptor.constraintDescriptors()[0];
        assertThat(constraintDescriptor.name()).isEqualTo(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_NAME);
        assertThat(constraintDescriptor.description())
                .isEqualTo(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_DESCRIPTION);
        // Negate the score as it represents a penalty.
        assertThat(constraintDescriptor.defaultWeight())
                .isEqualTo(TestdataConstraintProvider.NO_CONFLICTS_CONSTRAINT_WEIGHT.negate().toString());

        ConstraintGroupDescriptor defaultGroupDescriptor = modelDescriptor.getConstraintGroupDescriptors().get(1);
        assertSoftly(softly -> {
            softly.assertThat(defaultGroupDescriptor.id()).isEqualTo(DefaultConstraintGroupDescriptorFactory.DEFAULT_GROUP_ID);
            softly.assertThat(defaultGroupDescriptor.name())
                    .isEqualTo(DefaultConstraintGroupDescriptorFactory.DEFAULT_GROUP_NAME);
            softly.assertThat(defaultGroupDescriptor.description())
                    .isEqualTo(DefaultConstraintGroupDescriptorFactory.DEFAULT_GROUP_DESCRIPTION);
            softly.assertThat(defaultGroupDescriptor.icon()).isEqualTo(null);
            softly.assertThat(defaultGroupDescriptor.tags()).isEqualTo(null);
        });

        assertThat(defaultGroupDescriptor.constraintDescriptors()).hasSize(1);
        ConstraintDescriptor defaultGroupConstraintDescriptor = defaultGroupDescriptor.constraintDescriptors()[0];
        assertThat(defaultGroupConstraintDescriptor.id()).isEqualTo(TestdataConstraintProvider.ALL_PENALIZING_CONSTRAINT_ID);
    }
}
