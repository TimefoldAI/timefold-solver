package ai.timefold.solver.service.worker.impl.testdata;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.impl.storage.StorageObjectMapperWrapper;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.definition.internal.storage.Storage;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Real (non-mocked) {@code AbstractStorageService} implementation for tests, backed by {@link TestdataStorage}.
 */
public class TestdataStorageService extends
        AbstractStorageService<TestdataModelInput, TestdataModelConfigOverrides, TestdataModelInputMetrics, TestdataModelOutputMetrics, TestdataModelOutput, HardSoftScore, TestdataModelConstraintJustification> {

    public TestdataStorageService(Storage storage, StorageObjectMapperWrapper storageObjectMapperWrapper) {
        super(storage, storageObjectMapperWrapper);
    }

    @Override
    protected Class<?> getModelInputClass() {
        return TestdataModelInput.class;
    }

    @Override
    protected Class<?> getModelOutputClass() {
        return TestdataModelOutput.class;
    }

    @Override
    protected Class<?> getInputMetricsClass() {
        return TestdataModelInputMetrics.class;
    }

    @Override
    protected Class<?> getOutputMetricsClass() {
        return TestdataModelOutputMetrics.class;
    }

    @Override
    protected TypeReference<Configuration<TestdataModelConfigOverrides>> getConfigurationClass() {
        return new TypeReference<>() {
        };
    }
}
