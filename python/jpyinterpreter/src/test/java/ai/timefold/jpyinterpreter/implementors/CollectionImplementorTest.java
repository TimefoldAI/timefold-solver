package ai.timefold.jpyinterpreter.implementors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.CollectionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonSlice;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;
import ai.timefold.jpyinterpreter.util.function.TriFunction;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CollectionImplementorTest {

    @Test
    public void testGetIterator() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(CollectionOpDescriptor.GET_ITER)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Iterable javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Iterable.class);
        assertThat(javaFunction).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void testIteration() {
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

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(6L);
    }

    @Test
    public void testContains() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(CollectionOpDescriptor.CONTAINS_OP, 0)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Predicate javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Predicate.class);
        assertThat(javaFunction.test(1L)).isEqualTo(true);
        assertThat(javaFunction.test(4L)).isEqualTo(false);

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadParameter("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(CollectionOpDescriptor.CONTAINS_OP, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Predicate.class);
        assertThat(javaFunction.test(1L)).isEqualTo(false);
        assertThat(javaFunction.test(4L)).isEqualTo(true);
    }

    @Test
    public void testSet() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .set(3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));
    }

    @Test
    public void testListAppend() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .list(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .op(CollectionOpDescriptor.LIST_APPEND, 3)
                .op(CollectionOpDescriptor.LIST_APPEND, 2)
                .op(CollectionOpDescriptor.LIST_APPEND, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get())
                .isEqualTo(List.of(PythonInteger.valueOf(3), PythonInteger.valueOf(2), PythonInteger.valueOf(1)));
    }

    @Test
    public void testSetAdd() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .set(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .op(CollectionOpDescriptor.SET_ADD, 3)
                .op(CollectionOpDescriptor.SET_ADD, 2)
                .op(CollectionOpDescriptor.SET_ADD, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));
    }

    @Test
    public void testListExtend() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .list(0)
                .loadConstant(1)
                .list(1)
                .loadConstant(2)
                .loadConstant(3)
                .list(2)
                .op(CollectionOpDescriptor.LIST_EXTEND, 2)
                .op(CollectionOpDescriptor.LIST_EXTEND, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get())
                .isEqualTo(List.of(PythonInteger.valueOf(2), PythonInteger.valueOf(3), PythonInteger.valueOf(1)));
    }

    @Test
    public void testListToTuple() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .list(3)
                .op(CollectionOpDescriptor.LIST_TO_TUPLE)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        Object out = javaFunction.get();
        assertThat(out).isInstanceOf(PythonLikeTuple.class);
        assertThat(out).asList().containsExactly(1, 2, 3);
    }

    @Test
    public void testUnpackSequence() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("sequence")
                .loadParameter("sequence")
                .op(CollectionOpDescriptor.UNPACK_SEQUENCE, 3)
                .storeVariable("a")
                .storeVariable("b")
                .storeVariable("c")
                .loadVariable("a")
                .loadVariable("b")
                .loadVariable("c")
                .tuple(3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(List.of(1, 2, 3))).isEqualTo(List.of(1, 2, 3));
        assertThatCode(() -> javaFunction.apply(List.of(1, 2))).hasMessage("not enough values to unpack (expected 3, got 2)");
        assertThatCode(() -> javaFunction.apply(List.of(1, 2, 3, 4))).hasMessage("too many values to unpack (expected 3)");
    }

    @Test
    public void testUnpackSequenceWithTail() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("sequence")
                .loadParameter("sequence")
                .op(CollectionOpDescriptor.UNPACK_EX, 3)
                .storeVariable("a")
                .storeVariable("b")
                .storeVariable("c")
                .storeVariable("tail")
                .loadVariable("tail")
                .loadVariable("a")
                .op(CollectionOpDescriptor.LIST_APPEND, 1)
                .loadVariable("b")
                .op(CollectionOpDescriptor.LIST_APPEND, 1)
                .loadVariable("c")
                .op(CollectionOpDescriptor.LIST_APPEND, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Function.class);
        assertThat(javaFunction.apply(List.of(1, 2, 3))).isEqualTo(List.of(1, 2, 3));
        assertThatCode(() -> javaFunction.apply(List.of(1, 2))).hasMessage("not enough values to unpack (expected 3, got 2)");
        assertThat(javaFunction.apply(List.of(1, 2, 3, 4, 5))).isEqualTo(List.of(4, 5, 1, 2, 3));
    }

    @Test
    public void testSetUpdate() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .set(0)
                .loadConstant(1)
                .loadConstant(2)
                .set(2)
                .loadConstant(2)
                .loadConstant(3)
                .set(2)
                .op(CollectionOpDescriptor.SET_UPDATE, 2)
                .op(CollectionOpDescriptor.SET_UPDATE, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get())
                .isEqualTo(Set.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2), PythonInteger.valueOf(3)));
    }

    @Test
    public void testMapAdd() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .op(CollectionOpDescriptor.MAP_ADD, 5)
                .op(CollectionOpDescriptor.MAP_ADD, 3)
                .op(CollectionOpDescriptor.MAP_ADD, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testMapUpdate() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .dict(1)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .dict(2)
                .op(CollectionOpDescriptor.DICT_UPDATE, 2)
                .op(CollectionOpDescriptor.DICT_UPDATE, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testMapMergeNoDuplicates() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .dict(1)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .dict(2)
                .op(CollectionOpDescriptor.DICT_MERGE, 2)
                .op(CollectionOpDescriptor.DICT_MERGE, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testMapMergeDuplicates() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .dict(0)
                .loadConstant(1)
                .loadConstant(2)
                .dict(1)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .loadConstant(4)
                .dict(2)
                .op(CollectionOpDescriptor.DICT_MERGE, 2)
                .op(CollectionOpDescriptor.DICT_MERGE, 1)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThatCode(javaFunction::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testMap() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(3)
                .loadConstant(6)
                .dict(3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testConstKeyMap() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("a")
                .loadConstant(2)
                .loadConstant(4)
                .loadConstant(6)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .constDict(3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, Supplier.class);
        assertThat(javaFunction.get()).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2),
                PythonInteger.valueOf(2), PythonInteger.valueOf(4),
                PythonInteger.valueOf(3), PythonInteger.valueOf(6)));
    }

    @Test
    public void testSetItem() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("key", "value")
                .dict(0)
                .op(StackOpDescriptor.DUP_TOP)
                .loadParameter("value")
                .op(StackOpDescriptor.ROT_TWO)
                .loadParameter("key")
                .op(CollectionOpDescriptor.STORE_SUBSCR)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 2)).isEqualTo(Map.of(PythonInteger.valueOf(1), PythonInteger.valueOf(2)));

        pythonCompiledFunction = PythonFunctionBuilder.newFunction("key", "value")
                .loadConstant(1)
                .loadConstant(2)
                .list(2)
                .op(StackOpDescriptor.DUP_TOP)
                .loadParameter("value")
                .op(StackOpDescriptor.ROT_TWO)
                .loadParameter("key")
                .op(CollectionOpDescriptor.STORE_SUBSCR)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        javaFunction = PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 0)).isEqualTo(List.of(PythonInteger.valueOf(1), PythonInteger.valueOf(0)));
    }

    @Test
    public void testBuildSliceTwoArgs() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("start", "stop")
                .loadParameter("start")
                .loadParameter("stop")
                .op(CollectionOpDescriptor.BUILD_SLICE, 2)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        BiFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, BiFunction.class);
        assertThat(javaFunction.apply(1, 2))
                .isEqualTo(new PythonSlice(PythonInteger.valueOf(1), PythonInteger.valueOf(2), PythonInteger.valueOf(1)));
    }

    @Test
    public void testBuildSliceThreeArgs() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("start", "stop", "step")
                .loadParameter("start")
                .loadParameter("stop")
                .loadParameter("step")
                .op(CollectionOpDescriptor.BUILD_SLICE, 3)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        TriFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, TriFunction.class);
        assertThat(javaFunction.apply(1, 2, 3))
                .isEqualTo(new PythonSlice(PythonInteger.valueOf(1), PythonInteger.valueOf(2), PythonInteger.valueOf(3)));
    }

    @Test
    public void testSequenceSlice() {
        PythonCompiledFunction pythonCompiledFunction = PythonFunctionBuilder.newFunction("sequence", "start", "stop")
                .loadParameter("sequence")
                .loadParameter("start")
                .loadParameter("stop")
                .op(CollectionOpDescriptor.BUILD_SLICE, 2)
                .op(DunderOpDescriptor.BINARY_SUBSCR)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        TriFunction javaFunction =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(pythonCompiledFunction, TriFunction.class);
        assertThat(javaFunction.apply(new PythonLikeList(List.of(PythonInteger.valueOf(1),
                PythonInteger.valueOf(2),
                PythonInteger.valueOf(3),
                PythonInteger.valueOf(4),
                PythonInteger.valueOf(5))), 1, 2))
                .isEqualTo(new PythonLikeList(List.of(PythonInteger.valueOf(2))));

        assertThat(javaFunction.apply(PythonLikeTuple.fromItems(PythonInteger.valueOf(1),
                PythonInteger.valueOf(2),
                PythonInteger.valueOf(3),
                PythonInteger.valueOf(4),
                PythonInteger.valueOf(5)), 1, 2))
                .isEqualTo(PythonLikeTuple.fromItems(PythonInteger.valueOf(2)));
    }
}
