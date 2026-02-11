package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.noshadows.TestdataPinnedNoShadowsListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.noshadows.TestdataPinnedNoShadowsListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.noshadows.TestdataPinnedNoShadowsListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableEntity;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableSolution;
import ai.timefold.solver.core.testdomain.shadow.multiplelistener.TestdataListMultipleShadowVariableValue;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierConstraintProvider;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierSecondEntity;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class MultiConstraintAssertionTest {

    @Test
    void triggerVariableListenersListSingleSolution() {
        var constraintVerifier = ConstraintVerifier.build(new TestdataListMultipleShadowVariableConstraintProvider(),
                TestdataListMultipleShadowVariableSolution.class,
                TestdataListMultipleShadowVariableEntity.class,
                TestdataListMultipleShadowVariableValue.class);

        var solution = TestdataListMultipleShadowVariableSolution.generateSolution(1, 1);
        assertThatCode(() -> constraintVerifier.verifyThat()
                .givenSolution(solution)
                .settingAllShadowVariables()
                .scores(SimpleScore.of(30))) // -10 + 20 - 20 + 40
                .doesNotThrowAnyException();
    }

    @Test
    void checksScore() {
        var constraintVerifier = ConstraintVerifier.build(new TestdataConstraintVerifierConstraintProvider(),
                TestdataConstraintVerifierExtendedSolution.class,
                TestdataConstraintVerifierFirstEntity.class,
                TestdataConstraintVerifierSecondEntity.class);

        var solution = TestdataConstraintVerifierExtendedSolution.generateSolution(4, 5);

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
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
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
                .scores(SimpleScore.of(-3), "There should be no penalties"))
                .doesNotThrowAnyException();
    }

    private static final class TestdataDisallowsUnassignedListConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
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
                        TestdataPinnedNoShadowsListSolution.class,
                        TestdataPinnedNoShadowsListEntity.class,
                        TestdataPinnedNoShadowsListValue.class);

        var value1 = new TestdataPinnedNoShadowsListValue("v1");
        var value2 = new TestdataPinnedNoShadowsListValue("v2");
        var entity = new TestdataPinnedNoShadowsListEntity("eA", value1);
        value1.setIndex(0);
        var solution = new TestdataPinnedNoShadowsListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1, value2));

        assertThatCode(() -> constraintVerifier
                .verifyThat()
                .givenSolution(solution)
                .scores(SimpleScore.of(-3), "There should be no penalties"))
                .doesNotThrowAnyException();
    }

    private static final class TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider
            implements ConstraintProvider {

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    penalizeEveryAssignedValue(constraintFactory),
                    penalizeEveryValue(constraintFactory)
            };
        }

        private Constraint penalizeEveryAssignedValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataPinnedNoShadowsListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every unassigned value");
        }

        private Constraint penalizeEveryValue(ConstraintFactory constraintFactory) {
            return constraintFactory.forEachIncludingUnassigned(TestdataPinnedNoShadowsListValue.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Penalize every value");
        }

    }

}
