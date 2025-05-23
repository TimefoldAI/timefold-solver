package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.testdomain.TestdataValue;
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
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierJustificationProvider;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierSecondEntity;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierSolution;
import ai.timefold.solver.test.api.testdomain.justification.TestFirstJustification;
import ai.timefold.solver.test.api.testdomain.justification.TestSecondJustification;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class SingleConstraintAssertionTest {

    private final ConstraintVerifier<TestdataConstraintVerifierConstraintProvider, TestdataConstraintVerifierExtendedSolution> constraintVerifier =
            ConstraintVerifier.build(new TestdataConstraintVerifierConstraintProvider(),
                    TestdataConstraintVerifierExtendedSolution.class,
                    TestdataConstraintVerifierFirstEntity.class,
                    TestdataConstraintVerifierSecondEntity.class);

    private final ConstraintVerifier<TestdataListMultipleShadowVariableConstraintProvider, TestdataListMultipleShadowVariableSolution> shadowConstraintVerifier =
            ConstraintVerifier.build(new TestdataListMultipleShadowVariableConstraintProvider(),
                    TestdataListMultipleShadowVariableSolution.class,
                    TestdataListMultipleShadowVariableEntity.class,
                    TestdataListMultipleShadowVariableValue.class);

    private final ConstraintVerifier<TestdataConstraintVerifierJustificationProvider, TestdataConstraintVerifierSolution> constraintVerifierForJustification =
            ConstraintVerifier.build(new TestdataConstraintVerifierJustificationProvider(),
                    TestdataConstraintVerifierSolution.class,
                    TestdataConstraintVerifierFirstEntity.class);

    @Test
    void triggerVariableListenersListSingleSolution() {
        var solution = TestdataListMultipleShadowVariableSolution.generateSolution(2, 1);

        // Cascading update
        // Test cascade penalty
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeCascadingUpdate)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .penalizesBy(10)).doesNotThrowAnyException();

        // Test cascade reward
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::rewardCascadingUpdate)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .rewardsWith(20)).doesNotThrowAnyException();

        // Test cascade justification
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeCascadingUpdate)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .justifiesWith(DefaultConstraintJustification.of(SimpleScore.of(-10), solution.getValueList().get(0))))
                .doesNotThrowAnyException();

        // Test cascade indictment
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeCascadingUpdate)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .indictsWith(solution.getValueList().get(0)))
                .doesNotThrowAnyException();

        // Custom listener
        solution.getEntityList().get(0).setValueList(List.of(solution.getValueList().get(1)));
        solution.getValueList().get(0).setEntity(null);
        // Test listener penalty
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeListener)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .penalizesBy(20)).doesNotThrowAnyException();

        // Test listener reward
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::rewardListener)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .rewardsWith(40)).doesNotThrowAnyException();

        // Test listener justification
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeListener)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .justifiesWith(DefaultConstraintJustification.of(SimpleScore.of(-20), solution.getValueList().get(1))))
                .doesNotThrowAnyException();

        // Test listener indictment
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeListener)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .indictsWith(solution.getValueList().get(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void penalizesAndDoesNotReward() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
    void impactsMoreThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesMoreThan("There should be penalties", 0))
                .hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesMoreThan("There should be no penalties", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesMoreThan("There should be penalties", 1))
                .hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesMoreThan("There should only be one penalty", 2))
                .hasMessageContaining("There should only be one penalty")
                .hasMessageContaining("Expected penalty");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .rewardsMoreThan("There should not be rewards", 1))
                .hasMessageContaining("There should not be rewards")
                .hasMessageContaining("Expected reward");

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsMoreThan("There should be no rewards", 0))
                .hasMessageContaining("There should be no rewards")
                .hasMessageContaining("Expected reward more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsMoreThan("There should be no rewards", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsMoreThan("There should be rewards", 1))
                .hasMessageContaining("There should be rewards")
                .hasMessageContaining("Expected reward more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsMoreThan("There should only be one reward", 2))
                .hasMessageContaining("There should only be one reward")
                .hasMessageContaining("Expected reward");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .penalizesMoreThan("There should not be penalties", 1))
                .hasMessageContaining("There should not be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void impactsLessThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesLessThan("There should be penalties", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesLessThan("There should be no penalties", 1))
                .hasMessageContaining("There should be no penalties")
                .hasMessageContaining("Expected penalty less than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesLessThan("There should be less than 2 rewards", 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesLessThan("There be less than 3 rewards", 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .rewardsLessThan("There should be 1 reward", 2))
                .doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsLessThan("There should be no rewards", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsLessThan("There should be no rewards", 1))
                .hasMessageContaining("There should be no rewards")
                .hasMessageContaining("Expected reward less than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsLessThan("There should be less than 2 rewards", 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsLessThan("There should be less than 2 rewards", 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .penalizesLessThan("There should not be penalties", 2))
                .doesNotThrowAnyException();
    }

    @Test
    void impactsBy() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesBy("There should be no penalties", 0))
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
                .rewardsWith("There should be no rewards", 0))
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
    void impactsByMoreThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesByMoreThan("There should be no penalties", 0))
                .hasMessageContaining("There should be no penalties")
                .hasMessageContaining("Expected penalty more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesByMoreThan("There should be no penalties", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesByMoreThan("There should be penalties", 1))
                .hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesByMoreThan("There should only be one penalty", 2))
                .hasMessageContaining("There should only be one penalty")
                .hasMessageContaining("Expected penalty more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .rewardsWithMoreThan("There should not be rewards", 1))
                .hasMessageContaining("There should not be rewards")
                .hasMessageContaining("Expected reward");

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsWithMoreThan("There should be no rewards", 0))
                .hasMessageContaining("There should be no rewards")
                .hasMessageContaining("Expected reward more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWithMoreThan("There should be no rewards", 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWithMoreThan("There should be rewards", 1))
                .hasMessageContaining("There should be rewards")
                .hasMessageContaining("Expected reward more than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWithMoreThan("There should only be one reward", 2))
                .hasMessageContaining("There should only be one reward")
                .hasMessageContaining("Expected reward");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .penalizesByMoreThan("There should not be penalties", 1))
                .hasMessageContaining("There should not be penalties")
                .hasMessageContaining("Expected penalty");
    }

    @Test
    void impactsByLessThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesByLessThan("There should be no penalties", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesByLessThan("There should be penalties", 1))
                .hasMessageContaining("There should be penalties")
                .hasMessageContaining("Expected penalty less than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesByLessThan("There should be penalties", 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .penalizesByLessThan("There should only be one penalty", 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("A", new TestdataValue()))
                .rewardsWithLessThan("There should not be rewards", 2))
                .doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsWithLessThan("There should be no rewards", 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWithLessThan("There should be no rewards", 1))
                .hasMessageContaining("There should be no rewards")
                .hasMessageContaining("Expected reward less than");
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWithLessThan("There should be rewards", 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .rewardsWithLessThan("There should only be one reward", 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity("B", new TestdataValue()))
                .penalizesByLessThan("There should not be penalties", 2))
                .doesNotThrowAnyException();
    }

    @Test
    void penalizesByCountAndDoesNotReward() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
        var verifier =
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

        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be no penalties", 0))
                .hasMessageContaining("There should be no penalties");
        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be no penalties", 1))
                .hasMessageContaining("There should be no penalties");

        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();
        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 2)).doesNotThrowAnyException();
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
        var verifier =
                ConstraintVerifier.build(new TestdataDisallowsUnassignedListConstraintProvider(),
                        TestdataListSolution.class,
                        TestdataListEntity.class,
                        TestdataListValue.class);

        var value1 = new TestdataListValue("v1");
        var value2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("eA", value1);
        value1.setIndex(0);
        value1.setEntity(entity);

        assertThatCode(() -> verifier
                .verifyThat(TestdataDisallowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();
        assertThatCode(() -> verifier
                .verifyThat(TestdataDisallowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 2)).doesNotThrowAnyException();
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
        var verifier =
                ConstraintVerifier.build(new TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider(),
                        TestdataPinnedNoShadowsListSolution.class,
                        TestdataPinnedNoShadowsListEntity.class,
                        TestdataPinnedNoShadowsListValue.class);

        var value1 = new TestdataPinnedNoShadowsListValue("v1");
        var value2 = new TestdataPinnedNoShadowsListValue("v2");
        var entity = new TestdataPinnedNoShadowsListEntity("eA", value1);
        value1.setIndex(0);

        assertThatCode(() -> verifier
                .verifyThat(
                        TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 1)).doesNotThrowAnyException();
        assertThatCode(() -> verifier
                .verifyThat(TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes("There should be penalties", 2)).doesNotThrowAnyException();
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

    @Test
    void justifies() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWith(new TestFirstJustification("2")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:");

        // Multiple justifications
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestFirstJustification("Generated Entity 0"),
                                new TestFirstJustification("2")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:");

        // Invalid matches and classes
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestFirstJustification("Generated Entity 0"), new TestFirstJustification("2"),
                                new TestSecondJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestSecondJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("TestSecondJustification[id=1]")
                .hasMessageContaining("TestSecondJustification[id=1]");
    }

    @Test
    void justifiesWithCustomMessage() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith("Custom Message", new TestFirstJustification("2")))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWith("Custom Message", new TestFirstJustification("2")))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:");
    }

    @Test
    void justifiesEmptyMatches() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
                .hasMessageContaining("Expected")
                .hasMessageContaining("No Justification")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Unexpected but found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .justifiesWith(new TestFirstJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("No Justification")
                .hasMessageContaining("Expected but not found:");
    }

    @Test
    void justifiesExactly() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        // No error
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWithExactly(new TestFirstJustification("Generated Entity 0"),
                                new TestFirstJustification("Generated Entity 1"),
                                new TestFirstJustification("Generated Entity 2")))
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWithExactly(new TestFirstJustification("Generated Entity 0"),
                                new TestFirstJustification("Generated Entity 1"),
                                new TestFirstJustification("Generated Entity 2")))
                .doesNotThrowAnyException();

        // Different justification
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWithExactly(new TestFirstJustification("2")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("Unexpected but found:");
    }

    @Test
    void justifiesExactlyWithCustomMessage() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWithExactly("Custom Message", new TestFirstJustification("2")))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("Unexpected but found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .justifiesWithExactly("Custom Message", new TestFirstJustification("2")))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Justification: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=2]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("Unexpected but found:");
    }

    @Test
    void justifiesExactlyEmptyMatches() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .justifiesWithExactly())
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .justifiesWithExactly())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Expected")
                .hasMessageContaining("No Justification")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 0]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 1]")
                .hasMessageContaining("TestFirstJustification[id=Generated Entity 2]")
                .hasMessageContaining("Unexpected but found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .justifiesWithExactly(new TestFirstJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("No Justification")
                .hasMessageContaining("Expected but not found:");
    }

    @Test
    void indicts() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
        var badEntity =
                new TestdataConstraintVerifierFirstEntity("bad code", new TestdataValue("bad code"));
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith(badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:");

        // Multiple indictments
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith(solution.getEntityList().get(0), badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:");

        // Invalid matches and classes
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(solution.getEntityList().get(0), badEntity, "bad indictment"))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 0')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='bad code')")
                .hasMessageContaining("bad indictment")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 0')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 1')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 2')")
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='bad code')")
                .hasMessageContaining("bad indictment");
    }

    @Test
    void indictsWithCustomMessage() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        var badEntity =
                new TestdataConstraintVerifierFirstEntity("bad code", new TestdataValue("bad code"));
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWith("Custom Message", badEntity))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWith("Custom Message", badEntity))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:");
    }

    @Test
    void indictEmptyMatches() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

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
                .hasMessageContaining("Expected")
                .hasMessageContaining("No Indictment")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 0')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 1')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 2')")
                .hasMessageContaining("Unexpected but found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .indictsWith(new TestFirstJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("No Indictment")
                .hasMessageContaining("Expected but not found:");
    }

    @Test
    void indictsWithExactly() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        // No error
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWithExactly(solution.getEntityList().toArray()))
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWithExactly(solution.getEntityList().toArray()))
                .doesNotThrowAnyException();

        // Invalid indictment
        var badEntity =
                new TestdataConstraintVerifierFirstEntity("bad code", new TestdataValue("bad code"));
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWithExactly(badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWithExactly(badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("Unexpected but found:");
    }

    @Test
    void indictsWithExactlyWithCustomMessage() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        var badEntity =
                new TestdataConstraintVerifierFirstEntity("bad code", new TestdataValue("bad code"));
        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWithExactly("Custom Message", badEntity))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("Unexpected but found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .givenSolution(solution)
                        .indictsWithExactly("Custom Message", badEntity))
                .hasMessageContaining("Custom Message")
                .hasMessageContaining(
                        "Indictment: ai.timefold.solver.test.api.testdomain/Justify with first justification")
                .hasMessageContaining("Expected")
                .hasMessageContaining(badEntity.toString())
                .hasMessageContaining("Actual")
                .hasMessageContaining(solution.getEntityList().get(0).toString())
                .hasMessageContaining(solution.getEntityList().get(1).toString())
                .hasMessageContaining(solution.getEntityList().get(2).toString())
                .hasMessageContaining("Expected but not found:")
                .hasMessageContaining("Unexpected but found:");
    }

    @Test
    void indictsWithExactlyEmptyMatches() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .indictsWithExactly())
                .doesNotThrowAnyException();

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithFirstJustification)
                        .given(solution.getEntityList().toArray())
                        .indictsWithExactly())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Expected")
                .hasMessageContaining("No Indictment")
                .hasMessageContaining("Actual")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 0')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 1')")
                .hasMessageContaining("TestdataConstraintVerifierFirstEntity(code='Generated Entity 2')")
                .hasMessageContaining("Unexpected but found:");

        assertThatCode(
                () -> constraintVerifierForJustification
                        .verifyThat(TestdataConstraintVerifierJustificationProvider::justifyWithNoJustifications)
                        .given(solution.getEntityList().toArray())
                        .indictsWithExactly(new TestFirstJustification("1")))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Expected")
                .hasMessageContaining("TestFirstJustification[id=1]")
                .hasMessageContaining("Actual")
                .hasMessageContaining("No Indictment")
                .hasMessageContaining("Expected but not found:");
    }
}
