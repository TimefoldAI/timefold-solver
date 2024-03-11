package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.Set;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;

public interface BavetScoringConstraintStream<Solution_> {

    void setConstraint(BavetConstraint<Solution_> constraint);

    void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet);

}
