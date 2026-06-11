package ai.timefold.solver.service.definition.api.termination;

import java.time.Duration;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(additionalProperties = Schema.False.class)
public record SolverTerminationConfig(
        @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(
                description = "Maximum duration (ISO 8601 duration format) to keep the solver running (e.g. PT1H).",
                examples = {
                        "PT1H", "PT30M" }) Duration spentLimit,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(
                description = "Maximum unimproved score duration (ISO 8601 duration format). " +
                        "If the score has not improved during this period (e.g. PT5M), terminate the solver. " +
                        "If no value is provided, the default diminished returns termination will apply. " +
                        "If set, stepCountLimit must be empty. " +
                        "Warning: using this option will disable the default diminished returns termination which is recommended for most use cases.",
                examples = { "PT5M", "PT30S" }) @JsonInclude(JsonInclude.Include.NON_NULL) Duration unimprovedSpentLimit,
        @Schema(description = "Maximum solver step count. " +
                "The solver will stop solving after a pre-determined amount of steps. " +
                "Use when you require results independently of the hardware resources performance. " +
                "Use this termination if you want to benchmark your models, not recommended for production use. " +
                "If set, unimprovedSpentLimit must be empty. " +
                "Warning: using this option will disable the default diminished returns termination which is recommended for most use cases.",
                examples = { "1000", "10000" }) @JsonInclude(JsonInclude.Include.NON_NULL) Integer stepCountLimit,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(
                description = "Sliding window (ISO 8601 duration format) over which score improvement is " +
                        "measured by the diminished returns termination. Defaults to PT30S when omitted. " +
                        "Only takes effect when diminished returns is active (i.e. unimprovedSpentLimit and " +
                        "stepCountLimit are both empty).",
                examples = { "PT30S", "PT5M" }) Duration slidingWindowDuration,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(
                description = "Minimum ratio between current and initial improvement before the diminished " +
                        "returns termination kicks in. Must be strictly positive. Defaults to 0.0001 when omitted. " +
                        "Only takes effect when diminished returns is active (i.e. unimprovedSpentLimit and " +
                        "stepCountLimit are both empty).",
                examples = { "0.0001", "0.01" }) Double minimumImprovementRatio) {

    public SolverTerminationConfig {
        if (minimumImprovementRatio != null && minimumImprovementRatio <= 0) {
            throw new IllegalArgumentException(
                    "minimumImprovementRatio (" + minimumImprovementRatio + ") must be strictly positive.");
        }
    }

    public SolverTerminationConfig(Duration spentLimit, Duration unimprovedSpentLimit, Integer stepCountLimit) {
        this(spentLimit, unimprovedSpentLimit, stepCountLimit, null, null);
    }

    public SolverTerminationConfig(Duration spentLimit, Duration unimprovedSpentLimit) {
        this(spentLimit, unimprovedSpentLimit, null, null, null);
    }

    public SolverTerminationConfig override(SolverTerminationConfig configuration) {
        Duration spentLimit = this.spentLimit;
        Duration unimprovedSpentLimit = this.unimprovedSpentLimit;
        Integer stepCountLimit = this.stepCountLimit;
        Duration slidingWindowDuration = this.slidingWindowDuration;
        Double minimumImprovementRatio = this.minimumImprovementRatio;

        if (configuration == null) {
            return this;
        }

        if (spentLimit == null) {
            spentLimit = configuration.spentLimit();
        }

        // the unimprovedSpentLimit can be null for Diminished Returns termination
        if (unimprovedSpentLimit == null) {
            unimprovedSpentLimit = configuration.unimprovedSpentLimit();
        }

        if (stepCountLimit == null) {
            stepCountLimit = configuration.stepCountLimit();
        }

        if (slidingWindowDuration == null) {
            slidingWindowDuration = configuration.slidingWindowDuration();
        }

        if (minimumImprovementRatio == null) {
            minimumImprovementRatio = configuration.minimumImprovementRatio();
        }

        if (stepCountLimit != null && unimprovedSpentLimit != null) {
            throw new IllegalArgumentException("stepCountLimit and unimprovedSpentLimit cannot be set at the same time.");
        }

        return new SolverTerminationConfig(spentLimit, unimprovedSpentLimit, stepCountLimit, slidingWindowDuration,
                minimumImprovementRatio);
    }

}
