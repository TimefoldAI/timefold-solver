package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.AbstractBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.JoinBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.NeighborhoodsGroupNodeConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.BiNeighborhoodsJoinerComber;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractUniEnumeratingStream<Solution_, A> extends AbstractEnumeratingStream<Solution_>
        implements UniEnumeratingStream<Solution_, A> {

    protected AbstractUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory) {
        super(enumeratingStreamFactory, null);
    }

    protected AbstractUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            @Nullable AbstractEnumeratingStream<Solution_> parent) {
        super(enumeratingStreamFactory, parent);
    }

    @Override
    public final UniEnumeratingStream<Solution_, A> filter(UniNeighborhoodsPredicate<Solution_, A> filter) {
        return shareAndAddChild(new FilterUniEnumeratingStream<>(enumeratingStreamFactory, this, filter));
    }

    @Override
    public <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>... joiners) {
        var other = (AbstractUniEnumeratingStream<Solution_, B>) otherStream;
        var leftBridge = new ForeBridgeUniEnumeratingStream<Solution_, A>(enumeratingStreamFactory, this);
        var rightBridge = new ForeBridgeUniEnumeratingStream<Solution_, B>(enumeratingStreamFactory, other);
        var joinerComber = BiNeighborhoodsJoinerComber.<Solution_, A, B> comb(joiners);
        var joinStream = new JoinBiEnumeratingStream<>(enumeratingStreamFactory, leftBridge, rightBridge,
                joinerComber.mergedJoiner(), joinerComber.mergedFiltering());
        return enumeratingStreamFactory.share(joinStream, joinStream_ -> {
            // Connect the bridges upstream, as it is an actual new join.
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass, BiNeighborhoodsJoiner<A, B>... joiners) {
        return join(enumeratingStreamFactory.forEachNonDiscriminating(otherClass, false), joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B>... joiners) {
        return ifExists(enumeratingStreamFactory.forEachNonDiscriminating(otherClass, false), joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass,
            BiNeighborhoodsJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, enumeratingStreamFactory.forEachNonDiscriminating(otherClass, false), joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <B> UniEnumeratingStream<Solution_, A> ifExistsOrNot(boolean shouldExist,
            UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>[] joiners) {
        var other = (AbstractUniEnumeratingStream<Solution_, B>) otherStream;
        var joinerComber = BiNeighborhoodsJoinerComber.<Solution_, A, B> comb(joiners);
        var parentBridgeB =
                other.shareAndAddChild(new ForeBridgeUniEnumeratingStream<Solution_, B>(enumeratingStreamFactory, other));
        return enumeratingStreamFactory
                .share(new IfExistsUniEnumeratingStream<>(enumeratingStreamFactory, this, parentBridgeB, shouldExist,
                        joinerComber.mergedJoiner(), joinerComber.mergedFiltering()), childStreamList::add);
    }

    @Override
    public <GroupKey_> AbstractUniEnumeratingStream<Solution_, GroupKey_> groupBy(
            UniNeighborhoodsMapper<Solution_, A, GroupKey_> key) {
        return buildUniGroupBy(NeighborhoodsGroupNodeConstructor.uniOneKeyGroupBy(key));
    }

    @Override
    public <Result_> AbstractUniEnumeratingStream<Solution_, Result_> groupBy(
            UniNeighborhoodsCollector<Solution_, A, ?, Result_> collector) {
        return buildUniGroupBy(NeighborhoodsGroupNodeConstructor.uniZeroKeysGroupBy(collector));
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> AbstractBiEnumeratingStream<Solution_, GroupKeyA_, GroupKeyB_> groupBy(
            UniNeighborhoodsMapper<Solution_, A, GroupKeyA_> keyA,
            UniNeighborhoodsMapper<Solution_, A, GroupKeyB_> keyB) {
        return buildBiGroupBy(NeighborhoodsGroupNodeConstructor.uniTwoKeysGroupBy(keyA, keyB));
    }

    @Override
    public <GroupKey_, Result_> AbstractBiEnumeratingStream<Solution_, GroupKey_, Result_> groupBy(
            UniNeighborhoodsMapper<Solution_, A, GroupKey_> key,
            UniNeighborhoodsCollector<Solution_, A, ?, Result_> collector) {
        return buildBiGroupBy(NeighborhoodsGroupNodeConstructor.uniOneKeyAndCollectorGroupBy(key, collector));
    }

    private <NewA> AbstractUniEnumeratingStream<Solution_, NewA> buildUniGroupBy(
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new UniGroupUniEnumeratingStream<>(enumeratingStreamFactory, this, nodeConstructor));
        return enumeratingStreamFactory.share(new AftBridgeUniEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    private <NewA, NewB> AbstractBiEnumeratingStream<Solution_, NewA, NewB> buildBiGroupBy(
            NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new UniGroupBiEnumeratingStream<>(enumeratingStreamFactory, this, nodeConstructor));
        return enumeratingStreamFactory.share(new AftBridgeBiEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(UniNeighborhoodsMapper<Solution_, A, ResultA_> mapping) {
        var stream = shareAndAddChild(new UniMapUniEnumeratingStream<>(enumeratingStreamFactory, this, mapping));
        return enumeratingStreamFactory.share(new AftBridgeUniEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_> map(
            UniNeighborhoodsMapper<Solution_, A, ResultA_> mappingA,
            UniNeighborhoodsMapper<Solution_, A, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BiMapUniEnumeratingStream<>(enumeratingStreamFactory, this, mappingA, mappingB));
        return enumeratingStreamFactory.share(new AftBridgeBiEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public AbstractUniEnumeratingStream<Solution_, A> distinct() {
        if (guaranteesDistinct()) {
            return this; // Already distinct, no need to create a new stream.
        }
        return groupBy(ConstantLambdaUtils.neighborhoodsUniPickFirst());
    }

    public UniLeftDataset<Solution_, A> createLeftDataset() {
        var stream = shareAndAddChild(new LeftTerminalUniEnumeratingStream<>(enumeratingStreamFactory, this));
        return stream.getDataset();
    }

    public <Other_> UniRightDataset<Solution_, Other_, A>
            createRightDataset(BiNeighborhoodsJoinerComber<Solution_, Other_, A> joinerComber) {
        var stream = shareAndAddChild(new RightTerminalUniEnumeratingStream<>(enumeratingStreamFactory, this, joinerComber));
        return stream.getDataset();
    }

}
