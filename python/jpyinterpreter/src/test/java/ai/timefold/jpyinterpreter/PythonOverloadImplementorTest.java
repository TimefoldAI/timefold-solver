package ai.timefold.jpyinterpreter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

import org.junit.jupiter.api.Test;

public class PythonOverloadImplementorTest {

    @Test
    public void testSingleOverload() throws NoSuchMethodException {
        SingleOverload.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                SingleOverload.class.getMethod("overload")), BuiltinTypes.STRING_TYPE));
        PythonOverloadImplementor.createDispatchesFor(SingleOverload.TYPE);

        SingleOverload instance = new SingleOverload();
        PythonLikeFunction overload = (PythonLikeFunction) SingleOverload.TYPE.$getAttributeOrError("overload");
        assertThat(overload.$call(List.of(instance), Map.of(), null)).isEqualTo(PythonString.valueOf("1"));
    }

    @Test
    public void testDifferentArgCountOverloads() throws NoSuchMethodException {
        DifferentArgCountOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgCountOverloads.class.getMethod("overload")), BuiltinTypes.STRING_TYPE));
        DifferentArgCountOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgCountOverloads.class.getMethod("overload", PythonInteger.class)), BuiltinTypes.INT_TYPE,
                BuiltinTypes.INT_TYPE));
        PythonOverloadImplementor.createDispatchesFor(DifferentArgCountOverloads.TYPE);

        DifferentArgCountOverloads instance = new DifferentArgCountOverloads();
        PythonLikeFunction overload = (PythonLikeFunction) DifferentArgCountOverloads.TYPE.$getAttributeOrError("overload");
        assertThat(overload.$call(List.of(instance), Map.of(), null)).isEqualTo(PythonString.valueOf("1"));
        assertThat(overload.$call(List.of(instance, PythonInteger.valueOf(2)), Map.of(), null))
                .isEqualTo(PythonInteger.valueOf(2));
        assertThat(overload.$call(List.of(instance, PythonInteger.valueOf(3)), Map.of(), null))
                .isEqualTo(PythonInteger.valueOf(3));
    }

    @Test
    public void testDifferentArgTypeOverloads() throws NoSuchMethodException {
        DifferentArgTypeOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgTypeOverloads.class.getMethod("overload", PythonString.class)), BuiltinTypes.STRING_TYPE,
                BuiltinTypes.STRING_TYPE));
        DifferentArgTypeOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgTypeOverloads.class.getMethod("overload", PythonInteger.class)), BuiltinTypes.INT_TYPE,
                BuiltinTypes.INT_TYPE));
        DifferentArgTypeOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                DifferentArgTypeOverloads.class.getMethod("overload", PythonBoolean.class)), BuiltinTypes.BOOLEAN_TYPE,
                BuiltinTypes.BOOLEAN_TYPE));
        PythonOverloadImplementor.createDispatchesFor(DifferentArgTypeOverloads.TYPE);

        DifferentArgTypeOverloads instance = new DifferentArgTypeOverloads();
        PythonLikeFunction overload = (PythonLikeFunction) DifferentArgTypeOverloads.TYPE.$getAttributeOrError("overload");
        assertThat(overload.$call(List.of(instance, PythonString.valueOf("1")), Map.of(), null))
                .isEqualTo(PythonString.valueOf("1"));

        assertThat(overload.$call(List.of(instance, PythonInteger.valueOf(2)), Map.of(), null))
                .isEqualTo(PythonInteger.valueOf(2));
        assertThat(overload.$call(List.of(instance, PythonInteger.valueOf(3)), Map.of(), null))
                .isEqualTo(PythonInteger.valueOf(3));

        assertThat(overload.$call(List.of(instance, PythonBoolean.TRUE), Map.of(), null)).isEqualTo(PythonBoolean.FALSE);
        assertThat(overload.$call(List.of(instance, PythonBoolean.FALSE), Map.of(), null)).isEqualTo(PythonBoolean.TRUE);
    }

    @Test
    public void testVariousOverloads() throws NoSuchMethodException {
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload")), BuiltinTypes.STRING_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonString.class)), BuiltinTypes.STRING_TYPE,
                BuiltinTypes.STRING_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonInteger.class)), BuiltinTypes.INT_TYPE,
                BuiltinTypes.INT_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonString.class, PythonString.class)), BuiltinTypes.STRING_TYPE,
                BuiltinTypes.STRING_TYPE,
                BuiltinTypes.STRING_TYPE));
        VariousOverloads.TYPE.addMethod("overload", new PythonFunctionSignature(new MethodDescriptor(
                VariousOverloads.class.getMethod("overload", PythonInteger.class, PythonInteger.class)), BuiltinTypes.INT_TYPE,
                BuiltinTypes.INT_TYPE,
                BuiltinTypes.INT_TYPE));
        PythonOverloadImplementor.createDispatchesFor(VariousOverloads.TYPE);

        VariousOverloads instance = new VariousOverloads();
        PythonLikeFunction overload = (PythonLikeFunction) VariousOverloads.TYPE.$getAttributeOrError("overload");
        assertThat(overload.$call(List.of(instance), Map.of(), null)).isEqualTo(PythonString.valueOf("1"));
        assertThat(overload.$call(List.of(instance, PythonString.valueOf("a")), Map.of(), null))
                .isEqualTo(PythonString.valueOf("a 1"));
        assertThat(overload.$call(List.of(instance, PythonInteger.valueOf(2)), Map.of(), null))
                .isEqualTo(PythonInteger.valueOf(2));
        assertThat(overload.$call(List.of(instance, PythonString.valueOf("a"), PythonString.valueOf("b")), Map.of(), null))
                .isEqualTo(PythonString.valueOf("a b"));
        assertThat(overload.$call(List.of(instance, PythonInteger.valueOf(1), PythonInteger.valueOf(2)), Map.of(), null))
                .isEqualTo(PythonInteger.valueOf(3));
    }

    public static class SingleOverload extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("single-overload", SingleOverload.class);

        public SingleOverload() {
            super(TYPE);
        }

        public PythonString overload() {
            return PythonString.valueOf("1");
        }
    }

    public static class DifferentArgCountOverloads extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("different-arg-count-overloads", DifferentArgCountOverloads.class);

        public DifferentArgCountOverloads() {
            super(TYPE);
        }

        public PythonString overload() {
            return PythonString.valueOf("1");
        }

        public PythonInteger overload(PythonInteger arg) {
            return arg;
        }
    }

    public static class DifferentArgTypeOverloads extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("different-arg-type-overloads", DifferentArgTypeOverloads.class);

        public DifferentArgTypeOverloads() {
            super(TYPE);
        }

        public PythonString overload(PythonString string) {
            return string;
        }

        public PythonInteger overload(PythonInteger integer) {
            return integer;
        }

        public PythonBoolean overload(PythonBoolean bool) {
            return bool.not();
        }
    }

    public static class VariousOverloads extends AbstractPythonLikeObject {
        static PythonLikeType TYPE = new PythonLikeType("various-overloads", VariousOverloads.class);

        public VariousOverloads() {
            super(TYPE);
        }

        public PythonString overload() {
            return PythonString.valueOf("1");
        }

        public PythonString overload(PythonString string) {
            return PythonString.valueOf(string.getValue() + " 1");
        }

        public PythonInteger overload(PythonInteger integer) {
            return integer;
        }

        public PythonString overload(PythonString a, PythonString b) {
            return PythonString.valueOf(a.getValue() + " " + b.getValue());
        }

        public PythonInteger overload(PythonInteger a, PythonInteger b) {
            return PythonInteger.valueOf(a.getValue().intValue() + b.getValue().intValue());
        }
    }
}
