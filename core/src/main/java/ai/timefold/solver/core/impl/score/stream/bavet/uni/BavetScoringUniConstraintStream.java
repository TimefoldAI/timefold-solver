package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Scorer;

final class BavetScoringUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A>
        implements BavetScoringConstraintStream<Solution_> {

    private final UniImpactHandler<A> scoreImpact;
    private BavetConstraint<Solution_> constraint;

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, ToIntFunction<A> intMatchWeigher) {
        super(constraintFactory, parent);
        this.scoreImpact = new UniIntImpactHandler<>(intMatchWeigher);
    }

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, ToLongFunction<A> longMatchWeigher) {
        super(constraintFactory, parent);
        this.scoreImpact = new UniLongImpactHandler<>(longMatchWeigher);
    }

    public BavetScoringUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, Function<A, BigDecimal> bigDecimalMatchWeigher) {
        super(constraintFactory, parent);
        this.scoreImpact = new UniBigDecimalImpactHandler<>(bigDecimalMatchWeigher);
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

    private UniScoreImpacter<A> buildScoreImpacter(ConstraintMatchPolicy constraintMatchPolicy) {
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
