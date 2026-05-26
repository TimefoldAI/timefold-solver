package ai.timefold.solver.model.quarkus.deployment;

import static ai.timefold.solver.model.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.model.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.model.definition.internal.storage.Storage;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataConstraintProvider;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataEntity;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelConfig;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelConstraintJustification;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelConvertor;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelInput;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelInputMetrics;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelOutput;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataModelOutputMetrics;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataRest;
import ai.timefold.solver.model.quarkus.deployment.testdata.storage.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ModelsExtensionStorageGenerationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataEntity.class, TestdataModelConfig.class,
                            TestdataModelConstraintJustification.class, TestdataModelInput.class,
                            TestdataModelInputMetrics.class, TestdataModelOutput.class, TestdataModelOutputMetrics.class,
                            TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class,
                            TestdataModelConvertor.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

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
