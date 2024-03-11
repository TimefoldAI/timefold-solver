package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class GroupNodeConstructorWithoutAccumulate<Tuple_ extends AbstractTuple> implements GroupNodeConstructor<Tuple_> {

    private final Object equalityKey;
    private final NodeConstructorWithoutAccumulate<Tuple_> nodeConstructorFunction;

    public GroupNodeConstructorWithoutAccumulate(Object equalityKey,
            NodeConstructorWithoutAccumulate<Tuple_> nodeConstructorFunction) {
        this.equalityKey = equalityKey;
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
        TupleLifecycle<Tuple_> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(aftStreamChildList);
        int outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = nodeConstructorFunction.apply(groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode);
        buildHelper.addNode(node, bridgeStream);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        GroupNodeConstructorWithoutAccumulate<?> that = (GroupNodeConstructorWithoutAccumulate<?>) object;
        return Objects.equals(equalityKey, that.equalityKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equalityKey);
    }
}
