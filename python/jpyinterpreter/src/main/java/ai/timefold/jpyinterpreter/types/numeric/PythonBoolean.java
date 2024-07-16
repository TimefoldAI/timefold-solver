package ai.timefold.jpyinterpreter.types.numeric;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.jpyinterpreter.MethodDescriptor;
import ai.timefold.jpyinterpreter.PythonFunctionSignature;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.GlobalBuiltins;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;

public class PythonBoolean extends PythonInteger {
    public final static PythonBoolean TRUE = new PythonBoolean(true);
    public final static PythonBoolean FALSE = new PythonBoolean(false);

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonBoolean::registerMethods);
    }

    public static PythonLikeType registerMethods() {
        try {
            BuiltinTypes.BOOLEAN_TYPE.addUnaryMethod(PythonUnaryOperator.AS_BOOLEAN,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asBoolean")),
                            BuiltinTypes.BOOLEAN_TYPE));
            BuiltinTypes.BOOLEAN_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asString")),
                            BuiltinTypes.STRING_TYPE));
            BuiltinTypes.BOOLEAN_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                    new PythonFunctionSignature(new MethodDescriptor(
                            PythonBoolean.class.getMethod("asString")),
                            BuiltinTypes.STRING_TYPE));
            BuiltinTypes.BOOLEAN_TYPE.setConstructor(((positionalArguments, namedArguments, callerInstance) -> {
                if (namedArguments.size() > 1) {
                    throw new ValueError("bool does not take named arguments");
                }
                if (positionalArguments.isEmpty()) {
                    return FALSE;
                } else if (positionalArguments.size() == 1) {
                    return PythonBoolean.valueOf(PythonBoolean.isTruthful(positionalArguments.get(0)));
                } else {
                    throw new ValueError("bool expects 0 or 1 arguments, got " + positionalArguments.size());
                }
            }));

            GlobalBuiltins.addBuiltinConstant("True", TRUE);
            GlobalBuiltins.addBuiltinConstant("False", FALSE);
            return BuiltinTypes.BOOLEAN_TYPE;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final boolean booleanValue;

    private PythonBoolean(boolean booleanValue) {
        super(BuiltinTypes.BOOLEAN_TYPE, booleanValue ? BigInteger.ONE : BigInteger.ZERO);
        this.booleanValue = booleanValue;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }

    public PythonBoolean not() {
        if (this == TRUE) {
            return FALSE;
        } else {
            return TRUE;
        }
    }

    public static boolean isTruthful(PythonLikeObject tested) {
        if (tested instanceof PythonBoolean) {
            return tested == TRUE;
        } else if (tested instanceof PythonInteger) {
            return ((PythonInteger) tested).asBoolean() == TRUE;
        } else if (tested instanceof PythonFloat) {
            return ((PythonFloat) tested).asBoolean() == TRUE;
        } else if (tested instanceof PythonNone) {
            return false;
        } else if (tested instanceof Collection) {
            return ((Collection<?>) tested).size() == 0;
        } else if (tested instanceof Map) {
            return ((Map<?, ?>) tested).size() == 0;
        } else {
            PythonLikeType testedType = tested.$getType();
            PythonLikeFunction boolMethod = (PythonLikeFunction) testedType.$getAttributeOrNull("__bool__");
            if (boolMethod != null) {
                return isTruthful(boolMethod.$call(List.of(tested), Map.of(), null));
            }

            PythonLikeFunction lenMethod = (PythonLikeFunction) testedType.$getAttributeOrNull("__len__");
            if (lenMethod != null) {
                return isTruthful(lenMethod.$call(List.of(tested), Map.of(), null));
            }

            return true;
        }
    }

    public PythonBoolean asBoolean() {
        return this;
    }

    public static PythonBoolean valueOf(boolean result) {
        return (result) ? TRUE : FALSE;
    }

    @Override
    public PythonLikeType $getType() {
        return BuiltinTypes.BOOLEAN_TYPE;
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        if (this == TRUE) {
            return "True";
        } else {
            return "False";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonBoolean that = (PythonBoolean) o;
        return booleanValue == that.booleanValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue);
    }

    public PythonString asString() {
        return PythonString.valueOf(toString());
    }
}
