package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.Set;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraint;

public interface BavetScoringConstraintStream<Solution_> {

    void setConstraint(BavetConstraint<Solution_> constraint);

    void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet);

}
