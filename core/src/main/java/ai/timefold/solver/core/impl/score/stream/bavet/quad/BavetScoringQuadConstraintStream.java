package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Scorer;

final class BavetScoringQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetScoringConstraintStream<Solution_> {

    private final QuadImpactHandler<A, B, C, D> scoreImpact;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent, ToLongQuadFunction<A, B, C, D> longMatchWeigher) {
        super(constraintFactory, parent);
        this.scoreImpact = new QuadLongImpactHandler<>(longMatchWeigher);
    }

    public BavetScoringQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            QuadFunction<A, B, C, D, BigDecimal> bigDecimalMatchWeigher) {
        super(constraintFactory, parent);
        this.scoreImpact = new QuadBigDecimalImpactHandler<>(bigDecimalMatchWeigher);
    }

    @Override
    public void setConstraint(BavetConstraint<Solution_> constraint) {
        this.constraint = constraint;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        assertEmptyChildStreamList();
        var scoreImpacter = buildScoreImpacter(buildHelper.getScoreInliner().getConstraintMatchPolicy());
        var weightedScoreImpacter = buildHelper.getScoreInliner().buildWeightedScoreImpacter(constraint);
        var scorer = new Scorer<>(scoreImpacter, weightedScoreImpacter,
                buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, scorer);
    }

    private QuadScoreImpacter<A, B, C, D> buildScoreImpacter(ConstraintMatchPolicy constraintMatchPolicy) {
        return switch (constraintMatchPolicy) {
            case DISABLED -> scoreImpact::impactNaked;
            case ENABLED -> scoreImpact::impactFull;
            case ENABLED_WITHOUT_JUSTIFICATIONS -> scoreImpact::impactWithoutJustification;
        };
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    // No node sharing

    @Override
    public String toString() {
        return "Scoring(" + constraint.getConstraintRef() + ")";
    }

}
