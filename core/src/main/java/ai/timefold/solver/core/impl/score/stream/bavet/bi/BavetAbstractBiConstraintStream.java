package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import static ai.timefold.solver.core.impl.score.stream.common.bi.InnerBiConstraintStream.createDefaultIndictedObjectsMapping;
import static ai.timefold.solver.core.impl.score.stream.common.bi.InnerBiConstraintStream.createDefaultJustificationMapping;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.bavet.bi.Group0Mapping1CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group0Mapping2CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group0Mapping3CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group0Mapping4CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group1Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group1Mapping1CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group1Mapping2CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group1Mapping3CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group2Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group2Mapping1CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group2Mapping2CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group3Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group3Mapping1CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.bi.Group4Mapping0CollectorBiNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.tri.joiner.TriJoinerComber;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetAbstractQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetBiConcatQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetAbstractTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetBiConcatTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetJoinTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.score.stream.common.bi.BiConstraintBuilderImpl;
import ai.timefold.solver.core.impl.score.stream.common.bi.InnerBiConstraintStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

public abstract class BavetAbstractBiConstraintStream<Solution_, A, B> extends BavetAbstractConstraintStream<Solution_>
        implements InnerBiConstraintStream<A, B> {

    protected BavetAbstractBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    protected BavetAbstractBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************

    @Override
    public @NonNull BavetAbstractBiConstraintStream<Solution_, A, B> filter(@NonNull BiPredicate<A, B> predicate) {
        return shareAndAddChild(new BavetFilterBiConstraintStream<>(constraintFactory, this, predicate));
    }

    // ************************************************************************
    // Join
    // ************************************************************************

    @Override
    @SafeVarargs
    public final <C> @NonNull TriConstraintStream<A, B, C> join(@NonNull UniConstraintStream<C> otherStream,
            TriJoiner<A, B, C> @NonNull... joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, C>) otherStream;
        var joinerComber = TriJoinerComber.comb(joiners);
        var leftBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var joinStream = new BavetJoinTriConstraintStream<>(constraintFactory, leftBridge, rightBridge,
                joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering());
        return constraintFactory.share(joinStream, joinStream_ -> {
            // Connect the bridges upstream, as it is an actual new join.
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    @SafeVarargs
    @Override
    public final @NonNull <C> BiConstraintStream<A, B> ifExists(@NonNull UniConstraintStream<C> otherStream,
            @NonNull TriJoiner<A, B, C> @NonNull... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <C> @NonNull BiConstraintStream<A, B> ifNotExists(@NonNull UniConstraintStream<C> otherStream,
            TriJoiner<A, B, C>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <C> BiConstraintStream<A, B> ifExistsOrNot(boolean shouldExist, UniConstraintStream<C> otherStream,
            TriJoiner<A, B, C>[] joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, C>) otherStream;
        var joinerComber = TriJoinerComber.comb(joiners);
        var parentBridgeC = other.shareAndAddChild(new BavetForeBridgeUniConstraintStream<>(constraintFactory, other));
        return constraintFactory.share(
                new BavetIfExistsBiConstraintStream<>(constraintFactory, this, parentBridgeC,
                        shouldExist, joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()),
                childStreamList::add);
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public @NonNull <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            @NonNull BiConstraintCollector<A, B, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<UniTuple<Result_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collector, Group0Mapping1CollectorBiNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    private <NewA> UniConstraintStream<NewA> buildUniGroupBy(GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetUniGroupBiConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> @NonNull BiConstraintStream<ResultA_, ResultB_> groupBy(
            @NonNull BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
            @NonNull BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB) {
        GroupNodeConstructor<BiTuple<ResultA_, ResultB_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collectorA, collectorB, Group0Mapping2CollectorBiNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> BiConstraintStream<NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetBiGroupBiConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            @NonNull TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(@NonNull BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
                    @NonNull BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    @NonNull BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<ResultA_, ResultB_, ResultC_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collectorA, collectorB, collectorC, Group0Mapping3CollectorBiNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC> TriConstraintStream<NewA, NewB, NewC>
            buildTriGroupBy(GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetTriGroupBiConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(@NonNull BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
                    @NonNull BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    @NonNull BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    @NonNull BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<ResultA_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collectorA, collectorB, collectorC, collectorD,
                        Group0Mapping4CollectorBiNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC, NewD> QuadConstraintStream<NewA, NewB, NewC, NewD>
            buildQuadGroupBy(GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetQuadGroupBiConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <GroupKey_> @NonNull UniConstraintStream<GroupKey_> groupBy(@NonNull BiFunction<A, B, GroupKey_> groupKeyMapping) {
        GroupNodeConstructor<UniTuple<GroupKey_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, Group1Mapping0CollectorBiNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            @NonNull TriConstraintStream<GroupKey_, ResultB_, ResultC_>
            groupBy(@NonNull BiFunction<A, B, GroupKey_> groupKeyMapping,
                    @NonNull BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    @NonNull BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<GroupKey_, ResultB_, ResultC_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, Group1Mapping2CollectorBiNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(@NonNull BiFunction<A, B, GroupKey_> groupKeyMapping,
                    @NonNull BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    @NonNull BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    @NonNull BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKey_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, collectorD,
                        Group1Mapping3CollectorBiNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> @NonNull BiConstraintStream<GroupKey_, Result_> groupBy(
            @NonNull BiFunction<A, B, GroupKey_> groupKeyMapping,
            @NonNull BiConstraintCollector<A, B, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<BiTuple<GroupKey_, Result_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, collector, Group1Mapping1CollectorBiNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> @NonNull BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            @NonNull BiFunction<A, B, GroupKeyA_> groupKeyAMapping, @NonNull BiFunction<A, B, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorBiNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> @NonNull TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_>
            groupBy(
                    @NonNull BiFunction<A, B, GroupKeyA_> groupKeyAMapping,
                    @NonNull BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    @NonNull BiConstraintCollector<A, B, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, Result_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collector,
                        Group2Mapping1CollectorBiNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    @NonNull BiFunction<A, B, GroupKeyA_> groupKeyAMapping,
                    @NonNull BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    @NonNull BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    @NonNull BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collectorC, collectorD,
                        Group2Mapping2CollectorBiNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> @NonNull TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            @NonNull BiFunction<A, B, GroupKeyA_> groupKeyAMapping, @NonNull BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
            @NonNull BiFunction<A, B, GroupKeyC_> groupKeyCMapping) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_>> nodeConstructor =
                GroupNodeConstructor.threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping,
                        Group3Mapping0CollectorBiNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>
            groupBy(@NonNull BiFunction<A, B, GroupKeyA_> groupKeyAMapping,
                    @NonNull BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    @NonNull BiFunction<A, B, GroupKeyC_> groupKeyCMapping,
                    @NonNull BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping,
                        groupKeyCMapping, collectorD, Group3Mapping1CollectorBiNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            @NonNull QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(@NonNull BiFunction<A, B, GroupKeyA_> groupKeyAMapping,
                    @NonNull BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    @NonNull BiFunction<A, B, GroupKeyC_> groupKeyCMapping,
                    @NonNull BiFunction<A, B, GroupKeyD_> groupKeyDMapping) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>> nodeConstructor =
                GroupNodeConstructor.fourKeysGroupBy(
                        groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping,
                        Group4Mapping0CollectorBiNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    @Override
    public @NonNull BiConstraintStream<A, B> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.biPickFirst(),
                    ConstantLambdaUtils.biPickSecond());
        }
    }

    @Override
    public @NonNull BiConstraintStream<A, B> concat(@NonNull UniConstraintStream<A> otherStream,
            @NonNull Function<A, B> paddingFunction) {
        var other = (BavetAbstractUniConstraintStream<Solution_, A>) otherStream;
        var leftBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetUniConcatBiConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunction);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public @NonNull BiConstraintStream<A, B> concat(@NonNull BiConstraintStream<A, B> otherStream) {
        var other = (BavetAbstractBiConstraintStream<Solution_, A, B>) otherStream;
        var leftBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetBiConcatBiConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public @NonNull <C> TriConstraintStream<A, B, C> concat(@NonNull TriConstraintStream<A, B, C> otherStream,
            @NonNull BiFunction<A, B, C> paddingFunction) {
        var other = (BavetAbstractTriConstraintStream<Solution_, A, B, C>) otherStream;
        var leftBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetBiConcatTriConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunction);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <C, D> @NonNull QuadConstraintStream<A, B, C, D> concat(@NonNull QuadConstraintStream<A, B, C, D> otherStream,
            @NonNull BiFunction<A, B, C> paddingFunctionC, @NonNull BiFunction<A, B, D> paddingFunctionD) {
        var other = (BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>) otherStream;
        var leftBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetBiConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunctionC,
                paddingFunctionD);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <ResultA_> @NonNull UniConstraintStream<ResultA_> map(@NonNull BiFunction<A, B, ResultA_> mapping) {
        var stream = shareAndAddChild(new BavetUniMapBiConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> @NonNull BiConstraintStream<ResultA_, ResultB_> map(
            @NonNull BiFunction<A, B, ResultA_> mappingA,
            @NonNull BiFunction<A, B, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BavetBiMapBiConstraintStream<>(constraintFactory, this, mappingA, mappingB));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> @NonNull TriConstraintStream<ResultA_, ResultB_, ResultC_>
            map(@NonNull BiFunction<A, B, ResultA_> mappingA, @NonNull BiFunction<A, B, ResultB_> mappingB,
                    @NonNull BiFunction<A, B, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapBiConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC, false));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> @NonNull QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            @NonNull BiFunction<A, B, ResultA_> mappingA, @NonNull BiFunction<A, B, ResultB_> mappingB,
            @NonNull BiFunction<A, B, ResultC_> mappingC,
            @NonNull BiFunction<A, B, ResultD_> mappingD) {
        var stream = shareAndAddChild(new BavetQuadMapBiConstraintStream<>(constraintFactory, this, mappingA, mappingB,
                mappingC, mappingD, false));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultB_> @NonNull BiConstraintStream<A, ResultB_>
            flattenLast(@NonNull Function<B, @NonNull Iterable<ResultB_>> mapping) {
        var stream = shareAndAddChild(new BavetFlattenLastBiConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    // ************************************************************************
    // Other operations
    // ************************************************************************

    @Override
    public <ResultC_> @NonNull TriConstraintStream<A, B, ResultC_> expand(@NonNull BiFunction<A, B, ResultC_> mapping) {
        var stream = shareAndAddChild(
                new BavetTriMapBiConstraintStream<>(constraintFactory, this,
                        ConstantLambdaUtils.biPickFirst(),
                        ConstantLambdaUtils.biPickSecond(), mapping, true));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultC_, ResultD_> @NonNull QuadConstraintStream<A, B, ResultC_, ResultD_> expand(
            @NonNull BiFunction<A, B, ResultC_> mappingC,
            @NonNull BiFunction<A, B, ResultD_> mappingD) {
        var stream = shareAndAddChild(new BavetQuadMapBiConstraintStream<>(constraintFactory, this,
                ConstantLambdaUtils.biPickFirst(),
                ConstantLambdaUtils.biPickSecond(), mappingC, mappingD, true));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }
    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringBiConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, scoreImpactType, constraintWeight);
    }

    private <Score_ extends Score<Score_>> BiConstraintBuilderImpl<A, B, Score_> newTerminator(
            BavetScoringConstraintStream<Solution_> stream, ScoreImpactType impactType, Score_ constraintWeight) {
        return new BiConstraintBuilderImpl<>(
                (constraintPackage, constraintName, constraintDescription, constraintGroup, constraintWeight_, impactType_,
                        justificationMapping, indictedObjectsMapping) -> buildConstraint(constraintPackage, constraintName,
                                constraintDescription, constraintGroup, constraintWeight_, impactType_, justificationMapping,
                                indictedObjectsMapping, stream),
                impactType, constraintWeight);
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringBiConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, scoreImpactType, constraintWeight);
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringBiConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, scoreImpactType, constraintWeight);
    }

    @Override
    protected final TriFunction<A, B, Score<?>, DefaultConstraintJustification> getDefaultJustificationMapping() {
        return createDefaultJustificationMapping();
    }

    @Override
    protected final BiFunction<A, B, Collection<?>> getDefaultIndictedObjectsMapping() {
        return createDefaultIndictedObjectsMapping();
    }

}
