package ai.timefold.jpyinterpreter.builtins;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;

public class BinaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final BinaryDunderBuiltin DIVMOD = new BinaryDunderBuiltin(PythonBinaryOperator.DIVMOD);
    public static final BinaryDunderBuiltin ADD = new BinaryDunderBuiltin(PythonBinaryOperator.ADD);
    public static final BinaryDunderBuiltin LESS_THAN = new BinaryDunderBuiltin(PythonBinaryOperator.LESS_THAN);
    public static final BinaryDunderBuiltin GET_ITEM = new BinaryDunderBuiltin(PythonBinaryOperator.GET_ITEM);
    public static final BinaryDunderBuiltin GET_ATTRIBUTE = new BinaryDunderBuiltin(PythonBinaryOperator.GET_ATTRIBUTE);
    public static final BinaryDunderBuiltin POWER = new BinaryDunderBuiltin(PythonBinaryOperator.POWER);
    public static final BinaryDunderBuiltin FORMAT = new BinaryDunderBuiltin(PythonBinaryOperator.FORMAT);

    public BinaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public BinaryDunderBuiltin(PythonBinaryOperator operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();

        if (positionalArguments.size() != 2) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 2 positional arguments");
        }

        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeObject arg = positionalArguments.get(1);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.$getType().$getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object, arg), Map.of(), null);
    }

    public PythonLikeObject invoke(PythonLikeObject object, PythonLikeObject arg) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.$getType().$getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object, arg), Map.of(), null);
    }
}
