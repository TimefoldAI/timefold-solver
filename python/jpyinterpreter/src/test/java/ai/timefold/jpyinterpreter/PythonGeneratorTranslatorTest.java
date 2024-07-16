package ai.timefold.jpyinterpreter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.jpyinterpreter.opcodes.descriptor.CollectionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ExceptionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.GeneratorOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonGenerator;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.errors.AttributeError;
import ai.timefold.jpyinterpreter.types.errors.PythonAssertionError;
import ai.timefold.jpyinterpreter.types.errors.StopIteration;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;
import ai.timefold.jpyinterpreter.util.function.TriFunction;

import org.junit.jupiter.api.Test;

public class PythonGeneratorTranslatorTest {

    @Test
    public void testSimpleGenerator() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("value")
                .op(GeneratorOpDescriptor.GEN_START)
                .loadParameter("value")
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(1);

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testMultipleYieldsGenerator() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("value1", "value2", "value3")
                .op(GeneratorOpDescriptor.GEN_START)
                .loadParameter("value1")
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadParameter("value2")
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadParameter("value3")
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        TriFunction generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, TriFunction.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(1, 2, 3);

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testGeneratorWithLoop() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction()
                .op(GeneratorOpDescriptor.GEN_START)
                .loadConstant(1)
                .loadConstant(2)
                .loadConstant(3)
                .tuple(3)
                .op(CollectionOpDescriptor.GET_ITER)
                .loop(builder -> {
                    builder.op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP);
                })
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Supplier.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.get();

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testGeneratorWithTryExcept() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction()
                .op(GeneratorOpDescriptor.GEN_START)
                .tryCode(tryBuilder -> {
                    tryBuilder.loadConstant(1)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP)
                            .op(ExceptionOpDescriptor.LOAD_ASSERTION_ERROR)
                            .op(ExceptionOpDescriptor.RAISE_VARARGS, 1);
                }, true).except(PythonAssertionError.ASSERTION_ERROR_TYPE, exceptBuilder -> {
                    exceptBuilder.loadConstant(2)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP);
                }, false)
                .andFinally(finallyBuilder -> {
                    finallyBuilder.loadConstant(3)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP);
                }, false)
                .tryEnd()
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Supplier.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.get();

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testSendingValues() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("value1")
                .op(GeneratorOpDescriptor.GEN_START)
                .loadParameter("value1")
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(1);

        assertThat(generator.send(PythonInteger.valueOf(0))).isEqualTo(PythonInteger.valueOf(1)); // first sent value is ignored
        assertThat(generator.send(PythonInteger.valueOf(1))).isEqualTo(PythonInteger.valueOf(1));
        assertThat(generator.send(PythonInteger.valueOf(2))).isEqualTo(PythonInteger.valueOf(2));
        assertThatCode(() -> generator.send(PythonInteger.valueOf(3))).isInstanceOf(StopIteration.class)
                .matches(error -> ((StopIteration) error).getValue().equals(PythonInteger.valueOf(3)));
    }

    @Test
    public void testThrowingValues() {
        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction()
                .op(GeneratorOpDescriptor.GEN_START)
                .tryCode(tryBuilder -> {
                    tryBuilder
                            .loadConstant(false)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(ControlOpDescriptor.RETURN_VALUE);
                }, true)
                .except(ValueError.VALUE_ERROR_TYPE, exceptBuilder -> {
                    exceptBuilder.loadConstant(true)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP)
                            .loadConstant(null)
                            .op(ControlOpDescriptor.RETURN_VALUE);
                }, true)
                .tryEnd()
                .build();

        Supplier generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Supplier.class);
        PythonGenerator generator = (PythonGenerator) generatorCreator.get();

        assertThat(generator.next()).isEqualTo(PythonBoolean.FALSE);
        assertThat(generator.throwValue(new ValueError())).isEqualTo(PythonBoolean.TRUE);
        assertThatCode(() -> generator.next()).isInstanceOf(StopIteration.class);
    }

    @Test
    public void testSimpleYieldFromGenerator() {
        PythonCompiledFunction subgeneratorFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadConstant(2)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadConstant(3)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("subgenerator")
                .op(GeneratorOpDescriptor.GEN_START)
                .loadParameter("subgenerator")
                .op(GeneratorOpDescriptor.GET_YIELD_FROM_ITER)
                .loadConstant(null)
                .op(GeneratorOpDescriptor.YIELD_FROM)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier subgeneratorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(subgeneratorFunction, Supplier.class);

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);

        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(subgeneratorCreator.get());

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(generator::next).isInstanceOf(StopIteration.class)
                .matches(stopIteration -> ((StopIteration) stopIteration).getValue().equals(PythonInteger.valueOf(3)));

        generator = (PythonGenerator) generatorCreator.apply(new PythonLikeList<>(List.of(PythonInteger.valueOf(1),
                PythonInteger.valueOf(2),
                PythonInteger.valueOf(3))));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(2));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(3));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(generator::next).isInstanceOf(StopIteration.class)
                .matches(stopIteration -> ((StopIteration) stopIteration).getValue().equals(PythonInteger.valueOf(3)));

        generator = (PythonGenerator) generatorCreator.apply(new PythonLikeList<>());
        assertThat(generator.hasNext()).isFalse();
        assertThatCode(generator::next).isInstanceOf(StopIteration.class)
                .matches(stopIteration -> ((StopIteration) stopIteration).getValue().equals(PythonNone.INSTANCE));
    }

    @Test
    public void testSendYieldFromGenerator() {
        PythonCompiledFunction subgeneratorFunction = PythonFunctionBuilder.newFunction()
                .loadConstant(1)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .loadConstant(2)
                .op(DunderOpDescriptor.BINARY_MULTIPLY)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .loadConstant(3)
                .op(DunderOpDescriptor.BINARY_MULTIPLY)
                .op(GeneratorOpDescriptor.YIELD_VALUE)
                .op(StackOpDescriptor.POP_TOP)
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("subgenerator")
                .op(GeneratorOpDescriptor.GEN_START)
                .loadParameter("subgenerator")
                .op(GeneratorOpDescriptor.GET_YIELD_FROM_ITER)
                .loadConstant(null)
                .op(GeneratorOpDescriptor.YIELD_FROM)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier subgeneratorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(subgeneratorFunction, Supplier.class);

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);

        PythonGenerator generator = (PythonGenerator) generatorCreator.apply(subgeneratorCreator.get());

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        assertThat(generator.send(PythonInteger.valueOf(10))).isEqualTo(PythonInteger.valueOf(20));
        assertThat(generator.send(PythonInteger.valueOf(100))).isEqualTo(PythonInteger.valueOf(300));

        assertThat(generator.hasNext()).isFalse();
        assertThatCode(generator::next).isInstanceOf(StopIteration.class)
                .matches(stopIteration -> ((StopIteration) stopIteration).getValue().equals(PythonInteger.valueOf(300)));

        generator = (PythonGenerator) generatorCreator.apply(new PythonLikeList<>(List.of(PythonInteger.valueOf(1),
                PythonInteger.valueOf(2),
                PythonInteger.valueOf(3))));

        assertThat(generator.hasNext()).isTrue();
        assertThat(generator.next()).isEqualTo(PythonInteger.valueOf(1));

        PythonGenerator finalGenerator = generator;
        assertThatCode(() -> finalGenerator.send(PythonInteger.valueOf(1))).isInstanceOf(AttributeError.class);
    }

    @Test
    public void testThrowYieldFromGenerator() {
        PythonCompiledFunction subgeneratorFunction = PythonFunctionBuilder.newFunction()
                .tryCode(tryBuilder -> {
                    tryBuilder
                            .loadConstant(1)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP)
                            .loadConstant(null)
                            .op(ControlOpDescriptor.RETURN_VALUE);
                }, true)
                .except(PythonAssertionError.ASSERTION_ERROR_TYPE, exceptBuilder -> {
                    exceptBuilder.loadConstant(2)
                            .op(GeneratorOpDescriptor.YIELD_VALUE)
                            .op(StackOpDescriptor.POP_TOP)
                            .loadConstant(null)
                            .op(ControlOpDescriptor.RETURN_VALUE);
                }, true)
                .tryEnd()
                .build();

        PythonCompiledFunction generatorFunction = PythonFunctionBuilder.newFunction("subgenerator")
                .op(GeneratorOpDescriptor.GEN_START)
                .loadParameter("subgenerator")
                .op(GeneratorOpDescriptor.GET_YIELD_FROM_ITER)
                .loadConstant(null)
                .op(GeneratorOpDescriptor.YIELD_FROM)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Supplier subgeneratorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(subgeneratorFunction, Supplier.class);

        Function generatorCreator =
                PythonBytecodeToJavaBytecodeTranslator.translatePythonBytecode(generatorFunction, Function.class);

        PythonGenerator generator1 = (PythonGenerator) generatorCreator.apply(subgeneratorCreator.get());

        assertThat(generator1.hasNext()).isTrue();
        assertThat(generator1.next()).isEqualTo(PythonInteger.valueOf(1));
        assertThat(generator1.throwValue(new PythonAssertionError())).isEqualTo(PythonInteger.valueOf(2));
        assertThatCode(generator1::next).isInstanceOf(StopIteration.class)
                .matches(stopIteration -> ((StopIteration) stopIteration).getValue().equals(PythonInteger.valueOf(2)));

        PythonGenerator generator2 =
                (PythonGenerator) generatorCreator.apply(new PythonLikeList<>(List.of(PythonInteger.valueOf(1),
                        PythonInteger.valueOf(2),
                        PythonInteger.valueOf(3))));

        assertThat(generator2.hasNext()).isTrue();
        assertThat(generator2.next()).isEqualTo(PythonInteger.valueOf(1));
        assertThatCode(() -> generator2.throwValue(new PythonAssertionError())).isInstanceOf(PythonAssertionError.class);
    }
}
