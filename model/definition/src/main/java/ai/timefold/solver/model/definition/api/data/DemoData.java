package ai.timefold.solver.model.definition.api.data;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.domain.ModelRequest;

public record DemoData(DemoMetaData metaData,
        ModelRequest<? extends ModelInput, ? extends ModelConfigOverrides> modelRequest) {
}