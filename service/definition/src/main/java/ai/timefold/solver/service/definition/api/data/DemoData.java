package ai.timefold.solver.service.definition.api.data;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;

public record DemoData(DemoMetaData metaData,
        ModelRequest<? extends ModelInput, ? extends ModelConfigOverrides> modelRequest) {
}