package ai.timefold.solver.core.impl.bavet.common;

import java.util.List;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

final class GroupNodeConstructorWithAccumulate<Tuple_ extends AbstractTuple> extends AbstractGroupNodeConstructor<Tuple_> {

    private final NodeConstructorWithAccumulate<Tuple_> nodeConstructorFunction;

    public GroupNodeConstructorWithAccumulate(Object equalityKey,
            NodeConstructorWithAccumulate<Tuple_> nodeConstructorFunction) {
        super(equalityKey);
        this.nodeConstructorFunction = nodeConstructorFunction;
    }

    @Override
    public <Stream_ extends BavetStream> void build(AbstractNodeBuildHelper<Stream_> buildHelper, Stream_ parentTupleSource,
            Stream_ aftStream, List<Stream_> aftStreamChildList, Stream_ bridgeStream, EnvironmentMode environmentMode) {
        var groupStoreIndex = buildHelper.reserveTupleStoreIndex(parentTupleSource);
        var undoStoreIndex = buildHelper.reserveTupleStoreIndex(parentTupleSource);
        TupleLifecycle<Tuple_> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(aftStreamChildList);
        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = nodeConstructorFunction.apply(groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                environmentMode);
        buildHelper.addNode(node, bridgeStream);
    }

}
