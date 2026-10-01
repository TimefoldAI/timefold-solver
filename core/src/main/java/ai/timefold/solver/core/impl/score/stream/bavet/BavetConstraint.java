package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintMetadata;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BavetConstraint<Solution_> extends
        AbstractConstraint<Solution_, BavetConstraint<Solution_>, BavetConstraintFactory<Solution_>> {

    private final BavetScoringConstraintStream<Solution_> scoringConstraintStream;

    public BavetConstraint(BavetConstraintFactory<Solution_> constraintFactory, ConstraintMetadata constraintMetadata,
            Score<?> constraintWeight, ScoreImpactType scoreImpactType,
            Object justificationMapping, BavetScoringConstraintStream<Solution_> scoringConstraintStream) {
        super(constraintFactory, constraintMetadata, constraintWeight, scoreImpactType, justificationMapping);
        this.scoringConstraintStream = scoringConstraintStream;
    }

    public BavetScoringConstraintStream<Solution_> getScoringConstraintStream() {
        return scoringConstraintStream;
    }

    @Override
    public <JustificationMapping_> JustificationMapping_ getJustificationMapping() {
        return Objects.requireNonNull(super.getJustificationMapping());
    }

    @Override
    public String toString() {
        return "BavetConstraint(" + getConstraintRef() + ")";
    }

    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        scoringConstraintStream.collectActiveConstraintStreams(constraintStreamSet);
    }

}
