package ai.timefold.jpyinterpreter.dag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.OpcodeWithoutSource;
import ai.timefold.jpyinterpreter.opcodes.descriptor.CollectionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ExceptionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ModuleOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StringOpDescriptor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.PythonAssertionError;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;
import ai.timefold.jpyinterpreter.types.errors.PythonTraceback;
import ai.timefold.jpyinterpreter.types.errors.StopIteration;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

public class FlowGraphTest {

    private static PythonLikeType OBJECT_TYPE = BuiltinTypes.BASE_TYPE;

    static FlowGraph getFlowGraph(FunctionMetadata functionMetadata, StackMetadata initialStackMetadata,
            PythonCompiledFunction function) {
        List<Opcode> out = new ArrayList<>(function.instructionList.size());
        for (PythonBytecodeInstruction instruction : function.instructionList) {
            out.add(Opcode.lookupOpcodeForInstruction(instruction, PythonVersion.PYTHON_3_10));
        }
        return FlowGraph.createFlowGraph(functionMetadata, initialStackMetadata, out);
    }

    static FunctionMetadata getFunctionMetadata(PythonCompiledFunction function) {
        FunctionMetadata out = new FunctionMetadata();
        out.functionType = PythonBytecodeToJavaBytecodeTranslator.getFunctionType(function);
        out.className = FlowGraphTest.class.getName();
        out.pythonCompiledFunction = function;
        out.bytecodeCounterToLabelMap = new HashMap<>();
        out.bytecodeCounterToCodeArgumenterList = new HashMap<>();
        return out;
    }

    static StackMetadata getInitialStackMetadata(PythonCompiledFunction pythonCompiledFunction) {
        return new StackMetadata(new LocalVariableHelper(pythonCompiledFunction.getParameterTypes()
                .stream()
                .map(type -> Type.getType(type.getJavaTypeDescriptor()))
                .toArray(Type[]::new),
                pythonCompiledFunction));
    }

    static List<FrameData> getFrameData(FlowGraph flowGraph) {
        List<StackMetadata> stackMetadataList = flowGraph.getStackMetadataForOperations();
        List<FrameData> out = new ArrayList<>(stackMetadataList.size());

        for (int i = 0; i < stackMetadataList.size(); i++) {
            out.add(FrameData.from(i, stackMetadataList.get(i)));
        }

        return out;
    }

    @Test
    public void testStackMetadataForBasicOps() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant("Hi")
                .op(StackOpDescriptor.ROT_TWO)
                .tuple(2)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0),
                new FrameData(1).stack(BuiltinTypes.INT_TYPE),
                new FrameData(2).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.STRING_TYPE),
                new FrameData(3).stack(BuiltinTypes.STRING_TYPE, BuiltinTypes.INT_TYPE),
                new FrameData(4).stack(BuiltinTypes.TUPLE_TYPE));
    }

    @Test
    public void testStackMetadataForLocalVariables() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .storeVariable("one")
                .loadConstant("2")
                .storeVariable("two")
                .loadVariable("one")
                .loadVariable("two")
                .tuple(2)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0).locals(null, null),
                new FrameData(1).stack(BuiltinTypes.INT_TYPE).locals(null, null),
                new FrameData(2).stack().locals(BuiltinTypes.INT_TYPE, null),
                new FrameData(3).stack(BuiltinTypes.STRING_TYPE).locals(BuiltinTypes.INT_TYPE, null),
                new FrameData(4).stack().locals(BuiltinTypes.INT_TYPE, BuiltinTypes.STRING_TYPE),
                new FrameData(5).stack(BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE, BuiltinTypes.STRING_TYPE),
                new FrameData(6).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.STRING_TYPE).locals(BuiltinTypes.INT_TYPE,
                        BuiltinTypes.STRING_TYPE),
                new FrameData(7).stack(BuiltinTypes.TUPLE_TYPE).locals(BuiltinTypes.INT_TYPE, BuiltinTypes.STRING_TYPE));
    }

    @Test
    public void testStackMetadataForLoops() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(0)
                .storeVariable("sum")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(CollectionOpDescriptor.GET_ITER)
                .loop(block -> {
                    block.loadVariable("sum");
                    block.op(DunderOpDescriptor.BINARY_ADD);
                    block.storeVariable("sum");
                })
                .loadVariable("sum")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0).locals((PythonLikeType) null), // LOAD_CONSTANT
                new FrameData(1).stack(BuiltinTypes.INT_TYPE).locals((PythonLikeType) null), // STORE
                new FrameData(2).stack().locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(3).stack(BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(4).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(5).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE)
                        .locals(BuiltinTypes.INT_TYPE), // TUPLE(3)

                // Type information is lost because Tuple is not generic
                new FrameData(6).stack(BuiltinTypes.TUPLE_TYPE).locals(BuiltinTypes.INT_TYPE), // ITERATOR
                new FrameData(7).stack(BuiltinTypes.ITERATOR_TYPE).locals(OBJECT_TYPE), // NEXT
                new FrameData(8).stack(BuiltinTypes.ITERATOR_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // LOAD_VAR
                new FrameData(9).stack(BuiltinTypes.ITERATOR_TYPE, OBJECT_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // ADD
                new FrameData(10).stack(BuiltinTypes.ITERATOR_TYPE, OBJECT_TYPE).locals(OBJECT_TYPE), // STORE
                new FrameData(11).stack(BuiltinTypes.ITERATOR_TYPE).locals(OBJECT_TYPE), // JUMP_ABS
                new FrameData(12).stack().locals(OBJECT_TYPE), // NOP
                new FrameData(13).stack().locals(OBJECT_TYPE), // LOAD_VAR
                new FrameData(14).stack(OBJECT_TYPE).locals(OBJECT_TYPE) // RETURN
        );
    }

    @Test
    public void testStackMetadataForExceptions() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .tryCode(code -> {
                    code.loadConstant(5)
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

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0).stack(), // SETUP_TRY
                new FrameData(1).stack(), // SETUP_TRY
                new FrameData(2).stack(), // LOAD_CONSTANT
                new FrameData(3).stack(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(4).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE), // COMPARE
                new FrameData(5).stack(BuiltinTypes.BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                new FrameData(6).stack(), // LOAD_CONSTANT
                new FrameData(7).stack(BuiltinTypes.STRING_TYPE), // RETURN
                new FrameData(8).stack(), // NOP
                new FrameData(9).stack(), // LOAD_ASSERTION_ERROR
                new FrameData(10).stack(PythonAssertionError.ASSERTION_ERROR_TYPE), // RAISE
                new FrameData(11).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // except handler; DUP_TOP,
                new FrameData(12).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // LOAD_CONSTANT
                new FrameData(13).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        BuiltinTypes.TYPE_TYPE), // JUMP_IF_NOT_EXC_MATCH
                new FrameData(14).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(15).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(16).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE), // POP_TOP
                new FrameData(17).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE), // POP_EXCEPT
                new FrameData(18).stack(), // LOAD_CONSTANT
                new FrameData(19).stack(BuiltinTypes.STRING_TYPE), // RETURN
                new FrameData(20).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(21).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(22).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE) // RERAISE
        );
    }

    @Test
    public void testStackMetadataForTryFinally() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .tryCode(code -> {
                    code.loadConstant(1)
                            .loadConstant(1)
                            .compare(CompareOp.EQUALS)
                            .ifTrue(block -> {
                                block.op(ExceptionOpDescriptor.LOAD_ASSERTION_ERROR)
                                        .op(ExceptionOpDescriptor.RAISE_VARARGS, 1);
                            })
                            .loadConstant(1)
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

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0).stack(), // SETUP_TRY
                new FrameData(1).stack(), // SETUP_TRY
                new FrameData(2).stack(), // LOAD_CONSTANT
                new FrameData(3).stack(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(4).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE), // COMPARE
                new FrameData(5).stack(BuiltinTypes.BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                new FrameData(6).stack(), // LOAD_ASSERTION_ERROR
                new FrameData(7).stack(PythonAssertionError.ASSERTION_ERROR_TYPE), // RAISE
                new FrameData(8).stack(), // NOP
                new FrameData(9).stack(), // LOAD_CONSTANT
                new FrameData(10).stack(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(11).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE), // COMPARE
                new FrameData(12).stack(BuiltinTypes.BOOLEAN_TYPE), // POP_JUMP_IF_TRUE
                new FrameData(13).stack(), // LOAD_CONSTANT
                new FrameData(14).stack(StopIteration.STOP_ITERATION_TYPE), // RAISE
                new FrameData(15).stack(), // NOP
                new FrameData(16).stack(), // JUMP_ABSOLUTE
                new FrameData(17).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // except handler; DUP_TOP,
                new FrameData(18).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // LOAD_CONSTANT
                new FrameData(19).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        BuiltinTypes.TYPE_TYPE), // JUMP_IF_NOT_EXC_MATCH
                new FrameData(20).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(21).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(22).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE), // POP_TOP
                new FrameData(23).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE), // POP_EXCEPT
                new FrameData(24).stack(), // LOAD_CONSTANT
                new FrameData(25).stack(BuiltinTypes.STRING_TYPE), // STORE_GLOBAL
                new FrameData(26).stack(), // JUMP_ABSOLUTE
                new FrameData(27).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // RERAISE
                new FrameData(28).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // POP_TOP
                new FrameData(29).stack(), // POP_TOP
                new FrameData(30).stack(), // POP_TOP
                new FrameData(31).stack(BuiltinTypes.STRING_TYPE), // STORE
                new FrameData(32).stack(), // JUMP_ABSOLUTE
                new FrameData(33).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // NO-OP; Uncaught exception handler
                new FrameData(34).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), // LOAD-CONSTANT
                new FrameData(35).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE,
                        BuiltinTypes.STRING_TYPE), // STORE
                new FrameData(36).stack(BuiltinTypes.NONE_TYPE, BuiltinTypes.INT_TYPE, BuiltinTypes.NONE_TYPE,
                        PythonTraceback.TRACEBACK_TYPE, PythonBaseException.BASE_EXCEPTION_TYPE,
                        PythonBaseException.BASE_EXCEPTION_TYPE), //  POP-TOP
                new FrameData(37).stack(), // RERAISE
                new FrameData(38).stack(), // NO-OP; After try
                new FrameData(39).stack(BuiltinTypes.INT_TYPE));
    }

    @Test
    public void testStackMetadataForIfStatementsThatExitEarly() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .storeVariable("a")
                .loadVariable("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTrue(block -> {
                    block.loadConstant("10");
                    block.storeVariable("a");
                    block.loadVariable("a");
                    block.op(ControlOpDescriptor.RETURN_VALUE);
                })
                .loadConstant(-10)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0).stack().locals((PythonLikeType) null), // LOAD_CONSTANT
                new FrameData(1).stack(BuiltinTypes.INT_TYPE).locals((PythonLikeType) null), // STORE
                new FrameData(2).stack().locals(BuiltinTypes.INT_TYPE), // LOAD_VARIABLE
                new FrameData(3).stack(BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(4).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE), // COMPARE_OP
                new FrameData(5).stack(BuiltinTypes.BOOLEAN_TYPE).locals(BuiltinTypes.INT_TYPE), // POP_JUMP_IF_TRUE
                new FrameData(6).stack().locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(7).stack(BuiltinTypes.STRING_TYPE).locals(BuiltinTypes.INT_TYPE), // STORE
                new FrameData(8).stack().locals(BuiltinTypes.STRING_TYPE), // LOAD_VARIABLE
                new FrameData(9).stack(BuiltinTypes.STRING_TYPE).locals(BuiltinTypes.STRING_TYPE), // RETURN
                new FrameData(10).stack().locals(BuiltinTypes.INT_TYPE), // NOP
                new FrameData(11).stack().locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(12).stack(BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE) // RETURN
        );
    }

    @Test
    public void testStackMetadataForIfStatementsThatDoNotExitEarly() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(5)
                .storeVariable("a")
                .loadVariable("a")
                .loadConstant(5)
                .compare(CompareOp.LESS_THAN)
                .ifTrue(block -> {
                    block.loadConstant("10");
                    block.storeVariable("a");
                })
                .loadConstant(-10)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction);
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        List<FrameData> stackMetadataList = getFrameData(flowGraph);

        assertThat(stackMetadataList).containsExactly(
                new FrameData(0).stack().locals((PythonLikeType) null), // LOAD_CONSTANT
                new FrameData(1).stack(BuiltinTypes.INT_TYPE).locals((PythonLikeType) null), // STORE
                new FrameData(2).stack().locals(BuiltinTypes.INT_TYPE), // LOAD_VARIABLE
                new FrameData(3).stack(BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(4).stack(BuiltinTypes.INT_TYPE, BuiltinTypes.INT_TYPE).locals(BuiltinTypes.INT_TYPE), // COMPARE_OP
                new FrameData(5).stack(BuiltinTypes.BOOLEAN_TYPE).locals(BuiltinTypes.INT_TYPE), // POP_JUMP_IF_TRUE
                new FrameData(6).stack().locals(BuiltinTypes.INT_TYPE), // LOAD_CONSTANT
                new FrameData(7).stack(BuiltinTypes.STRING_TYPE).locals(BuiltinTypes.INT_TYPE), // STORE
                new FrameData(8).stack().locals(OBJECT_TYPE), // NOP
                new FrameData(9).stack().locals(OBJECT_TYPE), // LOAD_CONSTANT
                new FrameData(10).stack(BuiltinTypes.INT_TYPE).locals(OBJECT_TYPE) // RETURN
        );
    }

    @Test
    public void testStackMetadataWithExceptionDeadCode() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("self", "resource")
                .loadParameter("self")
                .getAttribute("fullname")
                .loadMethod("replace")
                .loadConstant(".")
                .loadConstant("/")
                .callMethod(2)
                .storeVariable("fullname_as_path")
                .loadVariable("fullname_as_path")
                .op(StringOpDescriptor.FORMAT_VALUE, 0)
                .loadConstant("/")
                .loadParameter("resource")
                .op(StringOpDescriptor.FORMAT_VALUE, 0)
                .op(StringOpDescriptor.BUILD_STRING, 3)
                .storeVariable("path")
                .loadConstant(0)
                .loadConstant(List.of(PythonString.valueOf("BytesIO")))
                .op(ModuleOpDescriptor.IMPORT_NAME, 2)
                .op(ModuleOpDescriptor.IMPORT_FROM, 3)
                .storeVariable("BytesIO")
                .op(StackOpDescriptor.POP_TOP)
                .op(ExceptionOpDescriptor.SETUP_FINALLY, 9)
                .loadVariable("BytesIO")
                .loadParameter("self")
                .getAttribute("zipimporter")
                .loadMethod("get_data")
                .loadVariable("path")
                .callMethod(1)
                .callFunction(1)
                .op(ExceptionOpDescriptor.POP_BLOCK)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .op(StackOpDescriptor.DUP_TOP)
                .loadGlobalVariable("OSError")
                .op(ControlOpDescriptor.JUMP_IF_NOT_EXC_MATCH, 42)
                .op(StackOpDescriptor.POP_TOP)
                .op(StackOpDescriptor.POP_TOP)
                .op(StackOpDescriptor.POP_TOP)
                .loadGlobalVariable("FileNotFoundError")
                .loadVariable("path")
                .callFunction(1)
                .op(ExceptionOpDescriptor.RAISE_VARARGS, 1)
                .op(ExceptionOpDescriptor.POP_EXCEPT)
                .op(ControlOpDescriptor.JUMP_FORWARD, 1)
                .op(ExceptionOpDescriptor.RERAISE, 0)
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        FunctionMetadata functionMetadata = getFunctionMetadata(pythonCompiledFunction);
        StackMetadata metadata = getInitialStackMetadata(pythonCompiledFunction)
                .setLocalVariableValueSource(0, ValueSourceInfo.of(new OpcodeWithoutSource(), BuiltinTypes.BASE_TYPE))
                .setLocalVariableValueSource(1, ValueSourceInfo.of(new OpcodeWithoutSource(), BuiltinTypes.BASE_TYPE));
        FlowGraph flowGraph = getFlowGraph(functionMetadata, metadata, pythonCompiledFunction);
        getFrameData(flowGraph);
        // simply test no exception is raised
    }

    private static class FrameData {
        int index;
        boolean isDead;
        List<PythonLikeType> stackTypes;
        List<PythonLikeType> localVariableTypes;
        List<PythonLikeType> cellTypes;

        public FrameData(int index) {
            this.index = index;
            isDead = false;
            stackTypes = new ArrayList<>();
            localVariableTypes = new ArrayList<>();
            cellTypes = new ArrayList<>();
        }

        public static FrameData from(int index, StackMetadata stackMetadata) {
            FrameData out = new FrameData(index);

            if (stackMetadata.isDeadCode()) {
                out.isDead = true;
                return out;
            }
            stackMetadata.getValueSourcesUpToStackIndex(stackMetadata.getStackSize()).forEach(valueSourceInfo -> {
                if (valueSourceInfo != null) {
                    out.stackTypes.add(valueSourceInfo.getValueType());
                } else {
                    out.stackTypes.add(null);
                }
            });
            IntStream.range(0, stackMetadata.localVariableHelper.getNumberOfLocalVariables()).forEach(i -> {
                ValueSourceInfo valueSourceInfo = stackMetadata.getLocalVariableValueSource(i);
                if (valueSourceInfo != null) {
                    out.localVariableTypes.add(valueSourceInfo.getValueType());
                } else {
                    out.localVariableTypes.add(null);
                }
            });
            IntStream.range(0, stackMetadata.localVariableHelper.getNumberOfCells()).forEach(i -> {
                ValueSourceInfo valueSourceInfo = stackMetadata.getCellVariableValueSource(i);
                if (valueSourceInfo != null) {
                    out.cellTypes.add(valueSourceInfo.getValueType());
                } else {
                    out.cellTypes.add(null);
                }
            });
            return out;
        }

        public FrameData copy() {
            FrameData out = new FrameData(index);
            out.isDead = isDead;
            out.stackTypes.addAll(stackTypes);
            out.localVariableTypes.addAll(localVariableTypes);
            out.cellTypes.addAll(cellTypes);
            return out;
        }

        public FrameData stack(PythonLikeType... valueTypes) {
            FrameData out = copy();
            out.stackTypes.addAll(Arrays.asList(valueTypes));
            return out;
        }

        public FrameData locals(PythonLikeType... valueTypes) {
            FrameData out = copy();
            out.localVariableTypes.addAll(Arrays.asList(valueTypes));
            return out;
        }

        public FrameData cells(PythonLikeType... valueTypes) {
            FrameData out = copy();
            out.cellTypes.addAll(Arrays.asList(valueTypes));
            return out;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FrameData frameData = (FrameData) o;
            return index == frameData.index
                    && isDead == frameData.isDead
                    && Objects.equals(stackTypes, frameData.stackTypes)
                    && Objects.equals(localVariableTypes, frameData.localVariableTypes)
                    && Objects.equals(cellTypes, frameData.cellTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, isDead, stackTypes, localVariableTypes, cellTypes);
        }

        @Override
        public String toString() {
            return "FrameData{" +
                    "index=" + index +
                    ", isDead=" + isDead +
                    ", stackTypes=" + stackTypes +
                    ", localVariableTypes=" + localVariableTypes +
                    ", cellTypes=" + cellTypes +
                    '}';
        }
    }
}
