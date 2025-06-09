package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.testdomain.constraintconfiguration.TestdataConstraintConfigurationSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public abstract class AbstractScoreDirectorSemanticsTest {

    private final SolutionDescriptor<TestdataConstraintConfigurationSolution> constraintConfigurationSolutionDescriptor =
            TestdataConstraintConfigurationSolution.buildSolutionDescriptor();
    private final SolutionDescriptor<TestdataPinnedListSolution> pinnedListSolutionDescriptor =
            TestdataPinnedListSolution.buildSolutionDescriptor();
    private final SolutionDescriptor<TestdataPinnedWithIndexListSolution> pinnedWithIndexListSolutionDescriptor =
            TestdataPinnedWithIndexListSolution.buildSolutionDescriptor();

    protected abstract ScoreDirectorFactory<TestdataConstraintConfigurationSolution, SimpleScore>
            buildScoreDirectorFactoryWithConstraintConfiguration(
                    SolutionDescriptor<TestdataConstraintConfigurationSolution> solutionDescriptor);

    protected abstract ScoreDirectorFactory<TestdataPinnedListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariableEntityPin(
                    SolutionDescriptor<TestdataPinnedListSolution> solutionDescriptor);

    protected abstract ScoreDirectorFactory<TestdataPinnedWithIndexListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariablePinIndex(
                    SolutionDescriptor<TestdataPinnedWithIndexListSolution> solutionDescriptor);

    @Test
    void independentScoreDirectors() {
        var scoreDirectorFactory =
                buildScoreDirectorFactoryWithConstraintConfiguration(constraintConfigurationSolutionDescriptor);

        // Create first score director, calculate score.
        var solution1 = TestdataConstraintConfigurationSolution.generateSolution(1, 1);
        try (var scoreDirector1 = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector1.setWorkingSolution(solution1);
            var score1 = scoreDirector1.calculateScore();
            assertThat(score1.raw()).isEqualTo(SimpleScore.of(1));

            // Create second score director, calculate score.
            var solution2 = TestdataConstraintConfigurationSolution.generateSolution(2, 2);
            try (var scoreDirector2 = scoreDirectorFactory.buildScoreDirector()) {
                scoreDirector2.setWorkingSolution(solution2);
                var score2 = scoreDirector2.calculateScore();
                assertThat(score2.raw()).isEqualTo(SimpleScore.of(2));

                // Ensure that the second score director did not influence the first.
                assertThat(scoreDirector1.calculateScore().raw()).isEqualTo(SimpleScore.of(1));

                // Make a change on the second score director, ensure it did not affect the first.
                var entity = solution2.getEntityList().get(1);
                scoreDirector2.beforeEntityRemoved(entity);
                solution2.getEntityList().remove(entity);
                scoreDirector2.afterEntityRemoved(entity);
                scoreDirector2.triggerVariableListeners();
                assertThat(scoreDirector2.calculateScore().raw()).isEqualTo(SimpleScore.of(1));
                assertThat(scoreDirector1.calculateScore().raw()).isEqualTo(SimpleScore.of(1));

                // Add the same entity to the first score director, ensure it did not affect the second.
                scoreDirector1.beforeEntityAdded(entity);
                solution1.getEntityList().add(entity);
                scoreDirector1.afterEntityAdded(entity);
                scoreDirector1.triggerVariableListeners();
                assertThat(scoreDirector1.calculateScore().raw()).isEqualTo(SimpleScore.of(2));
                assertThat(scoreDirector2.calculateScore().raw()).isEqualTo(SimpleScore.of(1));
            }
        }
    }

    @Test
    void solutionBasedScoreWeights() {
        var scoreDirectorFactory =
                buildScoreDirectorFactoryWithConstraintConfiguration(constraintConfigurationSolutionDescriptor);

        // Create score director, calculate score.
        var solution1 = TestdataConstraintConfigurationSolution.generateSolution(1, 1);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution1);
            var score1 = scoreDirector.calculateScore();
            assertThat(score1.raw()).isEqualTo(SimpleScore.ONE);

            // Set new solution with a different constraint weight, calculate score.
            var solution2 =
                    TestdataConstraintConfigurationSolution.generateSolution(1, 1);
            var constraintConfiguration = solution2.getConstraintConfiguration();
            constraintConfiguration.setFirstWeight(SimpleScore.of(2));
            scoreDirector.setWorkingSolution(solution2);
            var score2 = scoreDirector.calculateScore();
            assertThat(score2.raw()).isEqualTo(SimpleScore.of(2));

            // Set new solution with a disabled constraint, calculate score.
            constraintConfiguration.setFirstWeight(SimpleScore.ZERO);
            scoreDirector.setWorkingSolution(solution2);
            var score3 = scoreDirector.calculateScore();
            assertThat(score3.raw()).isEqualTo(SimpleScore.ZERO);
        }

    }

    @Test
    void mutableConstraintConfiguration() {
        var scoreDirectorFactory =
                buildScoreDirectorFactoryWithConstraintConfiguration(constraintConfigurationSolutionDescriptor);

        // Create score director, calculate score with a given constraint configuration.
        var solution = TestdataConstraintConfigurationSolution.generateSolution(1, 1);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            var score1 = scoreDirector.calculateScore();
            assertThat(score1.raw()).isEqualTo(SimpleScore.ONE);

            // Change constraint configuration on the current working solution.
            var constraintConfiguration = solution.getConstraintConfiguration();
            scoreDirector.beforeProblemPropertyChanged(constraintConfiguration);
            constraintConfiguration.setFirstWeight(SimpleScore.of(2));
            scoreDirector.afterProblemPropertyChanged(constraintConfiguration);
            var score2 = scoreDirector.calculateScore();
            assertThat(score2.raw()).isEqualTo(SimpleScore.of(2));
        }
    }

    @Test
    void constraintPresentEvenIfNoMatches() {
        var scoreDirectorFactory =
                buildScoreDirectorFactoryWithConstraintConfiguration(constraintConfigurationSolutionDescriptor);
        // Need constraint match support for this.
        Assumptions.assumeTrue(scoreDirectorFactory.supportsConstraintMatching());

        // Create score director, calculate score with a given constraint configuration.
        var solution = TestdataConstraintConfigurationSolution.generateSolution(1, 1);
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .build()) {
            scoreDirector.setWorkingSolution(solution);
            var score1 = scoreDirector.calculateScore();
            assertSoftly(softly -> {
                softly.assertThat(score1.isFullyAssigned()).isTrue();
                softly.assertThat(score1.raw().score()).isEqualTo(1);
                softly.assertThat(scoreDirector.getConstraintMatchTotalMap())
                        .containsOnlyKeys("ai.timefold.solver.core.testdomain.constraintconfiguration/First weight");
            });

            // Make sure nothing matches, but the constraint is still present.
            var entity = scoreDirector.getWorkingSolution().getEntityList().get(0);
            scoreDirector.beforeVariableChanged(entity, "value");
            entity.setValue(null);
            scoreDirector.afterVariableChanged(entity, "value");
            var score2 = scoreDirector.calculateScore();
            assertSoftly(softly -> {
                softly.assertThat(score2.isFullyAssigned()).isFalse();
                softly.assertThat(score2.raw().score()).isZero();
                softly.assertThat(scoreDirector.getConstraintMatchTotalMap())
                        .containsOnlyKeys("ai.timefold.solver.core.testdomain.constraintconfiguration/First weight");
            });
        }
    }

    @Test
    void listVariableEntityPinningSupported() {
        var scoreDirectorFactory = buildScoreDirectorFactoryWithListVariableEntityPin(pinnedListSolutionDescriptor);
        var solution = TestdataPinnedListSolution.generateUninitializedSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValueList(List.of(solution.getValueList().get(0)));
        firstEntity.setPinned(true);

        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            var score1 = scoreDirector.calculateScore();
            assertThat(score1).isEqualTo(InnerScore.withUnassignedCount(SimpleScore.of(-2), 1));

            var workingSolution = scoreDirector.getWorkingSolution();
            var secondEntity = workingSolution.getEntityList().get(1);
            scoreDirector.beforeListVariableElementAssigned(secondEntity, "valueList", 0);
            secondEntity.setValueList(List.of(workingSolution.getValueList().get(1)));
            scoreDirector.afterListVariableElementAssigned(secondEntity, "valueList", 0);
            scoreDirector.triggerVariableListeners();
            var score2 = scoreDirector.calculateScore();
            assertThat(score2.raw()).isEqualTo(SimpleScore.of(-2));
        }
    }

    @Test
    void listVariableIndexPinningSupported() {
        var scoreDirectorFactory =
                buildScoreDirectorFactoryWithListVariablePinIndex(pinnedWithIndexListSolutionDescriptor);
        var solution = TestdataPinnedWithIndexListSolution.generateUninitializedSolution(3, 3);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValueList(List.of(solution.getValueList().get(0)));
        firstEntity.setPinned(true);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValueList(List.of(solution.getValueList().get(1)));
        secondEntity.setPlanningPinToIndex(1);

        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            var score1 = scoreDirector.calculateScore();
            assertThat(score1).isEqualTo(InnerScore.withUnassignedCount(SimpleScore.of(-3), 1));

            var workingSolution = scoreDirector.getWorkingSolution();
            var thirdEntity = workingSolution.getEntityList().get(2);
            scoreDirector.beforeListVariableElementAssigned(thirdEntity, "valueList", 0);
            thirdEntity.setValueList(List.of(workingSolution.getValueList().get(2)));
            scoreDirector.afterListVariableElementAssigned(thirdEntity, "valueList", 0);
            scoreDirector.triggerVariableListeners();
            var score2 = scoreDirector.calculateScore();
            assertThat(score2.raw()).isEqualTo(SimpleScore.of(-3));
        }
    }

}
