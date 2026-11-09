package ai.timefold.solver.service.definition.preview.api.demo;

import ai.timefold.solver.service.definition.spi.dto.ModelConfigOverrides;
import ai.timefold.solver.service.definition.spi.dto.ModelInput;
import ai.timefold.solver.service.definition.api.dto.dataset.ModelRequest;

public record DemoData(DemoMetaData metaData,
        ModelRequest<? extends ModelInput, ? extends ModelConfigOverrides> modelRequest) {
}