package ai.timefold.solver.service.definition.internal.dataset;

import ai.timefold.solver.service.definition.api.dto.dataset.Configuration;

public record SolverInput<ModelInput_, ModelOutput_, ModelConfigurationOverrides_>(
        ModelInput_ modelInput,
        Configuration<ModelConfigurationOverrides_> configuration,
        ModelOutput_ modelOutput) {
}
