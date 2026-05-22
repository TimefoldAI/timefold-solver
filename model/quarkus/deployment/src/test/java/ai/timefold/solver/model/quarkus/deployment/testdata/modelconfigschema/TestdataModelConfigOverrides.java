package ai.timefold.solver.model.quarkus.deployment.testdata.modelconfigschema;

import java.time.Duration;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.domain.DataFormat;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class TestdataModelConfigOverrides implements ModelConfigOverrides {

    public static final long DEFAULT_WEIGHT_ONE = 1L;

    @Schema(name = "constraintWeight", title = "Constraint weight", description = "Constraint weight description")
    private long constraintWeight = DEFAULT_WEIGHT_ONE;

    @Schema(name = "primitiveParam", format = DataFormat.Values.PERCENTAGE)
    private long primitive = DEFAULT_WEIGHT_ONE;

    @Schema(name = "primitiveArrayParam", title = "Array of numbers", format = DataFormat.Values.NUMBER)
    private int[] primitiveArray = new int[0];

    @Schema(name = "duration", title = "Duration", format = DataFormat.Values.DURATION)
    private Duration duration = Duration.ZERO;

    // Getters added due to serialization in tests when the extension is created (@RegisterExtension)

    @Schema(description = "String parameter")
    private String string;

    public long getConstraintWeight() {
        return constraintWeight;
    }

    public long getPrimitive() {
        return primitive;
    }

    public int[] getPrimitiveArray() {
        return primitiveArray;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getString() {
        return string;
    }
}
