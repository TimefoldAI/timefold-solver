package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.opcodes.descriptor.CollectionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ExceptionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.PythonAssertionError;
import ai.timefold.jpyinterpreter.types.errors.PythonException;
import ai.timefold.jpyinterpreter.types.errors.PythonTraceback;
import ai.timefold.jpyinterpreter.types.errors.StopIteration;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ExceptionImplementorTest {
    @Test
    public void testTryExcept() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .tryCode(code -> {
                    code.loadParameter("item")
                            .loadConstant(5)
                            .compare(CompareOp.LESS_THAN)
                            .ifTrue(block -> {
                                block.loadConstant("Try").op(ControlOpDescriptor.RETURN_VALUE);
                            })
                            .op(ExceptionOpDescriptor.LOAD_ASSERTION_ERROR)
                            .op(ExceptionOpDescriptor.RAISE_VARARGS, 1);
                }, true)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").op(ControlOpDescriptor.RETURN_VALUE);
                }, true)
                .tryEnd()
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);

        assertThat(javaFunction.apply(1)).isEqualTo("Try");
        assertThat(javaFunction.apply(6)).isEqualTo("Assert");
    }

    @Test
    public void testTryExceptFinally() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("item")
                .loadConstant(null).storeGlobalVariable("exception")
                .loadConstant("Before Try").storeGlobalVariable("finally")
                .tryCode(code -> {
                    code.loadParameter("item")
                            .loadConstant(1)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.op(ExceptionOpDescriptor.LOAD_ASSERTION_ERROR)
                                        .op(ExceptionOpDescriptor.RAISE_VARARGS, 1);
                            })
                            .loadParameter("item")
                            .loadConstant(2)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.loadConstant(new StopIteration())
                                        .op(ExceptionOpDescriptor.RAISE_VARARGS, 1);
                            });
                }, false)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, except -> {
                    except.loadConstant("Assert").storeGlobalVariable("exception");
                }, false)
                .andFinally(code -> {
                    code.loadConstant("Finally")
                            .storeGlobalVariable("finally");
                }, false)
                .tryEnd()
                .loadConstant(1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Class javaFunctionClass =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecodeToClass(pythonCompiledFunction, Function.class);

        Map<String, PythonLikeObject> globalsMap = new HashMap<>();
        PythonInterpreter interpreter = Mockito.mock(PythonInterpreter.class);

        Mockito.when(interpreter.getGlobal(Mockito.any(), Mockito.any()))
                .thenAnswer(invocationOnMock -> globalsMap.get(invocationOnMock.getArgument(0, String.class)));
        Mockito.doAnswer(invocationOnMock -> {
            globalsMap.put(invocationOnMock.getArgument(1, String.class),
                    invocationOnMock.getArgument(2, PythonLikeObject.class));
            return null;
        }).when(interpreter).setGlobal(Mockito.any(), Mockito.any(), Mockito.any());

        Function javaFunction =
                (Function) PythonBytecodeToJavaBytecodeTranslator.createInstance(javaFunctionClass, interpreter);

        assertThat(javaFunction.apply(0)).isEqualTo(1);
        assertThat(globalsMap.get("exception")).isEqualTo(PythonNone.INSTANCE);
        assertThat(globalsMap.get("finally")).isEqualTo(PythonString.valueOf("Finally"));

        assertThat(javaFunction.apply(1)).isEqualTo(1);
        assertThat(globalsMap.get("exception")).isEqualTo(PythonString.valueOf("Assert"));
        assertThat(globalsMap.get("finally")).isEqualTo(PythonString.valueOf("Finally"));

        assertThatCode(() -> javaFunction.apply(2)).isInstanceOf(StopIteration.class);
        assertThat(globalsMap.get("exception")).isEqualTo(PythonNone.INSTANCE);
        assertThat(globalsMap.get("finally")).isEqualTo(PythonString.valueOf("Finally"));
    }

    @Test
    public void testExceptionInLoop() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("start", "last_values")
                .loadGlobalVariable("reversed")
                .loadParameter("last_values")
                .callFunction(1)
                .op(CollectionOpDescriptor.GET_ITER)
                .loop(loopBuilder -> {
                    loopBuilder.storeVariable("last_value")
                            .tryCode(tryBuilder -> {
                                tryBuilder.loadVariable("last_value")
                                        .loadConstant(1)
                                        .op(DunderOpDescriptor.BINARY_ADD)
                                        .op(ExceptionOpDescriptor.POP_BLOCK)
                                        .op(StackOpDescriptor.ROT_TWO)
                                        .op(StackOpDescriptor.POP_TOP)
                                        .op(ControlOpDescriptor.RETURN_VALUE);
                            }, true).except(PythonException.EXCEPTION_TYPE, exceptBuilder -> {
                                exceptBuilder
                                        .op(ControlOpDescriptor.JUMP_ABSOLUTE, 4);
                            }, true)
                            .tryEnd();
                }, true)
                .loadParameter("start")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiFunction biFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
    }

    @Test
    public void testWithBlocksWithoutException() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("cxt")
                .loadConstant(0)
                .storeVariable("result")
                .loadParameter("cxt")
                .with(withBuilder -> {
                    withBuilder
                            .loadConstant(1)
                            .op(DunderOpDescriptor.BINARY_ADD)
                            .storeVariable("result");
                })
                .loadVariable("result")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function function =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);

        TestContextManager contextManager = new TestContextManager();
        assertThat(function.apply(contextManager)).isEqualTo(2);
        assertThat(contextManager.hasExited()).isTrue();
        assertThat(contextManager.getException()).isNull();
    }

    @Test
    public void testWithBlocksWithException() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("cxt")
                .loadParameter("cxt")
                .with(withBuilder -> {
                    withBuilder
                            .ifTrue(ifBuilder -> {
                                ifBuilder.op(ExceptionOpDescriptor.LOAD_ASSERTION_ERROR)
                                        .op(ExceptionOpDescriptor.RERAISE);
                            });
                })
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function function =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);

        TestContextManager contextManager = new TestContextManager();
        assertThatCode(() -> function.apply(contextManager)).isInstanceOf(PythonAssertionError.class);
        assertThat(contextManager.hasExited()).isTrue();
        assertThat(contextManager.getException()).isInstanceOf(PythonAssertionError.class);
    }

    public static class TestContextManager {
        Throwable exception;
        boolean exitCalled;

        public TestContextManager() {
            exception = null;
            exitCalled = false;
        }

        public PythonInteger __enter__() {
            return PythonInteger.valueOf(1);
        }

        public PythonBoolean __exit__(PythonLikeType type, Throwable exception, PythonTraceback traceback) {
            exitCalled = true;
            this.exception = exception;
            return PythonBoolean.FALSE;
        }

        public boolean hasExited() {
            return exitCalled;
        }

        public Throwable getException() {
            return exception;
        }
    }
}
