package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.List;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

public interface GroupNodeConstructor<Tuple_ extends AbstractTuple> {

    static <CollectorA_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collector, NoKeysOneCollectorGroupByBuilder<CollectorA_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(collector,
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collector, nextNodesTupleLifecycle, outputStoreSize,
                        environmentMode));
    }

    static <CollectorA_, CollectorB_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB,
                    NoKeysTwoCollectorsGroupByBuilder<CollectorA_, CollectorB_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Pair.of(collectorA, collectorB),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collectorA, collectorB, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <CollectorA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB, CollectorC_ collectorC,
                    NoKeysThreeCollectorsGroupByBuilder<CollectorA_, CollectorB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Triple.of(collectorA, collectorB, collectorC),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collectorA, collectorB, collectorC,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB, CollectorC_ collectorC, CollectorD_ collectorD,
                    NoKeysFourCollectorsGroupByBuilder<CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Quadruple.of(collectorA, collectorB, collectorC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collectorA, collectorB, collectorC, collectorD,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <KeyA_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMapping, OneKeyNoCollectorsGroupByBuilder<KeyA_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(keyMapping,
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMapping,
                        groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB,
                    OneKeyOneCollectorGroupByBuilder<KeyA_, CollectorB_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Pair.of(keyMappingA, collectorB),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, groupStoreIndex, undoStoreIndex, collectorB, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB, CollectorC_ collectorC,
                    OneKeyTwoCollectorsGroupByBuilder<KeyA_, CollectorB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Triple.of(keyMappingA, collectorB, collectorC),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, groupStoreIndex, undoStoreIndex, collectorB, collectorC,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB, CollectorC_ collectorC, CollectorD_ collectorD,
                    OneKeyThreeCollectorsGroupByBuilder<KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Quadruple.of(keyMappingA, collectorB, collectorC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, groupStoreIndex, undoStoreIndex, collectorB, collectorC, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB,
                    TwoKeysNoCollectorsGroupByBuilder<KeyA_, KeyB_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(Pair.of(keyMappingA, keyMappingB),
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize,
                        environmentMode));
    }

    static <KeyA_, KeyB_, CollectorC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, CollectorC_ collectorC,
                    TwoKeysOneCollectorGroupByBuilder<KeyA_, KeyB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Triple.of(keyMappingA, keyMappingB, collectorC),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, groupStoreIndex, undoStoreIndex, collectorC,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, CollectorC_ collectorC, CollectorD_ collectorD,
                    TwoKeysTwoCollectorsGroupByBuilder<KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Quadruple.of(keyMappingA, keyMappingB, collectorC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, groupStoreIndex, undoStoreIndex, collectorC, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            threeKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC,
                    ThreeKeysNoCollectorsGroupByBuilder<KeyA_, KeyB_, KeyC_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(Triple.of(keyMappingA, keyMappingB, keyMappingC),
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, keyMappingC, groupStoreIndex, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            threeKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC, CollectorD_ collectorD,
                    ThreeKeysOneCollectorGroupByBuilder<KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(Quadruple.of(keyMappingA, keyMappingB, keyMappingC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, keyMappingC, groupStoreIndex, undoStoreIndex, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, KeyD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            fourKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC, KeyD_ keyMappingD,
                    FourKeysNoCollectorsGroupByBuilder<KeyA_, KeyB_, KeyC_, KeyD_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(Quadruple.of(keyMappingA, keyMappingB, keyMappingC, keyMappingD),
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, keyMappingC, keyMappingD, groupStoreIndex,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    @FunctionalInterface
    interface NodeConstructorWithAccumulate<Tuple_ extends AbstractTuple> {

        AbstractNode apply(int groupStoreIndex, int undoStoreIndex, TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
                int outputStoreSize, EnvironmentMode environmentMode);

    }

    @FunctionalInterface
    interface NodeConstructorWithoutAccumulate<Tuple_ extends AbstractTuple> {

        AbstractNode apply(int groupStoreIndex, TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize,
                EnvironmentMode environmentMode);

    }

    @FunctionalInterface
    interface NoKeysOneCollectorGroupByBuilder<CollectorA_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collector,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface NoKeysTwoCollectorsGroupByBuilder<CollectorA_, CollectorB_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface NoKeysThreeCollectorsGroupByBuilder<CollectorA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                CollectorC_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface NoKeysFourCollectorsGroupByBuilder<CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                CollectorC_ collectorC,
                CollectorD_ collectorD,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface OneKeyNoCollectorsGroupByBuilder<KeyA_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface TwoKeysNoCollectorsGroupByBuilder<KeyA_, KeyB_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB, int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface ThreeKeysNoCollectorsGroupByBuilder<KeyA_, KeyB_, KeyC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface FourKeysNoCollectorsGroupByBuilder<KeyA_, KeyB_, KeyC_, KeyD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                KeyD_ keyMappingD,
                int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface OneKeyOneCollectorGroupByBuilder<KeyA_, CollectorB_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex, int undoStoreIndex,
                CollectorB_ collector,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface OneKeyTwoCollectorsGroupByBuilder<KeyA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex, int undoStoreIndex,
                CollectorB_ collectorA,
                CollectorC_ collectorB,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface OneKeyThreeCollectorsGroupByBuilder<KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex, int undoStoreIndex,
                CollectorB_ collectorA,
                CollectorC_ collectorB,
                CollectorD_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface TwoKeysOneCollectorGroupByBuilder<KeyA_, KeyB_, CollectorC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB, int groupStoreIndex, int undoStoreIndex,
                CollectorC_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface TwoKeysTwoCollectorsGroupByBuilder<KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                int groupStoreIndex, int undoStoreIndex,
                CollectorC_ collectorC,
                CollectorD_ collectorD,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface ThreeKeysOneCollectorGroupByBuilder<KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                int groupStoreIndex, int undoStoreIndex,
                CollectorD_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    <Solution_, Score_ extends Score<Score_>> void build(NodeBuildHelper<Score_> buildHelper,
            BavetAbstractConstraintStream<Solution_> parentTupleSource,
            BavetAbstractConstraintStream<Solution_> aftStream, List<? extends ConstraintStream> aftStreamChildList,
            BavetAbstractConstraintStream<Solution_> thisStream,
            List<? extends ConstraintStream> thisStreamChildList, EnvironmentMode environmentMode);

}
