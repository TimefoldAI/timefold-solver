package ai.timefold.solver.core.impl.bavet.common;

import java.util.List;
import java.util.function.IntSupplier;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * Each Group...Node with at least one collector have a constructor with the following signature:
 * {@code
 * Group...Node(<keyMappings>, IntSupplier storeIndexReserver, <collectors>,
 * TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize,
 * Environment environmentMode)}
 * <p>
 * The Group...Nodes with no collectors have a constructor with the following signature:
 * {@code Group...Node(<keyMappings>, IntSupplier storeIndexReserver, TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
 * int outputStoreSize, Environment environmentMode)}
 * <p>
 * storeIndexReserver allows the node to reserve everything it needs itself,
 * from a shared, order-independent, monotonic per-stream counter (see AbstractNodeBuildHelper#reserveTupleStoreIndex),
 * the same way {@link AbstractJoinNode} reserves its own store indices from the tracker it receives.
 * <p>
 * The interfaces in this file correspond to each of the possible signatures of the Group...Node constructor.
 * These interfaces are thus covariant with a particular GroupXMappingYCollector...Node signature,
 * allowing a method reference to be used.
 * To reduce the number of interfaces,
 * we use Collector..._ and Key..._ generics
 * instead of the classes UniConstraintCollector/Function, BiConstraintCollector/BiFunction, ....
 *
 * @param <Tuple_> Although unused here,
 *        it is used in its two implementations:
 *        {@link GroupNodeConstructorWithAccumulate} and {@link GroupNodeConstructorWithoutAccumulate}.
 *        Serves here as a type hint for the compiler,
 *        allowing it to correctly infer the types to use in the lambda
 *        being passed to their constructors.
 */
public sealed interface GroupNodeConstructor<Tuple_ extends Tuple>
        permits AbstractGroupNodeConstructor {

    static <CollectorA_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collector, GroupBy0Mapping1CollectorNodeBuilder<CollectorA_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(collector,
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        storeIndexReserver, collector, nextNodesTupleLifecycle, outputStoreSize,
                        environmentMode));
    }

    static <CollectorA_, CollectorB_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB,
                    GroupBy0Mapping2CollectorNodeBuilder<CollectorA_, CollectorB_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(new Pair<>(collectorA, collectorB),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        storeIndexReserver, collectorA, collectorB, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <CollectorA_, CollectorB_, CollectorC_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB, CollectorC_ collectorC,
                    GroupBy0Mapping3CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Triple<>(collectorA, collectorB, collectorC),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        storeIndexReserver, collectorA, collectorB, collectorC,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            zeroKeysGroupBy(CollectorA_ collectorA, CollectorB_ collectorB, CollectorC_ collectorC, CollectorD_ collectorD,
                    GroupBy0Mapping4CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<>(collectorA, collectorB, collectorC,
                        collectorD),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        storeIndexReserver, collectorA, collectorB, collectorC, collectorD,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <KeyA_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMapping, GroupBy1Mapping0CollectorNodeBuilder<KeyA_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(keyMapping,
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMapping,
                        storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB,
                    GroupBy1Mapping1CollectorNodeBuilder<KeyA_, CollectorB_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(new Pair<>(keyMappingA, collectorB),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, storeIndexReserver, collectorB, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, CollectorC_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB, CollectorC_ collectorC,
                    GroupBy1Mapping2CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Triple<>(keyMappingA, collectorB, collectorC),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, storeIndexReserver, collectorB, collectorC,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            oneKeyGroupBy(KeyA_ keyMappingA, CollectorB_ collectorB, CollectorC_ collectorC, CollectorD_ collectorD,
                    GroupBy1Mapping3CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<>(keyMappingA, collectorB, collectorC, collectorD),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, storeIndexReserver, collectorB, collectorC, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB,
                    GroupBy2Mapping0CollectorNodeBuilder<KeyA_, KeyB_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(new Pair<>(keyMappingA, keyMappingB),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize,
                        environmentMode));
    }

    static <KeyA_, KeyB_, CollectorC_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, CollectorC_ collectorC,
                    GroupBy2Mapping1CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Triple<>(keyMappingA, keyMappingB, collectorC),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, storeIndexReserver, collectorC,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            twoKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, CollectorC_ collectorC, CollectorD_ collectorD,
                    GroupBy2Mapping2CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<>(keyMappingA, keyMappingB, collectorC, collectorD),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, storeIndexReserver, collectorC, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            threeKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC,
                    GroupBy3Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(
                new Triple<>(keyMappingA, keyMappingB, keyMappingC),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, keyMappingC, storeIndexReserver, nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            threeKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC, CollectorD_ collectorD,
                    GroupBy3Mapping1CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_> builder) {
        return new GroupNodeConstructorWithAccumulate<>(
                new Quadruple<>(keyMappingA, keyMappingB, keyMappingC, collectorD),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(
                        keyMappingA, keyMappingB, keyMappingC, storeIndexReserver, collectorD,
                        nextNodesTupleLifecycle,
                        outputStoreSize, environmentMode));
    }

    static <KeyA_, KeyB_, KeyC_, KeyD_, Tuple_ extends Tuple> GroupNodeConstructor<Tuple_>
            fourKeysGroupBy(KeyA_ keyMappingA, KeyB_ keyMappingB, KeyC_ keyMappingC, KeyD_ keyMappingD,
                    GroupBy4Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, KeyD_, Tuple_> builder) {
        return new GroupNodeConstructorWithoutAccumulate<>(
                new Quadruple<>(keyMappingA, keyMappingB, keyMappingC, keyMappingD),
                (storeIndexReserver, nextNodesTupleLifecycle, outputStoreSize, environmentMode) -> builder.build(keyMappingA,
                        keyMappingB, keyMappingC, keyMappingD, storeIndexReserver,
                        nextNodesTupleLifecycle, outputStoreSize, environmentMode));
    }

    @FunctionalInterface
    interface NodeConstructorWithAccumulate<Tuple_ extends Tuple> {

        AbstractNode apply(IntSupplier storeIndexReserver, TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
                int outputStoreSize, EnvironmentMode environmentMode);

    }

    @FunctionalInterface
    interface NodeConstructorWithoutAccumulate<Tuple_ extends Tuple> {

        AbstractNode apply(IntSupplier storeIndexReserver, TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
                int outputStoreSize, EnvironmentMode environmentMode);

    }

    @FunctionalInterface
    interface GroupBy0Mapping1CollectorNodeBuilder<CollectorA_, Tuple_ extends Tuple> {
        AbstractNode build(IntSupplier storeIndexReserver,
                CollectorA_ collector,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy0Mapping2CollectorNodeBuilder<CollectorA_, CollectorB_, Tuple_ extends Tuple> {
        AbstractNode build(IntSupplier storeIndexReserver,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy0Mapping3CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, Tuple_ extends Tuple> {
        AbstractNode build(IntSupplier storeIndexReserver,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                CollectorC_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy0Mapping4CollectorNodeBuilder<CollectorA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends Tuple> {
        AbstractNode build(IntSupplier storeIndexReserver,
                CollectorA_ collectorA,
                CollectorB_ collectorB,
                CollectorC_ collectorC,
                CollectorD_ collectorD,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping0CollectorNodeBuilder<KeyA_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMapping,
                IntSupplier storeIndexReserver,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy2Mapping0CollectorNodeBuilder<KeyA_, KeyB_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB, IntSupplier storeIndexReserver,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy3Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                IntSupplier storeIndexReserver,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy4Mapping0CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, KeyD_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                KeyD_ keyMappingD,
                IntSupplier storeIndexReserver,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping1CollectorNodeBuilder<KeyA_, CollectorB_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMapping,
                IntSupplier storeIndexReserver,
                CollectorB_ collector,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping2CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMapping,
                IntSupplier storeIndexReserver,
                CollectorB_ collectorA,
                CollectorC_ collectorB,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy1Mapping3CollectorNodeBuilder<KeyA_, CollectorB_, CollectorC_, CollectorD_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMapping,
                IntSupplier storeIndexReserver,
                CollectorB_ collectorA,
                CollectorC_ collectorB,
                CollectorD_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy2Mapping1CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB, IntSupplier storeIndexReserver,
                CollectorC_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy2Mapping2CollectorNodeBuilder<KeyA_, KeyB_, CollectorC_, CollectorD_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                IntSupplier storeIndexReserver,
                CollectorC_ collectorC,
                CollectorD_ collectorD,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    @FunctionalInterface
    interface GroupBy3Mapping1CollectorNodeBuilder<KeyA_, KeyB_, KeyC_, CollectorD_, Tuple_ extends Tuple> {
        AbstractNode build(KeyA_ keyMappingA,
                KeyB_ keyMappingB,
                KeyC_ keyMappingC,
                IntSupplier storeIndexReserver,
                CollectorD_ collectorC,
                TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int outputStoreSize, EnvironmentMode environmentMode);
    }

    <Stream_ extends BavetStream> void build(AbstractNodeBuildHelper<Stream_> buildHelper, Stream_ parentTupleSource,
            Stream_ aftStream, List<Stream_> aftStreamChildList, Stream_ thisStream, EnvironmentMode environmentMode);

}
