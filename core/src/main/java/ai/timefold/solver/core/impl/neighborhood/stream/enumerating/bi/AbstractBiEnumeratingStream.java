package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.bi.Group2Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.function.BiEnumeratingMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.function.BiEnumeratingPredicate;

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
    public final BiEnumeratingStream<Solution_, A, B> filter(BiEnumeratingPredicate<Solution_, A, B> filter) {
        return shareAndAddChild(new FilterBiEnumeratingStream<>(enumeratingStreamFactory, this, filter));
    }

    protected <GroupKeyA_, GroupKeyB_> AbstractBiEnumeratingStream<Solution_, GroupKeyA_, GroupKeyB_>
            groupBy(BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorBiNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> AbstractBiEnumeratingStream<Solution_, NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BiGroupBiEnumeratingStream<>(enumeratingStreamFactory, this, nodeConstructor));
        return enumeratingStreamFactory.share(new AftBridgeBiEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(BiEnumeratingMapper<Solution_, A, B, ResultA_> mapping) {
        var stream = shareAndAddChild(new UniMapBiEnumeratingStream<>(enumeratingStreamFactory, this, mapping));
        return enumeratingStreamFactory.share(new AftBridgeUniEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_>
            map(BiEnumeratingMapper<Solution_, A, B, ResultA_> mappingA,
                    BiEnumeratingMapper<Solution_, A, B, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BiMapBiEnumeratingStream<>(enumeratingStreamFactory, this, mappingA, mappingB));
        return enumeratingStreamFactory.share(new AftBridgeBiEnumeratingStream<>(enumeratingStreamFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public AbstractBiEnumeratingStream<Solution_, A, B> distinct() {
        if (guaranteesDistinct()) {
            return this; // Already distinct, no need to create a new stream.
        }
        return groupBy(ConstantLambdaUtils.biPickFirst(), ConstantLambdaUtils.biPickSecond());
    }

}
