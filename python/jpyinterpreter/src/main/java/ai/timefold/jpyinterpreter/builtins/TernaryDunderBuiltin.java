package ai.timefold.jpyinterpreter.builtins;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;

public class TernaryDunderBuiltin implements PythonLikeFunction {
    private final String DUNDER_METHOD_NAME;

    public static final TernaryDunderBuiltin POWER = new TernaryDunderBuiltin("__pow__");
    public static final TernaryDunderBuiltin SETATTR = new TernaryDunderBuiltin(PythonTernaryOperator.SET_ATTRIBUTE);
    public static final TernaryDunderBuiltin GET_DESCRIPTOR = new TernaryDunderBuiltin(PythonTernaryOperator.GET);

    public TernaryDunderBuiltin(String dunderMethodName) {
        DUNDER_METHOD_NAME = dunderMethodName;
    }

    public TernaryDunderBuiltin(PythonTernaryOperator operator) {
        DUNDER_METHOD_NAME = operator.getDunderMethod();
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        if (positionalArguments.size() != 3) {
            throw new ValueError("Function " + DUNDER_METHOD_NAME + " expects 3 positional arguments");
        }

        PythonLikeObject object = positionalArguments.get(0);
        PythonLikeObject arg1 = positionalArguments.get(1);
        PythonLikeObject arg2 = positionalArguments.get(2);
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.$getType().$getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object, arg1, arg2), Map.of(), null);
    }

    public PythonLikeObject invoke(PythonLikeObject object, PythonLikeObject arg1, PythonLikeObject arg2) {
        PythonLikeFunction dunderMethod = (PythonLikeFunction) object.$getType().$getAttributeOrError(DUNDER_METHOD_NAME);
        return dunderMethod.$call(List.of(object, arg1, arg2), Map.of(), null);
    }
}
