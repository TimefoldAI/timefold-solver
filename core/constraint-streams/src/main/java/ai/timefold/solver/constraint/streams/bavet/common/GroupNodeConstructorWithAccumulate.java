package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.List;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

final class GroupNodeConstructorWithAccumulate<Tuple_ extends AbstractTuple> implements GroupNodeConstructor<Tuple_> {

    private final NodeConstructorWithAccumulate<Tuple_> nodeConstructorFunction;

    public GroupNodeConstructorWithAccumulate(NodeConstructorWithAccumulate<Tuple_> nodeConstructorFunction) {
        this.nodeConstructorFunction = nodeConstructorFunction;
    }

    @Override
    public <Solution_, Score_ extends Score<Score_>> void build(NodeBuildHelper<Score_> buildHelper,
            BavetAbstractConstraintStream<Solution_> parentTupleSource,
            BavetAbstractConstraintStream<Solution_> aftStream, List<? extends ConstraintStream> aftStreamChildList,
            BavetAbstractConstraintStream<Solution_> bridgeStream, List<? extends ConstraintStream> bridgeStreamChildList,
            EnvironmentMode environmentMode) {
        if (!bridgeStreamChildList.isEmpty()) {
            throw new IllegalStateException("Impossible state: the stream (" + bridgeStream
                    + ") has an non-empty childStreamList (" + bridgeStreamChildList + ") but it's a groupBy bridge.");
        }
        int groupStoreIndex = buildHelper.reserveTupleStoreIndex(parentTupleSource);
        int undoStoreIndex = buildHelper.reserveTupleStoreIndex(parentTupleSource);
        TupleLifecycle<Tuple_> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(aftStreamChildList);
        int dirtyListPositionStoreIndex = buildHelper.reserveTupleStoreIndex(aftStream);
        int outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = nodeConstructorFunction.apply(groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                dirtyListPositionStoreIndex, environmentMode);
        buildHelper.addNode(node, bridgeStream);
    }
}
