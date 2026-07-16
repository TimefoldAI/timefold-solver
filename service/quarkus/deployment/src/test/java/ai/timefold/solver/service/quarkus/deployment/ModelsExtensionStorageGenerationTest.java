package ai.timefold.solver.service.quarkus.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.definition.internal.storage.Storage;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelConfig;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelConstraintJustification;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelConvertor;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelInput;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelInputMetrics;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelOutput;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataModelOutputMetrics;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.storage.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ModelsExtensionStorageGenerationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataEntity.class,
            TestdataModelConfig.class, TestdataModelConstraintJustification.class, TestdataModelInput.class,
            TestdataModelInputMetrics.class, TestdataModelOutput.class, TestdataModelOutputMetrics.class,
            TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class, TestdataModelConvertor.class);

    @Inject
    Storage<TestdataModelOutput> storage;

    @Inject
    AbstractStorageService<TestdataModelInput, TestdataModelConfig, TestdataModelInputMetrics, TestdataModelOutputMetrics, TestdataModelOutput, SimpleScore, TestdataModelConstraintJustification> storageService;

    @Test
    void testStorageClassGeneratedForModel() {
        assertNotNull(storage);
        assertNotNull(storageService);
    }
}
