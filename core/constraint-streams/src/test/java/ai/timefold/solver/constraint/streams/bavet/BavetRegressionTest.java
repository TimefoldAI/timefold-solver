package ai.timefold.solver.constraint.streams.bavet;

import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;

import ai.timefold.solver.constraint.streams.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.TestTemplate;

final class BavetRegressionTest extends AbstractConstraintStreamTest {

    protected BavetRegressionTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver Github Issue 186</a>
     */
    @TestTemplate
    public void filteringJoinNullConflict() {
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

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
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
    public void filteringIfExistsNullConflict() {
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

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
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
    public void filteringIfNotExistsNullConflict() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataEntity.class)
                                        .ifNotExists(TestdataEntity.class,
                                                filtering((a, b) -> (a.getValue() != b.getValue())))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_NAME)
                        });

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
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
    public void filteringJoinNullConflictDifferentNodes() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingNullVars(TestdataEntity.class)
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

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        // TODO update the assertions once no longer throwing exceptions
        assertScore(scoreDirector,
                assertMatch(entity2, entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        // TODO update the assertions once no longer throwing exceptions
        assertScore(scoreDirector,
                assertMatch(entity1, entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        // TODO update the assertions once no longer throwing exceptions
        assertScore(scoreDirector,
                assertMatch(entity2, entity2));
    }

    /**
     * Like {@link #filteringIfExistsNullConflict()}, but using two different forEach nodes.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictDifferentNodes() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingNullVars(TestdataEntity.class)
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

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
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
    public void filteringIfNotExistsNullConflictDifferentNodes() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEachIncludingNullVars(TestdataEntity.class)
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

        TestdataSolution solution = TestdataSolution.generateSolution(1, 2);
        TestdataEntity entity1 = solution.getEntityList().get(0);
        TestdataEntity entity2 = solution.getEntityList().get(1);
        TestdataValue value = solution.getValueList().get(0);
        entity1.setValue(null);
        entity2.setValue(value);

        /*
         * forEachInclNull propagates entity1 and entity2; entity1 gets filtered out.
         * forEachExclNull propagates entity2.
         * Tuple (entity2, entity2) comes in, values are equal, therefore not exists, therefore entity2 penalized.
         */
        scoreDirector.setWorkingSolution(solution);
        // TODO update the assertions once no longer throwing exceptions
        assertScore(scoreDirector,
                assertMatch(entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        // TODO update the assertions once no longer throwing exceptions
        assertScore(scoreDirector,
                assertMatch(entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        // TODO update the assertions once no longer throwing exceptions
        assertScore(scoreDirector,
                assertMatch(entity2));
    }

}
