package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import static ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor.fourKeysGroupBy;
import static ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor.oneKeyGroupBy;
import static ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor.threeKeysGroupBy;
import static ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor.twoKeysGroupBy;
import static ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor.zeroKeysGroupBy;
import static ai.timefold.solver.core.impl.score.stream.common.tri.InnerTriConstraintStream.createDefaultIndictedObjectsMapping;
import static ai.timefold.solver.core.impl.score.stream.common.tri.InnerTriConstraintStream.createDefaultJustificationMapping;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.quad.joiner.QuadJoinerComber;
import ai.timefold.solver.core.impl.bavet.tri.Group0Mapping1CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group0Mapping2CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group0Mapping3CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group0Mapping4CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group1Mapping0CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group1Mapping1CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group1Mapping2CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group1Mapping3CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group2Mapping0CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group2Mapping1CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group2Mapping2CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group3Mapping0CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group3Mapping1CollectorTriNode;
import ai.timefold.solver.core.impl.bavet.tri.Group4Mapping0CollectorTriNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetAbstractBiConstraintStream;
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
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetJoinQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetTriConcatQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.score.stream.common.tri.InnerTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.tri.TriConstraintBuilderImpl;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

public abstract class BavetAbstractTriConstraintStream<Solution_, A, B, C> extends BavetAbstractConstraintStream<Solution_>
        implements InnerTriConstraintStream<A, B, C> {

    protected BavetAbstractTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    protected BavetAbstractTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************

    @Override
    public @NonNull TriConstraintStream<A, B, C> filter(@NonNull TriPredicate<A, B, C> predicate) {
        return shareAndAddChild(new BavetFilterTriConstraintStream<>(constraintFactory, this, predicate));
    }

    // ************************************************************************
    // Join
    // ************************************************************************

    @Override
    @SafeVarargs
    public final <D> @NonNull QuadConstraintStream<A, B, C, D> join(@NonNull UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D> @NonNull... joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, D>) otherStream;
        var joinerComber = QuadJoinerComber.comb(joiners);
        var leftBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var joinStream = new BavetJoinQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge,
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
    public final @NonNull <D> TriConstraintStream<A, B, C> ifExists(@NonNull UniConstraintStream<D> otherStream,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final @NonNull <D> TriConstraintStream<A, B, C> ifNotExists(@NonNull UniConstraintStream<D> otherStream,
            @NonNull QuadJoiner<A, B, C, D> @NonNull... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <D> TriConstraintStream<A, B, C> ifExistsOrNot(boolean shouldExist, UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D>[] joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, D>) otherStream;
        var joinerComber = QuadJoinerComber.comb(joiners);
        var parentBridgeD = other.shareAndAddChild(new BavetForeBridgeUniConstraintStream<>(constraintFactory, other));
        return constraintFactory.share(
                new BavetIfExistsTriConstraintStream<>(constraintFactory, this, parentBridgeD,
                        shouldExist, joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()),
                childStreamList::add);
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public @NonNull <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            @NonNull TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<UniTuple<Result_>> nodeConstructor =
                zeroKeysGroupBy(collector, Group0Mapping1CollectorTriNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    private <NewA> UniConstraintStream<NewA>
            buildUniGroupBy(GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetUniGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public @NonNull <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            @NonNull TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
            @NonNull TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB) {
        GroupNodeConstructor<BiTuple<ResultA_, ResultB_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, Group0Mapping2CollectorTriNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> BiConstraintStream<NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetBiGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public @NonNull <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(@NonNull TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<ResultA_, ResultB_, ResultC_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, collectorC, Group0Mapping3CollectorTriNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC> TriConstraintStream<NewA, NewB, NewC>
            buildTriGroupBy(GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetTriGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public @NonNull <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(@NonNull TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<ResultA_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, collectorC, collectorD, Group0Mapping4CollectorTriNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC, NewD> QuadConstraintStream<NewA, NewB, NewC, NewD>
            buildQuadGroupBy(GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetQuadGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public @NonNull <GroupKey_> UniConstraintStream<GroupKey_>
            groupBy(@NonNull TriFunction<A, B, C, GroupKey_> groupKeyMapping) {
        GroupNodeConstructor<UniTuple<GroupKey_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, Group1Mapping0CollectorTriNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            @NonNull TriConstraintStream<GroupKey_, ResultB_, ResultC_>
            groupBy(@NonNull TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<GroupKey_, ResultB_, ResultC_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, Group1Mapping2CollectorTriNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(@NonNull TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKey_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, collectorD, Group1Mapping3CollectorTriNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            @NonNull TriFunction<A, B, C, GroupKey_> groupKeyMapping,
            @NonNull TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<BiTuple<GroupKey_, Result_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collector, Group1Mapping1CollectorTriNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            @NonNull TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
            @NonNull TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorTriNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_>
            groupBy(
                    @NonNull TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, Result_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collector, Group2Mapping1CollectorTriNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    @NonNull TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collectorC, collectorD, Group2Mapping2CollectorTriNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            @NonNull TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
            @NonNull TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            @NonNull TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_>> nodeConstructor =
                threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, Group3Mapping0CollectorTriNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>
            groupBy(@NonNull TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping,
                    @NonNull TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>> nodeConstructor =
                threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, collectorD,
                        Group3Mapping1CollectorTriNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public @NonNull <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(@NonNull TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping,
                    @NonNull TriFunction<A, B, C, GroupKeyD_> groupKeyDMapping) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>> nodeConstructor =
                fourKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping,
                        Group4Mapping0CollectorTriNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    @Override
    public @NonNull TriConstraintStream<A, B, C> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.triPickFirst(),
                    ConstantLambdaUtils.triPickSecond(),
                    ConstantLambdaUtils.triPickThird());
        }
    }

    @Override
    public @NonNull TriConstraintStream<A, B, C> concat(@NonNull UniConstraintStream<A> otherStream,
            @NonNull Function<A, B> paddingFunctionB,
            @NonNull Function<A, C> paddingFunctionC) {
        var other = (BavetAbstractUniConstraintStream<Solution_, A>) otherStream;
        var leftBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetUniConcatTriConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunctionB,
                paddingFunctionC);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public @NonNull TriConstraintStream<A, B, C> concat(@NonNull BiConstraintStream<A, B> otherStream,
            @NonNull BiFunction<A, B, C> paddingFunction) {
        var other = (BavetAbstractBiConstraintStream<Solution_, A, B>) otherStream;
        var leftBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetBiConcatTriConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunction);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public @NonNull TriConstraintStream<A, B, C> concat(@NonNull TriConstraintStream<A, B, C> otherStream) {
        var other = (BavetAbstractTriConstraintStream<Solution_, A, B, C>) otherStream;
        var leftBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetTriConcatTriConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public @NonNull <D> QuadConstraintStream<A, B, C, D> concat(@NonNull QuadConstraintStream<A, B, C, D> otherStream,
            @NonNull TriFunction<A, B, C, D> paddingFunction) {
        var other = (BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>) otherStream;
        var leftBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, other);
        var concatStream =
                new BavetTriConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunction);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public @NonNull <ResultA_> UniConstraintStream<ResultA_> map(@NonNull TriFunction<A, B, C, ResultA_> mapping) {
        var stream = shareAndAddChild(new BavetUniMapTriConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public @NonNull <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(
            @NonNull TriFunction<A, B, C, ResultA_> mappingA,
            @NonNull TriFunction<A, B, C, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BavetBiMapTriConstraintStream<>(constraintFactory, this, mappingA, mappingB));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public @NonNull <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            @NonNull TriFunction<A, B, C, ResultA_> mappingA, @NonNull TriFunction<A, B, C, ResultB_> mappingB,
            @NonNull TriFunction<A, B, C, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapTriConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> @NonNull QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            @NonNull TriFunction<A, B, C, ResultA_> mappingA, @NonNull TriFunction<A, B, C, ResultB_> mappingB,
            @NonNull TriFunction<A, B, C, ResultC_> mappingC, @NonNull TriFunction<A, B, C, ResultD_> mappingD) {
        var stream = shareAndAddChild(new BavetQuadMapTriConstraintStream<>(constraintFactory, this, mappingA, mappingB,
                mappingC, mappingD, false));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public @NonNull <ResultC_> TriConstraintStream<A, B, ResultC_>
            flattenLast(@NonNull Function<C, Iterable<ResultC_>> mapping) {
        var stream = shareAndAddChild(new BavetFlattenLastTriConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Other operations
    // ************************************************************************

    @Override
    public @NonNull <ResultD_> QuadConstraintStream<A, B, C, ResultD_> expand(@NonNull TriFunction<A, B, C, ResultD_> mapping) {
        var stream = shareAndAddChild(new BavetQuadMapTriConstraintStream<>(constraintFactory, this,
                ConstantLambdaUtils.triPickFirst(),
                ConstantLambdaUtils.triPickSecond(),
                ConstantLambdaUtils.triPickThird(), mapping, true));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringTriConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    private <Score_ extends Score<Score_>> TriConstraintBuilderImpl<A, B, C, Score_>
            newTerminator(BavetScoringConstraintStream<Solution_> stream, Score_ constraintWeight, ScoreImpactType impactType) {
        return new TriConstraintBuilderImpl<>(
                (constraintPackage, constraintName, constraintDescription, constraintGroup, constraintWeight_, impactType_,
                        justificationMapping, indictedObjectsMapping) -> buildConstraint(constraintPackage, constraintName,
                                constraintDescription, constraintGroup, constraintWeight_, impactType_, justificationMapping,
                                indictedObjectsMapping, stream),
                impactType, constraintWeight);
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringTriConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringTriConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    protected final QuadFunction<A, B, C, Score<?>, DefaultConstraintJustification> getDefaultJustificationMapping() {
        return createDefaultJustificationMapping();
    }

    @Override
    protected final TriFunction<A, B, C, Collection<?>> getDefaultIndictedObjectsMapping() {
        return createDefaultIndictedObjectsMapping();
    }

}
