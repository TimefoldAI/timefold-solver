package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.Collections;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows.TestdataPinnedNoShadowsListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows.TestdataPinnedNoShadowsListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows.TestdataPinnedNoShadowsListValue;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierConstraintProvider;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierJustificationProvider;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierSecondEntity;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierSolution;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestFirstJustification;
import ai.timefold.solver.test.api.score.stream.testdata.justification.TestSecondJustification;

import org.junit.jupiter.api.Test;

class SingleConstraintAssertionTest {

    private final ConstraintVerifier<TestdataConstraintVerifierConstraintProvider, TestdataConstraintVerifierExtendedSolution> constraintVerifier =
            ConstraintVerifier.build(new TestdataConstraintVerifierConstraintProvider(),
                    TestdataConstraintVerifierExtendedSolution.class,
                    TestdataConstraintVerifierFirstEntity.class,
                    TestdataConstraintVerifierSecondEntity.class);

    private final ConstraintVerifier<TestdataConstraintVerifierJustificationProvider, TestdataConstraintVerifierSolution> constraintVerifierForJustification =
            ConstraintVerifier.build(new TestdataConstraintVerifierJustificationProvider(),
                    TestdataConstraintVerifierSolution.class,
                    TestdataConstraintVerifierFirstEntity.class);

    @Test
    void penalizesAndDoesNotReward() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes("There should be penalties.")).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards("There should be rewards")).hasMessageContaining("There should be rewards")
                .hasMessageContaining("Expected reward");
    }

    @Test
    void rewardsButDoesNotPenalize() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards("There should be rewards")).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes("There should be penalties.")).hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void impacts() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizes("There should be no penalties", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizes("There should be no penalties", 0))
                .hasMessageContaining("There should be no penalties")
                .hasMessageContaining("Expected penalty");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizes("There should be penalties", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizes("There should only be one penalty", 2))
                .hasMessageContaining("There should only be one penalty")
                .hasMessageContaining("Expected penalty");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .rewards("There should not be rewards", 1))
                .hasMessageContaining("There should not be rewards")
                .hasMessageContaining("Expected reward");

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewards("There should be no rewards", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewards("There should be no rewards", 0))
                .hasMessageContaining("There should be no rewards")
                .hasMessageContaining("Expected reward");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewards("There should be rewards", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewards("There should only be one reward", 2))
                .hasMessageContaining("There should only be one reward")
                .hasMessageContaining("Expected reward");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .penalizes("There should not be penalties", 1))
                .hasMessageContaining("There should not be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void impactsBy() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesBy("There should no penalties", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesBy("There should be no penalties", 0))
                .hasMessageContaining("There should be no penalties")
                .hasMessageContaining("Expected penalty");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesBy("There should be penalties", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesBy("There should only be one penalty", 2))
                .hasMessageContaining("There should only be one penalty")
                .hasMessageContaining("Expected penalty");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .rewardsWith("There should not be rewards", 1))
                .hasMessageContaining("There should not be rewards")
                .hasMessageContaining("Expected reward");

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsWith("There should no rewards", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWith("There should be no rewards", 0))
                .hasMessageContaining("There should be no rewards")
                .hasMessageContaining("Expected reward");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWith("There should be rewards", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWith("There should only be one reward", 2))
                .hasMessageContaining("There should only be one reward")
                .hasMessageContaining("Expected reward");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .penalizesBy("There should not be penalties", 1))
                .hasMessageContaining("There should not be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void penalizesByCountAndDoesNotReward() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes("There should be penalties.", 3)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards("There should be rewards", 1)).hasMessageContaining("There should be rewards")
                .hasMessageContaining("Expected reward");
    }

    @Test
    void penalizesByBigDecimal() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizesBy("There should be penalties.", BigDecimal.valueOf(3))).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizesBy("There should be penalties.", new BigDecimal("3.01")))
                .hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void rewardsByCountButDoesNotPenalize() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards("There should be rewards", 3)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes("There should be penalties.", 1)).hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void rewardsByBigDecimal() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewardsWith("There should be rewards", BigDecimal.valueOf(3))).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewardsWith("There should be rewards.", new BigDecimal("3.01")))
                .hasMessageContaining("There should be rewards")
                .hasMessageContaining("Expected reward");
    }

    @Test
    void uniquePairShouldWorkOnStringPlanningId() {
        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataConstraintVerifierConstraintProvider::differentStringEntityHaveDifferentValues)
                .given(new TestdataConstraintVerifierSecondEntity("A", "1"),
                        new TestdataConstraintVerifierSecondEntity("B", "1"))
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataConstraintVerifierConstraintProvider::differentStringEntityHaveDifferentValues)
                .given(new TestdataConstraintVerifierSecondEntity("A", "1"),
                        new TestdataConstraintVerifierSecondEntity("B", "1"))
                .rewards("There should be rewards", 1)).hasMessageContaining("There should be rewards")
                .hasMessageContaining("Expected reward");
    }

    @Test
    void listVarUnassignedWhileAllowsUnassigned() {
        var constraintVerifier =
                ConstraintVerifier.build(new TestdataAllowsUnassignedListConstraintProvider(),
                        TestdataAllowsUnassignedValuesListSolution.class,
                        TestdataAllowsUnassignedValuesListEntity.class,
                        TestdataAllowsUnassignedValuesListValue.class);

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var entity = new TestdataAllowsUnassignedValuesListEntity("eA");
        entity.setValueList(Collections.singletonList(value1));
        value1.setIndex(0);
        value1.setEntity(entity);

        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be no penalties", 0))
                .hasMessageContaining("There should be no penalties");
        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be no penalties", 1))
                .hasMessageContaining("There should be no penalties");

        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 2)).doesNotThrowAnyException();
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
        var constraintVerifier =
                ConstraintVerifier.build(new TestdataDisallowsUnassignedListConstraintProvider(),
                        TestdataListSolution.class,
                        TestdataListEntity.class,
                        TestdataListValue.class);

        var value1 = new TestdataListValue("v1");
        var value2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("eA", value1);
        value1.setIndex(0);
        value1.setEntity(entity);

        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataDisallowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataDisallowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 2)).doesNotThrowAnyException();
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
                        TestdataPinnedNoShadowsListSolution.class,
                        TestdataPinnedNoShadowsListEntity.class,
                        TestdataPinnedNoShadowsListValue.class);

        var value1 = new TestdataPinnedNoShadowsListValue("v1");
        var value2 = new TestdataPinnedNoShadowsListValue("v2");
        var entity = new TestdataPinnedNoShadowsListEntity("eA", value1);
        value1.setIndex(0);

        assertThatCode(() -> constraintVerifier
                .verifyThat(
                        TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 2)).doesNotThrowAnyException();
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

    @Test
    void justifies() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        // No error
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestFirstJustification("Generated Entity 0"),
                                new TestFirstJustification("Generated Entity 1")))
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWith(new TestFirstJustification("Generated Entity 0"),
                                new TestFirstJustification("Generated Entity 1")))
                .doesNotThrowAnyException();

        // Different justification
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestFirstJustification("2")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWith(new TestFirstJustification("2")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");
    }

    @Test
    void justifiesWithCustomMessage() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith("Custom Message", new TestFirstJustification("2")))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWith("Custom Message", new TestFirstJustification("2")))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");
    }

    @Test
    void justifiesWithNoMatch() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestSecondJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("No match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestSecondJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWith(new TestSecondJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("No match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestSecondJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");
    }

    @Test
    void justifiesEmptyMatches() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith())
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("No Justification")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestFirstJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("No Justification");
    }

    @Test
    void indicts() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        // No error
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(solution.getEntityList().toArray()))
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith(solution.getEntityList().toArray()))
                .doesNotThrowAnyException();

        // Invalid indictment
        TestdataConstraintVerifierFirstEntity badEntity =
                new TestdataConstraintVerifierFirstEntity("bad code", new TestdataValue("bad code"));
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString());

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith(badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString());
    }

    @Test
    void indictsWithCustomMessage() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        TestdataConstraintVerifierFirstEntity badEntity =
                new TestdataConstraintVerifierFirstEntity("bad code", new TestdataValue("bad code"));
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith("Custom Message", badEntity))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString());

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith("Custom Message", badEntity))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString());
    }

    @Test
    void indictsWithNoMatch() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(solution.getEntityList().get(0), "bad indictment"))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("No match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("bad indictment")
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString());

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith(solution.getEntityList().get(0), "bad indictment"))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.score.stream.testdata/Justify with first justification")
                .hasMessageContaining("No match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("bad indictment")
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString());
    }

    @Test
    void indictEmptyMatches() {
        TestdataConstraintVerifierSolution solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .indictsWith())
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("No Indictment")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 0')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 1')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 2')");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(new TestFirstJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Invalid match")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("No Indictment");
    }
}
