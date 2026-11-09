package ai.timefold.solver.service.maps.service.client.util;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.service.definition.spi.dto.ModelConfigOverrides;
import ai.timefold.solver.service.definition.spi.dto.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.dto.dataset.Configuration;
import ai.timefold.solver.service.definition.spi.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.spi.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.test.Mock;

@Mock
public class DummyStorageService extends
        AbstractStorageService<DummyModelInput, ModelConfigOverrides, ModelInputMetrics, ModelOutputMetrics, DummyModelOutput, SimpleScore, ModelConstraintJustification> {

    @Override
    protected Class<?> getModelInputClass() {
        return null;
    }

    @Override
    protected Class<?> getModelOutputClass() {
        return null;
    }

    @Override
    protected Class<?> getInputMetricsClass() {
        return null;
    }

    @Override
    protected Class<?> getOutputMetricsClass() {
        return null;
    }

    @Override
    protected TypeReference<Configuration<ModelConfigOverrides>> getConfigurationClass() {
        return null;
    }
}
