package ai.timefold.solver.model.definition.api.domain;

import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.model.definition.api.metrics.ModelOutputMetrics;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelResponse<Score_, ModelOutput_ extends ModelOutput, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics>(
        @Schema(nullable = true,
                description = "The model dataset metadata.") Metadata<Score_> metadata,

        @Schema(nullable = true,
                description = "The solution to the requested model input.") ModelOutput_ modelOutput,

        @Schema(nullable = true,
                description = "Key metrics aggregated from the model input.") InputMetrics_ inputMetrics,

        @Schema(nullable = true,
                description = "Key metrics aggregated from the model output.",
                name = "kpis") @JsonProperty("kpis") OutputMetrics_ outputMetrics) {

    // For backward compatibility; effectively an alias for "metadata", duplicated in the response JSON.
    @Schema(nullable = true, description = "The model run metadata. Deprecated in favor of \"metadata\"", deprecated = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Metadata<Score_> getRun() {
        return metadata;
    }

    public enum ModelResponseAttribute {
        RUN("run"),
        METADATA("metadata"),
        MODEL_OUTPUT("modelOutput"),
        INPUT_METRICS("inputMetrics"),
        KPIS("kpis");

        private final String value;

        ModelResponseAttribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
