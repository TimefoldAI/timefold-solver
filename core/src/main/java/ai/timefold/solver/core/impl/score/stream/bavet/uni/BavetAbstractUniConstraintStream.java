package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import static ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor.fourKeysGroupBy;
import static ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor.oneKeyGroupBy;
import static ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor.threeKeysGroupBy;
import static ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor.twoKeysGroupBy;
import static ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor.zeroKeysGroupBy;
import static ai.timefold.solver.core.impl.score.stream.common.uni.InnerUniConstraintStream.createDefaultIndictedObjectsMapping;
import static ai.timefold.solver.core.impl.score.stream.common.uni.InnerUniConstraintStream.createDefaultJustificationMapping;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetAbstractBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetJoinBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetUniConcatBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetAbstractQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.quad.BavetUniConcatQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetAbstractTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetUniConcatTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.score.stream.common.bi.BiJoinerComber;
import ai.timefold.solver.core.impl.score.stream.common.uni.InnerUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.uni.UniConstraintBuilderImpl;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

public abstract class BavetAbstractUniConstraintStream<Solution_, A> extends BavetAbstractConstraintStream<Solution_>
        implements InnerUniConstraintStream<A> {

    protected BavetAbstractUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    protected BavetAbstractUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************

    @Override
    public BavetAbstractUniConstraintStream<Solution_, A> filter(Predicate<A> predicate) {
        return shareAndAddChild(new BavetFilterUniConstraintStream<>(constraintFactory, this, predicate));
    }

    // ************************************************************************
    // Join
    // ************************************************************************

    @Override
    @SafeVarargs
    public final <B> BiConstraintStream<A, B> join(UniConstraintStream<B> otherStream,
            BiJoiner<A, B>... joiners) {
        BiJoinerComber<A, B> joinerComber = BiJoinerComber.comb(joiners);
        return join(otherStream, joinerComber);
    }

    @Override
    public final <B> BiConstraintStream<A, B> join(UniConstraintStream<B> otherStream, BiJoinerComber<A, B> joinerComber) {
        var other = (BavetAbstractUniConstraintStream<Solution_, B>) otherStream;
        var leftBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var joinStream = new BavetJoinBiConstraintStream<>(constraintFactory, leftBridge, rightBridge,
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
    public final <B> UniConstraintStream<A> ifExists(UniConstraintStream<B> otherStream, BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <B> UniConstraintStream<A> ifNotExists(UniConstraintStream<B> otherStream, BiJoiner<A, B>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <B> UniConstraintStream<A> ifExistsOrNot(boolean shouldExist, UniConstraintStream<B> otherStream,
            BiJoiner<A, B>[] joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, B>) otherStream;
        var joinerComber = BiJoinerComber.comb(joiners);
        var parentBridgeB = other.shareAndAddChild(new BavetForeBridgeUniConstraintStream<>(constraintFactory, other));
        return constraintFactory.share(
                new BavetIfExistsUniConstraintStream<>(constraintFactory, this, parentBridgeB,
                        shouldExist, joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()),
                childStreamList::add);
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<UniTuple<Result_>> nodeConstructor =
                zeroKeysGroupBy(collector, Group0Mapping1CollectorUniNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    private <NewA> UniConstraintStream<NewA> buildUniGroupBy(GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetUniGroupUniConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
            UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB) {
        GroupNodeConstructor<BiTuple<ResultA_, ResultB_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, Group0Mapping2CollectorUniNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> BiConstraintStream<NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetBiGroupUniConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<ResultA_, ResultB_, ResultC_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, collectorC, Group0Mapping3CollectorUniNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC> TriConstraintStream<NewA, NewB, NewC>
            buildTriGroupBy(GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetTriGroupUniConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<ResultA_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, collectorC, collectorD, Group0Mapping4CollectorUniNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC, NewD> QuadConstraintStream<NewA, NewB, NewC, NewD>
            buildQuadGroupBy(GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetQuadGroupUniConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(Function<A, GroupKey_> groupKeyMapping) {
        GroupNodeConstructor<UniTuple<GroupKey_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, Group1Mapping0CollectorUniNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(Function<A, GroupKey_> groupKeyMapping,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<GroupKey_, ResultB_, ResultC_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, Group1Mapping2CollectorUniNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(Function<A, GroupKey_> groupKeyMapping,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKey_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, collectorD, Group1Mapping3CollectorUniNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            Function<A, GroupKey_> groupKeyMapping,
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<BiTuple<GroupKey_, Result_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collector, Group1Mapping1CollectorUniNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorUniNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, Result_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collector, Group2Mapping1CollectorUniNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collectorC, collectorD, Group2Mapping2CollectorUniNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
            Function<A, GroupKeyC_> groupKeyCMapping) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_>> nodeConstructor =
                threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, Group3Mapping0CollectorUniNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>
            groupBy(Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
                    Function<A, GroupKeyC_> groupKeyCMapping,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>> nodeConstructor =
                threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping,
                        groupKeyCMapping, collectorD, Group3Mapping1CollectorUniNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
                    Function<A, GroupKeyC_> groupKeyCMapping, Function<A, GroupKeyD_> groupKeyDMapping) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>> nodeConstructor =
                fourKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping,
                        Group4Mapping0CollectorUniNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    @Override
    public UniConstraintStream<A> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.identity());
        }
    }

    @Override
    public UniConstraintStream<A> concat(UniConstraintStream<A> otherStream) {
        var other = (BavetAbstractUniConstraintStream<Solution_, A>) otherStream;
        var leftBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetUniConcatUniConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <B> BiConstraintStream<A, B> concat(BiConstraintStream<A, B> otherStream, Function<A, B> paddingFunction) {
        var other = (BavetAbstractBiConstraintStream<Solution_, A, B>) otherStream;
        var leftBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetUniConcatBiConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunction);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <B, C> TriConstraintStream<A, B, C> concat(TriConstraintStream<A, B, C> otherStream, Function<A, B> paddingFunctionB,
            Function<A, C> paddingFunctionC) {
        var other = (BavetAbstractTriConstraintStream<Solution_, A, B, C>) otherStream;
        var leftBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetUniConcatTriConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunctionB,
                paddingFunctionC);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <B, C, D> QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream,
            Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC, Function<A, D> paddingFunctionD) {
        var other = (BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>) otherStream;
        var leftBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, other);
        var concatStream =
                new BavetUniConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunctionB,
                        paddingFunctionC, paddingFunctionD);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(Function<A, ResultA_> mapping) {
        var stream = shareAndAddChild(new BavetUniMapUniConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(Function<A, ResultA_> mappingA,
            Function<A, ResultB_> mappingB) {
        var stream =
                shareAndAddChild(new BavetBiMapUniConstraintStream<>(constraintFactory, this, mappingA, mappingB, false));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(Function<A, ResultA_> mappingA,
            Function<A, ResultB_> mappingB, Function<A, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapUniConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC, false));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            Function<A, ResultA_> mappingA, Function<A, ResultB_> mappingB, Function<A, ResultC_> mappingC,
            Function<A, ResultD_> mappingD) {
        var stream = shareAndAddChild(new BavetQuadMapUniConstraintStream<>(constraintFactory, this, mappingA, mappingB,
                mappingC, mappingD, false));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> flattenLast(Function<A, Iterable<ResultA_>> mapping) {
        var stream = shareAndAddChild(new BavetFlattenLastUniConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Other operations
    // ************************************************************************

    @Override
    public <ResultB_> BiConstraintStream<A, ResultB_> expand(Function<A, ResultB_> mapping) {
        var stream =
                shareAndAddChild(
                        new BavetBiMapUniConstraintStream<>(constraintFactory, this,
                                ConstantLambdaUtils.identity(), mapping, true));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultB_, ResultC_> TriConstraintStream<A, ResultB_, ResultC_> expand(Function<A, ResultB_> mappingB,
            Function<A, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapUniConstraintStream<>(constraintFactory, this, ConstantLambdaUtils.identity(),
                        mappingB, mappingC,
                        true));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultB_, ResultC_, ResultD_> QuadConstraintStream<A, ResultB_, ResultC_, ResultD_>
            expand(Function<A, ResultB_> mappingB, Function<A, ResultC_> mappingC, Function<A, ResultD_> mappingD) {
        var stream = shareAndAddChild(
                new BavetQuadMapUniConstraintStream<>(constraintFactory, this, ConstantLambdaUtils.identity(),
                        mappingB,
                        mappingC, mappingD, true));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            ToIntFunction<A> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringUniConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    private <Score_ extends Score<Score_>> UniConstraintBuilderImpl<A, Score_> newTerminator(
            BavetScoringConstraintStream<Solution_> stream, Score_ constraintWeight, ScoreImpactType impactType) {
        return new UniConstraintBuilderImpl<>(
                (constraintPackage, constraintName, constraintDescription, constraintWeight_, impactType_, justificationMapping,
                        indictedObjectsMapping) -> buildConstraint(constraintPackage, constraintName, constraintDescription,
                                constraintWeight_, impactType_, justificationMapping, indictedObjectsMapping, stream),
                impactType, constraintWeight);
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            ToLongFunction<A> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringUniConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            Function<A, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringUniConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    protected final BiFunction<A, Score<?>, DefaultConstraintJustification> getDefaultJustificationMapping() {
        return createDefaultJustificationMapping();
    }

    @Override
    protected final Function<A, Collection<?>> getDefaultIndictedObjectsMapping() {
        return createDefaultIndictedObjectsMapping();
    }

}
