package ai.timefold.solver.model.definition.api.domain;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.configuration.MapsConfiguration;
import ai.timefold.solver.model.definition.api.configuration.ResourcesConfiguration;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

// avoid schema name "Configuration" because it clashes with openapi-generator "Configuration"
@Schema(name = "ModelConfiguration", additionalProperties = Schema.False.class)
public record Configuration<ModelConfigurationOverrides_>(
        @Schema(nullable = true,
                description = "The run configuration.") @JsonInclude(JsonInclude.Include.NON_EMPTY) RunConfiguration run,
        @Schema(nullable = true,
                description = "The model configuration. Impacts the quality of solution.") @JsonInclude(JsonInclude.Include.NON_EMPTY) ModelConfig<ModelConfigurationOverrides_> model,
        @Schema(nullable = true,
                readOnly = true) @JsonInclude(JsonInclude.Include.NON_EMPTY) ResourcesConfiguration resourcesConfiguration,
        @Schema(nullable = true,
                readOnly = true) @JsonInclude(JsonInclude.Include.NON_EMPTY) MapsConfiguration mapsConfiguration) {

    public Configuration(RunConfiguration run, ModelConfig<ModelConfigurationOverrides_> model) {
        this(run, model, null, null);
    }

    /**
     * Returns a copy of this instance with given run configuration.
     *
     * @param run the run configuration to set in the created copy
     * @return a copy of this instance with given run configuration, never null
     */
    public Configuration<ModelConfigurationOverrides_> withRun(RunConfiguration run) {
        return new Configuration<>(run, model(), resourcesConfiguration(), mapsConfiguration());
    }

    /**
     * Returns a new empty {@link Configuration} instance.
     *
     * @return the empty configuration (with both run and model configuration empty)
     * @param <ModelConfigurationOverrides_> the specific type of {@link ModelConfigOverrides} used in the request
     */
    public static <ModelConfigurationOverrides_ extends ModelConfigOverrides> Configuration<ModelConfigurationOverrides_>
            empty() {
        return new Configuration<>(null, ModelConfig.empty());
    }

    /**
     * Returns the {@link ModelConfig} from the given {@link Configuration}, or an {@link ModelConfig#empty()} if the
     * configuration or its underlying model configuration is null.
     *
     * @param configuration the configuration to extract the model configuration from
     * @return the model configuration, never null
     */
    public static <ModelConfigurationOverrides_ extends ModelConfigOverrides> ModelConfig<ModelConfigurationOverrides_>
            getSafeModelConfig(Configuration<ModelConfigurationOverrides_> configuration) {
        return configuration == null || configuration.model() == null
                ? ModelConfig.empty()
                : configuration.model();
    }
}
