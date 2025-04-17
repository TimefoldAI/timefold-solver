package ai.timefold.solver.core.api.score.analysis;

import static ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy.FETCH_ALL;
import static ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy.FETCH_MATCH_COUNT;
import static ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy.FETCH_SHALLOW;
import static ai.timefold.solver.core.impl.score.director.InnerScoreDirector.getConstraintAnalysis;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;

import org.junit.jupiter.api.Test;

class ScoreAnalysisTest {

    @Test
    void empty() {
        var scoreAnalysis = new ScoreAnalysis<>(SimpleScore.of(0), Collections.emptyMap());
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(0), Collections.emptyMap());

        var diff = scoreAnalysis.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(diff.score()).isEqualTo(SimpleScore.of(0));
            softly.assertThat(diff.constraintMap()).isEmpty();
        });

        var summary = scoreAnalysis.summarize();
        assertThat(summary)
                .isEqualTo("""
                        Explanation of score (0):
                            Constraint matches:
                        """);
    }

    @Test
    void summarize() {
        var constraintPackage = "constraintPackage";
        var constraintName1 = "constraint1";
        var constraintName2 = "constraint2";
        var constraintName3 = "constraint3";
        var constraintId1 = ConstraintRef.of(constraintPackage, constraintName1);
        var constraintId2 = ConstraintRef.of(constraintPackage, constraintName2);
        var constraintId3 = ConstraintRef.of(constraintPackage, constraintName3);

        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(1));
        addConstraintMatch(constraintMatchTotal, SimpleScore.of(2), "A", "B", "C");
        addConstraintMatch(constraintMatchTotal, SimpleScore.of(4), "A", "B");
        addConstraintMatch(constraintMatchTotal, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal, SimpleScore.of(7), "C");
        addConstraintMatch(constraintMatchTotal, SimpleScore.of(8));
        var constraintMatchTotal2 = new DefaultConstraintMatchTotal<>(constraintId2, SimpleScore.of(3));
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(3), "B", "C", "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(9), "C", "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(10), "D");
        addConstraintMatch(constraintMatchTotal2, SimpleScore.of(12));
        var emptyConstraintMatchTotal1 = new DefaultConstraintMatchTotal<>(constraintId3, SimpleScore.of(0));
        var constraintAnalysisMap = Map.of(
                constraintMatchTotal.getConstraintRef(),
                getConstraintAnalysis(constraintMatchTotal, ScoreAnalysisFetchPolicy.FETCH_ALL),
                constraintMatchTotal2.getConstraintRef(),
                getConstraintAnalysis(constraintMatchTotal2, ScoreAnalysisFetchPolicy.FETCH_ALL),
                emptyConstraintMatchTotal1.getConstraintRef(),
                getConstraintAnalysis(emptyConstraintMatchTotal1, ScoreAnalysisFetchPolicy.FETCH_ALL));
        var scoreAnalysis = new ScoreAnalysis<>(SimpleScore.of(67), constraintAnalysisMap);

        // Single constraint analysis
        var constraintSummary = constraintAnalysisMap.get(constraintMatchTotal.getConstraintRef()).summarize();
        assertThat(constraintSummary)
                .isEqualTo("""
                        Explanation of score (27):
                            Constraint matches:
                                27: constraint (constraint1) has 5 matches:
                                    2: justified with ([A, B, C])
                                    4: justified with ([A, B])
                                    6: justified with ([B, C])
                                    ...
                        """);

        // Complete score analysis
        var summary = scoreAnalysis.summarize();
        assertThat(scoreAnalysis.getConstraintAnalysis(constraintName1).matchCount()).isEqualTo(5);
        assertThat(summary)
                .isEqualTo("""
                        Explanation of score (67):
                            Constraint matches:
                                0: constraint (constraint3) has no matches.
                                27: constraint (constraint1) has 5 matches:
                                    2: justified with ([A, B, C])
                                    4: justified with ([A, B])
                                    6: justified with ([B, C])
                                    ...
                                40: constraint (constraint2) has 5 matches:
                                    3: justified with ([B, C, D])
                                    6: justified with ([B, C])
                                    9: justified with ([C, D])
                                    ...
                        """);
    }

    @Test
    void summarizeUninitializedSolution() {
        var constraintPackage = "constraintPackage";
        var constraintName1 = "constraint1";
        var constraintName2 = "constraint2";
        var constraintId1 = ConstraintRef.of(constraintPackage, constraintName1);
        var constraintId2 = ConstraintRef.of(constraintPackage, constraintName2);

        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(0));
        var constraintMatchTotal2 = new DefaultConstraintMatchTotal<>(constraintId2, SimpleScore.of(0));
        var constraintAnalysisMap = Map.of(
                constraintMatchTotal.getConstraintRef(),
                getConstraintAnalysis(constraintMatchTotal, ScoreAnalysisFetchPolicy.FETCH_ALL),
                constraintMatchTotal2.getConstraintRef(),
                getConstraintAnalysis(constraintMatchTotal2, ScoreAnalysisFetchPolicy.FETCH_ALL));
        var scoreAnalysis = new ScoreAnalysis<>(SimpleScore.ZERO, constraintAnalysisMap, false);

        // Single constraint analysis
        var constraintSummary = constraintAnalysisMap.get(constraintMatchTotal.getConstraintRef()).summarize();
        assertThat(constraintSummary)
                .isEqualTo("""
                        Explanation of score (0):
                            Constraint matches:
                                0: constraint (constraint1) has no matches.
                        """);

        // Complete score analysis
        var summary = scoreAnalysis.summarize();
        assertThat(scoreAnalysis.getConstraintAnalysis(constraintName1).matchCount()).isZero();
        assertThat(summary)
                .isEqualTo("""
                        Explanation of score (0):
                            Constraint matches:
                                0: constraint (constraint1) has no matches.
                                0: constraint (constraint2) has no matches.
                        """);
    }

    @Test
    void failFastSummarize() {
        var constraintPackage = "constraintPackage";
        var constraintName1 = "constraint1";
        var constraintId1 = ConstraintRef.of(constraintPackage, constraintName1);

        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(1));
        addConstraintMatch(constraintMatchTotal, SimpleScore.of(2), "A", "B", "C");
        var constraintAnalysisMap = Map.of(
                constraintMatchTotal.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal, FETCH_SHALLOW));
        var scoreAnalysis = new ScoreAnalysis<>(SimpleScore.of(3), constraintAnalysisMap);

        assertThatThrownBy(scoreAnalysis::summarize)
                .hasMessageContaining("The constraint matches must be non-null");

        assertThat(constraintAnalysisMap.values().stream().findFirst().get().matchCount())
                .isEqualTo(-1);
    }

    @Test
    void diffWithConstraintMatchesWithoutMatchAnalysis() {
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
                constraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal1, FETCH_SHALLOW),
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, FETCH_SHALLOW),
                emptyConstraintMatchTotal1.getConstraintRef(),
                getConstraintAnalysis(emptyConstraintMatchTotal1, FETCH_SHALLOW));
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
                emptyConstraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal2, FETCH_SHALLOW),
                constraintMatchTotal3.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal3, FETCH_SHALLOW),
                constraintMatchTotal4.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal4, FETCH_SHALLOW));
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(42), constraintAnalysisMap2);

        var diff = scoreAnalysis1.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(diff.score()).isEqualTo(SimpleScore.of(8));
            softly.assertThat(diff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal2.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var constraintAnalysis1 = diff.getConstraintAnalysis(constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis1.score()).isEqualTo(SimpleScore.of(20));
            softly.assertThat(constraintAnalysis1.matches()).isNull();
        });
        // Matches for constraint2 still not present.
        var constraintAnalysis2 = diff.getConstraintAnalysis(constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis2.score()).isEqualTo(SimpleScore.of(18));
            softly.assertThat(constraintAnalysis2.matches()).isNull();
        });
        // Matches for constraint3 not present.
        var constraintAnalysis3 = diff.getConstraintAnalysis(constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis3.score()).isEqualTo(SimpleScore.of(-30));
            softly.assertThat(constraintAnalysis3.matches()).isNull();
        });

        var reverseDiff = scoreAnalysis2.diff(scoreAnalysis1);
        assertSoftly(softly -> {
            softly.assertThat(reverseDiff.score()).isEqualTo(SimpleScore.of(-8));
            softly.assertThat(reverseDiff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal3.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var reverseConstraintAnalysis1 = reverseDiff.getConstraintAnalysis(constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis1.score()).isEqualTo(SimpleScore.of(-20));
            softly.assertThat(reverseConstraintAnalysis1.matches()).isNull();
        });
        // Matches for constraint2 still not present.
        var reverseConstraintAnalysis2 = reverseDiff.getConstraintAnalysis(constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis2.score()).isEqualTo(SimpleScore.of(-18));
            softly.assertThat(reverseConstraintAnalysis2.matches()).isNull();
        });
        // Matches for constraint3 not present in reverse.
        var reverseConstraintAnalysis3 = reverseDiff.getConstraintAnalysis(constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis3.score()).isEqualTo(SimpleScore.of(30));
            softly.assertThat(reverseConstraintAnalysis3.matches()).isNull();
        });
    }

    @Test
    void diffWithConstraintMatchesWithMatchCountOnly() {
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
                constraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal1, FETCH_MATCH_COUNT),
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, FETCH_MATCH_COUNT),
                emptyConstraintMatchTotal1.getConstraintRef(),
                getConstraintAnalysis(emptyConstraintMatchTotal1, FETCH_MATCH_COUNT));
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
                emptyConstraintMatchTotal2.getConstraintRef(),
                getConstraintAnalysis(emptyConstraintMatchTotal2, FETCH_MATCH_COUNT),
                constraintMatchTotal3.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal3, FETCH_MATCH_COUNT),
                constraintMatchTotal4.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal4, FETCH_MATCH_COUNT));
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(42), constraintAnalysisMap2);

        var diff = scoreAnalysis1.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(diff.score()).isEqualTo(SimpleScore.of(8));
            softly.assertThat(diff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal2.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var constraintAnalysis1 = diff.getConstraintAnalysis(constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis1.score()).isEqualTo(SimpleScore.of(20));
            softly.assertThat(constraintAnalysis1.matches()).isNull();
            softly.assertThat(constraintAnalysis1.matchCount()).isGreaterThan(0);
        });
        // Matches for constraint2 still not present.
        var constraintAnalysis2 = diff.getConstraintAnalysis(constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis2.score()).isEqualTo(SimpleScore.of(18));
            softly.assertThat(constraintAnalysis2.matches()).isNull();
            softly.assertThat(constraintAnalysis2.matchCount()).isGreaterThan(0);
        });
        // Matches for constraint3 not present.
        var constraintAnalysis3 = diff.getConstraintAnalysis(constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis3.score()).isEqualTo(SimpleScore.of(-30));
            softly.assertThat(constraintAnalysis3.matches()).isNull();
            softly.assertThat(constraintAnalysis3.matchCount()).isLessThan(0);
        });

        var reverseDiff = scoreAnalysis2.diff(scoreAnalysis1);
        assertSoftly(softly -> {
            softly.assertThat(reverseDiff.score()).isEqualTo(SimpleScore.of(-8));
            softly.assertThat(reverseDiff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal3.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var reverseConstraintAnalysis1 = reverseDiff.getConstraintAnalysis(constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis1.score()).isEqualTo(SimpleScore.of(-20));
            softly.assertThat(reverseConstraintAnalysis1.matches()).isNull();
            softly.assertThat(reverseConstraintAnalysis1.matchCount()).isLessThan(0);
        });
        // Matches for constraint2 still not present.
        var reverseConstraintAnalysis2 = reverseDiff.getConstraintAnalysis(constraintName2);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis2.score()).isEqualTo(SimpleScore.of(-18));
            softly.assertThat(reverseConstraintAnalysis2.matches()).isNull();
            softly.assertThat(reverseConstraintAnalysis2.matchCount()).isLessThan(0);
        });
        // Matches for constraint3 not present in reverse.
        var reverseConstraintAnalysis3 = reverseDiff.getConstraintAnalysis(constraintName3);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis3.score()).isEqualTo(SimpleScore.of(30));
            softly.assertThat(reverseConstraintAnalysis3.matches()).isNull();
            softly.assertThat(reverseConstraintAnalysis3.matchCount()).isGreaterThan(0);
        });
    }

    @Test
    void diffWithConstraintMatchesAndMatchAnalysis() {
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
                constraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal1, FETCH_ALL),
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, FETCH_ALL),
                emptyConstraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal1, FETCH_ALL));
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
                emptyConstraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal2, FETCH_ALL),
                constraintMatchTotal3.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal3, FETCH_ALL),
                constraintMatchTotal4.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal4, FETCH_ALL));
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(42), constraintAnalysisMap2);

        var diff = scoreAnalysis1.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(diff.score()).isEqualTo(SimpleScore.of(8));
            softly.assertThat(diff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal2.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 present.
        var constraintAnalysis1 = diff.getConstraintAnalysis(constraintName1);
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
        var constraintAnalysis2 = diff.getConstraintAnalysis(constraintName2);
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
        var constraintAnalysis3 = diff.getConstraintAnalysis(constraintName3);
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

        var reverseDiff = scoreAnalysis2.diff(scoreAnalysis1);
        assertSoftly(softly -> {
            softly.assertThat(reverseDiff.score()).isEqualTo(SimpleScore.of(-8));
            softly.assertThat(reverseDiff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal3.getConstraintRef(),
                            constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var reverseConstraintAnalysis1 = reverseDiff.getConstraintAnalysis(constraintName1);
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
        var reverseConstraintAnalysis2 = reverseDiff.getConstraintAnalysis(constraintName2);
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
        var reverseConstraintAnalysis3 = reverseDiff.getConstraintAnalysis(constraintName3);
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

    @Test
    void diffWithConstraintMatchesAndMatchAnalysisWithSomeIdenticalMatches() {
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
                constraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal1, FETCH_ALL),
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, FETCH_ALL),
                emptyConstraintMatchTotal1.getConstraintRef(), getConstraintAnalysis(emptyConstraintMatchTotal1, FETCH_ALL));
        var scoreAnalysis1 = new ScoreAnalysis<>(SimpleScore.of(50), constraintAnalysisMap1);

        var constraintMatchTotal3 = new DefaultConstraintMatchTotal<>(constraintId1, SimpleScore.of(2));
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(2), "A", "B", "C");
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(4), "B", "C");
        addConstraintMatch(constraintMatchTotal3, SimpleScore.of(6), "A", "B");
        var constraintMatchTotal4 = new DefaultConstraintMatchTotal<>(constraintId3, SimpleScore.of(3));
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(3), "B", "C", "D");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(6), "B", "C");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(9), "C", "D");
        addConstraintMatch(constraintMatchTotal4, SimpleScore.of(12));
        var constraintAnalysisMap2 = Map.of(
                constraintMatchTotal2.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal2, FETCH_ALL),
                constraintMatchTotal3.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal3, FETCH_ALL),
                constraintMatchTotal4.getConstraintRef(), getConstraintAnalysis(constraintMatchTotal4, FETCH_ALL));
        var scoreAnalysis2 = new ScoreAnalysis<>(SimpleScore.of(42), constraintAnalysisMap2);

        var diff = scoreAnalysis1.diff(scoreAnalysis2);
        assertSoftly(softly -> {
            softly.assertThat(diff.score()).isEqualTo(SimpleScore.of(8));
            softly.assertThat(diff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 present.
        var constraintAnalysis1 = diff.getConstraintAnalysis(constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(constraintAnalysis1.score()).isEqualTo(SimpleScore.of(8));
            var matchAnalyses = constraintAnalysis1.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), -2, "A", "B"),
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), 2, "B", "C"),
                            matchAnalysisOf(constraintAnalysis1.constraintRef(), 8));
        });
        // Identical matches for constraint2 present in both
        var constraintAnalysis2 = diff.getConstraintAnalysis(constraintName2);
        assertThat(constraintAnalysis2).isNull();

        // Matches for constraint3 not present.
        var constraintAnalysis3 = diff.getConstraintAnalysis(constraintName3);
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

        var reverseDiff = scoreAnalysis2.diff(scoreAnalysis1);
        assertSoftly(softly -> {
            softly.assertThat(reverseDiff.score()).isEqualTo(SimpleScore.of(-8));
            softly.assertThat(reverseDiff.constraintMap())
                    .containsOnlyKeys(constraintMatchTotal1.getConstraintRef(), constraintMatchTotal4.getConstraintRef());
        });
        // Matches for constraint1 not present.
        var reverseConstraintAnalysis1 = reverseDiff.getConstraintAnalysis(constraintName1);
        assertSoftly(softly -> {
            softly.assertThat(reverseConstraintAnalysis1.score()).isEqualTo(SimpleScore.of(-8));
            var matchAnalyses = reverseConstraintAnalysis1.matches();
            softly.assertThat(matchAnalyses)
                    .containsOnly(
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), 2, "A", "B"),
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), -2, "B", "C"),
                            matchAnalysisOf(reverseConstraintAnalysis1.constraintRef(), -8));
        });
        // Identical matches for constraint2 present in both
        var reverseConstraintAnalysis2 = reverseDiff.getConstraintAnalysis(constraintName2);
        assertThat(reverseConstraintAnalysis2).isNull();
        // Matches for constraint3 present in reverse.
        var reverseConstraintAnalysis3 = reverseDiff.getConstraintAnalysis(constraintName3);
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
