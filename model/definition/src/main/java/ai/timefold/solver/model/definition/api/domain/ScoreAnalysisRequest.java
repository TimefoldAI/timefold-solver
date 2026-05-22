package ai.timefold.solver.model.definition.api.domain;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelInput;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(additionalProperties = Schema.False.class)
public record ScoreAnalysisRequest<ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides>(
        @Schema(nullable = true,
                description = "Configuration for the score analysis.") ScoreAnalysisConfig<ModelConfigurationOverrides_> config,
        @Schema(required = true, description = "The model input to analyze.") ModelInput_ modelInput) {

    @JsonCreator
    public ScoreAnalysisRequest(@JsonProperty("config") ScoreAnalysisConfig<ModelConfigurationOverrides_> config,
            @JsonProperty("modelInput") ModelInput_ modelInput) {
        this.config = config == null ? ScoreAnalysisConfig.empty() : config;
        this.modelInput = modelInput;
    }

    /**
     * Returns a new {@link ScoreAnalysisRequest} instance with empty configuration.
     *
     * @param modelInput the model input to be included in the request
     */
    public ScoreAnalysisRequest(ModelInput_ modelInput) {
        this(null, modelInput);
    }
}
