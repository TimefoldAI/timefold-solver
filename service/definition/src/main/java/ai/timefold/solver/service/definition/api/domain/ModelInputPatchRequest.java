package ai.timefold.solver.service.definition.api.domain;

import java.util.List;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelInputPatch;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ModelInputPatchRequest<ModelConfigurationOverrides_ extends ModelConfigOverrides>(
        @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(
                description = "Optional configuration to be applied when solving the patched dataset.") Configuration<ModelConfigurationOverrides_> config,
        @Schema(required = true,
                description = "List of patches to be applied to the original dataset.") List<ModelInputPatch> patch) {

}
