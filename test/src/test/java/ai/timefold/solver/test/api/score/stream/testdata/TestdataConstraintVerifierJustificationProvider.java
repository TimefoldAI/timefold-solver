package ai.timefold.solver.test.api.score.stream.testdata;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestFirstComparableJustification;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestFirstJustification;

public final class TestdataConstraintVerifierJustificationProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                justifyWithFirstJustification(constraintFactory),
                justifyWithFirstComparableJustification(constraintFactory)
        };
    }

    public Constraint justifyWithFirstJustification(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((entity, score) -> new TestFirstJustification(1))
                .asConstraint("Justify with first justification");
    }

    public Constraint justifyWithFirstComparableJustification(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((entity, score) -> new TestFirstComparableJustification(1))
                .asConstraint("Justify with first comparable justification");
    }

}
