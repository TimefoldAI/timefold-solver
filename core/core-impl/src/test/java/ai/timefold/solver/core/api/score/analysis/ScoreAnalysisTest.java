package ai.timefold.solver.core.api.score.analysis;

import static ai.timefold.solver.core.impl.score.director.InnerScoreDirector.getConstraintAnalysis;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;

import org.junit.jupiter.api.Test;

class ScoreAnalysisTest {

    @Test
    void compareWithConstraintMatchesWithoutMatchAnalysis() {
        var constraintPackage = "constraintPackage";
        var constraintName1 = "constraint1";
        var constraintName2 = "constraint2";
        var constraintName3 = "constraint3";
        var constraintId1 = ConstraintRef.of(constraintPackage, constraintName1);
        var constraintId2 = ConstraintRef.of(constraintPackage, constraintName2);
        var constraintId3 = ConstraintRef.of(constraintPackage, constraintName3);

        var constraintMatchTotal1 = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(1));
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(2), "A", "B", "C");
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(4), "A", "B");
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(8));
        var constraintMatchTotal2 = new DefaultConstraintMatchTotal<>(constraintId2, SimpleScore.of(3));
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(3), "B", "C", "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(9), "C", "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(12));
        var emptyConstraintMatchTotal1 = new DefaultConstraintMatchTotal<>(constraintId3, SimpleScore.of(0));
        var constraintAnalysisMap1 = Map.of(
                constraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal1, false),
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, false),
                emptyConstraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal1, false));
        var scoreAnalysis1 = new ScoreAnalysis<>(SimpleScore.of(50), constraintAnalysisMap1);

        var emptyConstraintMatchTotal2 = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(0));
        var constraintMatchTotal3 = new DefaultConstraintMatchTotal<>(constraintId2, SimpleScore.of(2));
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(2), "A", "B", "C");
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(4), "B", "C");
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(6), "A", "B");
        var constraintMatchTotal4 = new DefaultConstraintMatchTotal<>(constraintId3, SimpleScore.of(3));
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(3), "B", "C", "D");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(9), "C", "D");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(12));
        var constraintAnalysisMap2 = Map.of(
                emptyConstraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal2, false),
                constraintMatchTotal3.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal3, false),
                constraintMatchTotal4.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal4, false));
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(42), constraintAnalysisMap2);

        var comparison = scoreAnalysis1.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(comparison.score()).isEqualTo(SimpleScore.of(8));
            softly.assertThat(comparison.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal2.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var constraintAnalysis1 = comparison.getConstraintAnalysis(constraintPackage, constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis1.score()).isEqualTo(SimpleScore.of(20));
            softly.assertThat(constraintAnalysis1.matches()).isNull();
        });
        // Matches for constraint2 still not present.
        var constraintAnalysis2 = comparison.getConstraintAnalysis(constraintPackage, constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis2.score()).isEqualTo(SimpleScore.of(18));
            softly.assertThat(constraintAnalysis2.matches()).isNull();
        });
        // Matches for constraint3 not present.
        var constraintAnalysis3 = comparison.getConstraintAnalysis(constraintPackage, constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis3.score()).isEqualTo(SimpleScore.of(-30));
            softly.assertThat(constraintAnalysis3.matches()).isNull();
        });

        var reverseComparison = scoreAnalysis2.diff(scoreAnalysis1);
        assertSoftly(softly -> {
            softly.assertThat(reverseComparison.score()).isEqualTo(SimpleScore.of(-8));
            softly.assertThat(reverseComparison.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal3.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var reverseConstraintAnalysis1 = reverseComparison.getConstraintAnalysis(constraintPackage, constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis1.score()).isEqualTo(SimpleScore.of(-20));
            softly.assertThat(reverseConstraintAnalysis1.matches()).isNull();
        });
        // Matches for constraint2 still not present.
        var reverseConstraintAnalysis2 = reverseComparison.getConstraintAnalysis(constraintPackage, constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis2.score()).isEqualTo(SimpleScore.of(-18));
            softly.assertThat(reverseConstraintAnalysis2.matches()).isNull();
        });
        // Matches for constraint3 not present in reverse.
        var reverseConstraintAnalysis3 = reverseComparison.getConstraintAnalysis(constraintPackage, constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis3.score()).isEqualTo(SimpleScore.of(30));
            softly.assertThat(reverseConstraintAnalysis3.matches()).isNull();
        });
    }

    @Test
    void compareWithConstraintMatchesAndMatchAnalysis() {
        var constraintPackage = "constraintPackage";
        var constraintName1 = "constraint1";
        var constraintName2 = "constraint2";
        var constraintName3 = "constraint3";
        var constraintId1 = ConstraintRef.of(constraintPackage, constraintName1);
        var constraintId2 = ConstraintRef.of(constraintPackage, constraintName2);
        var constraintId3 = ConstraintRef.of(constraintPackage, constraintName3);

        var constraintMatchTotal1 = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(1));
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(2), "A", "B", "C");
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(4), "A", "B");
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal1, SimpleScore.of(8));
        var constraintMatchTotal2 = new DefaultConstraintMatchTotal<>(constraintId2, SimpleScore.of(3));
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(3), "B", "C", "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(9), "C", "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(12));
        var emptyConstraintMatchTotal1 = new DefaultConstraintMatchTotal<>(constraintId3, SimpleScore.of(0));
        var constraintAnalysisMap1 = Map.of(
                constraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal1, true),
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, true),
                emptyConstraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal1, true));
        var scoreAnalysis1 = new ScoreAnalysis<>(SimpleScore.of(50), constraintAnalysisMap1);

        var emptyConstraintMatchTotal2 = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(0));
        var constraintMatchTotal3 = new DefaultConstraintMatchTotal<>(constraintId2, SimpleScore.of(2));
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(2), "A", "B", "C");
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(4), "B", "C");
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(6), "A", "B");
        var constraintMatchTotal4 = new DefaultConstraintMatchTotal<>(constraintId3, SimpleScore.of(3));
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(3), "B", "C", "D");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(9), "C", "D");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(12));
        var constraintAnalysisMap2 = Map.of(
                emptyConstraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal2, true),
                constraintMatchTotal3.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal3, true),
                constraintMatchTotal4.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal4, true));
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(42), constraintAnalysisMap2);

        var comparison = scoreAnalysis1.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(comparison.score()).isEqualTo(SimpleScore.of(8));
            softly.assertThat(comparison.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal2.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 present.
        var constraintAnalysis1 = comparison.getConstraintAnalysis(constraintPackage, constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis1.score()).isEqualTo(SimpleScore.of(20));
            var matchAnalyses = constraintAnalysis1.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), 2, "A", "B", "C"),
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), 4, "A", "B"),
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), 6, "B", "C"),
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), 8));
        });
        // Matches for constraint2 present in both.
        var constraintAnalysis2 = comparison.getConstraintAnalysis(constraintPackage, constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis2.score()).isEqualTo(SimpleScore.of(18));
            var matchAnalyses = constraintAnalysis2.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(constraintAnalysis2.constraintRef(), 3, "B", "C", "D"),
                            matchAnalysisOf(constraintAnalysis2.constraintRef(), 2, "B", "C"),
                            matchAnalysisOf(constraintAnalysis2.constraintRef(), 9, "C", "D"),
                            matchAnalysisOf(constraintAnalysis2.constraintRef(), 12),
                            matchAnalysisOf(constraintAnalysis2.constraintRef(), -2, "A", "B", "C"),
                            matchAnalysisOf(constraintAnalysis2.constraintRef(), -6, "A", "B"));
        });
        // Matches for constraint3 not present.
        var constraintAnalysis3 = comparison.getConstraintAnalysis(constraintPackage, constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis3.score()).isEqualTo(SimpleScore.of(-30));
            var matchAnalyses = constraintAnalysis3.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(constraintAnalysis3.constraintRef(), -3, "B", "C", "D"),
                            matchAnalysisOf(constraintAnalysis3.constraintRef(), -6, "B", "C"),
                            matchAnalysisOf(constraintAnalysis3.constraintRef(), -9, "C", "D"),
                            matchAnalysisOf(constraintAnalysis3.constraintRef(), -12));
        });

        var reverseComparison = scoreAnalysis2.diff(scoreAnalysis1);
        assertSoftly(softly -> {
            softly.assertThat(reverseComparison.score()).isEqualTo(SimpleScore.of(-8));
            softly.assertThat(reverseComparison.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal3.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var reverseConstraintAnalysis1 = reverseComparison.getConstraintAnalysis(constraintPackage, constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis1.score()).isEqualTo(SimpleScore.of(-20));
            var matchAnalyses = reverseConstraintAnalysis1.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), -2, "A", "B", "C"),
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), -4, "A", "B"),
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), -6, "B", "C"),
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), -8));
        });
        // Matches for constraint2 present in both.
        var reverseConstraintAnalysis2 = reverseComparison.getConstraintAnalysis(constraintPackage, constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis2.score()).isEqualTo(SimpleScore.of(-18));
            var matchAnalyses = reverseConstraintAnalysis2.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(reverseConstraintAnalysis2.constraintRef(), -3, "B", "C", "D"),
                            matchAnalysisOf(reverseConstraintAnalysis2.constraintRef(), -2, "B", "C"),
                            matchAnalysisOf(reverseConstraintAnalysis2.constraintRef(), -9, "C", "D"),
                            matchAnalysisOf(reverseConstraintAnalysis2.constraintRef(), -12),
                            matchAnalysisOf(reverseConstraintAnalysis2.constraintRef(), 2, "A", "B", "C"),
                            matchAnalysisOf(reverseConstraintAnalysis2.constraintRef(), 6, "A", "B"));
        });
        // Matches for constraint3 present in reverse.
        var reverseConstraintAnalysis3 = reverseComparison.getConstraintAnalysis(constraintPackage, constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis3.score()).isEqualTo(SimpleScore.of(30));
            var matchAnalyses = reverseConstraintAnalysis3.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(matchAnalysisOf(reverseConstraintAnalysis3.constraintRef(), 3, "B", "C", "D"),
                            matchAnalysisOf(reverseConstraintAnalysis3.constraintRef(), 6, "B", "C"),
                            matchAnalysisOf(reverseConstraintAnalysis3.constraintRef(), 9, "C", "D"),
                            matchAnalysisOf(reverseConstraintAnalysis3.constraintRef(), 12));
        });
    }

    private void addConstraintMatch(DefaultConstraintMatchTotal<SimpleScore> constraintMatchTotal, SimpleScore impact,
            Object... indictments) {
        constraintMatchTotal.addConstraintMatch(DefaultConstraintJustification.of(impact, indictments),
                Arrays.asList(indictments), impact);
    }

    private static MatchAnalysis<SimpleScore> matchAnalysisOf(ConstraintRef constraintRef, int score, Object... facts) {
        var simpleScore = SimpleScore.of(score);
        return new MatchAnalysis<>(constraintRef, simpleScore, DefaultConstraintJustification.of(simpleScore, facts));
    }

}
