package ai.timefold.solver.service.definition.preview.api.demo;

import ai.timefold.solver.service.definition.api.dto.dataset.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelInput;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelRequest;

public record DemoData(DemoMetaData metaData,
        ModelRequest<? extends ModelInput, ? extends ModelConfigOverrides> modelRequest) {
}