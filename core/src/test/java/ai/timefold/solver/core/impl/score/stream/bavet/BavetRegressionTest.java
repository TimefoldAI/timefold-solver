package ai.timefold.solver.core.impl.score.stream.bavet;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity1).withIndictedObjects(entity1),
                assertMatch(entity2, entity2).withIndictedObjects(entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1).withIndictedObjects(entity1),
                assertMatch(entity2, entity2).withIndictedObjects(entity2));

        // Put both to null.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1).withIndictedObjects(entity1),
                assertMatch(entity1, entity2).withIndictedObjects(entity1, entity2),
                assertMatch(entity2, entity1).withIndictedObjects(entity1, entity2),
                assertMatch(entity2, entity2).withIndictedObjects(entity2));
    }

    @TestTemplate
    public void filteringJoinNullConflictRight() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value1);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1));

            // Unassign+assign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value2));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value2);

            assertScore(scoreDirector,
                    assertMatch(value2, value2).withIndictedObjects(value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1));
        }

    }

    @TestTemplate
    public void filteringJoinNullConflictRightUnindexed() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value1);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1));

            // Unassign+assign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value2));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value2);

            assertScore(scoreDirector,
                    assertMatch(value2, value2).withIndictedObjects(value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1));
        }

    }

    @TestTemplate
    public void filteringJoinNullConflictRightViaIfExists() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .ifExists(TestdataAllowsUnassignedValuesListEntity.class),
                                                equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value1);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1));

            // Unassign+assign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value2));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value2);

            assertScore(scoreDirector,
                    assertMatch(value2, value2).withIndictedObjects(value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1));
        }

    }

    @TestTemplate
    public void filteringJoinNullConflictRightUnassignOne() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value1);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value2);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().addAll(List.of(value1, value2));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value2);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1),
                    assertMatch(value1, value2).withIndictedObjects(value1, value2),
                    assertMatch(value2, value1).withIndictedObjects(value1, value2),
                    assertMatch(value2, value2).withIndictedObjects(value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2).withIndictedObjects(value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1).withIndictedObjects(value1),
                    assertMatch(value1, value2).withIndictedObjects(value1, value2),
                    assertMatch(value2, value1).withIndictedObjects(value1, value2),
                    assertMatch(value2, value2).withIndictedObjects(value2));
        }

    }

    @TestTemplate
    public void filteringIfExistsNullConflictRight() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .ifExists(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value1);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value1);

            assertScore(scoreDirector,
                    assertMatch(value1));

            // Unassign+assign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value2));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value2);

            assertScore(scoreDirector,
                    assertMatch(value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1));
        }

    }

    @TestTemplate
    public void filteringIfExistsNullConflictRightUnindexed() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .ifExists(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value1);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", value1);

            assertScore(scoreDirector,
                    assertMatch(value1));

            // Unassign+assign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value2));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value2);

            assertScore(scoreDirector,
                    assertMatch(value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1));
        }

    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver GitHub Issue 186</a>
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity2).withIndictedObjects(entity2)); // Only entity1 is left, because forEach/join ignore nulls.

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1).withIndictedObjects(entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2, entity2).withIndictedObjects(entity2));
    }

    /**
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver GitHub Issue 186</a>
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
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
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/186">Timefold Solver GitHub Issue 186</a>
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
        entity1.setValue(null);
        entity2.setValue(value);

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity2, entity2).withIndictedObjects(entity2));

        // Switch entity1 and entity2 values; now entity2 has null and entity1 does not.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(null);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity1).withIndictedObjects(entity1));

        // Switch entity1 and entity2 values again to test the same from the other side.
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(null);
        scoreDirector.afterVariableChanged(entity1, "value");
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity2, entity2).withIndictedObjects(entity2));
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });

        var solution = TestdataSolution.generateSolution(1, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().getFirst();
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
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/828">Timefold Solver GitHub Issue 828</a>
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
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
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        });
        var solution = TestdataListMultipleShadowVariableSolution.generateSolution(2, 1);
        // We don't want to update shadows for this test!
        scoreDirector.setWorkingSolutionWithoutUpdatingShadows(solution);
        scoreDirector.clearPendingShadowVariableUpdates();
        assertThat(solution.getValueList().stream().allMatch(v -> v.getCascadeValue() == 2))
                .isTrue(); // two if it is null
    }

}
