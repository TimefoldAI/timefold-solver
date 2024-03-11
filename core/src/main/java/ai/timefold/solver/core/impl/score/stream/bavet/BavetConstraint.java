package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

public final class BavetConstraint<Solution_>
        extends AbstractConstraint<Solution_, BavetConstraint<Solution_>, BavetConstraintFactory<Solution_>> {

    private final BavetScoringConstraintStream<Solution_> scoringConstraintStream;

    public BavetConstraint(BavetConstraintFactory<Solution_> constraintFactory, ConstraintRef constraintRef,
            Function<Solution_, Score<?>> constraintWeightExtractor, ScoreImpactType scoreImpactType,
            Object justificationMapping, Object indictedObjectsMapping, boolean isConstraintWeightConfigurable,
            BavetScoringConstraintStream<Solution_> scoringConstraintStream) {
        super(constraintFactory, constraintRef, constraintWeightExtractor, scoreImpactType, isConstraintWeightConfigurable,
                justificationMapping, indictedObjectsMapping);
        this.scoringConstraintStream = scoringConstraintStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public String toString() {
        return "BavetConstraint(" + getConstraintRef() + ")";
    }

    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        scoringConstraintStream.collectActiveConstraintStreams(constraintStreamSet);
    }

}
