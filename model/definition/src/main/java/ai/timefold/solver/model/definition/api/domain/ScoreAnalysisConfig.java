package ai.timefold.solver.model.definition.api.domain;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(additionalProperties = Schema.False.class)
public record ScoreAnalysisConfig<ModelConfigurationOverrides_>(
        @Schema(nullable = true,
                description = "Model configuration for the score analysis.") ModelConfig<ModelConfigurationOverrides_> model) {

    public static <ModelConfigurationOverrides_> ScoreAnalysisConfig<ModelConfigurationOverrides_> empty() {
        return new ScoreAnalysisConfig<>(null);
    }
}
