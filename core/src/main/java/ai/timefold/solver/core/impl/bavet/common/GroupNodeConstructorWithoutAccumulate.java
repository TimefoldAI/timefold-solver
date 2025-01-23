package ai.timefold.solver.core.impl.bavet.common;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

final class GroupNodeConstructorWithoutAccumulate<Tuple_ extends AbstractTuple> implements GroupNodeConstructor<Tuple_> {

    private final Object equalityKey;
    private final NodeConstructorWithoutAccumulate<Tuple_> nodeConstructorFunction;

    public GroupNodeConstructorWithoutAccumulate(Object equalityKey,
            NodeConstructorWithoutAccumulate<Tuple_> nodeConstructorFunction) {
        this.equalityKey = equalityKey;
        this.nodeConstructorFunction = nodeConstructorFunction;
    }

    @Override
    public <Stream_ extends BavetStream> void build(AbstractNodeBuildHelper<Stream_> buildHelper, Stream_ parentTupleSource,
            Stream_ aftStream, List<Stream_> aftStreamChildList, Stream_ bridgeStream, List<Stream_> bridgeStreamChildList,
            EnvironmentMode environmentMode) {
        if (!bridgeStreamChildList.isEmpty()) {
            throw new IllegalStateException("Impossible state: the stream (" + bridgeStream
                    + ") has an non-empty childStreamList (" + bridgeStreamChildList + ") but it's a groupBy bridge.");
        }
        var groupStoreIndex = buildHelper.reserveTupleStoreIndex(parentTupleSource);
        TupleLifecycle<Tuple_> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(aftStreamChildList);
        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = nodeConstructorFunction.apply(groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode);
        buildHelper.addNode(node, bridgeStream);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (GroupNodeConstructorWithoutAccumulate<?>) object;
        return Objects.equals(equalityKey, that.equalityKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equalityKey);
    }
}
