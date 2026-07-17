package ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema;

import java.math.BigDecimal;
import java.time.Duration;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.domain.DataFormat;

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

    @Schema(description = "String parameter", nullable = true)
    private String string;

    @Schema(name = "bigDecimalConstraintWeight")
    private BigDecimal bigDecimalConstraintWeight = BigDecimal.ZERO;

    // Getters added due to serialization in tests when the extension is created (@RegisterExtension)

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

    public BigDecimal getBigDecimalConstraintWeight() {
        return bigDecimalConstraintWeight;
    }
}
