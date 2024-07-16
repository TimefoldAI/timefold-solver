package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class KeyboardInterrupt extends PythonBaseException {
    final public static PythonLikeType KEYBOARD_INTERRUPT_TYPE =
            new PythonLikeType("KeyboardInterrupt", KeyboardInterrupt.class, List.of(PythonBaseException.BASE_EXCEPTION_TYPE)),
            $TYPE = KEYBOARD_INTERRUPT_TYPE;

    static {
        KEYBOARD_INTERRUPT_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new KeyboardInterrupt(KEYBOARD_INTERRUPT_TYPE,
                        positionalArguments)));
    }

    public KeyboardInterrupt(PythonLikeType type) {
        super(type);
    }

    public KeyboardInterrupt(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public KeyboardInterrupt(PythonLikeType type, String message) {
        super(type, message);
    }
}
