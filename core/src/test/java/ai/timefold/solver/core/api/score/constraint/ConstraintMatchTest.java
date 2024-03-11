package ai.timefold.solver.core.api.score.constraint;

import static ai.timefold.solver.core.api.score.buildin.simple.SimpleScore.ONE;
import static ai.timefold.solver.core.api.score.buildin.simple.SimpleScore.ZERO;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.impl.testdata.util.PlannerAssert;

import org.junit.jupiter.api.Test;

class ConstraintMatchTest {

    @Test
    void equalsAndHashCode() { // No CM should equal any other.
        ConstraintMatch<SimpleScore> constraintMatch = buildConstraintMatch("a. b", "c", ZERO, "e1");
        PlannerAssert.assertObjectsAreEqual(constraintMatch, constraintMatch);
        ConstraintMatch<SimpleScore> constraintMatch2 = buildConstraintMatch("a. b", "c", ZERO, "e1");
        // Cast to avoid Comparable checks.
        PlannerAssert.assertObjectsAreNotEqual(constraintMatch, (Object) constraintMatch2);
    }

    private <Score_ extends Score<Score_>> ConstraintMatch<Score_> buildConstraintMatch(String constraintPackage,
            String constraintName, Score_ score, Object... indictments) {
        return new ConstraintMatch<>(ConstraintRef.of(constraintPackage, constraintName),
                DefaultConstraintJustification.of(score, indictments),
                Arrays.asList(indictments), score);
    }

    @Test
    void compareTo() {
        PlannerAssert.assertCompareToOrder(
                buildConstraintMatch("a.b", "a", ZERO, "a"),
                buildConstraintMatch("a.b", "a", ZERO, "a", "aa"),
                buildConstraintMatch("a.b", "a", ZERO, "a", "ab"),
                buildConstraintMatch("a.b", "a", ZERO, "a", "c"),
                buildConstraintMatch("a.b", "a", ZERO, "a", "aa", "a"),
                buildConstraintMatch("a.b", "a", ZERO, "a", "aa", "b"),
                buildConstraintMatch("a.b", "a", ONE, "a", "aa"),
                buildConstraintMatch("a.b", "b", ZERO, "a", "aa"),
                buildConstraintMatch("a.b", "b", ZERO, "a", "ab"),
                buildConstraintMatch("a.b", "b", ZERO, "a", "c"),
                buildConstraintMatch("a.c", "a", ZERO, "a", "aa"),
                buildConstraintMatch("a.c", "a", ZERO, "a", "ab"),
                buildConstraintMatch("a.c", "a", ZERO, "a", "c"));
    }

}
