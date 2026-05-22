package ai.timefold.solver.model.definition.api.configuration;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Resource configuration.
 *
 * @param memory The amount of memory in mebibytes (Mi).
 * @param labels Optional labels (selected from labels available in the plan) to use for scheduling of pods
 */
public record ResourcesConfiguration(@JsonInclude(Include.NON_EMPTY) Double memory,
        @JsonInclude(Include.NON_NULL) Map<String, String> labels) {

    public ResourcesConfiguration override(ResourcesConfiguration configuration) {
        Double finalMemory = memory;
        Map<String, String> finalLabels = labels;

        if (configuration == null) {
            return this;
        }

        if (finalMemory == null) {
            finalMemory = configuration.memory();
        }
        if (finalLabels == null) {
            finalLabels = configuration.labels();
        }

        return new ResourcesConfiguration(finalMemory, finalLabels);
    }

}
