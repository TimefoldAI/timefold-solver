package ai.timefold.jpyinterpreter.types;

import ai.timefold.jpyinterpreter.builtins.GlobalBuiltins;

public class NotImplemented extends AbstractPythonLikeObject {
    public static final NotImplemented INSTANCE;
    public static final PythonLikeType NOT_IMPLEMENTED_TYPE = new PythonLikeType("NotImplementedType", NotImplemented.class);
    public static final PythonLikeType $TYPE = NOT_IMPLEMENTED_TYPE;

    static {
        INSTANCE = new NotImplemented();

        GlobalBuiltins.addBuiltinConstant("NotImplemented", INSTANCE);
    }

    private NotImplemented() {
        super(NOT_IMPLEMENTED_TYPE);
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        return "NotImplemented";
    }
}
