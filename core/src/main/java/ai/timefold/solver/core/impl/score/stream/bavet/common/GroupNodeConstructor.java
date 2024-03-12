package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

public interface GroupNodeConstructor<Tuple_ extends AbstractTuple> {
    // Although Tuple_ is unused in GroupNodeConstructor,
    // it is used in its two implementations: GroupNodeConstructorWithAccumulate
    // and GroupNodeConstructorWithoutAccumulate. The Tuple_ here serves as a type hint
    // for the compiler, allowing it to correctly infer the types to use in the lambda
    // being passed to GroupNodeConstructorWithAccumulate's and
    // GroupNodeConstructorWithoutAccumulate's constructor.

    // Each Group...Node with at least one collector have a constructor with the following signature:
    // Group...Node(<keyMappings>, int groupStoreIndex, int undoStoreIndex, <collectors>,
    // TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize,
    // Environment environmentMode)
    //
    // The Group...Nodes with no collectors have a constructor with the following signature:
    // Group...Node(<keyMappings>, int groupStoreIndex, TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
    // int outputStoreSize, Environment environmentMode)
    //
    // TupleLifecycle<Tuple_> in the constructor is the reason why having Tuple_ in the
    // generic signature of this interface is useful.
    //
    // The interfaces in this file correspond to each of the possible signatures of the
    // Group...Node constructor. These interfaces are thus covariant with a particular
    // GroupXMappingYCollector...Node signature, allowing a method reference to be used.
    // To reduce the number of interfaces, we use Collector..._ and Key..._ generics
    // (instead of the classes UniConstraintCollector/Function, BiConstraintCollector/BiFunction, ...).
    static <CollectorA_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collector, GroupBy0Mapping1CollectorNodeBuilder<CollectorA_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(collector,
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collector, nextNodesTupleLifecycle, outputStoreSize,
                        environmentMode));
    }

    static <CollectorA_, CollectorB_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB,
                    GroupBy0Mapping2CollectorNodeBuilder<CollectorA_, CollectorB_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(new Pair<>(collectorA, collectorB),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collectorA, collectorB, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <CollectorA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB, CollectorC_ collectorC,
                    GroupBy0Mapping3CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Triple<CollectorA_, CollectorB_, CollectorC_>(collectorA, collectorB, collectorC),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collectorA, collectorB, collectorC,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB, CollectorC_ collectorC, CollectorD_ collectorD,
                    GroupBy0Mapping4CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<CollectorA_, CollectorB_, CollectorC_, CollectorD_>(collectorA, collectorB, collectorC,
                        collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        groupStoreIndex, undoStoreIndex, collectorA, collectorB, collectorC, collectorD,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <KeyA_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMapping, GroupBy1Mapping0CollectorNodeBuilder<KeyA_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(keyMapping,
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMapping,
                        groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB,
                    GroupBy1Mapping1CollectorNodeBuilder<KeyA_, CollectorB_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(new Pair<>(keyMappingA, collectorB),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, groupStoreIndex, undoStoreIndex, collectorB, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB, CollectorC_ collectorC,
                    GroupBy1Mapping2CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Triple<KeyA_, CollectorB_, CollectorC_>(keyMappingA, collectorB, collectorC),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, groupStoreIndex, undoStoreIndex, collectorB, collectorC,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB, CollectorC_ collectorC, CollectorD_ collectorD,
                    GroupBy1Mapping3CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<KeyA_, CollectorB_, CollectorC_, CollectorD_>(keyMappingA, collectorB, collectorC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, groupStoreIndex, undoStoreIndex, collectorB, collectorC, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB,
                    GroupBy2Mapping0CollectorNodeBuilder<KeyA_, KeyB_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(new Pair<>(keyMappingA, keyMappingB),
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize,
                        environmentMode));
    }

    static <KeyA_, KeyB_, CollectorC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, CollectorC_ collectorC,
                    GroupBy2Mapping1CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Triple<KeyA_, KeyB_, CollectorC_>(keyMappingA, keyMappingB, collectorC),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, groupStoreIndex, undoStoreIndex, collectorC,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, CollectorC_ collectorC, CollectorD_ collectorD,
                    GroupBy2Mapping2CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<KeyA_, KeyB_, CollectorC_, CollectorD_>(keyMappingA, keyMappingB, collectorC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, groupStoreIndex, undoStoreIndex, collectorC, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            threeKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC,
                    GroupBy3Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(
                new Triple<KeyA_, KeyB_, KeyC_>(keyMappingA, keyMappingB, keyMappingC),
                (groupStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, keyMappingC, groupStoreIndex, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            threeKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC, CollectorD_ collectorD,
                    GroupBy3Mapping1CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<KeyA_, KeyB_, KeyC_, CollectorD_>(keyMappingA, keyMappingB, keyMappingC, collectorD),
                (groupStoreIndex, undoStoreIndex, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, keyMappingC, groupStoreIndex, undoStoreIndex, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, KeyD_, Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            fourKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC, KeyD_ keyMappingD,
                    GroupBy4Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, KeyD_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(
                new Quadruple<KeyA_, KeyB_, KeyC_, KeyD_>(keyMappingA, keyMappingB, keyMappingC, keyMappingD),
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
    interface GroupBy0Mapping1CollectorNodeBuilder<CollectorA_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collector,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy0Mapping2CollectorNodeBuilder<CollectorA_, CollectorB_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy0Mapping3CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                CollectorC_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy0Mapping4CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(int groupStoreIndex, int undoStoreIndex,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                CollectorC_ collectorC,
                CollectorD_ collectorD,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping0CollectorNodeBuilder<KeyA_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy2Mapping0CollectorNodeBuilder<KeyA_, KeyB_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB, int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy3Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy4Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, KeyD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                KeyD_ keyMappingD,
                int groupStoreIndex,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping1CollectorNodeBuilder<KeyA_, CollectorB_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex, int undoStoreIndex,
                CollectorB_ collector,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping2CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex, int undoStoreIndex,
                CollectorB_ collectorA,
                CollectorC_ collectorB,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping3CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMapping,
                int groupStoreIndex, int undoStoreIndex,
                CollectorB_ collectorA,
                CollectorC_ collectorB,
                CollectorD_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy2Mapping1CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB, int groupStoreIndex, int undoStoreIndex,
                CollectorC_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy2Mapping2CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_ extends AbstractTuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                int groupStoreIndex, int undoStoreIndex,
                CollectorC_ collectorC,
                CollectorD_ collectorD,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy3Mapping1CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_ extends AbstractTuple> {
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
