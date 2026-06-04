package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.List;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.bi.Group0Mapping1CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group1Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group1Mapping1CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group2Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.Group0Mapping1CollectorUniNode;
import ai.timefold.solver.core.impl.bavet.uni.Group1Mapping0CollectorUniNode;
import ai.timefold.solver.core.impl.bavet.uni.Group1Mapping1CollectorUniNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.NeighborhoodsCollectorUtils;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface NeighborhoodsGroupNodeConstructor<Solution_, Tuple_ extends Tuple>
        permits AbstractNeighborhoodsGroupNodeConstructor {

    <Stream_ extends BavetStream> void build(AbstractNodeBuildHelper<Stream_> buildHelper,
            Stream_ parentTupleSource, Stream_ aftStream, List<Stream_> aftStreamChildList,
            Stream_ thisStream, EnvironmentMode environmentMode, SolutionView<Solution_> view);

    static <Solution_, A, GroupKey_>
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<GroupKey_>>
            uniOneKeyGroupBy(UniNeighborhoodsMapper<Solution_, A, GroupKey_> key) {
        return new NeighborhoodsGroupNodeConstructorWithoutAccumulate<>(key,
                view -> GroupNodeConstructor.oneKeyGroupBy(key.toFunction(view),
                        Group1Mapping0CollectorUniNode::new));
    }

    @SuppressWarnings("unchecked")
    static <Solution_, A, Result_>
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<Result_>>
            uniZeroKeysGroupBy(UniNeighborhoodsCollector<Solution_, A, ?, Result_> collector) {
        return new NeighborhoodsGroupNodeConstructorWithAccumulate<>(collector,
                view -> GroupNodeConstructor.zeroKeysGroupBy(
                        NeighborhoodsCollectorUtils.toConstraintCollector(
                                (UniNeighborhoodsCollector<Solution_, A, Object, Result_>) collector, view),
                        Group0Mapping1CollectorUniNode::new));
    }

    @SuppressWarnings("unchecked")
    static <Solution_, A, GroupKey_, Result_>
            NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<GroupKey_, Result_>>
            uniOneKeyAndCollectorGroupBy(UniNeighborhoodsMapper<Solution_, A, GroupKey_> key,
                    UniNeighborhoodsCollector<Solution_, A, ?, Result_> collector) {
        return new NeighborhoodsGroupNodeConstructorWithAccumulate<>(new Pair<>(key, collector),
                view -> GroupNodeConstructor.oneKeyGroupBy(key.toFunction(view),
                        NeighborhoodsCollectorUtils.toConstraintCollector(
                                (UniNeighborhoodsCollector<Solution_, A, Object, Result_>) collector, view),
                        Group1Mapping1CollectorUniNode::new));
    }

    static <Solution_, A, B, GroupKey_>
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<GroupKey_>>
            biOneKeyGroupBy(BiNeighborhoodsMapper<Solution_, A, B, GroupKey_> key) {
        return new NeighborhoodsGroupNodeConstructorWithoutAccumulate<>(key,
                view -> GroupNodeConstructor.oneKeyGroupBy(key.toBiFunction(view),
                        Group1Mapping0CollectorBiNode::new));
    }

    static <Solution_, A, B, GroupKeyA_, GroupKeyB_>
            NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<GroupKeyA_, GroupKeyB_>>
            biTwoKeysGroupBy(BiNeighborhoodsMapper<Solution_, A, B, GroupKeyA_> keyA,
                    BiNeighborhoodsMapper<Solution_, A, B, GroupKeyB_> keyB) {
        return new NeighborhoodsGroupNodeConstructorWithoutAccumulate<>(new Pair<>(keyA, keyB),
                view -> GroupNodeConstructor.twoKeysGroupBy(keyA.toBiFunction(view), keyB.toBiFunction(view),
                        Group2Mapping0CollectorBiNode::new));
    }

    @SuppressWarnings("unchecked")
    static <Solution_, A, B, Result_>
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<Result_>>
            biZeroKeysGroupBy(BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> collector) {
        return new NeighborhoodsGroupNodeConstructorWithAccumulate<>(collector,
                view -> GroupNodeConstructor.zeroKeysGroupBy(
                        NeighborhoodsCollectorUtils.toConstraintCollector(
                                (BiNeighborhoodsCollector<Solution_, A, B, Object, Result_>) collector, view),
                        Group0Mapping1CollectorBiNode::new));
    }

    @SuppressWarnings("unchecked")
    static <Solution_, A, B, GroupKey_, Result_>
            NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<GroupKey_, Result_>>
            biOneKeyAndCollectorGroupBy(BiNeighborhoodsMapper<Solution_, A, B, GroupKey_> key,
                    BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> collector) {
        return new NeighborhoodsGroupNodeConstructorWithAccumulate<>(new Pair<>(key, collector),
                view -> GroupNodeConstructor.oneKeyGroupBy(key.toBiFunction(view),
                        NeighborhoodsCollectorUtils.toConstraintCollector(
                                (BiNeighborhoodsCollector<Solution_, A, B, Object, Result_>) collector, view),
                        Group1Mapping1CollectorBiNode::new));
    }
}
