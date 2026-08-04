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

import org.junit.jupiter.api.Disabled;
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
                    assertMatch(value1, value1));

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
                    assertMatch(value2, value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1));
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
                    assertMatch(value1, value1));

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
                    assertMatch(value2, value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1));
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
                    assertMatch(value1, value1));

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
                    assertMatch(value2, value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.setValueList(List.of(value1));
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1));
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
                    assertMatch(value1, value1),
                    assertMatch(value1, value2),
                    assertMatch(value2, value1),
                    assertMatch(value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2));

            // Reassign and check result.
            scoreDirector.beforeListVariableElementAssigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
            entity.getValueList().add(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 2);
            scoreDirector.afterListVariableElementAssigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value1, value1),
                    assertMatch(value1, value2),
                    assertMatch(value2, value1),
                    assertMatch(value2, value2));
        }

    }

    /**
     * Like {@link #filteringJoinNullConflictRightUnassignOne()}, but the null-checking filter sits behind
     * a 3-hop self-join instead of a single bi-join. A bi-join's left input is always the root tuple
     * (whose retraction is applied eagerly), so the existing single-hop guard is enough there. Two hops
     * deeper, the left input is itself a join product whose own retraction is deferred to a later
     * network layer — exposing a stale, "still active" tuple to the filtering predicate.
     */
    @TestTemplate
    public void filteringJoinNullConflictRightQuadJoinUnassignOne() {
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
                                                        TestdataAllowsUnassignedValuesListValue::getEntity))
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal((a, b) -> a.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity))
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal((a, b, c) -> a.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b, c, d) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    Objects.requireNonNull(c.getEntity());
                                                    Objects.requireNonNull(d.getEntity());
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
                    assertMatch(value1, value1, value1, value1),
                    assertMatch(value1, value1, value1, value2),
                    assertMatch(value1, value1, value2, value1),
                    assertMatch(value1, value1, value2, value2),
                    assertMatch(value1, value2, value1, value1),
                    assertMatch(value1, value2, value1, value2),
                    assertMatch(value1, value2, value2, value1),
                    assertMatch(value1, value2, value2, value2),
                    assertMatch(value2, value1, value1, value1),
                    assertMatch(value2, value1, value1, value2),
                    assertMatch(value2, value1, value2, value1),
                    assertMatch(value2, value1, value2, value2),
                    assertMatch(value2, value2, value1, value1),
                    assertMatch(value2, value2, value1, value2),
                    assertMatch(value2, value2, value2, value1),
                    assertMatch(value2, value2, value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2, value2, value2));
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
     * Like {@link #filteringIfExistsNullConflictRight()}, but the left input to ifExists is a
     * 3-hop self-join product instead of the root tuple directly — the ifExists counterpart to
     * {@link #filteringJoinNullConflictRightQuadJoinUnassignOne()}.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictDeepLeftUnassignOne() {
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
                                                        TestdataAllowsUnassignedValuesListValue::getEntity))
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal((a, b) -> a.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity))
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal((a, b, c) -> a.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity))
                                        .ifExists(TestdataAllowsUnassignedValuesListValue.class,
                                                equal((a, b, c, d) -> a.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b, c, d, e) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    Objects.requireNonNull(c.getEntity());
                                                    Objects.requireNonNull(d.getEntity());
                                                    Objects.requireNonNull(e.getEntity());
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
                    assertMatch(value1, value1, value1, value1),
                    assertMatch(value1, value1, value1, value2),
                    assertMatch(value1, value1, value2, value1),
                    assertMatch(value1, value1, value2, value2),
                    assertMatch(value1, value2, value1, value1),
                    assertMatch(value1, value2, value1, value2),
                    assertMatch(value1, value2, value2, value1),
                    assertMatch(value1, value2, value2, value2),
                    assertMatch(value2, value1, value1, value1),
                    assertMatch(value2, value1, value1, value2),
                    assertMatch(value2, value1, value2, value1),
                    assertMatch(value2, value1, value2, value2),
                    assertMatch(value2, value2, value1, value1),
                    assertMatch(value2, value2, value1, value2),
                    assertMatch(value2, value2, value2, value1),
                    assertMatch(value2, value2, value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2, value2, value2));
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

    /**
     * Like {@link #filteringJoinNullConflictRightUnassignOne()}, but the left input to the filtering join
     * is derived via a {@code .map()} from an earlier join's output, instead of being that earlier join's
     * product directly. A map node's own retraction of its output is deferred to its parent join's flush
     * the same way an intermediate join's own output is — same race, one more node type in the chain.
     */
    @TestTemplate
    public void filteringJoinNullConflictThroughMapUnassignOne() {
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
                                                equal(Function.identity(), Function.identity()))
                                        .map((TestdataAllowsUnassignedValuesListValue a,
                                                TestdataAllowsUnassignedValuesListValue b) -> a)
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
                    assertMatch(value1, value1),
                    assertMatch(value1, value2),
                    assertMatch(value2, value1),
                    assertMatch(value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2));
        }
    }

    /**
     * Like {@link #filteringJoinNullConflictThroughMapUnassignOne()}, but with {@code .flatten()} instead
     * of {@code .map()} in the middle of the chain. {@link ai.timefold.solver.core.impl.bavet.common.AbstractFlattenNode}
     * has the identical eager-state/deferred-notification pattern as map, so it should reproduce the same way.
     */
    @TestTemplate
    public void filteringJoinNullConflictThroughFlattenUnassignOne() {
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
                                                equal(Function.identity(), Function.identity()))
                                        .flatten((TestdataAllowsUnassignedValuesListValue a,
                                                TestdataAllowsUnassignedValuesListValue b) -> List.of(a))
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal((x, y, z) -> z.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((x, y, z, w) -> {
                                                    Objects.requireNonNull(x.getEntity());
                                                    Objects.requireNonNull(y.getEntity());
                                                    Objects.requireNonNull(z.getEntity());
                                                    Objects.requireNonNull(w.getEntity());
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
                    assertMatch(value1, value1, value1, value1),
                    assertMatch(value1, value1, value1, value2),
                    assertMatch(value2, value2, value2, value1),
                    assertMatch(value2, value2, value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2, value2, value2));
        }
    }

    /**
     * Like {@link #filteringJoinNullConflictThroughMapUnassignOne()}, but with {@code .groupBy()} instead
     * of {@code .map()} in the middle of the chain. Unlike map/flatten, a group tuple is an N:1 aggregate
     * with no single input tuple whose activity implies the group's own, so this one is expected to remain
     * broken even after the fix — see "groupBy stays unfixed".
     */
    @TestTemplate
    @Disabled("Known ceiling: groupBy's output is an N:1 aggregate with no single parent tuple to track "
            + "(see Tuple#isActiveTransitively); this reproduces the still-open stale-activity race.")
    public void filteringJoinNullConflictThroughGroupByUnassignOne() {
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
                                                equal(Function.identity(), Function.identity()))
                                        .groupBy((TestdataAllowsUnassignedValuesListValue a,
                                                TestdataAllowsUnassignedValuesListValue b) -> a)
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
                    assertMatch(value1, value1),
                    assertMatch(value1, value2),
                    assertMatch(value2, value1),
                    assertMatch(value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2));
        }
    }

}
