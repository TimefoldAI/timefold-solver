package ai.timefold.solver.examples.tsp.score;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.examples.tsp.domain.Domicile;
import ai.timefold.solver.examples.tsp.domain.Visit;

public final class TspConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                distanceToPreviousStandstill(constraintFactory),
                distanceFromLastVisitToDomicile(constraintFactory)
        };
    }

    private Constraint distanceToPreviousStandstill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
                .penalizeLong(SimpleLongScore.ONE, Visit::getDistanceFromPreviousStandstill)
                .asConstraint("Distance to previous standstill");
    }

    private Constraint distanceFromLastVisitToDomicile(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
                .ifNotExists(Visit.class,
                        Joiners.equal(visit -> visit, Visit::getPreviousStandstill))
                .join(Domicile.class)
                .penalizeLong(SimpleLongScore.ONE,
                        Visit::getDistanceTo)
                .asConstraint("Distance from last visit to domicile");
    }

}
