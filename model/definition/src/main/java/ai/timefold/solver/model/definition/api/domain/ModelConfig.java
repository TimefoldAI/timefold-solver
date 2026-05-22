package ai.timefold.solver.model.definition.api.domain;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(additionalProperties = Schema.False.class)
public record ModelConfig<ModelConfigurationOverrides_>(
        // need to specify type = Object to avoid generating an empty enum
        @Schema(nullable = true, type = SchemaType.OBJECT,
                description = "The configuration of individual (soft) constraints weights and additional global model configuration attributes.") @JsonInclude(JsonInclude.Include.NON_EMPTY) ModelConfigurationOverrides_ overrides) {

    public static <ModelConfigurationOverrides_ extends ModelConfigOverrides> ModelConfig<ModelConfigurationOverrides_>
            empty() {
        return new ModelConfig<>(null);
    }
}
