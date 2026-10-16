package ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile;

import java.time.Duration;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Record-based model config overrides without a no-arg constructor.
 * Used to verify {@link ai.timefold.solver.service.quarkus.deployment.util.EmptyInstances}
 * can instantiate records for default config profile generation.
 */
public record TestdataModelConfigOverrides(
        // Make sure the field is not omitted by the model descriptor even if it's null by default.
        @JsonFormat(shape = JsonFormat.Shape.STRING) @JsonInclude(JsonInclude.Include.NON_NULL) Duration maximumTimeBurden)
        implements
            ModelConfigOverrides {
}
