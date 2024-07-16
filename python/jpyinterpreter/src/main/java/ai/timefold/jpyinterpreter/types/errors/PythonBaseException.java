package ai.timefold.jpyinterpreter.types.errors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.wrappers.JavaObjectWrapper;

/**
 * Python base class for all exceptions. Equivalent to Java's {@link Throwable}.
 */
public class PythonBaseException extends RuntimeException implements PythonLikeObject {
    final public static PythonLikeType BASE_EXCEPTION_TYPE = new PythonLikeType("BaseException", PythonBaseException.class),
            $TYPE = BASE_EXCEPTION_TYPE;

    static {
        BASE_EXCEPTION_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new PythonBaseException(BASE_EXCEPTION_TYPE,
                        positionalArguments)));
    }

    Map<String, PythonLikeObject> dict;

    final PythonLikeType type;
    final List<PythonLikeObject> args;

    private static String getMessageFromArgs(List<PythonLikeObject> args) {
        if (args.size() < 1) {
            return null;
        }

        if (args.get(0) instanceof PythonString) {
            return ((PythonString) args.get(0)).getValue();
        }

        return null;
    }

    public PythonBaseException(PythonLikeType type) {
        this(type, Collections.emptyList());
    }

    public PythonBaseException(PythonLikeType type, List<PythonLikeObject> args) {
        super(getMessageFromArgs(args));
        this.type = type;
        this.args = args;
        this.dict = new HashMap<>();
        $setAttribute("args", PythonLikeTuple.fromList(args));
        $setAttribute("__cause__", PythonNone.INSTANCE);
    }

    public PythonBaseException(PythonLikeType type, String message) {
        super(message);
        this.type = type;
        this.args = List.of(PythonString.valueOf(message));
        this.dict = new HashMap<>();
        $setAttribute("args", PythonLikeTuple.fromList(args));
        $setAttribute("__cause__", PythonNone.INSTANCE);
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        super.initCause(cause);
        if (cause instanceof PythonLikeObject pythonError) {
            $setAttribute("__cause__", pythonError);
        } else {
            $setAttribute("__cause__", new JavaObjectWrapper(cause));
        }
        return this;
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String attributeName) {
        return dict.get(attributeName);
    }

    @Override
    public void $setAttribute(String attributeName, PythonLikeObject value) {
        dict.put(attributeName, value);
    }

    @Override
    public void $deleteAttribute(String attributeName) {
        dict.remove(attributeName);
    }

    public PythonLikeTuple $getArgs() {
        return (PythonLikeTuple) $getAttributeOrError("args");
    }

    @Override
    public PythonLikeType $getType() {
        return type;
    }
}
