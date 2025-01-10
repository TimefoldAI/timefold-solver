package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.equals.TestdataEqualsByCodeEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.equals.TestdataEqualsByCodeEntity;
import ai.timefold.solver.core.impl.testdata.domain.equals.TestdataEqualsByCodeSolution;
import ai.timefold.solver.core.impl.testdata.domain.equals.list.TestdataEqualsByCodeListEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.equals.list.TestdataEqualsByCodeListEntity;
import ai.timefold.solver.core.impl.testdata.domain.equals.list.TestdataEqualsByCodeListSolution;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanningSolutionDiffTest {

    @Nested
    @DisplayName("Diff of two solutions called without the preview feature enabled")
    class PlanningSolutionDiffPreviewNotEnabledTest {

        private final SolutionManager<TestdataEqualsByCodeSolution, SimpleScore> solutionManager =
                SolutionManager.create(SolverFactory.create(new SolverConfig()
                        .withSolutionClass(TestdataEqualsByCodeSolution.class)
                        .withEntityClasses(TestdataEqualsByCodeEntity.class)
                        .withEasyScoreCalculatorClass(TestdataEqualsByCodeEasyScoreCalculator.class)));

        @Test
        void failsFast() {
            assertThatThrownBy(
                    () -> solutionManager.diff(new TestdataEqualsByCodeSolution(), new TestdataEqualsByCodeSolution()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(PreviewFeature.PLANNING_SOLUTION_DIFF.name());
        }

    }

    @Nested
    @DisplayName("Diff of two solutions of the same class with a single basic variable")
    class BasicVariablePlanningSolutionDiffTest {

        private final SolutionManager<TestdataEqualsByCodeSolution, SimpleScore> solutionManager =
                SolutionManager.create(SolverFactory.create(new SolverConfig()
                        .withPreviewFeature(PreviewFeature.PLANNING_SOLUTION_DIFF)
                        .withSolutionClass(TestdataEqualsByCodeSolution.class)
                        .withEntityClasses(TestdataEqualsByCodeEntity.class)
                        .withEasyScoreCalculatorClass(TestdataEqualsByCodeEasyScoreCalculator.class)));

        @Nested
        @DisplayName("Where the two solutions have identical contents")
        class BasicVariablePlanningSolutionDiffIdenticalContentsTest {

            private final TestdataEqualsByCodeSolution oldSolution =
                    TestdataEqualsByCodeSolution.generateSolution("Old Solution", 2, 10);
            private final TestdataEqualsByCodeSolution newSolution =
                    TestdataEqualsByCodeSolution.generateSolution("New Solution", 2, 10);
            private final PlanningSolutionDiff<TestdataEqualsByCodeSolution> diff =
                    solutionManager.diff(oldSolution, newSolution);

            @Test
            void hasFieldsSet() {
                assertSoftly(softly -> {
                    softly.assertThat(diff.solutionMetaModel()).isNotNull();
                    softly.assertThat(diff.addedEntities()).isEmpty();
                    softly.assertThat(diff.removedEntities()).isEmpty();
                    softly.assertThat(diff.oldSolution()).isSameAs(oldSolution);
                    softly.assertThat(diff.newSolution()).isSameAs(newSolution);
                });
            }

            @Test
            void providesNoEntityDiffs() {
                assertSoftly(softly -> {
                    softly.assertThat(diff.entityDiff(newSolution.getEntityList().get(0))).isNull();
                    softly.assertThat(diff.entityDiff(oldSolution.getEntityList().get(0))).isNull();
                    softly.assertThat(diff.entityDiffs()).isEmpty();
                    softly.assertThat(diff.entityDiffs(TestdataEntity.class)).isEmpty();
                    softly.assertThat(diff.entityDiffs(TestdataEqualsByCodeEntity.class)).isEmpty();
                });
            }

            @Test
            void hasHelpfulToString() {
                assertThat(diff)
                        .hasToString(
                                """
                                        Difference(s) between old planning solution (Old Solution) and new planning solution (New Solution):

                                        - Old solution entities not present in new solution:
                                          (None.)

                                        - New solution entities not present in old solution:
                                          (None.)

                                        - Entities changed between the solutions:
                                          (None.)
                                        """);
            }
        }

        @Nested
        @DisplayName("Where the two solutions have different contents")
        class BasicVariablePlanningSolutionDiffDifferentEntitiesTest {

            private final TestdataEqualsByCodeSolution oldSolution =
                    TestdataEqualsByCodeSolution.generateSolution("Old Solution", 2, 10);
            private final TestdataEqualsByCodeEntity oldEntityRemoved = oldSolution.getEntityList().remove(0);
            private final TestdataEqualsByCodeSolution newSolution =
                    TestdataEqualsByCodeSolution.generateSolution("New Solution", 2, 10);
            private final TestdataEqualsByCodeEntity newEntityRemoved = newSolution.getEntityList().remove(9);
            private PlanningSolutionDiff<TestdataEqualsByCodeSolution> diff;

            @BeforeEach
            void beforeEach() {
                newSolution.getEntityList().forEach(entity -> {
                    var newValue = entity.getValue() == newSolution.getValueList().get(0) ? newSolution.getValueList().get(1)
                            : newSolution.getValueList().get(0);
                    entity.setValue(newValue);
                });
                diff = solutionManager.diff(oldSolution, newSolution);
            }

            @Test
            void hasFieldsSet() {
                assertSoftly(softly -> {
                    softly.assertThat(diff.solutionMetaModel()).isNotNull();
                    softly.assertThat(diff.addedEntities()).containsExactly(oldEntityRemoved);
                    softly.assertThat(diff.removedEntities()).containsExactly(newEntityRemoved);
                    softly.assertThat(diff.oldSolution()).isSameAs(oldSolution);
                    softly.assertThat(diff.newSolution()).isSameAs(newSolution);
                });
            }

            @Test
            void providesEntityDiffs() { // 8 entities are different.
                assertSoftly(softly -> {
                    softly.assertThat(diff.entityDiff(oldEntityRemoved)).isNull();
                    softly.assertThat(diff.entityDiff(newEntityRemoved)).isNull();
                    softly.assertThat(diff.entityDiffs()).hasSize(8);
                    softly.assertThat(diff.entityDiffs(TestdataEntity.class)).isEmpty();
                    softly.assertThat(diff.entityDiffs(TestdataEqualsByCodeEntity.class)).hasSize(8);
                });
            }

            @Test
            void hasHelpfulToString() {
                assertThat(diff)
                        .hasToString(
                                """
                                        Difference(s) between old planning solution (Old Solution) and new planning solution (New Solution):

                                        - Old solution entities not present in new solution:
                                          Generated Entity 9

                                        - New solution entities not present in old solution:
                                          Generated Entity 0

                                        - Entities changed between the solutions:
                                          TestdataEqualsByCodeEntity Generated Entity 4 (Generated Value 0 -> Generated Value 1)
                                          TestdataEqualsByCodeEntity Generated Entity 5 (Generated Value 1 -> Generated Value 0)
                                          TestdataEqualsByCodeEntity Generated Entity 6 (Generated Value 0 -> Generated Value 1)
                                          TestdataEqualsByCodeEntity Generated Entity 7 (Generated Value 1 -> Generated Value 0)
                                          TestdataEqualsByCodeEntity Generated Entity 8 (Generated Value 0 -> Generated Value 1)
                                          ...
                                        """);
            }
        }

    }

    @Nested
    @DisplayName("Diff of two solutions of the same class with a list variable")
    class ListVariablePlanningSolutionDiffTest {

        private final SolutionManager<TestdataEqualsByCodeListSolution, SimpleScore> solutionManager =
                SolutionManager.create(SolverFactory.create(new SolverConfig()
                        .withPreviewFeature(PreviewFeature.PLANNING_SOLUTION_DIFF)
                        .withSolutionClass(TestdataEqualsByCodeListSolution.class)
                        .withEntityClasses(TestdataEqualsByCodeListEntity.class)
                        .withEasyScoreCalculatorClass(TestdataEqualsByCodeListEasyScoreCalculator.class)));

        @Nested
        @DisplayName("Where the two solutions have identical contents")
        class ListVariablePlanningSolutionDiffIdenticalContentsTest {

            private final TestdataEqualsByCodeListSolution oldSolution =
                    TestdataEqualsByCodeListSolution.generateSolution("Old Solution", 2, 10)
                            .initialize();
            private final TestdataEqualsByCodeListSolution newSolution =
                    TestdataEqualsByCodeListSolution.generateSolution("New Solution", 2, 10)
                            .initialize();
            private final PlanningSolutionDiff<TestdataEqualsByCodeListSolution> diff =
                    solutionManager.diff(oldSolution, newSolution);

            @Test
            void hasFieldsSet() {
                assertSoftly(softly -> {
                    softly.assertThat(diff.solutionMetaModel()).isNotNull();
                    softly.assertThat(diff.addedEntities()).isEmpty();
                    softly.assertThat(diff.removedEntities()).isEmpty();
                    softly.assertThat(diff.oldSolution()).isSameAs(oldSolution);
                    softly.assertThat(diff.newSolution()).isSameAs(newSolution);
                });
            }

            @Test
            void providesNoEntityDiffs() {
                assertSoftly(softly -> {
                    softly.assertThat(diff.entityDiff(newSolution.getEntityList().get(0))).isNull();
                    softly.assertThat(diff.entityDiff(oldSolution.getEntityList().get(0))).isNull();
                    softly.assertThat(diff.entityDiffs()).isEmpty();
                    softly.assertThat(diff.entityDiffs(TestdataEntity.class)).isEmpty();
                    softly.assertThat(diff.entityDiffs(TestdataEqualsByCodeEntity.class)).isEmpty();
                });
            }

            @Test
            void hasHelpfulToString() {
                assertThat(diff)
                        .hasToString(
                                """
                                        Difference(s) between old planning solution (Old Solution) and new planning solution (New Solution):

                                        - Old solution entities not present in new solution:
                                          (None.)

                                        - New solution entities not present in old solution:
                                          (None.)

                                        - Entities changed between the solutions:
                                          (None.)
                                        """);
            }
        }

        @Nested
        @DisplayName("Where the two solutions have different contents")
        class ListVariablePlanningSolutionDiffDifferentEntitiesTest {

            private final TestdataEqualsByCodeListSolution oldSolution =
                    TestdataEqualsByCodeListSolution.generateSolution("Old Solution", 3, 10)
                            .initialize();
            private final TestdataEqualsByCodeListEntity oldEntityRemoved = oldSolution.getEntityList().remove(0);
            private final TestdataEqualsByCodeListSolution newSolution =
                    TestdataEqualsByCodeListSolution.generateSolution("New Solution", 4, 10)
                            .initialize();
            private final TestdataEqualsByCodeListEntity newEntityRemoved = newSolution.getEntityList().remove(9);
            private PlanningSolutionDiff<TestdataEqualsByCodeListSolution> diff =
                    solutionManager.diff(oldSolution, newSolution);

            @Test
            void hasFieldsSet() {
                assertSoftly(softly -> {
                    softly.assertThat(diff.solutionMetaModel()).isNotNull();
                    softly.assertThat(diff.addedEntities()).containsExactly(oldEntityRemoved);
                    softly.assertThat(diff.removedEntities()).containsExactly(newEntityRemoved);
                    softly.assertThat(diff.oldSolution()).isSameAs(oldSolution);
                    softly.assertThat(diff.newSolution()).isSameAs(newSolution);
                });
            }

            @Test
            void providesEntityDiffs() { // 8 entities are different.
                assertSoftly(softly -> {
                    softly.assertThat(diff.entityDiff(oldEntityRemoved)).isNull();
                    softly.assertThat(diff.entityDiff(newEntityRemoved)).isNull();
                    softly.assertThat(diff.entityDiffs()).hasSize(1);
                    softly.assertThat(diff.entityDiffs(TestdataEqualsByCodeEntity.class)).isEmpty();
                    softly.assertThat(diff.entityDiffs(TestdataEqualsByCodeListEntity.class)).hasSize(1);
                });
            }

            @Test
            void hasHelpfulToString() {
                assertThat(diff)
                        .hasToString(
                                """
                                        Difference(s) between old planning solution (Old Solution) and new planning solution (New Solution):

                                        - Old solution entities not present in new solution:
                                          Generated Entity 9

                                        - New solution entities not present in old solution:
                                          Generated Entity 0

                                        - Entities changed between the solutions:
                                          TestdataEqualsByCodeListEntity Generated Entity 3 ([] -> [Generated Value 3])
                                        """);
            }
        }

    }

}
