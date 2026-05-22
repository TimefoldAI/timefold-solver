package ai.timefold.solver.model.definition.api;

import ai.timefold.solver.model.definition.api.domain.Configuration;

public record SolverInput<ModelInput_, ModelOutput_, ModelConfigurationOverrides_>(
        ModelInput_ modelInput,
        Configuration<ModelConfigurationOverrides_> configuration,
        ModelOutput_ modelOutput) {
}
