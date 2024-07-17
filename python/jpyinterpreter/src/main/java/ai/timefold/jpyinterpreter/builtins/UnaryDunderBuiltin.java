package ai.timefold.jpyinterpreter.builtins;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;

public class UnaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final UnaryDunderBuiltin ABS = new UnaryDunderBuiltin(PythonUnaryOperator.ABS);
    public static final UnaryDunderBuiltin HASH = new UnaryDunderBuiltin(PythonUnaryOperator.HASH);
    public static final UnaryDunderBuiltin INT = new UnaryDunderBuiltin(PythonUnaryOperator.AS_INT);
    public static final UnaryDunderBuiltin FLOAT = new UnaryDunderBuiltin(PythonUnaryOperator.AS_FLOAT);
    public static final UnaryDunderBuiltin INDEX = new UnaryDunderBuiltin(PythonUnaryOperator.AS_INDEX);
    public static final UnaryDunderBuiltin ITERATOR = new UnaryDunderBuiltin(PythonUnaryOperator.ITERATOR);
    public static final UnaryDunderBuiltin LENGTH = new UnaryDunderBuiltin(PythonUnaryOperator.LENGTH);
    public static final UnaryDunderBuiltin NEXT = new UnaryDunderBuiltin(PythonUnaryOperator.NEXT);
    public static final UnaryDunderBuiltin REVERSED = new UnaryDunderBuiltin(PythonUnaryOperator.REVERSED);
    public static final UnaryDunderBuiltin REPRESENTATION = new UnaryDunderBuiltin(PythonUnaryOperator.REPRESENTATION);
    public static final UnaryDunderBuiltin STR = new UnaryDunderBuiltin(PythonUnaryOperator.AS_STRING);

    public UnaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public UnaryDunderBuiltin(PythonUnaryOperator operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        if (positionalArguments.size() != 1) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 1 positional argument");
        }
        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.$getType().$getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object), Map.of(), null);
    }

    public PythonLikeObject invoke(PythonLikeObject object) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.$getType().$getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object), Map.of(), null);
    }
}
