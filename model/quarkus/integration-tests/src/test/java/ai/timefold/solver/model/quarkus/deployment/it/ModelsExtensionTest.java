package ai.timefold.solver.model.quarkus.deployment.it;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.model.definition.impl.storage.inmemory.InMemoryStorage;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.model.definition.internal.storage.Storage;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelsExtensionTest {

    @Inject
    Storage<TestdataSolution> storage;

    @Inject
    AbstractStorageService<TestdataSolution, TestdataModelConfig, TestdataModelInputMetrics, TestdataModelOutputMetrics, TestdataSolution, HardMediumSoftScore, TestdataModelConstraintJustification> storageService;

    @Test
    void testStorageClassGeneratedForModel() {
        assertNotNull(storage);
        assertInstanceOf(InMemoryStorage.class, storage);

        assertNotNull(storageService);
    }
}
