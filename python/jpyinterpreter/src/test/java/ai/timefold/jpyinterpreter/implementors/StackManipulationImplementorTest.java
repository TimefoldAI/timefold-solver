package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Supplier;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

public class StackManipulationImplementorTest {
    @Test
    public void testRot2() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .op(StackOpDescriptor.ROT_TWO)
                .tuple(2)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(2, 1));
    }

    @Test
    public void testRot3() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .op(StackOpDescriptor.ROT_THREE)
                .tuple(3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(3, 1, 2));
    }

    @Test
    public void testRot4() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .loadConstant(4)
                .op(StackOpDescriptor.ROT_FOUR)
                .tuple(4)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier<?> javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);

        assertThat(javaFunction.get()).isEqualTo(List.of(4, 1, 2, 3));
    }
}
