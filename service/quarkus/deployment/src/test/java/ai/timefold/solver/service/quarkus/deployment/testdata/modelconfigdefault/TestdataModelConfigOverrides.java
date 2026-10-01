package ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigdefault;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;

public class TestdataModelConfigOverrides implements ModelConfigOverrides {

    public static final long DEFAULT_WEIGHT_ONE = 1L;

    private long constraintWeight = DEFAULT_WEIGHT_ONE;

    private long primitive = DEFAULT_WEIGHT_ONE;

    // Getters added due to serialization in tests when the extension is created (@RegisterExtension)

    private int[] primitiveArray = new int[0];

    private Duration duration = Duration.ZERO;

    private String string;

    private List<String> stringList = new ArrayList<>();

    private String[] stringArray = new String[0];

    private ObjectParameter object;

    private List<ObjectParameter> objectList = new ArrayList<>();

    private ObjectParameter[] objectArray = new ObjectParameter[0];

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

    public List<String> getStringList() {
        return stringList;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public ObjectParameter getObject() {
        return object;
    }

    public List<ObjectParameter> getObjectList() {
        return objectList;
    }

    public ObjectParameter[] getObjectArray() {
        return objectArray;
    }
}
