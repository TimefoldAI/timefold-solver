package ai.timefold.solver.core.impl.move.streams.dataset.bi;

import ai.timefold.solver.core.impl.bavet.bi.Group2Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeBiDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataMapper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

@NullMarked
public abstract class AbstractBiDataStream<Solution_, A, B> extends AbstractDataStream<Solution_>
        implements BiDataStream<Solution_, A, B> {

    protected AbstractBiDataStream(DataStreamFactory<Solution_> dataStreamFactory) {
        super(dataStreamFactory, null);
    }

    protected AbstractBiDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            @Nullable AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public final BiDataStream<Solution_, A, B> filter(BiDataFilter<Solution_, A, B> filter) {
        return shareAndAddChild(new FilterBiDataStream<>(dataStreamFactory, this, filter));
    }


    protected <GroupKeyA_, GroupKeyB_> AbstractBiDataStream<Solution_, GroupKeyA_, GroupKeyB_> groupBy(BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorBiNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> AbstractBiDataStream<Solution_, NewA, NewB> buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BiGroupBiDataStream<>(dataStreamFactory, this, nodeConstructor));
        return dataStreamFactory.share(new AftBridgeBiDataStream<>(dataStreamFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_> UniDataStream<Solution_, ResultA_> map(BiDataMapper<Solution_, A, B, ResultA_> mapping) {
        var stream = shareAndAddChild(new UniMapBiDataStream<>(dataStreamFactory, this, mapping));
        return dataStreamFactory.share(new AftBridgeUniDataStream<>(dataStreamFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiDataStream<Solution_, ResultA_, ResultB_> map(BiDataMapper<Solution_, A, B, ResultA_> mappingA, BiDataMapper<Solution_, A, B, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BiMapBiDataStream<>(dataStreamFactory, this, mappingA, mappingB));
        return dataStreamFactory.share(new AftBridgeBiDataStream<>(dataStreamFactory, stream), stream::setAftBridge);
    }

    @Override
    public AbstractBiDataStream<Solution_, A, B> distinct() {
        if (guaranteesDistinct()) {
            return this; // Already distinct, no need to create a new stream.
        }
        return groupBy(ConstantLambdaUtils.biPickFirst(), ConstantLambdaUtils.biPickSecond());
    }

    public BiDataset<Solution_, A, B> createDataset() {
        var stream = shareAndAddChild(new TerminalBiDataStream<>(dataStreamFactory, this));
        return stream.getDataset();
    }

}
