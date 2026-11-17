package ai.timefold.solver.service.rest.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.service.definition.api.dto.dataset.Configuration;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelInput;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelOutput;
import ai.timefold.solver.service.definition.api.dto.dataset.RunConfiguration;
import ai.timefold.solver.service.definition.api.dto.patch.ModelInputPatchRequest;
import ai.timefold.solver.service.definition.api.dto.scoreanalysis.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.dto.validation.issue.Issue;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.rest.DatasetSelector;
import ai.timefold.solver.service.definition.api.rest.OperationOnPost;
import ai.timefold.solver.service.definition.impl.solver.SolverWorkerFacade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AbstractModelAPIResourceTest {

    private static final Configuration<ModelConfigOverrides> CONFIGURATION = Configuration.<ModelConfigOverrides> empty()
            .withRun(new RunConfiguration("config-name", null, null, Set.of("config-tag")));

    private final AtomicReference<FacadeCall> lastFacadeCall = new AtomicReference<>();
    private final AbstractModelAPIResource<ModelInput, ModelOutput, ModelConfigOverrides, Object, ModelInputMetrics, ModelOutputMetrics, ModelConstraintJustification, Issue> resource =
            new AbstractModelAPIResource<>(null, recordingFacade(), null) {
            };

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "null, NONE, patchDataset, config-name",
            "query-name, NONE, patchDataset, query-name",
            "null, SOLVE, patchAndSolveDataset, config-name" })
    void fromPatchTakesTheNameAndTheTagsOfTheRunConfiguration(String name, OperationOnPost operation, String expectedMethod,
            String expectedRunName) {
        var patchRequest = new ModelInputPatchRequest<>(CONFIGURATION, List.of());

        resource.fromPatch("parent-id", name, DatasetSelector.UNSOLVED, operation, patchRequest);

        assertThat(lastFacadeCall.get().method()).isEqualTo(expectedMethod);
        assertThat(lastFacadeCall.get().arguments())
                .containsExactly("parent-id", DatasetSelector.UNSOLVED, expectedRunName, Set.of("config-tag"), patchRequest);
    }

    @Test
    void fromPatchWithoutConfigurationTakesTheNameQueryParameterOnly() {
        var patchRequest = new ModelInputPatchRequest<ModelConfigOverrides>(null, List.of());

        resource.fromPatch("parent-id", "query-name", DatasetSelector.UNSOLVED, OperationOnPost.NONE, patchRequest);

        assertThat(lastFacadeCall.get().arguments())
                .containsExactly("parent-id", DatasetSelector.UNSOLVED, "query-name", null, patchRequest);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = { "null, config-name", "query-name, query-name" })
    void fromInputTakesTheNameAndTheTagsOfTheRunConfiguration(String name, String expectedRunName) {
        resource.fromInput("parent-id", name, DatasetSelector.UNSOLVED, OperationOnPost.NONE, CONFIGURATION);

        assertThat(lastFacadeCall.get().method()).isEqualTo("createDataset");
        assertThat(lastFacadeCall.get().arguments())
                .containsExactly("parent-id", DatasetSelector.UNSOLVED, expectedRunName, Set.of("config-tag"), CONFIGURATION);
    }

    private SolverWorkerFacade recordingFacade() {
        return (SolverWorkerFacade) Proxy.newProxyInstance(SolverWorkerFacade.class.getClassLoader(),
                new Class<?>[] { SolverWorkerFacade.class },
                (proxy, method, arguments) -> {
                    lastFacadeCall.set(new FacadeCall(method.getName(), arguments));
                    return null;
                });
    }

    private record FacadeCall(String method, Object[] arguments) {
    }
}
