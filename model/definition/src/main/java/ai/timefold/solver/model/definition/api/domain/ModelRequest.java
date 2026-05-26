package ai.timefold.solver.model.definition.api.domain;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelInput;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(additionalProperties = Schema.False.class)
public record ModelRequest<ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides>(
        @Schema(nullable = true,
                name = "config",
                description = "The configuration of the model request. If not provided, defaults of the model are used.") @JsonProperty("config") Configuration<ModelConfigurationOverrides_> configuration,

        @Schema(required = true, description = "The model input to solve.") ModelInput_ modelInput) {

    /**
     * Returns a new {@link ModelRequest} instance with null configuration.
     *
     * @param modelInput the model input to be included in the request
     */
    public ModelRequest(ModelInput_ modelInput) {
        this(null, modelInput);
    }

    /**
     * Returns a copy of this instance with given configuration.
     *
     * @param configuration the model request configuration to set in the created copy
     * @return a copy of this instance with given configuration, never null
     */
    public ModelRequest<ModelInput_, ModelConfigurationOverrides_>
            withConfiguration(Configuration<ModelConfigurationOverrides_> configuration) {
        return new ModelRequest<>(configuration, modelInput);
    }

    @JsonIgnore
    public ModelConfig<ModelConfigurationOverrides_> getModelConfig() {
        return Configuration.getSafeModelConfig(configuration);
    }

    public enum ModelRequestAttribute {
        CONFIG("config"),
        MODEL_INPUT("modelInput");

        private final String value;

        ModelRequestAttribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

}
