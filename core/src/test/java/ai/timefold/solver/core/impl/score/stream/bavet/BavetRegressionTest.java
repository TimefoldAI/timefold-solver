package ai.timefold.solver.core.impl.score.stream.bavet;

import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableSolution;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableValue;

import org.junit.jupiter.api.TestTemplate;

final class BavetRegressionTest extends AbstractConstraintStreamTest {

    public BavetRegressionTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

    @TestTemplate
    void joinWithNullKeyFromRight() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingUnassigned(TestdataEntity.class)
                                        .join(factory.forEachIncludingUnassigned(TestdataEntity.class),
                                                Joiners.equal(TestdataEntity::getValue))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity1),
                assertMatch(entity2, entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1),
                assertMatch(entity2, entity2));

        // Put both to null.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1),
                assertMatch(entity1, entity2),
                assertMatch(entity2, entity1),
                assertMatch(entity2, entity2));
    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver Github Issue 186</a>
     */
    @TestTemplate
    void filteringJoinNullConflict() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .join(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though forEach() should have eliminated it.");
                                                    } else if (b.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of B is null even though join()'s inner forEach() should have eliminated it.");
                                                    }
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity2)); // Only entity1 is left, because forEach/join ignore nulls.

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2, entity2));
    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver Github Issue 186</a>
     */
    @TestTemplate
    void filteringIfExistsNullConflict() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .ifExists(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though forEach() should have eliminated it.");
                                                    }
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2)); // Only entity1 is left, because forEach/ifExists ignore nulls.

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2));
    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver Github Issue 186</a>
     */
    @TestTemplate
    void filteringIfNotExistsNullConflict() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .ifNotExists(TestdataEntity.class,
                                                filtering((a, b) -> (a.getValue() != b.getValue())))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        /*
         * forEachExclNull propagates entity2.
         * The tuple (entity2, entity2) therefore exists, but the values are equal.
         * Therefore entity2 should be scored.
         */
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2));
    }

    /**
     * Like {@link #filteringJoinNullConflict()}, but using two different forEach nodes.
     */
    @TestTemplate
    void filteringJoinNullConflictDifferentNodes() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingUnassigned(TestdataEntity.class)
                                        .filter(a -> a.getValue() != null)
                                        .join(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though filter() should have eliminated it.");
                                                    } else if (b.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of B is null even though join()'s inner forEach() should have eliminated it.");
                                                    }
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2, entity2));
    }

    /**
     * Like {@link #filteringIfExistsNullConflict()}, but using two different forEach nodes.
     */
    @TestTemplate
    void filteringIfExistsNullConflictDifferentNodes() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingUnassigned(TestdataEntity.class)
                                        .filter(a -> a.getValue() != null)
                                        .ifExists(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though filter() should have eliminated it.");
                                                    } else if (b.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of B is null even though ifExists()'s inner forEach() should have eliminated it.");
                                                    }
                                                    return a.getValue() != b.getValue();
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector);

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector);
    }

    /**
     * Like {@link #filteringIfExistsNullConflict()}, but using two different forEach nodes.
     */
    @TestTemplate
    void filteringIfNotExistsNullConflictDifferentNodes() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingUnassigned(TestdataEntity.class)
                                        .filter(a -> a.getValue() != null)
                                        .ifNotExists(TestdataEntity.class,
                                                filtering((a, b) -> {
                                                    if (a.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of A is null even though filter() should have eliminated it.");
                                                    } else if (b.getValue() == null) {
                                                        throw new IllegalStateException(
                                                                "Impossible state: value of B is null even though ifExists()'s inner forEach() should have eliminated it.");
                                                    }
                                                    return a.getValue() != b.getValue();
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        /*
         * forEachInclNull propagates entity1 and entity2; entity1 gets filtered out.
         * forEachExclNull propagates entity2.
         * Tuple (entity2, entity2) comes in, values are equal, therefore not exists, therefore entity2 penalized.
         */
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2));
    }

    @TestTemplate
    void mapPlanningEntityChanges() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingUnassigned(TestdataEntity.class)
                                        .map(Function.identity())
                                        .filter(e -> e.getValue() != null)
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Now make entity1 and entity2 both be non-null
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));
    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/828">Timefold Solver Github Issue 828</a>
     */
    @TestTemplate
    void concatSameTupleDeadAndAlive() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .filter(e -> e.getValue().getCode().equals("A"))
                                        .concat(factory.forEach(TestdataEntity.class))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var valueA = solution.getValueList().get(0);
        valueA.setCode("A");
        var valueB = solution.getValueList().get(1);
        valueB.setCode("B");

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity1),
                assertMatch(entity2));

        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(valueB);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(valueA);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity2));

        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(valueA);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(valueB);
        scoreDirector.afterVariableChanged(entity2, "value");
        // Do not recalculate score, since this is undo

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(valueA);
        scoreDirector.afterVariableChanged(entity2, "value");
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(valueB);
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity2));

        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(valueB);
        scoreDirector.afterVariableChanged(entity2, "value");
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(valueA);
        scoreDirector.afterVariableChanged(entity1, "value");

        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity1),
                assertMatch(entity2));
    }

    @TestTemplate
    @SuppressWarnings("unchecked")
    void clearEvents() {
        var scoreDirector =
                (BavetConstraintStreamScoreDirector<TestdataListMultipleShadowVariableSolution, SimpleScore>) buildScoreDirector(
                        TestdataListMultipleShadowVariableSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataListMultipleShadowVariableValue.class)
                                        .penalize(SimpleScore.ONE, TestdataListMultipleShadowVariableValue::getCascadeValue)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });
        var solution = TestdataListMultipleShadowVariableSolution.generateSolution(2, 1);
        scoreDirector.setWorkingSolution(solution);
        scoreDirector.clearShadowVariablesListenerQueue();
        assertThat(solution.getValueList().stream().allMatch(v -> v.getListenerValue() == 0))
                .isTrue(); // zero if it is null
        assertThat(solution.getValueList().stream().allMatch(v -> v.getCascadeValue() == 2))
                .isTrue(); // two if it is null
    }

}
