package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import static ai.timefold.solver.core.impl.score.stream.common.quad.InnerQuadConstraintStream.createDefaultIndictedObjectsMapping;
import static ai.timefold.solver.core.impl.score.stream.common.quad.InnerQuadConstraintStream.createDefaultJustificationMapping;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.bi.BavetAbstractBiConstraintStream;
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
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetAbstractTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.score.stream.common.penta.PentaJoinerComber;
import ai.timefold.solver.core.impl.score.stream.common.quad.InnerQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.quad.QuadConstraintBuilderImpl;

public abstract class BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractConstraintStream<Solution_>
        implements InnerQuadConstraintStream<A, B, C, D> {

    protected BavetAbstractQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    protected BavetAbstractQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************

    @Override
    public BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> filter(QuadPredicate<A, B, C, D> predicate) {
        return shareAndAddChild(new BavetFilterQuadConstraintStream<>(constraintFactory, this, predicate));
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    @SafeVarargs
    @Override
    public final <E> QuadConstraintStream<A, B, C, D> ifExists(UniConstraintStream<E> otherStream,
            PentaJoiner<A, B, C, D, E>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <E> QuadConstraintStream<A, B, C, D> ifNotExists(UniConstraintStream<E> otherStream,
            PentaJoiner<A, B, C, D, E>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <E> QuadConstraintStream<A, B, C, D> ifExistsOrNot(boolean shouldExist,
            UniConstraintStream<E> otherStream, PentaJoiner<A, B, C, D, E>[] joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, E>) otherStream;
        var joinerComber = PentaJoinerComber.comb(joiners);
        var parentBridgeD = other.shareAndAddChild(new BavetForeBridgeUniConstraintStream<>(constraintFactory, other));
        return constraintFactory.share(
                new BavetIfExistsQuadConstraintStream<>(constraintFactory, this, parentBridgeD,
                        shouldExist, joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()),
                childStreamList::add);
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<UniTuple<Result_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collector, Group0Mapping1CollectorQuadNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    private <NewA> UniConstraintStream<NewA> buildUniGroupBy(GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetUniGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
            QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB) {
        GroupNodeConstructor<BiTuple<ResultA_, ResultB_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collectorA, collectorB, Group0Mapping2CollectorQuadNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> BiConstraintStream<NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetBiGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<ResultA_, ResultB_, ResultC_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collectorA, collectorB, collectorC, Group0Mapping3CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC> TriConstraintStream<NewA, NewB, NewC>
            buildTriGroupBy(GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetTriGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<ResultA_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.zeroKeysGroupBy(collectorA, collectorB, collectorC, collectorD,
                        Group0Mapping4CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC, NewD> QuadConstraintStream<NewA, NewB, NewC, NewD>
            buildQuadGroupBy(GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetQuadGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping) {
        GroupNodeConstructor<UniTuple<GroupKey_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, Group1Mapping0CollectorQuadNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<GroupKey_, ResultB_, ResultC_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, collectorB, collectorC,
                        Group1Mapping2CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKey_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, collectorD,
                        Group1Mapping3CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<BiTuple<GroupKey_, Result_>> nodeConstructor =
                GroupNodeConstructor.oneKeyGroupBy(groupKeyMapping, collector, Group1Mapping1CollectorQuadNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
            QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorQuadNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
            QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, Result_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collector,
                        Group2Mapping1CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collectorC, collectorD,
                        Group2Mapping2CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping, QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
            QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_>> nodeConstructor =
                GroupNodeConstructor.threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping,
                        Group3Mapping0CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>
            groupBy(QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>> nodeConstructor =
                GroupNodeConstructor.threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, collectorD,
                        Group3Mapping1CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping,
                    QuadFunction<A, B, C, D, GroupKeyD_> groupKeyDMapping) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>> nodeConstructor =
                GroupNodeConstructor.fourKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping,
                        Group4Mapping0CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    @Override
    public QuadConstraintStream<A, B, C, D> concat(UniConstraintStream<A> otherStream, Function<A, B> paddingFunctionB,
            Function<A, C> paddingFunctionC, Function<A, D> paddingFunctionD) {
        var other = (BavetAbstractUniConstraintStream<Solution_, A>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
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
    public QuadConstraintStream<A, B, C, D> concat(BiConstraintStream<A, B> otherStream, BiFunction<A, B, C> paddingFunctionC,
            BiFunction<A, B, D> paddingFunctionD) {
        var other = (BavetAbstractBiConstraintStream<Solution_, A, B>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetBiConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunctionC,
                paddingFunctionD);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public QuadConstraintStream<A, B, C, D> concat(TriConstraintStream<A, B, C> otherStream,
            TriFunction<A, B, C, D> paddingFunction) {
        var other = (BavetAbstractTriConstraintStream<Solution_, A, B, C>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, other);
        var concatStream =
                new BavetTriConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge, paddingFunction);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream) {
        var other = (BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetQuadConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(QuadFunction<A, B, C, D, ResultA_> mapping) {
        var stream = shareAndAddChild(new BavetUniMapQuadConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(QuadFunction<A, B, C, D, ResultA_> mappingA,
            QuadFunction<A, B, C, D, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BavetBiMapQuadConstraintStream<>(constraintFactory, this, mappingA, mappingB));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            QuadFunction<A, B, C, D, ResultA_> mappingA, QuadFunction<A, B, C, D, ResultB_> mappingB,
            QuadFunction<A, B, C, D, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapQuadConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            QuadFunction<A, B, C, D, ResultA_> mappingA, QuadFunction<A, B, C, D, ResultB_> mappingB,
            QuadFunction<A, B, C, D, ResultC_> mappingC, QuadFunction<A, B, C, D, ResultD_> mappingD) {
        var stream = shareAndAddChild(
                new BavetQuadMapQuadConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC, mappingD));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultD_> QuadConstraintStream<A, B, C, ResultD_> flattenLast(Function<D, Iterable<ResultD_>> mapping) {
        var stream = shareAndAddChild(new BavetFlattenLastQuadConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringQuadConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    private <Score_ extends Score<Score_>> QuadConstraintBuilderImpl<A, B, C, D, Score_> newTerminator(
            BavetScoringConstraintStream<Solution_> stream, Score_ constraintWeight, ScoreImpactType impactType) {
        return new QuadConstraintBuilderImpl<>(
                (constraintPackage, constraintName, constraintDescription, constraintWeight_, impactType_, justificationMapping,
                        indictedObjectsMapping) -> buildConstraint(constraintPackage, constraintName, constraintDescription,
                                constraintWeight_, impactType_, justificationMapping, indictedObjectsMapping, stream),
                impactType, constraintWeight);
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringQuadConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringQuadConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    protected final PentaFunction<A, B, C, D, Score<?>, DefaultConstraintJustification> getDefaultJustificationMapping() {
        return createDefaultJustificationMapping();
    }

    @Override
    protected final QuadFunction<A, B, C, D, Collection<?>> getDefaultIndictedObjectsMapping() {
        return createDefaultIndictedObjectsMapping();
    }

}
