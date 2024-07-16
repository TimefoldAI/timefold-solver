package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class SyntaxWarning extends Warning {
    public final static PythonLikeType SYNTAX_WARNING_TYPE =
            new PythonLikeType("SyntaxWarning", SyntaxWarning.class, List.of(WARNING_TYPE)),
            $TYPE = SYNTAX_WARNING_TYPE;

    static {
        SYNTAX_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new SyntaxWarning(SYNTAX_WARNING_TYPE,
                        positionalArguments)));
    }

    public SyntaxWarning() {
        super(SYNTAX_WARNING_TYPE);
    }

    public SyntaxWarning(String message) {
        super(SYNTAX_WARNING_TYPE, message);
    }

    public SyntaxWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public SyntaxWarning(PythonLikeType type) {
        super(type);
    }

    public SyntaxWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
