package ai.timefold.solver.test.api.score.stream.testdata;

import java.util.Set;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestFirstJustification;

public final class TestdataConstraintVerifierJustificationProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                justifyWithFirstJustification(constraintFactory),
                justifyWithNoJustifications(constraintFactory)
        };
    }

    public Constraint justifyWithFirstJustification(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .penalize(HardSoftScore.ONE_HARD)
                .justifyWith((entity, score) -> new TestFirstJustification(entity.getCode()))
                .indictWith(Set::of)
                .asConstraint("Justify with first justification");
    }

    public Constraint justifyWithNoJustifications(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TestdataConstraintVerifierFirstEntity.class)
                .filter(entity -> entity.getCode().equals("Should not filter"))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Justify without justifications and indictments");
    }

}
