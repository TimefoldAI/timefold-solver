package ai.timefold.solver.service.quarkus.deployment.it;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.definition.internal.storage.Storage;
import ai.timefold.solver.service.storage.inmemory.InMemoryStorage;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelsExtensionTest {

    @Inject
    Storage storage;

    @Inject
    AbstractStorageService<TestdataSolution, TestdataModelConfig, TestdataModelInputMetrics, TestdataModelOutputMetrics, TestdataSolution, HardMediumSoftScore, TestdataModelConstraintJustification> storageService;

    @Test
    void testStorageClassGeneratedForModel() {
        assertNotNull(storage);
        assertInstanceOf(InMemoryStorage.class, storage);

        assertNotNull(storageService);
    }
}
