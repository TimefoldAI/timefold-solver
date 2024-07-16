package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VariableImplementorTest {
    @Test
    public void testGlobalVariables() {
        PythonCompiledFunction setterCompiledFunction = PythonFunctionBuilder.newFunction("value")
                .loadParameter("value")
                .storeGlobalVariable("my_global")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction getterCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadGlobalVariable("my_global")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        AtomicReference<PythonLikeObject> myGlobalReference = new AtomicReference<>();
        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);

        Mockito.when(interpreter.getGlobal(Mockito.any(), Mockito.eq("my_global")))
                .thenAnswer(invocationOnMock -> myGlobalReference.get());
        Mockito.doAnswer(invocationOnMock -> {
            myGlobalReference.set(invocationOnMock.getArgument(2, PythonLikeObject.class));
            return null;
        }).when(interpreter).setGlobal(Mockito.any(), Mockito.eq("my_global"), Mockito.any());

        Class<? extends Consumer> setterFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(setterCompiledFunction, Consumer.class);
        Class<? extends Supplier> getterFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(getterCompiledFunction, Supplier.class);

        Consumer setter = PythonBytecodeToJavaBytecodeTranslator.createInstance(setterFunctionClass, interpreter);
        Supplier getter = PythonBytecodeToJavaBytecodeTranslator.createInstance(getterFunctionClass, interpreter);

        setter.accept("Value 1");

        Mockito.verify(interpreter).setGlobal(Mockito.any(), Mockito.eq("my_global"),
                Mockito.eq(PythonString.valueOf("Value 1")));
        assertThat(getter.get()).isEqualTo(PythonString.valueOf("Value 1"));

        setter.accept("Value 2");

        Mockito.verify(interpreter).setGlobal(Mockito.any(), Mockito.eq("my_global"),
                Mockito.eq(PythonString.valueOf("Value 2")));
        assertThat(getter.get()).isEqualTo(PythonString.valueOf("Value 2"));
    }
}
