package ai.timefold.solver.constraint.streams.bavet.tri;

import static ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor.of;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeTriConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.constraint.streams.bavet.quad.BavetJoinQuadConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.constraint.streams.common.RetrievalSemantics;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.quad.QuadJoinerComber;
import ai.timefold.solver.constraint.streams.common.tri.InnerTriConstraintStream;
import ai.timefold.solver.constraint.streams.common.tri.TriConstraintBuilderImpl;
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
    public BavetAbstractTriConstraintStream<Solution_, A, B, C> filter(TriPredicate<A, B, C> predicate) {
        return shareAndAddChild(new BavetFilterTriConstraintStream<>(constraintFactory, this, predicate));
    }

    // ************************************************************************
    // Join
    // ************************************************************************

    @Override
    @SafeVarargs
    public final <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D>... joiners) {
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
    public final <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifExists(constraintFactory.forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    @Override
    public final <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifExists(constraintFactory.forEachIncludingNullVars(otherClass), joiners);
        } else {
            return ifExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    public final <D> TriConstraintStream<A, B, C> ifExists(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifNotExists(constraintFactory.forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifNotExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    @Override
    public final <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifNotExists(constraintFactory.forEachIncludingNullVars(otherClass), joiners);
        } else {
            return ifNotExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    public final <D> TriConstraintStream<A, B, C> ifNotExists(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D>... joiners) {
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
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<UniTuple<Result_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group0Mapping1CollectorTriNode<>(groupStoreIndex, undoStoreIndex, collector,
                                tupleLifecycle, outputStoreSize, environmentMode));
        return buildUniGroupBy(nodeConstructor);
    }

    private <NewA> UniConstraintStream<NewA>
            buildUniGroupBy(GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetUniGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
            TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB) {
        GroupNodeConstructor<BiTuple<ResultA_, ResultB_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group0Mapping2CollectorTriNode<>(groupStoreIndex, undoStoreIndex, collectorA,
                                collectorB, tupleLifecycle, outputStoreSize, environmentMode));
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> BiConstraintStream<NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetBiGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<ResultA_, ResultB_, ResultC_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group0Mapping3CollectorTriNode<>(groupStoreIndex, undoStoreIndex, collectorA,
                                collectorB, collectorC, tupleLifecycle, outputStoreSize, environmentMode));
        return buildTriGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC> TriConstraintStream<NewA, NewB, NewC>
            buildTriGroupBy(GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetTriGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<ResultA_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group0Mapping4CollectorTriNode<>(groupStoreIndex, undoStoreIndex, collectorA,
                                collectorB, collectorC, collectorD, tupleLifecycle, outputStoreSize, environmentMode));
        return buildQuadGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC, NewD> QuadConstraintStream<NewA, NewB, NewC, NewD>
            buildQuadGroupBy(GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetQuadGroupTriConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(TriFunction<A, B, C, GroupKey_> groupKeyMapping) {
        GroupNodeConstructor<UniTuple<GroupKey_>> nodeConstructor =
                of((groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode) -> new Group1Mapping0CollectorTriNode<>(
                        groupKeyMapping, groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode));
        return buildUniGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<GroupKey_, ResultB_, ResultC_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group1Mapping2CollectorTriNode<>(groupKeyMapping, groupStoreIndex,
                                undoStoreIndex, collectorB, collectorC, tupleLifecycle, outputStoreSize, environmentMode));
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKey_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group1Mapping3CollectorTriNode<>(groupKeyMapping, groupStoreIndex,
                                undoStoreIndex, collectorB, collectorC, collectorD, tupleLifecycle, outputStoreSize,
                                environmentMode));
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            TriFunction<A, B, C, GroupKey_> groupKeyMapping,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<BiTuple<GroupKey_, Result_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group1Mapping1CollectorTriNode<>(groupKeyMapping, groupStoreIndex,
                                undoStoreIndex, collector, tupleLifecycle, outputStoreSize, environmentMode));
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
            TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                of((groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode) -> new Group2Mapping0CollectorTriNode<>(
                        groupKeyAMapping, groupKeyBMapping, groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode));
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
            TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, Result_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group2Mapping1CollectorTriNode<>(groupKeyAMapping, groupKeyBMapping,
                                groupStoreIndex, undoStoreIndex, collector, tupleLifecycle, outputStoreSize, environmentMode));
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group2Mapping2CollectorTriNode<>(groupKeyAMapping, groupKeyBMapping,
                                groupStoreIndex, undoStoreIndex, collectorC, collectorD, tupleLifecycle, outputStoreSize,
                                environmentMode));
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_>> nodeConstructor =
                of((groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode) -> new Group3Mapping0CollectorTriNode<>(
                        groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode));
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>
            groupBy(TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>> nodeConstructor =
                of((groupStoreIndex, undoStoreIndex, tupleLifecycle, outputStoreSize,
                        environmentMode) -> new Group3Mapping1CollectorTriNode<>(groupKeyAMapping, groupKeyBMapping,
                                groupKeyCMapping, groupStoreIndex, undoStoreIndex, collectorD, tupleLifecycle, outputStoreSize,
                                environmentMode));
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping,
                    TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping,
                    TriFunction<A, B, C, GroupKeyD_> groupKeyDMapping) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>> nodeConstructor =
                of((groupStoreIndex, tupleLifecycle, outputStoreSize, environmentMode) -> new Group4Mapping0CollectorTriNode<>(
                        groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping, groupStoreIndex, tupleLifecycle,
                        outputStoreSize, environmentMode));
        return buildQuadGroupBy(nodeConstructor);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    @Override
    public TriConstraintStream<A, B, C> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy((a, b, c) -> a, (a, b, c) -> b, (a, b, c) -> c);
        }
    }

    @Override
    public TriConstraintStream<A, B, C> concat(TriConstraintStream<A, B, C> otherStream) {
        var other = (BavetAbstractTriConstraintStream<Solution_, A, B, C>) otherStream;
        var leftBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetConcatTriConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream, as it is an actual new join.
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(TriFunction<A, B, C, ResultA_> mapping) {
        var stream = shareAndAddChild(new BavetUniMapTriConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(TriFunction<A, B, C, ResultA_> mappingA,
            TriFunction<A, B, C, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BavetBiMapTriConstraintStream<>(constraintFactory, this, mappingA, mappingB));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            TriFunction<A, B, C, ResultA_> mappingA, TriFunction<A, B, C, ResultB_> mappingB,
            TriFunction<A, B, C, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapTriConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            TriFunction<A, B, C, ResultA_> mappingA, TriFunction<A, B, C, ResultB_> mappingB,
            TriFunction<A, B, C, ResultC_> mappingC, TriFunction<A, B, C, ResultD_> mappingD) {
        var stream = shareAndAddChild(new BavetQuadMapTriConstraintStream<>(constraintFactory, this, mappingA, mappingB,
                mappingC, mappingD, false));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultC_> TriConstraintStream<A, B, ResultC_> flattenLast(Function<C, Iterable<ResultC_>> mapping) {
        var stream = shareAndAddChild(new BavetFlattenLastTriConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Other operations
    // ************************************************************************

    @Override
    public <ResultD_> QuadConstraintStream<A, B, C, ResultD_> expand(TriFunction<A, B, C, ResultD_> mapping) {
        var stream = shareAndAddChild(new BavetQuadMapTriConstraintStream<>(constraintFactory, this, (a, b, c) -> a,
                (a, b, c) -> b, (a, b, c) -> c, mapping, true));
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
                (constraintPackage, constraintName, constraintWeight_, impactType_, justificationMapping,
                        indictedObjectsMapping) -> buildConstraint(constraintPackage, constraintName, constraintWeight_,
                                impactType_, justificationMapping, indictedObjectsMapping, stream),
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
        return InnerTriConstraintStream.createDefaultJustificationMapping();
    }

    @Override
    protected final TriFunction<A, B, C, Collection<?>> getDefaultIndictedObjectsMapping() {
        return InnerTriConstraintStream.createDefaultIndictedObjectsMapping();
    }

}
