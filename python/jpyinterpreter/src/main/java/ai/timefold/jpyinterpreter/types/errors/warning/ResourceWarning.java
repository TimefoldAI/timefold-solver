package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class ResourceWarning extends Warning {
    public final static PythonLikeType RESOURCE_WARNING_TYPE =
            new PythonLikeType("ResourceWarning", ResourceWarning.class, List.of(WARNING_TYPE)),
            $TYPE = RESOURCE_WARNING_TYPE;

    static {
        RESOURCE_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new ResourceWarning(RESOURCE_WARNING_TYPE,
                        positionalArguments)));
    }

    public ResourceWarning() {
        super(RESOURCE_WARNING_TYPE);
    }

    public ResourceWarning(String message) {
        super(RESOURCE_WARNING_TYPE, message);
    }

    public ResourceWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ResourceWarning(PythonLikeType type) {
        super(type);
    }

    public ResourceWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
