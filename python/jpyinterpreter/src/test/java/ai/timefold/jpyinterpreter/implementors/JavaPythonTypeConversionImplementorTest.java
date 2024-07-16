package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongToIntFunction;
import java.util.function.Predicate;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JavaPythonTypeConversionImplementorTest {
    @Test
    public void testLoadParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("b")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);

        assertThat(javaFunction.apply("a", "b")).isEqualTo("b");
        assertThat(javaFunction.apply(1, 2)).isEqualTo(2);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply("a", "b")).isEqualTo("a");
        assertThat(javaFunction.apply(1, 2)).isEqualTo(1);
    }

    @Test
    public void testLoadPrimitiveParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        LongToIntFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, LongToIntFunction.class);

        assertThat(javaFunction.applyAsInt(1L)).isEqualTo(1);
    }

    @Test
    public void testReturnBoolean() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("ignored")
                .loadConstant(Boolean.TRUE)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Predicate javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Predicate.class);

        assertThat(javaFunction.test("Hi")).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("ignored")
                .loadConstant(Boolean.FALSE)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Predicate.class);

        assertThat(javaFunction.test("Hi")).isEqualTo(false);
    }

    @Test
    public void testReturnVoid() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("ignored")
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Consumer javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Consumer.class);

        assertThatCode(() -> javaFunction.accept(null)).doesNotThrowAnyException();
    }
}
