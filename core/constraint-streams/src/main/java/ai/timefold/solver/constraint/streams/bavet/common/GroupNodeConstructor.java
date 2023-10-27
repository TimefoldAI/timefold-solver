package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.List;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

public interface GroupNodeConstructor<Tuple_ extends AbstractTuple> {

    static <Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            of(NodeConstructorWithAccumulate<Tuple_> nodeConstructorWithAccumulate, Object... equalityArgs) {
        return new GroupNodeConstructorWithAccumulate<>(nodeConstructorWithAccumulate, equalityArgs);
    }

    static <Tuple_ extends AbstractTuple> GroupNodeConstructor<Tuple_>
            of(NodeConstructorWithoutAccumulate<Tuple_> nodeConstructorWithoutAccumulate, Object... equalityArgs) {
        return new GroupNodeConstructorWithoutAccumulate<>(nodeConstructorWithoutAccumulate, equalityArgs);
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

    <Solution_, Score_ extends Score<Score_>> void build(NodeBuildHelper<Score_> buildHelper,
            BavetAbstractConstraintStream<Solution_> parentTupleSource,
            BavetAbstractConstraintStream<Solution_> aftStream, List<? extends ConstraintStream> aftStreamChildList,
            BavetAbstractConstraintStream<Solution_> thisStream,
            List<? extends ConstraintStream> thisStreamChildList, EnvironmentMode environmentMode);

}
