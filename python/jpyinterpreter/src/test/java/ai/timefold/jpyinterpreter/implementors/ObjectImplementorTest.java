package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.jpyinterpreter.MyObject;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ObjectOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ObjectImplementorTest {

    @Test
    public void testIs() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(StackOpDescriptor.DUP_TOP)
                .op(ObjectOpDescriptor.IS_OP, 0)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.TRUE);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(StackOpDescriptor.DUP_TOP)
                .loadConstant(0)
                .op(DunderOpDescriptor.BINARY_ADD)
                .op(ObjectOpDescriptor.IS_OP, 0)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.FALSE);

        // Test is not
        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(StackOpDescriptor.DUP_TOP)
                .op(ObjectOpDescriptor.IS_OP, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.FALSE);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .op(StackOpDescriptor.DUP_TOP)
                .loadConstant(0)
                .op(DunderOpDescriptor.BINARY_ADD)
                .op(ObjectOpDescriptor.IS_OP, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(PythonBoolean.TRUE);
    }

    @Test
    public void testGetAttribute() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadParameter("item")
                .getAttribute("name")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        MyObject object = new MyObject();
        object.name = "My name";
        assertThat(javaFunction.apply(object)).isEqualTo("My name");
    }

    @Test
    public void testSetAttribute() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item", "value")
                .loadParameter("value")
                .loadParameter("item")
                .storeAttribute("name")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiConsumer javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiConsumer.class);
        MyObject object = new MyObject();
        object.name = "My name";
        javaFunction.accept(object, "New name");
        assertThat(object.name).isEqualTo("New name");
    }

}
