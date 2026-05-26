package ai.timefold.solver.model.maps.service.client.util;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelConstraintJustification;
import ai.timefold.solver.model.definition.api.domain.Configuration;
import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.model.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;

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
