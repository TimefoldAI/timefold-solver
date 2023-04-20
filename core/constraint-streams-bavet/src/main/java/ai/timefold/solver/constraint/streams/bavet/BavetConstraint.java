package ai.timefold.solver.constraint.streams.bavet;

import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;

public final class BavetConstraint<Solution_>
        extends AbstractConstraint<Solution_, BavetConstraint<Solution_>, BavetConstraintFactory<Solution_>> {

    private final BavetScoringConstraintStream<Solution_> scoringConstraintStream;

    public BavetConstraint(BavetConstraintFactory<Solution_> constraintFactory, String constraintPackage,
            String constraintName, Function<Solution_, Score<?>> constraintWeightExtractor,
            ScoreImpactType scoreImpactType, Object justificationMapping, Object indictedObjectsMapping,
            boolean isConstraintWeightConfigurable, BavetScoringConstraintStream<Solution_> scoringConstraintStream) {
        super(constraintFactory, constraintPackage, constraintName, constraintWeightExtractor, scoreImpactType,
                isConstraintWeightConfigurable, justificationMapping, indictedObjectsMapping);
        this.scoringConstraintStream = scoringConstraintStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public String toString() {
        return "BavetConstraint(" + getConstraintId() + ")";
    }

    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        scoringConstraintStream.collectActiveConstraintStreams(constraintStreamSet);
    }

}
