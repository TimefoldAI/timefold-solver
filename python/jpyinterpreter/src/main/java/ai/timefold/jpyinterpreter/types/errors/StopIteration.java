package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;

/**
 * Error thrown when a Python iterator has no more values to return.
 */
public class StopIteration extends PythonException {
    public static final PythonLikeType STOP_ITERATION_TYPE = new PythonLikeType("StopIteration",
            StopIteration.class, List.of(EXCEPTION_TYPE)),
            $TYPE = STOP_ITERATION_TYPE;

    static {
        STOP_ITERATION_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new StopIteration(STOP_ITERATION_TYPE,
                        positionalArguments)));
    }

    private final PythonLikeObject value;

    public StopIteration() {
        this(PythonNone.INSTANCE);
    }

    public StopIteration(PythonLikeObject value) {
        this(STOP_ITERATION_TYPE, List.of(value));
    }

    public StopIteration(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
        if (args.size() > 0) {
            value = args.get(0);
        } else {
            value = PythonNone.INSTANCE;
        }
    }

    public PythonLikeObject getValue() {
        return value;
    }

    /**
     * This exception acts as a signal, and should be low cost
     * 
     * @return this
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        // Do nothing
        return this;
    }
}
