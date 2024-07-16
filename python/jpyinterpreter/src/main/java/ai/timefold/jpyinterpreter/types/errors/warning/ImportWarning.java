package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class ImportWarning extends Warning {
    public final static PythonLikeType IMPORT_WARNING_TYPE =
            new PythonLikeType("ImportWarning", ImportWarning.class, List.of(WARNING_TYPE)),
            $TYPE = IMPORT_WARNING_TYPE;

    static {
        IMPORT_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new ImportWarning(IMPORT_WARNING_TYPE,
                        positionalArguments)));
    }

    public ImportWarning() {
        super(IMPORT_WARNING_TYPE);
    }

    public ImportWarning(String message) {
        super(IMPORT_WARNING_TYPE, message);
    }

    public ImportWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ImportWarning(PythonLikeType type) {
        super(type);
    }

    public ImportWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
