package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StringOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class StringImplementorTest {
    @Test
    public void testBuildString() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant("My name")
                .loadConstant(" is ")
                .loadConstant("awesome!")
                .op(StringOpDescriptor.BUILD_STRING, 3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo("My name is awesome!");
    }

    @Test
    public void testFormat() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item", "format")
                .loadParameter("item")
                .loadParameter("format")
                .op(StringOpDescriptor.FORMAT_VALUE, 4)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);

        assertThat(javaFunction.apply(12, "x")).isEqualTo("c");
        assertThat(javaFunction.apply(12, "o")).isEqualTo("14");
        assertThat(javaFunction.apply(12, "")).isEqualTo("12");
        assertThat(javaFunction.apply("hello", "")).isEqualTo("hello");
    }

    @Test
    public void testFormatWithoutParameter() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .op(StringOpDescriptor.FORMAT_VALUE, 0)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);

        assertThat(javaFunction.apply(12)).isEqualTo("12");
        assertThat(javaFunction.apply("hello")).isEqualTo("hello");
    }

    @Test
    public void testPrint() {
        PythonCompiledFunction compiledFunction = PythonFunctionBuilder.newFunction("to_print")
                .loadParameter("to_print")
                .op(StringOpDescriptor.PRINT_EXPR)
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);

        Mockito.when(interpreter.getGlobal(Mockito.anyMap(), Mockito.same("print")))
                .thenReturn(((PythonLikeFunction) (pos, keywords, callerInstance) -> {
                    interpreter.write(UnaryDunderBuiltin.STR.invoke(pos.get(0)) + "\n");
                    return PythonNone.INSTANCE;
                }));

        Class<? extends Consumer> functionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(compiledFunction, Consumer.class);

        Consumer consumer = PythonBytecodeToJavaBytecodeTranslator.createInstance(functionClass, interpreter);

        consumer.accept("Value 1");

        Mockito.verify(interpreter).write("Value 1\n");

        Mockito.clearInvocations(interpreter);

        consumer.accept(10);

        Mockito.verify(interpreter).write("10\n");
    }
}
