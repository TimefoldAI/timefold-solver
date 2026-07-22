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
     * a 3-hop self-join instead of a single bi-join. A bi-join's left input is always the root tuple,
     * which sits at layer 0 and is never itself mid-retraction when this join reads it, so a single hop
     * can't expose the race this test targets. Two hops deeper, the left input is itself a join product
     * living in a strictly later network layer -- reading it here is exactly the case
     * {@code AbstractJoinNode#prepareForSettle()} exists to close: without deferring the cross-match to
     * this node's own layer turn, the left tuple's retraction may not yet have propagated through its own
     * layer by the time this join reads it, exposing a stale, "still active" tuple to the filtering
     * predicate.
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

    /**
     * Like {@link #filteringJoinNullConflictRightQuadJoinUnassignOne()}, but the last join carries no
     * filtering predicate. Non-filtering joins never dereference a fact through a user predicate --
     * {@code AbstractJoinNode#insertOutTupleIfActiveFiltered} skips {@code testFiltering} entirely for
     * them and inserts unconditionally -- so a stale-but-"active" read here can only ever produce an
     * out-tuple that the true retraction (arriving later, deeper in the same layer chain) cleans up via
     * the retracted tuple's own out-tuple list. The final score must converge correctly regardless of
     * read timing, which is exactly why non-filtering joins skip the deferred cross-match machinery
     * entirely (see {@code AbstractJoinNode#hasDeferredWork()}).
     */
    @TestTemplate
    public void joinNullConflictRightQuadJoinUnassignOneNonFiltering() {
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
     * Like {@link #filteringJoinNullConflictRightQuadJoinUnassignOne()}, but the trigger is an in-place
     * list reorder (an UPDATE on the index/previous/next shadow variables) instead of an unassign (a
     * RETRACT). Both values stay assigned throughout, so this exercises the deferred cross-match on the
     * update path specifically -- {@code AbstractJoinNode#innerUpdateLeft}'s filtering branch enqueues the
     * left tuple and lets {@code prepareForSettle()} re-run its cross-match once every layer has settled
     * -- rather than the insert-path deferral the unassign-based tests above exercise. Neither the join
     * key ({@code getEntity()}, untouched by a reorder) nor the filter's null-checks are affected by a
     * reorder, so the match set must be identical before and after; a stale read on the update path
     * would corrupt it or throw.
     */
    @TestTemplate
    public void filteringJoinNullConflictThroughQuadJoinReorder() {
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

            // Swap value1 and value2's positions in place.
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 2);
            entity.setValueList(List.of(value2, value1));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);

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
     * The mirror image of {@link #filteringIfExistsNullConflictDeepLeftUnassignOne()}: the deep 3-hop
     * self-join chain is ifExists' *right* input instead of its left, so the stale read this time is on
     * the right tuple, going through {@code AbstractIfExistsNode#updateCounterLeft(counter, rightTuple)}.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictDeepRightUnassignOne() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .ifExists(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                        .map(v -> v),
                                                        equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                                TestdataAllowsUnassignedValuesListValue::getEntity))
                                                .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                        .map(v -> v),
                                                        equal((a, b) -> a.getEntity(),
                                                                TestdataAllowsUnassignedValuesListValue::getEntity))
                                                .map((a, b, c) -> a),
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
                    assertMatch(value1),
                    assertMatch(value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2));
        }
    }

    /**
     * Like {@link #filteringIfExistsNullConflictDeepLeftUnassignOne()}, but instead of unassigning a
     * value, reorders the list in place -- the ifExists counterpart to
     * {@link #filteringJoinNullConflictThroughQuadJoinReorder()}. Triggers a pure UPDATE (index/
     * previous/next shadow changes only, same composite key) through the chain into the ifExists node,
     * rather than a retract+insert.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictThroughQuadJoinReorder() {
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

            // Swap value1 and value2's positions in place -- pure UPDATE, no assign/unassign.
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 2);
            entity.setValueList(List.of(value2, value1));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);

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
        }
    }

    /**
     * Like {@link #filteringIfExistsNullConflictDeepLeftUnassignOne()}, but ifNotExists instead of
     * ifExists, exercising the inverted counter logic ({@code countRight == 0}) through the same 3-hop
     * self-join chain on the left input.
     */
    @TestTemplate
    public void filteringIfNotExistsNullConflictDeepLeftUnassignOne() {
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
                                        .ifNotExists(TestdataAllowsUnassignedValuesListValue.class,
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

            // The filtering predicate always returns true whenever a match is even attempted, so
            // ifNotExists never fires: every left quad has at least one matching right value.
            assertScore(scoreDirector);

            // Unassign and check result: still no match, but this must not throw on a stale read.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector);
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

    /**
     * Like {@link #filteringJoinNullConflictRightUnassignOne()}, but the left input to the filtering join
     * is derived via a {@code .map()} from an earlier join's output, instead of being that earlier join's
     * product directly. Map retracts its own out-tuple only once its input tuple retracts, at its own
     * layer's turn -- the same layer-ordering shape as an intermediate join's own output, just with one
     * more node type standing between the root and the filtering join that reads it.
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
     * retracts its own out-tuples only once its input tuple retracts, at its own layer's turn -- the same
     * layer-ordering shape as map, so it should reproduce the same way.
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
     * of {@code .map()} in the middle of the chain. A group's out-tuple aggregates over every contributor
     * mapped into it (an N:1 relationship, unlike map/flatten's 1:1) and only retracts once the group
     * becomes empty, at its own layer's turn -- the same layer-ordering shape as map and flatten, so it
     * should reproduce the same way despite the different membership-tracking mechanism underneath.
     */
    @TestTemplate
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

    /**
     * Like {@link #filteringJoinNullConflictThroughGroupByUnassignOne()},
     * but with many contributors mapped into a single group instead of one.
     * Confirms {@code Group}'s contributor tracking correctly identifies that the group survives
     * when only one of many contributors dies,
     * with the dying contributor's value excluded downstream.
     */
    @TestTemplate
    public void filteringJoinNullConflictThroughLargeGroupUnassignOne() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(10, 1);
        var entity = solution.getEntityList().getFirst();
        var values = solution.getValueList();

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(Function.identity(), Function.identity()))
                                        .groupBy((TestdataAllowsUnassignedValuesListValue a,
                                                TestdataAllowsUnassignedValuesListValue b) -> a.getEntity())
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(Function.identity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((groupEntity, value) -> {
                                                    Objects.requireNonNull(groupEntity);
                                                    Objects.requireNonNull(value.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            for (var value : values) {
                scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value);
            }
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().addAll(values);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, values.size());
            for (var i = values.size() - 1; i >= 0; i--) {
                scoreDirector.afterListVariableElementAssigned(entity, "valueList", values.get(i));
            }

            assertScore(scoreDirector,
                    values.stream().map(value -> assertMatch(entity, value)).toArray(AssertableMatch[]::new));

            // Unassign one of the ten contributors; the group (keyed by the shared entity) still has nine
            // and survives.
            var valueToUnassign = values.getFirst();
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, valueToUnassign);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, values.size());
            entity.getValueList().remove(valueToUnassign);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, values.size() - 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, valueToUnassign);

            assertScore(scoreDirector,
                    values.stream()
                            .filter(value -> value != valueToUnassign)
                            .map(value -> assertMatch(entity, value))
                            .toArray(AssertableMatch[]::new));
        }
    }

    /**
     * Like {@link #filteringJoinNullConflictThroughLargeGroupUnassignOne()}, but unassigning every
     * contributor in the same transaction. Confirms the group itself is correctly, fully retracted
     * downstream once its {@code TupleList} of contributors becomes empty — exercises
     * {@code Group#isEmpty()}/{@code AbstractGroupNode#killOutTuple} for a many-contributor group, not
     * just the single-contributor case the disabled-then-fixed test above already covers.
     */
    @TestTemplate
    public void filteringJoinNullConflictThroughLargeGroupUnassignAll() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(10, 1);
        var entity = solution.getEntityList().getFirst();
        var values = solution.getValueList();

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(Function.identity(), Function.identity()))
                                        .groupBy((TestdataAllowsUnassignedValuesListValue a,
                                                TestdataAllowsUnassignedValuesListValue b) -> a.getEntity())
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(Function.identity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((groupEntity, value) -> {
                                                    Objects.requireNonNull(groupEntity);
                                                    Objects.requireNonNull(value.getEntity());
                                                    return true;
                                                }))
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            for (var value : values) {
                scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value);
            }
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().addAll(values);
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, values.size());
            for (var i = values.size() - 1; i >= 0; i--) {
                scoreDirector.afterListVariableElementAssigned(entity, "valueList", values.get(i));
            }

            assertScore(scoreDirector,
                    values.stream().map(value -> assertMatch(entity, value)).toArray(AssertableMatch[]::new));

            // Unassign every contributor at once; the group must fully retract, with nothing left downstream.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            for (var value : values) {
                scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value);
            }
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, values.size());
            entity.getValueList().clear();
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 0);
            for (var i = values.size() - 1; i >= 0; i--) {
                scoreDirector.afterListVariableElementUnassigned(variableDescriptor, values.get(i));
            }

            assertScore(scoreDirector);
        }
    }

}
