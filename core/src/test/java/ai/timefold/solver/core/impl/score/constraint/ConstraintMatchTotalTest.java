package ai.timefold.solver.core.impl.score.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.junit.jupiter.api.Test;

class ConstraintMatchTotalTest {

    @Test
    void getScoreTotal() {
        TestdataEntity e1 = new TestdataEntity("e1");
        TestdataEntity e2 = new TestdataEntity("e2");
        TestdataEntity e3 = new TestdataEntity("e3");
        ConstraintMatchTotal<SimpleScore> constraintMatchTotal =
                new ConstraintMatchTotal<>(ConstraintRef.of("constraint1"), SimpleScore.ZERO);
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.ZERO);

        ConstraintMatch<SimpleScore> match1 =
                constraintMatchTotal.addConstraintMatch(List.of(e1, e2), SimpleScore.of(-1));
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.of(-1));
        ConstraintMatch<SimpleScore> match2 =
                constraintMatchTotal.addConstraintMatch(List.of(e1, e3), SimpleScore.of(-20));
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.of(-21));
        // Almost duplicate, but e2 and e1 are in reverse order, so different justification
        ConstraintMatch<SimpleScore> match3 =
                constraintMatchTotal.addConstraintMatch(List.of(e2, e1), SimpleScore.of(-300));
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.of(-321));

        constraintMatchTotal.removeConstraintMatch(match2);
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.of(-301));
        constraintMatchTotal.removeConstraintMatch(match1);
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.of(-300));
        constraintMatchTotal.removeConstraintMatch(match3);
        assertThat(constraintMatchTotal.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(
                new ConstraintMatchTotal<>(ConstraintRef.of("c"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("c"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("c"), SimpleScore.of(-7)));
        PlannerAssert.assertObjectsAreNotEqual(
                new ConstraintMatchTotal<>(ConstraintRef.of("c"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("d"), SimpleScore.ZERO));
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                new ConstraintMatchTotal<>(ConstraintRef.of("a"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("aa"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("ab"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("b"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("c"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("ca"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("cb"), SimpleScore.ZERO),
                new ConstraintMatchTotal<>(ConstraintRef.of("d"), SimpleScore.ZERO));
    }

}
