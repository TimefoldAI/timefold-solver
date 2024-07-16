package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JumpImplementorTest {
    @Test
    public void testIfTrue() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTrue(block -> {
                    block.loadConstant(10);
                    block.op(ControlOpDescriptor.RETURN_VALUE);
                })
                .loadConstant(-10)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(10L);
        assertThat(javaFunction.apply(10L)).isEqualTo(-10L);
    }

    @Test
    public void testIfFalse() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifFalse(block -> {
                    block.loadConstant(10);
                    block.op(ControlOpDescriptor.RETURN_VALUE);
                })
                .loadConstant(-10)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(-10L);
        assertThat(javaFunction.apply(10L)).isEqualTo(10L);
    }

    @Test
    public void testIfTrueKeepTop() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTruePopTop(block -> {
                    block.loadConstant(true);
                    block.op(ControlOpDescriptor.RETURN_VALUE);
                })
                .op(ControlOpDescriptor.RETURN_VALUE) // Top is False (block was skipped)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(PythonBoolean.TRUE);
        assertThat(javaFunction.apply(10L)).isEqualTo(PythonBoolean.FALSE);
    }

    @Test
    public void testIfFalseKeepTop() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifFalsePopTop(block -> {
                    block.loadConstant(false);
                    block.op(ControlOpDescriptor.RETURN_VALUE);
                })
                .op(ControlOpDescriptor.RETURN_VALUE) // Top is True (block was skipped)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(1L)).isEqualTo(PythonBoolean.TRUE);
        assertThat(javaFunction.apply(10L)).isEqualTo(PythonBoolean.FALSE);
    }
}
