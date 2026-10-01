package ai.timefold.solver.core.impl.score.stream.bavet;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.toList;
import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableSolution;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableValue;

import org.junit.jupiter.api.TestTemplate;

final class BavetRegressionTest extends AbstractConstraintStreamTest {

    public BavetRegressionTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

    /**
     * The plain-update predicate flip that {@code AbstractNodeBuildHelper}'s "deeper parent settles late" rule
     * exists for, and that no other test in this suite constructs.
     * <p>
     * The join key is {@link TestdataLavishEntity#getEntityGroup()}, which is not a planning variable,
     * so flipping the value changes neither the composite key nor {@code forEach} membership
     * (both values are non-null, so the entity never leaves the filtered forEach node).
     * What reaches the deferring join is therefore a plain update, not a retract and not a key change -
     * the two shapes that let {@code filteringJoinNullConflictBehindDeferringJoinUnassignOne} pass
     * with and without the rule.
     * <p>
     * The last join's inputs are one layer apart, so without the rule it reads the opposite side eagerly.
     * Its shallow input sits in a layer below the deferring join,
     * and therefore propagates into it while the deferring join's out-tuples are still unreconciled:
     * they report themselves active even though their predicate no longer holds.
     * The downstream predicate asserts that it never observes one.
     */
    @TestTemplate
    void filteringJoinPredicateFlipBehindDeferringJoin() {
        assertPredicateFlipBehindDeferringJoin(true);
    }

    /**
     * {@link #filteringJoinPredicateFlipBehindDeferringJoin()} without the {@code equal} joiner,
     * so both joins are unindexed and take {@code AbstractUnindexedJoinNode}'s cross-match path.
     */
    @TestTemplate
    void filteringJoinPredicateFlipBehindDeferringJoinUnindexed() {
        assertPredicateFlipBehindDeferringJoin(false);
    }

    private void assertPredicateFlipBehindDeferringJoin(boolean indexed) {
        var solution = buildFlipSolution();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var entityA = solution.getEntityList().get(0);
        var entityB = solution.getEntityList().get(1);

        ConstraintProvider constraintProvider = factory -> {
            var deferringJoin = factory.forEach(TestdataLavishEntity.class)
                    // Inputs two layers apart, so this join defers its cross-match.
                    .join(factory.forEach(TestdataLavishEntity.class)
                            .map(e -> e)
                            .map(e -> e),
                            indexed
                                    ? new BiJoiner[] {
                                            equal(TestdataLavishEntity::getEntityGroup,
                                                    TestdataLavishEntity::getEntityGroup),
                                            filtering((TestdataLavishEntity a, TestdataLavishEntity b) -> a
                                                    .getValue() == value1) }
                                    : new BiJoiner[] {
                                            filtering((TestdataLavishEntity a, TestdataLavishEntity b) -> a
                                                    .getValue() == value1) });
            return new Constraint[] {
                    // Inputs one layer apart, but the deeper input is the deferring join above.
                    deferringJoin.join(factory.forEach(TestdataLavishEntity.class)
                            .map(e -> e)
                            .map(e -> e),
                            indexed
                                    ? new TriJoiner[] {
                                            equal((TestdataLavishEntity a, TestdataLavishEntity b) -> a.getEntityGroup(),
                                                    TestdataLavishEntity::getEntityGroup),
                                            filtering((TestdataLavishEntity a, TestdataLavishEntity b,
                                                    TestdataLavishEntity c) -> assertNotStale(a, value1)) }
                                    : new TriJoiner[] {
                                            filtering((TestdataLavishEntity a, TestdataLavishEntity b,
                                                    TestdataLavishEntity c) -> assertNotStale(a, value1)) })
                            .penalize(SimpleScore.ONE)
                            .asConstraint(TEST_CONSTRAINT_ID)
            };
        };

        try (InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataLavishSolution.buildSolutionDescriptor(), constraintProvider)) {
            scoreDirector.setWorkingSolution(solution);
            // Both entities hold value1, so the deferring join keeps all four pairs
            // and each pairs with both entities downstream.
            assertScore(scoreDirector,
                    assertMatch(entityA, entityA, entityA), assertMatch(entityA, entityA, entityB),
                    assertMatch(entityA, entityB, entityA), assertMatch(entityA, entityB, entityB),
                    assertMatch(entityB, entityA, entityA), assertMatch(entityB, entityA, entityB),
                    assertMatch(entityB, entityB, entityA), assertMatch(entityB, entityB, entityB));

            // The flip: a plain variable change between two non-null values.
            scoreDirector.beforeVariableChanged(entityA, TestdataLavishEntity.VALUE_FIELD);
            entityA.setValue(value2);
            scoreDirector.afterVariableChanged(entityA, TestdataLavishEntity.VALUE_FIELD);

            // The deferring join's predicate no longer holds for entityA on the left.
            assertScore(scoreDirector,
                    assertMatch(entityB, entityA, entityA), assertMatch(entityB, entityA, entityB),
                    assertMatch(entityB, entityB, entityA), assertMatch(entityB, entityB, entityB));
            assertScoreMatchesFromScratch(scoreDirector, solution, constraintProvider);
        }
    }

    /**
     * Probes the shape the "deeper parent settles late" rule deliberately declines to protect:
     * a {@code map} sits between the deferring join and its consumer,
     * so the consumer's deeper input is the map rather than the deferring join
     * and no extra layer of distance is added.
     * If a plain-update flip can make the consumer observe an unreconciled tuple here,
     * the rule is incomplete and the map needs to carry its ancestor's late-settle status.
     */
    @TestTemplate
    void filteringJoinPredicateFlipBehindMappedDeferringJoin() {
        var solution = buildFlipSolution();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var entityA = solution.getEntityList().get(0);
        var entityB = solution.getEntityList().get(1);

        ConstraintProvider constraintProvider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishEntity.class)
                        // Inputs two layers apart, so this join defers.
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .map(e -> e)
                                .map(e -> e),
                                equal(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getEntityGroup),
                                filtering((a, b) -> a.getValue() == value1))
                        // A single-input node between the deferring join and its consumer.
                        .map((a, b) -> a)
                        // The deeper input is now the map, which never defers, so the rule adds nothing.
                        .join(factory.forEach(TestdataLavishEntity.class)
                                .map(e -> e)
                                .map(e -> e)
                                .map(e -> e),
                                equal(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getEntityGroup),
                                filtering((a, b) -> assertNotStale(a, value1)))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_ID)
        };

        try (InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataLavishSolution.buildSolutionDescriptor(), constraintProvider)) {
            scoreDirector.setWorkingSolution(solution);
            assertScore(scoreDirector,
                    assertMatch(entityA, entityA), assertMatch(entityA, entityB),
                    assertMatch(entityA, entityA), assertMatch(entityA, entityB),
                    assertMatch(entityB, entityA), assertMatch(entityB, entityB),
                    assertMatch(entityB, entityA), assertMatch(entityB, entityB));

            scoreDirector.beforeVariableChanged(entityA, TestdataLavishEntity.VALUE_FIELD);
            entityA.setValue(value2);
            scoreDirector.afterVariableChanged(entityA, TestdataLavishEntity.VALUE_FIELD);

            assertScore(scoreDirector,
                    assertMatch(entityB, entityA), assertMatch(entityB, entityB),
                    assertMatch(entityB, entityA), assertMatch(entityB, entityB));
            assertScoreMatchesFromScratch(scoreDirector, solution, constraintProvider);
        }
    }

    /**
     * The {@code ifExists} counterpart: the node's two inputs are two layers apart, so it defers,
     * and a plain-update flip of its filtering predicate must still be reconciled before it propagates.
     * {@code AbstractIfExistsNode} received the same deferral treatment as the join,
     * so it needs the same coverage.
     */
    @TestTemplate
    void filteringIfExistsPredicateFlipWhenDeferring() {
        assertIfExistsPredicateFlipWhenDeferring(true);
    }

    /**
     * {@link #filteringIfExistsPredicateFlipWhenDeferring()} inverted:
     * {@code ifNotExists} counts the same matches and negates the answer,
     * so a missed reconciliation shows up as the complement.
     */
    @TestTemplate
    void filteringIfNotExistsPredicateFlipWhenDeferring() {
        assertIfExistsPredicateFlipWhenDeferring(false);
    }

    private void assertIfExistsPredicateFlipWhenDeferring(boolean exists) {
        var solution = buildFlipSolution();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var entityA = solution.getEntityList().get(0);
        var entityB = solution.getEntityList().get(1);

        ConstraintProvider constraintProvider = factory -> {
            // Two maps put the left input two layers above the right, so the node defers.
            var left = factory.forEach(TestdataLavishEntity.class)
                    .map(e -> e)
                    .map(e -> e);
            var joiners = new BiJoiner[] {
                    equal(TestdataLavishEntity::getEntityGroup, TestdataLavishEntity::getEntityGroup),
                    filtering((TestdataLavishEntity a, TestdataLavishEntity b) -> b.getValue() == value1) };
            var filtered = exists
                    ? left.ifExists(TestdataLavishEntity.class, joiners)
                    : left.ifNotExists(TestdataLavishEntity.class, joiners);
            return new Constraint[] {
                    filtered.penalize(SimpleScore.ONE).asConstraint(TEST_CONSTRAINT_ID)
            };
        };

        try (InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataLavishSolution.buildSolutionDescriptor(), constraintProvider)) {
            scoreDirector.setWorkingSolution(solution);
            // Both entities hold value1, so every left tuple has a match.
            if (exists) {
                assertScore(scoreDirector, assertMatch(entityA), assertMatch(entityB));
            } else {
                assertScore(scoreDirector);
            }

            // The flip: entityA stops matching, but entityB still does, so every left tuple keeps a match.
            scoreDirector.beforeVariableChanged(entityA, TestdataLavishEntity.VALUE_FIELD);
            entityA.setValue(value2);
            scoreDirector.afterVariableChanged(entityA, TestdataLavishEntity.VALUE_FIELD);
            if (exists) {
                assertScore(scoreDirector, assertMatch(entityA), assertMatch(entityB));
            } else {
                assertScore(scoreDirector);
            }
            assertScoreMatchesFromScratch(scoreDirector, solution, constraintProvider);

            // Flip the last matching entity too, so every counter drops to zero.
            scoreDirector.beforeVariableChanged(entityB, TestdataLavishEntity.VALUE_FIELD);
            entityB.setValue(value2);
            scoreDirector.afterVariableChanged(entityB, TestdataLavishEntity.VALUE_FIELD);
            if (exists) {
                assertScore(scoreDirector);
            } else {
                assertScore(scoreDirector, assertMatch(entityA), assertMatch(entityB));
            }
            assertScoreMatchesFromScratch(scoreDirector, solution, constraintProvider);
        }
    }

    /**
     * Two entities in one group, holding the first of two values.
     * The entity group is not a planning variable, so it is a join key that a value flip cannot disturb.
     */
    private static TestdataLavishSolution buildFlipSolution() {
        var solution = TestdataLavishSolution.generateSolution(1, 0, 1, 0);
        var valueGroup = solution.getFirstValueGroup();
        var value1 = new TestdataLavishValue("MyValue 1", valueGroup);
        var value2 = new TestdataLavishValue("MyValue 2", valueGroup);
        solution.getValueList().addAll(List.of(value1, value2));
        var entityGroup = solution.getFirstEntityGroup();
        solution.getEntityList().addAll(List.of(
                new TestdataLavishEntity("MyEntity A", entityGroup, value1),
                new TestdataLavishEntity("MyEntity B", entityGroup, value1)));
        return solution;
    }

    /**
     * The deferring join upstream only emits pairs whose left entity still holds {@code expectedValue}.
     * Seeing any other one means its out-tuple was read before it had been reconciled.
     */
    private static boolean assertNotStale(TestdataLavishEntity entity, TestdataLavishValue expectedValue) {
        if (entity.getValue() != expectedValue) {
            throw new IllegalStateException(
                    "Impossible state: the entity (%s) was read behind a deferring join even though its predicate no longer holds."
                            .formatted(entity));
        }
        return true;
    }

    /**
     * {@link #assertScore} only compares the incremental session against hand-written matches;
     * under {@link ai.timefold.solver.core.config.solver.EnvironmentMode#PHASE_ASSERT}
     * nothing recalculates from scratch.
     * A wrong expectation could therefore hide a stale read, so compare against a fresh session too.
     */
    private <Solution_> void assertScoreMatchesFromScratch(InnerScoreDirector<Solution_, SimpleScore> scoreDirector,
            Solution_ solution, ConstraintProvider constraintProvider) {
        try (InnerScoreDirector<Solution_, SimpleScore> fromScratch =
                buildScoreDirector(scoreDirector.getSolutionDescriptor(), constraintProvider)) {
            fromScratch.setWorkingSolution(solution);
            assertThat(scoreDirector.calculateScore())
                    .as("The incrementally calculated score differs from the score calculated from scratch.")
                    .isEqualTo(fromScratch.calculateScore());
        }
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
     * The last join's deeper input is a {@code map}, which never defers,
     * so the rule does not add its extra layer -
     * yet that map's own ancestor is a deferring join.
     * Layers here: the deferring join is 3 (inputs 0 and 2), the map above it 4, the shallow input 3,
     * so the last join is settle distance 1 and reads eagerly.
     * That eager read does reach a doomed tuple, but the per-read {@code isActive()} guards catch it;
     * see {@link #filteringJoinPredicateFlipBehindMappedDeferringJoin()}, which pins exactly that.
     */
    @TestTemplate
    public void filteringJoinNullConflictBehindMappedDeferringJoinUnassignOne() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        // Inputs two layers apart, so this join defers.
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v)
                                                .map(v -> v),
                                                equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        // A single-input node between the deferring join and its consumer.
                                        .map((a, b) -> a)
                                        // Inputs one layer apart, so this join reads eagerly,
                                        // and its deeper input is the map rather than the deferring join.
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v)
                                                .map(v -> v)
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
                    assertMatch(value1, value1),
                    assertMatch(value1, value2),
                    assertMatch(value1, value2),
                    assertMatch(value2, value1),
                    assertMatch(value2, value1),
                    assertMatch(value2, value2),
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
     * Pins the layer shape that {@code AbstractNodeBuildHelper}'s "deeper parent settles late" rule exists for:
     * the last join's two inputs are only one layer apart (so the layer distance alone would let it read
     * eagerly), but its deeper input is itself a filtering join that defers. That parent only decides which
     * of its out-tuples the predicate dooms in its own {@code prepareForSettle()}, one layer after an
     * ordinary parent would have retracted them.
     * <p>
     * This test passes with and without that rule, so it is shape coverage, not a reproducer: here the
     * unassignment reaches the deferring join as a retract and as a composite-key change, both of which
     * retract its out-tuples eagerly and mark them non-active in time. The remaining case the rule guards,
     * where a deferring parent's own predicate flips on a plain update, is reproduced by
     * {@link #filteringJoinPredicateFlipBehindDeferringJoin()}.
     */
    @TestTemplate
    public void filteringJoinNullConflictBehindDeferringJoinUnassignOne() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().getFirst();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        // Two layers apart, so this join defers.
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v)
                                                .map(v -> v),
                                                equal(TestdataAllowsUnassignedValuesListValue::getEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    return true;
                                                }))
                                        // One layer apart, but the deeper input is the deferring join above.
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v)
                                                .map(v -> v),
                                                equal((a, b) -> a.getEntity(),
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((a, b, c) -> {
                                                    Objects.requireNonNull(a.getEntity());
                                                    Objects.requireNonNull(b.getEntity());
                                                    Objects.requireNonNull(c.getEntity());
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
                    assertMatch(value1, value1, value1),
                    assertMatch(value1, value1, value2),
                    assertMatch(value1, value2, value1),
                    assertMatch(value1, value2, value2),
                    assertMatch(value2, value1, value1),
                    assertMatch(value2, value1, value2),
                    assertMatch(value2, value2, value1),
                    assertMatch(value2, value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2, value2));
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
     * The {@code ifExists} counterpart of {@link #filteringJoinNullConflictThroughMapUnassignOne()}.
     * The same {@code .map()} producer feeds a filtering {@code ifExists} instead of a filtering join.
     * <p>
     * Not covered by the join test above: {@code AbstractIfExistsNode} drains its pending queues
     * right before left, the opposite order to {@code AbstractJoinNode}, and it reconciles through
     * {@code updateCounterLeft}/{@code updateCounterRight} against a shared {@code ExistsCounter}
     * rather than through per-pair out-tuples.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictThroughMapUnassignOne() {
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
     * The {@code ifExists} counterpart of {@link #filteringJoinNullConflictThroughFlattenUnassignOne()}.
     * The same {@code .flatten()} producer feeds a filtering {@code ifExists} instead of a filtering join.
     * <p>
     * Not covered by the join test above: {@code AbstractIfExistsNode} drains its pending queues
     * right before left, the opposite order to {@code AbstractJoinNode}, and it reconciles through
     * {@code updateCounterLeft}/{@code updateCounterRight} against a shared {@code ExistsCounter}
     * rather than through per-pair out-tuples.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictThroughFlattenUnassignOne() {
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
                                        .ifExists(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
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
                    assertMatch(value1, value1, value1),
                    assertMatch(value2, value2, value2));

            // Unassign and check result.
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
            entity.getValueList().remove(value1);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, 0, 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, value1);

            assertScore(scoreDirector,
                    assertMatch(value2, value2, value2));
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
     * The {@code ifExists} counterpart of {@link #filteringJoinNullConflictThroughGroupByUnassignOne()}.
     * The same {@code .groupBy()} producer feeds a filtering {@code ifExists} instead of a filtering join.
     * <p>
     * Not covered by the join test above: {@code AbstractIfExistsNode} drains its pending queues
     * right before left, the opposite order to {@code AbstractJoinNode}, and it reconciles through
     * {@code updateCounterLeft}/{@code updateCounterRight} against a shared {@code ExistsCounter}
     * rather than through per-pair out-tuples.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictThroughGroupByUnassignOne() {
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
     * The {@code ifExists} counterpart of {@link #filteringJoinNullConflictThroughLargeGroupUnassignOne()}.
     * <p>
     * Deliberately not a literal mirror: it uses <b>two</b> entities where the join test uses one.
     * {@code ifExists} emits the left tuple only, so with a single entity the whole constraint collapses
     * to one group and one match, and nothing but a {@code NullPointerException} could ever fail it.
     * Two groups keep the match set able to detect a wrong answer as well as a stale read.
     * <p>
     * Not covered by the join test above: {@code AbstractIfExistsNode} drains its pending queues
     * right before left, the opposite order to {@code AbstractJoinNode}, and it reconciles through
     * {@code updateCounterLeft}/{@code updateCounterRight} against a shared {@code ExistsCounter}
     * rather than through per-pair out-tuples.
     * <p>
     * There is deliberately no {@code ifExists} mirror of
     * {@link #filteringJoinNullConflictThroughLargeGroupUnassignAll()}: unassigning every contributor
     * retracts the group outright, and retraction never calls the filtering predicate on the
     * {@code ifExists} side, so that shape stays green under every way of breaking
     * {@code AbstractIfExistsNode} and would be a test that cannot fail.
     */
    @TestTemplate
    public void filteringIfExistsNullConflictThroughLargeGroupUnassignOne() {
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(10, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var values = solution.getValueList();
        var values1 = values.subList(0, 5);
        var values2 = values.subList(5, 10);

        try (InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, SimpleScore> scoreDirector =
                buildScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor(),
                        factory -> new Constraint[] {
                                factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                        .join(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal(Function.identity(), Function.identity()))
                                        .groupBy((TestdataAllowsUnassignedValuesListValue a,
                                                TestdataAllowsUnassignedValuesListValue b) -> a.getEntity(),
                                                toList((TestdataAllowsUnassignedValuesListValue a,
                                                        TestdataAllowsUnassignedValuesListValue b) -> a))
                                        .ifExists(factory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                                                .map(v -> v),
                                                equal((groupEntity, groupedValues) -> groupEntity,
                                                        TestdataAllowsUnassignedValuesListValue::getEntity),
                                                filtering((groupEntity, groupedValues, value) -> {
                                                    Objects.requireNonNull(groupEntity);
                                                    for (var groupedValue : groupedValues) {
                                                        Objects.requireNonNull(groupedValue.getEntity());
                                                    }
                                                    Objects.requireNonNull(value.getEntity());
                                                    return true;
                                                }))
                                        .map((groupEntity, groupedValues) -> groupEntity)
                                        .penalize(SimpleScore.ONE)
                                        .asConstraint(TEST_CONSTRAINT_ID)
                        })) {

            scoreDirector.setWorkingSolution(solution);
            assignAllTo(scoreDirector, entity1, values1);
            assignAllTo(scoreDirector, entity2, values2);

            assertScore(scoreDirector,
                    assertMatch(entity1),
                    assertMatch(entity2));

            // Unassign one of entity1's five contributors; its group still has four and survives,
            // and entity2's group must be left entirely alone.
            var valueToUnassign = values1.getFirst();
            var variableDescriptor = scoreDirector.getSolutionDescriptor()
                    .getListVariableDescriptor();
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, valueToUnassign);
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity1, 0, values1.size());
            entity1.getValueList().remove(valueToUnassign);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity1, 0, values1.size() - 1);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, valueToUnassign);

            assertScore(scoreDirector,
                    assertMatch(entity1),
                    assertMatch(entity2));
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

    /**
     * Assigns every value in {@code values} to {@code entity}'s list variable in one transaction,
     * firing the after-events in reverse order, exactly as the single-entity tests above do inline.
     */
    private <Solution_> void assignAllTo(InnerScoreDirector<Solution_, SimpleScore> scoreDirector,
            TestdataAllowsUnassignedValuesListEntity entity, List<TestdataAllowsUnassignedValuesListValue> values) {
        for (var value : values) {
            scoreDirector.beforeListVariableElementAssigned(entity, "valueList", value);
        }
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.getValueList().addAll(values);
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, values.size());
        for (var i = values.size() - 1; i >= 0; i--) {
            scoreDirector.afterListVariableElementAssigned(entity, "valueList", values.get(i));
        }
    }

}
