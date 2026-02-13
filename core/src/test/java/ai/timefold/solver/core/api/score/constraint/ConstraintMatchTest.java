package ai.timefold.solver.core.api.score.constraint;

import static ai.timefold.solver.core.api.score.SimpleScore.ONE;
import static ai.timefold.solver.core.api.score.SimpleScore.ZERO;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.junit.jupiter.api.Test;

class ConstraintMatchTest {

    @Test
    void equalsAndHashCode() { // No CM should equal any other.
        ConstraintMatch<SimpleScore> constraintMatch = buildConstraintMatch("c", ZERO, "e1");
        PlannerAssert.assertObjectsAreEqual(constraintMatch, constraintMatch);
        ConstraintMatch<SimpleScore> constraintMatch2 = buildConstraintMatch("c", ZERO, "e1");
        // Cast to avoid Comparable checks.
        PlannerAssert.assertObjectsAreNotEqual(constraintMatch, (Object) constraintMatch2);
    }

    private <Score_ extends Score<Score_>> ConstraintMatch<Score_> buildConstraintMatch(String constraintName, Score_ score,
            Object... indictments) {
        return new ConstraintMatch<>(ConstraintRef.of(constraintName),
                DefaultConstraintJustification.of(score, indictments),
                Arrays.asList(indictments), score);
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                buildConstraintMatch("a", ZERO, "a"),
                buildConstraintMatch("a", ZERO, "a", "aa"),
                buildConstraintMatch("a", ZERO, "a", "ab"),
                buildConstraintMatch("a", ZERO, "a", "c"),
                buildConstraintMatch("a", ZERO, "a", "aa", "a"),
                buildConstraintMatch("a", ZERO, "a", "aa", "b"),
                buildConstraintMatch("a", ONE, "a", "aa"),
                buildConstraintMatch("b", ZERO, "a", "aa"),
                buildConstraintMatch("b", ZERO, "a", "ab"),
                buildConstraintMatch("b", ZERO, "a", "c"));
    }

}
