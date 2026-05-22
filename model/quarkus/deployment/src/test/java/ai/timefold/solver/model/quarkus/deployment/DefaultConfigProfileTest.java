package ai.timefold.solver.model.quarkus.deployment;

import static ai.timefold.solver.model.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.model.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.configuration.ConfigurationProfile;
import ai.timefold.solver.model.quarkus.deployment.testdata.defaultconfigprofile.TestdataConstraintProvider;
import ai.timefold.solver.model.quarkus.deployment.testdata.defaultconfigprofile.TestdataEntity;
import ai.timefold.solver.model.quarkus.deployment.testdata.defaultconfigprofile.TestdataModelConfigOverrides;
import ai.timefold.solver.model.quarkus.deployment.testdata.defaultconfigprofile.TestdataRest;
import ai.timefold.solver.model.quarkus.deployment.testdata.defaultconfigprofile.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class DefaultConfigProfileTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataModelConfigOverrides.class, TestdataEntity.class,
                            TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void defaultConfigProfileContainsAllProperties() throws IOException {
        File defaultConfigProfile =
                new File("target/timefold/timefold-solver-model-quarkus-deployment/default-config-profile/default-config.json");

        ConfigurationProfile configurationProfile =
                objectMapper.readValue(defaultConfigProfile, ConfigurationProfile.class);
        Map<String, Object> modelConfiguration = configurationProfile.getModelConfiguration();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(modelConfiguration).containsEntry("maximumTimeBurden", null);
        });
    }
}
