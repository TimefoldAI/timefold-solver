package ai.timefold.solver.service.quarkus.deployment;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.configuration.ConfigurationProfile;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusExtensionTest;

public class DefaultConfigProfileTest {

    @RegisterExtension
    static final QuarkusExtensionTest config =
            ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataModelConfigOverrides.class,
                    TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class);

    @Inject
    ObjectMapper objectMapper;

    @Test
    void defaultConfigProfileContainsAllProperties() throws IOException {
        File defaultConfigProfile =
                new File(
                        "target/timefold/timefold-solver-service-quarkus-deployment/default-config-profile/default-config.json");

        ConfigurationProfile configurationProfile =
                objectMapper.readValue(defaultConfigProfile, ConfigurationProfile.class);
        Map<String, Object> modelConfiguration = configurationProfile.getModelConfiguration();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(modelConfiguration).containsEntry("maximumTimeBurden", null);
        });
    }
}
