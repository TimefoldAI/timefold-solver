package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.NeighborhoodsGroupNodeConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractBiEnumeratingStream<Solution_, A, B> extends AbstractEnumeratingStream<Solution_>
        implements BiEnumeratingStream<Solution_, A, B> {

    protected AbstractBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory) {
        super(enumeratingStreamFactory, null);
    }

    protected AbstractBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            @Nullable AbstractEnumeratingStream<Solution_> parent) {
        super(enumeratingStreamFactory, parent);
    }

    @Override
    public final BiEnumeratingStream<Solution_, A, B> filter(BiNeighborhoodsPredicate<Solution_, A, B> filter) {
        return shareAndAddChild(new FilterBiEnumeratingStream<>(enumeratingStreamFactory, this, filter));
    }

    @Override
    public <GroupKey_> AbstractUniEnumeratingStream<Solution_, GroupKey_> groupBy(
            BiNeighborhoodsMapper<Solution_, A, B, GroupKey_> key) {
        return buildUniGroupBy(NeighborhoodsGroupNodeConstructor.biOneKeyGroupBy(key));
    }

    @Override
    public <Result_> AbstractUniEnumeratingStream<Solution_, Result_> groupBy(
            BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> collector) {
        return buildUniGroupBy(NeighborhoodsGroupNodeConstructor.biZeroKeysGroupBy(collector));
    }

    private <GroupKeyA_, GroupKeyB_> AbstractBiEnumeratingStream<Solution_, GroupKeyA_, GroupKeyB_> groupBy(
            BiNeighborhoodsMapper<Solution_, A, B, GroupKeyA_> keyA,
            BiNeighborhoodsMapper<Solution_, A, B, GroupKeyB_> keyB) {
        return buildBiGroupBy(NeighborhoodsGroupNodeConstructor.biTwoKeysGroupBy(keyA, keyB));
    }

    @Override
    public <GroupKey_, Result_> AbstractBiEnumeratingStream<Solution_, GroupKey_, Result_> groupBy(
            BiNeighborhoodsMapper<Solution_, A, B, GroupKey_> key,
            BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> collector) {
        return buildBiGroupBy(NeighborhoodsGroupNodeConstructor.biOneKeyAndCollectorGroupBy(key, collector));
    }

    private <NewA> AbstractUniEnumeratingStream<Solution_, NewA> buildUniGroupBy(
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BiGroupUniEnumeratingStream<>(enumeratingStreamFactory, this, nodeConstructor));
        return enumeratingStreamFactory.share(new AftBridgeUniEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    private <NewA, NewB> AbstractBiEnumeratingStream<Solution_, NewA, NewB> buildBiGroupBy(
            NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BiGroupBiEnumeratingStream<>(enumeratingStreamFactory, this, nodeConstructor));
        return enumeratingStreamFactory.share(new AftBridgeBiEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(BiNeighborhoodsMapper<Solution_, A, B, ResultA_> mapping) {
        var stream = shareAndAddChild(new UniMapBiEnumeratingStream<>(enumeratingStreamFactory, this, mapping));
        return enumeratingStreamFactory.share(new AftBridgeUniEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_>
            map(BiNeighborhoodsMapper<Solution_, A, B, ResultA_> mappingA,
                    BiNeighborhoodsMapper<Solution_, A, B, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BiMapBiEnumeratingStream<>(enumeratingStreamFactory, this, mappingA, mappingB));
        return enumeratingStreamFactory.share(new AftBridgeBiEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public AbstractBiEnumeratingStream<Solution_, A, B> distinct() {
        if (guaranteesDistinct()) {
            return this; // Already distinct, no need to create a new stream.
        }
        return groupBy(ConstantLambdaUtils.neighborhoodsBiPickFirst(), ConstantLambdaUtils.neighborhoodsBiPickSecond());
    }

}
