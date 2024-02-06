package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListValue;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierConstraintProvider;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierSecondEntity;

import org.junit.jupiter.api.Test;

class MultiConstraintAssertionTest {

    @Test
    void checksScore() {
        var constraintVerifier = ConstraintVerifier.build(new TestdataConstraintVerifierConstraintProvider(),
                TestdataConstraintVerifierExtendedSolution.class,
                TestdataConstraintVerifierFirstEntity.class,
                TestdataConstraintVerifierSecondEntity.class);

        TestdataConstraintVerifierExtendedSolution solution = TestdataConstraintVerifierExtendedSolution.generateSolution(4, 5);

        assertThatCode(() -> constraintVerifier.verifyThat()
                .givenSolution(solution)
                .scores(HardSoftScore.of(-15, 3), "There should be no penalties"))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat()
                .givenSolution(solution)
                .scores(HardSoftScore.of(1, 1), "There should be penalties"))
                .hasMessageContaining("There should be penalties");
    }

    @Test
    void listVarUnassignedWhileAllowsUnassigned() {
        var constraintVerifier = ConstraintVerifier.build(new TestdataAllowsUnassignedListConstraintProvider(),
                TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class,
                TestdataAllowsUnassignedValuesListValue.class);

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var entity = new TestdataAllowsUnassignedValuesListEntity("eA");
        entity.setValueList(Collections.singletonList(value1));
        value1.setIndex(0);
        value1.setEntity(entity);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1, value2));

        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.of(-3), "There should be no penalties"))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.of(-2), "There should be penalties"))
                .hasMessageContaining("There should be penalties");
    }

    private static final class TestdataAllowsUnassignedListConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    penalizeEveryAssignedValue(constraintFactory),
                    penalizeEveryValue(constraintFactory)
            };
        }

        private Constraint penalizeEveryAssignedValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataAllowsUnassignedValuesListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every unassigned value");
        }

        private Constraint penalizeEveryValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEachIncludingUnassigned(TestdataAllowsUnassignedValuesListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every value");
        }

    }

    @Test
    void listVarUnassignedWhileDisallowsUnassigned() {
        var constraintVerifier = ConstraintVerifier.build(new TestdataDisallowsUnassignedListConstraintProvider(),
                TestdataListSolution.class,
                TestdataListEntity.class,
                TestdataListValue.class);

        var value1 = new TestdataListValue("v1");
        var value2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("eA", value1);
        value1.setIndex(0);
        value1.setEntity(entity);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1, value2));

        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.ofUninitialized(-1, -3), "There should be no penalties"))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.of(-3), "One value is not assigned, therefore uninitialized"))
                .hasMessageContaining("One value is not assigned, therefore uninitialized");
    }

    private static final class TestdataDisallowsUnassignedListConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    penalizeEveryAssignedValue(constraintFactory),
                    penalizeEveryValue(constraintFactory)
            };
        }

        private Constraint penalizeEveryAssignedValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every unassigned value");
        }

        private Constraint penalizeEveryValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEachIncludingUnassigned(TestdataListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every value");
        }

    }

    @Test
    void listVarUnassignedWhileDisallowsUnassigned_noInverseRelationShadowVar() {
        var constraintVerifier =
                ConstraintVerifier.build(new TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider(),
                        TestdataPinnedListSolution.class,
                        TestdataPinnedListEntity.class,
                        TestdataPinnedListValue.class);

        var value1 = new TestdataPinnedListValue("v1");
        var value2 = new TestdataPinnedListValue("v2");
        var entity = new TestdataPinnedListEntity("eA", value1);
        value1.setIndex(0);
        var solution = new TestdataPinnedListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1, value2));

        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.ofUninitialized(-1, -3), "There should be no penalties"))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.of(-3), "One value is not assigned, therefore uninitialized"))
                .hasMessageContaining("One value is not assigned, therefore uninitialized");
    }

    private static final class TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider
            implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    penalizeEveryAssignedValue(constraintFactory),
                    penalizeEveryValue(constraintFactory)
            };
        }

        private Constraint penalizeEveryAssignedValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataPinnedListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every unassigned value");
        }

        private Constraint penalizeEveryValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEachIncludingUnassigned(TestdataPinnedListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every value");
        }

    }

}
