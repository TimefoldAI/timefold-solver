package ai.timefold.solver.service.definition.api.domain;

import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

public record RunConfiguration(
        @Schema(nullable = true,
                description = "Optional name to be given to the dataset. If not provided, the name will be generated.") @Size(
                        min = 0, max = 255) String name,
        @JsonInclude(JsonInclude.Include.NON_NULL) SolverTerminationConfig termination,
        @Schema(nullable = true,
                description = "Optional maximum number of threads to be used for solving.",
                minimum = "1") @JsonInclude(JsonInclude.Include.NON_EMPTY) @Positive Integer maxThreadCount,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(
                description = "Optional tags to be assigned to the dataset.") @Size(max = 100) Set<String> tags,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(hidden = true) Map<String, String> options) {

    public RunConfiguration(String name, SolverTerminationConfig termination, Integer maxThreadCount, Set<String> tags) {
        this(name, termination, maxThreadCount, tags, null);
    }

    public RunConfiguration(String name, SolverTerminationConfig termination) {
        this(name, termination, null, Set.of());
    }

    public RunConfiguration(Integer maxThreadCount, SolverTerminationConfig termination) {
        this(null, termination, maxThreadCount, Set.of());
    }

    public RunConfiguration(String name) {
        this(name, null, null, Set.of());
    }

    /**
     * Returns a copy of this instance with given termination.
     *
     * @param termination the termination configuration to set in the created copy
     * @return a copy of this instance with given termination, never null
     */
    public RunConfiguration withTermination(SolverTerminationConfig termination) {
        return new RunConfiguration(name(), termination, maxThreadCount(), tags(), options());
    }

    public RunConfiguration override(RunConfiguration configuration) {
        String finalName = name;
        SolverTerminationConfig finalTermination = termination;
        Integer finalMaxThreadCount = maxThreadCount;
        Set<String> finalTags = tags;
        Map<String, String> finalOptions = options;

        if (configuration == null) {
            return this;
        }

        if (finalName == null) {
            finalName = configuration.name();
        }

        if (finalMaxThreadCount == null) {
            finalMaxThreadCount = configuration.maxThreadCount();
        }

        if (finalOptions == null) {
            finalOptions = configuration.options();
        }

        if (finalTermination == null) {
            finalTermination = configuration.termination();
        } else {
            finalTermination = finalTermination.override(configuration.termination());
        }

        if ((finalTags == null || finalTags.isEmpty()) && configuration.tags() != null && !configuration.tags().isEmpty()) {
            finalTags = configuration.tags();
        }

        return new RunConfiguration(finalName, finalTermination, finalMaxThreadCount, finalTags, finalOptions);
    }
}
