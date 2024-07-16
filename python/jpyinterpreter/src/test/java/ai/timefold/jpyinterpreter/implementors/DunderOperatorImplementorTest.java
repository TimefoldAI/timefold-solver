package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DunderOperatorImplementorTest {
    @Test
    public void testComparisons() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiPredicate javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.LESS_THAN_OR_EQUALS)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.GREATER_THAN_OR_EQUALS)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);
    }

    @Test
    public void testEquals() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.EQUALS)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiPredicate javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(false);
        assertThat(javaFunction.test(2, 1)).isEqualTo(false);
        assertThat(javaFunction.test(1, 1)).isEqualTo(true);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .compare(CompareOp.NOT_EQUALS)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiPredicate.class);

        assertThat(javaFunction.test(1, 2)).isEqualTo(true);
        assertThat(javaFunction.test(2, 1)).isEqualTo(true);
        assertThat(javaFunction.test(1, 1)).isEqualTo(false);
    }

    private BiFunction getMathFunction(DunderOpDescriptor opcodeIdentifier) {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a", "b")
                .loadParameter("a")
                .loadParameter("b")
                .op(opcodeIdentifier)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        return PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
    }

    @Test
    public void testMathOp() {
        BiFunction javaFunction = getMathFunction(DunderOpDescriptor.BINARY_ADD);

        assertThat(javaFunction.apply(1L, 2L)).isEqualTo(3L);

        javaFunction = getMathFunction(DunderOpDescriptor.BINARY_SUBTRACT);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);

        javaFunction = getMathFunction(DunderOpDescriptor.BINARY_TRUE_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1.5d);

        javaFunction = getMathFunction(DunderOpDescriptor.BINARY_FLOOR_DIVIDE);
        assertThat(javaFunction.apply(3L, 2L)).isEqualTo(1L);
    }
}
