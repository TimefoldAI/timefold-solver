package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
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
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyEntity;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyValue;
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

    private final ConstraintVerifier<TestdataDependencyConstraintProvider, TestdataDependencySolution> constraintVerifierForConsistency =
            ConstraintVerifier.build(new TestdataDependencyConstraintProvider(),
                    TestdataDependencySolution.class,
                    TestdataDependencyEntity.class,
                    TestdataDependencyValue.class);

    private static final String REWARD_CODE = "A";
    private static final String PENALIZE_CODE = "B";

    private static final String THERE_SHOULD_BE_PENALTIES = "There should be penalties";
    private static final String THERE_SHOULD_BE_NO_PENALTIES = "There should be no penalties";
    private static final String EXPECTED_PENALTY_MORE_THAN = "Expected penalty more than";
    private static final String EXPECTED_PENALTY_LESS_THAN = "Expected penalty less than";
    private static final String EXPECTED_EXACTLY_ONE_PENALTY = "There should only be one penalty";
    private static final String EXPECTED_PENALTY = "Expected penalty";

    private static final String THERE_SHOULD_BE_REWARDS = "There should be rewards";
    private static final String THERE_SHOULD_BE_NO_REWARDS = "There should be no rewards";
    private static final String EXPECTED_REWARD_MORE_THAN = "Expected reward more than";
    private static final String EXPECTED_REWARD_LESS_THAN = "Expected reward less than";
    private static final String EXPECTED_EXACTLY_ONE_REWARD = "There should only be one reward";
    private static final String EXPECTED_REWARD = "Expected reward";

    private static final String THERE_SHOULD_BE_NO_IMPACT = "There should be no impact";

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
                .justifiesWith(DefaultConstraintJustification.of(SimpleScore.of(-10), solution.getValueList().getFirst())))
                .doesNotThrowAnyException();

        // Test cascade indictment
        assertThatCode(() -> shadowConstraintVerifier
                .verifyThat(TestdataListMultipleShadowVariableConstraintProvider::penalizeCascadingUpdate)
                .givenSolution(solution)
                .settingAllShadowVariables()
                .indictsWith(solution.getValueList().getFirst()))
                .doesNotThrowAnyException();
    }

    @Test
    void failsIfNonGivenEntitiesAreReferenced() {
        var startTime = LocalDate.of(2000, 1, 1).atTime(LocalTime.MIDNIGHT);
        var entity = new TestdataDependencyEntity(startTime);
        var dependency = new TestdataDependencyValue("dependency", Duration.ofHours(1));
        var previous = new TestdataDependencyValue("previous", Duration.ofHours(1));
        var current = new TestdataDependencyValue("current", Duration.ofHours(1), List.of(dependency));

        dependency.setEntity(entity);
        previous.setEntity(entity);
        current.setEntity(entity);

        dependency.setPreviousValue(previous);
        current.setPreviousValue(dependency);

        assertThatCode(() -> constraintVerifierForConsistency
                .verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .given(current)
                // current should have an end time 3 hours pass the start time of the entity
                .penalizesBy(180))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(
                        "Found referenced entities that were not given",
                        "The entity's (current{endTime=null}) shadow variable (startTime) refers to a declarative shadow variable on a non-given entity (dependency{endTime=null})",
                        "The entity's (dependency{endTime=null}) shadow variable (startTime) refers to a declarative shadow variable on a non-given entity (previous{endTime=null})");
    }

    @Test
    void penalizesAndDoesNotReward() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes(THERE_SHOULD_BE_PENALTIES)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards(THERE_SHOULD_BE_REWARDS)).hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void rewardsButDoesNotPenalize() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards(THERE_SHOULD_BE_REWARDS)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes(THERE_SHOULD_BE_PENALTIES)).hasMessageContaining(THERE_SHOULD_BE_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);
    }

    @Test
    void hasNoImpact() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        // Test with no entities - should have no impact
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given()
                .hasNoImpact(THERE_SHOULD_BE_NO_IMPACT)).doesNotThrowAnyException();

        // Test without custom message - no entities
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given()
                .hasNoImpact()).doesNotThrowAnyException();

        // Test with entities that trigger penalties - should fail
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .hasNoImpact(THERE_SHOULD_BE_NO_IMPACT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(THERE_SHOULD_BE_NO_IMPACT)
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");

        // Test with entities that trigger penalties - without custom message
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .hasNoImpact())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");

        // Test with entities that trigger rewards - should fail
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .hasNoImpact(THERE_SHOULD_BE_NO_IMPACT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(THERE_SHOULD_BE_NO_IMPACT)
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");

        // Test with entities that trigger rewards - without custom message
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .hasNoImpact())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");
    }

    @Test
    void hasNoImpactWithMixedConstraint() {
        // Test mixed constraint with no entities - should have no impact
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .hasNoImpact()).doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .hasNoImpact(THERE_SHOULD_BE_NO_IMPACT)).doesNotThrowAnyException();

        // Test mixed constraint with entities that cause penalties - should fail
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .hasNoImpact(THERE_SHOULD_BE_NO_IMPACT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(THERE_SHOULD_BE_NO_IMPACT)
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");

        // Test mixed constraint with entities that cause penalties - without custom message
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .hasNoImpact())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");

        // Test mixed constraint with entities that cause rewards - should fail
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .hasNoImpact(THERE_SHOULD_BE_NO_IMPACT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(THERE_SHOULD_BE_NO_IMPACT)
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");

        // Test mixed constraint with entities that cause rewards - without custom message
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .hasNoImpact())
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining("Constraint")
                .hasMessageContaining("Expected")
                .hasMessageContaining("no impact")
                .hasMessageContaining("Actual impact");
    }

    @Test
    void impacts() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewards(THERE_SHOULD_BE_NO_REWARDS, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewards(THERE_SHOULD_BE_NO_REWARDS, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewards(THERE_SHOULD_BE_REWARDS, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewards(EXPECTED_EXACTLY_ONE_REWARD, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_REWARD)
                .hasMessageContaining(EXPECTED_REWARD);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .penalizes(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizes(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizes(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizes(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizes(EXPECTED_EXACTLY_ONE_PENALTY, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_PENALTY)
                .hasMessageContaining(EXPECTED_PENALTY);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .rewards(THERE_SHOULD_BE_NO_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void impactsMoreThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsMoreThan(THERE_SHOULD_BE_REWARDS, 0))
                .hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsMoreThan(THERE_SHOULD_BE_NO_REWARDS, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsMoreThan(THERE_SHOULD_BE_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsMoreThan(EXPECTED_EXACTLY_ONE_REWARD, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_REWARD)
                .hasMessageContaining(EXPECTED_REWARD);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .penalizesMoreThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesMoreThan(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesMoreThan(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesMoreThan(THERE_SHOULD_BE_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesMoreThan(EXPECTED_EXACTLY_ONE_PENALTY, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_PENALTY)
                .hasMessageContaining(EXPECTED_PENALTY);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .rewardsMoreThan(THERE_SHOULD_BE_NO_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void impactsLessThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsLessThan(THERE_SHOULD_BE_REWARDS, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsLessThan(THERE_SHOULD_BE_NO_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD_LESS_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsLessThan("There should be less than 2 rewards", 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsLessThan("There be less than 3 rewards", 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .penalizesLessThan("There should be 1 reward", 2))
                .doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesLessThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesLessThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY_LESS_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesLessThan("There should be less than 2 penalities", 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesLessThan("There should be less than 2 penalities", 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .rewardsLessThan(THERE_SHOULD_BE_NO_REWARDS, 2))
                .doesNotThrowAnyException();
    }

    @Test
    void impactsBy() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsWith(THERE_SHOULD_BE_NO_REWARDS, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWith(THERE_SHOULD_BE_NO_REWARDS, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWith(THERE_SHOULD_BE_REWARDS, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWith(EXPECTED_EXACTLY_ONE_REWARD, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_REWARD)
                .hasMessageContaining(EXPECTED_REWARD);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .penalizesBy(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesBy(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesBy(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesBy(THERE_SHOULD_BE_PENALTIES, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesBy(EXPECTED_EXACTLY_ONE_PENALTY, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_PENALTY)
                .hasMessageContaining(EXPECTED_PENALTY);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .rewardsWith(THERE_SHOULD_BE_NO_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void impactsByMoreThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsWithMoreThan(THERE_SHOULD_BE_NO_REWARDS, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWithMoreThan(THERE_SHOULD_BE_NO_REWARDS, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWithMoreThan(THERE_SHOULD_BE_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWithMoreThan(EXPECTED_EXACTLY_ONE_REWARD, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_REWARD)
                .hasMessageContaining(EXPECTED_REWARD_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .penalizesByMoreThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesByMoreThan(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesByMoreThan(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesByMoreThan(THERE_SHOULD_BE_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY_MORE_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesByMoreThan(EXPECTED_EXACTLY_ONE_PENALTY, 2))
                .hasMessageContaining(EXPECTED_EXACTLY_ONE_PENALTY)
                .hasMessageContaining(EXPECTED_PENALTY);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .rewardsWithMoreThan(THERE_SHOULD_BE_NO_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void impactsByLessThan() {
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .rewardsWithLessThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWithLessThan(THERE_SHOULD_BE_REWARDS, 1))
                .hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD_LESS_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWithLessThan(THERE_SHOULD_BE_REWARDS, 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .rewardsWithLessThan(EXPECTED_EXACTLY_ONE_REWARD, 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(REWARD_CODE, new TestdataValue()))
                .penalizesByLessThan(THERE_SHOULD_BE_NO_PENALTIES, 2))
                .doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given()
                .penalizesByLessThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesByLessThan(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY_LESS_THAN);
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesByLessThan(THERE_SHOULD_BE_PENALTIES, 2))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .penalizesByLessThan(EXPECTED_EXACTLY_ONE_PENALTY, 3))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::impactEveryEntity)
                .given(new TestdataConstraintVerifierFirstEntity(PENALIZE_CODE, new TestdataValue()))
                .rewardsWithLessThan(THERE_SHOULD_BE_NO_REWARDS, 2))
                .doesNotThrowAnyException();
    }

    @Test
    void penalizesByCountAndDoesNotReward() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes(THERE_SHOULD_BE_PENALTIES, 3)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards(THERE_SHOULD_BE_REWARDS, 1)).hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void penalizesByBigDecimal() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizesBy(THERE_SHOULD_BE_PENALTIES, BigDecimal.valueOf(3))).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::penalizeEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizesBy(THERE_SHOULD_BE_PENALTIES, new BigDecimal("3.01")))
                .hasMessageContaining(THERE_SHOULD_BE_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);
    }

    @Test
    void rewardsByCountButDoesNotPenalize() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewards(THERE_SHOULD_BE_REWARDS, 3)).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .penalizes(THERE_SHOULD_BE_PENALTIES, 1)).hasMessageContaining(THERE_SHOULD_BE_PENALTIES)
                .hasMessageContaining(EXPECTED_PENALTY);
    }

    @Test
    void rewardsByBigDecimal() {
        var solution = TestdataConstraintVerifierSolution.generateSolution(2, 3);

        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewardsWith(THERE_SHOULD_BE_REWARDS, BigDecimal.valueOf(3))).doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat(TestdataConstraintVerifierConstraintProvider::rewardEveryEntity)
                .given(solution.getEntityList().toArray())
                .rewardsWith(THERE_SHOULD_BE_REWARDS, new BigDecimal("3.01")))
                .hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
    }

    @Test
    void uniquePairShouldWorkOnStringPlanningId() {
        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataConstraintVerifierConstraintProvider::differentStringEntityHaveDifferentValues)
                .given(new TestdataConstraintVerifierSecondEntity("A", "1"),
                        new TestdataConstraintVerifierSecondEntity("B", "1"))
                .penalizes(THERE_SHOULD_BE_PENALTIES, 1)).doesNotThrowAnyException();

        assertThatCode(() -> constraintVerifier
                .verifyThat(TestdataConstraintVerifierConstraintProvider::differentStringEntityHaveDifferentValues)
                .given(new TestdataConstraintVerifierSecondEntity("A", "1"),
                        new TestdataConstraintVerifierSecondEntity("B", "1"))
                .rewards(THERE_SHOULD_BE_REWARDS, 1)).hasMessageContaining(THERE_SHOULD_BE_REWARDS)
                .hasMessageContaining(EXPECTED_REWARD);
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
                .penalizes(THERE_SHOULD_BE_NO_PENALTIES, 0))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES);
        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes(THERE_SHOULD_BE_NO_PENALTIES, 1))
                .hasMessageContaining(THERE_SHOULD_BE_NO_PENALTIES);

        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryAssignedValue)
                .given(entity, value1, value2)
                .penalizes(THERE_SHOULD_BE_PENALTIES, 1)).doesNotThrowAnyException();
        assertThatCode(() -> verifier
                .verifyThat(TestdataAllowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes(THERE_SHOULD_BE_PENALTIES, 2)).doesNotThrowAnyException();
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
                .penalizes(THERE_SHOULD_BE_PENALTIES, 1)).doesNotThrowAnyException();
        assertThatCode(() -> verifier
                .verifyThat(TestdataDisallowsUnassignedListConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes(THERE_SHOULD_BE_PENALTIES, 2)).doesNotThrowAnyException();
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
                .penalizes(THERE_SHOULD_BE_PENALTIES, 1)).doesNotThrowAnyException();
        assertThatCode(() -> verifier
                .verifyThat(TestdataDisallowsUnassignedListWithoutInverseShadowVarConstraintProvider::penalizeEveryValue)
                .given(entity, value1, value2)
                .penalizes(THERE_SHOULD_BE_PENALTIES, 2)).doesNotThrowAnyException();
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Justification: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        .indictsWith(solution.getEntityList().getFirst(), badEntity))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: Justify with first justification")
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
                        .indictsWith(solution.getEntityList().getFirst(), badEntity, "bad indictment"))
                .hasMessageContaining("Broken expectation")
                .hasMessageContaining(
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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
                        "Indictment: Justify with first justification")
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

    @Test
    void shouldUpdateInternalConsistencyStateIfInconsistentIsNull() {
        var dependency = new TestdataDependencyValue("dependency", Duration.ofHours(1L));
        var dependent = new TestdataDependencyValue("dependent", Duration.ofHours(1L), List.of(dependency));
        var entity = new TestdataDependencyEntity(LocalDateTime.MIN);

        dependent.setEntity(entity);
        dependency.setEntity(entity);

        dependent.setPreviousValue(null);
        dependency.setPreviousValue(dependent);

        dependent.setIsInvalid(null);
        dependency.setIsInvalid(null);

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .given(dependency, dependent)
                .penalizes(0);

        assertThat(dependent.getIsInvalid()).isNull();
        assertThat(dependency.getIsInvalid()).isNull();

        dependent.setPreviousValue(dependency);
        dependency.setPreviousValue(null);
        dependent.setStartTime(LocalDateTime.MIN);
        dependent.setEndTime(LocalDateTime.MIN.plusHours(1));
        dependency.setStartTime(LocalDateTime.MIN.plusHours(1));
        dependency.setEndTime(LocalDateTime.MIN.plusHours(2));

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .given(dependency, dependent)
                // 60 + 120 = 180
                .penalizesBy(180);

        assertThat(dependent.getIsInvalid()).isNull();
        assertThat(dependency.getIsInvalid()).isNull();
    }

    @Test
    void shouldNotUpdateInternalConsistencyStateIfInconsistentIsSpecified() {
        var dependency = new TestdataDependencyValue("dependency", Duration.ofHours(1L));
        var dependent = new TestdataDependencyValue("dependent", Duration.ofHours(1L), List.of(dependency));
        var entity = new TestdataDependencyEntity(LocalDateTime.MIN);

        dependent.setEntity(entity);
        dependency.setEntity(entity);

        dependent.setPreviousValue(null);
        dependency.setPreviousValue(dependent);

        dependency.setIsInvalid(false);
        dependency.setStartTime(LocalDateTime.MIN.plusHours(1));
        dependency.setEndTime(LocalDateTime.MIN.plusHours(2));

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .given(dependency, dependent)
                .penalizesBy(120);

        var singleValue = new TestdataDependencyValue("single", Duration.ofHours(1L));
        singleValue.setIsInvalid(false);

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::penalizeInconsistentTasks)
                .given(singleValue)
                .penalizes(0);

        singleValue.setIsInvalid(true);

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::penalizeInconsistentTasks)
                .given(singleValue)
                .penalizes(1);
    }

    @Test
    void shouldUpdateInternalConsistencyStateIfInconsistentIsNullGivenSolution() {
        var dependency = new TestdataDependencyValue("dependency", Duration.ofHours(1L));
        var dependent = new TestdataDependencyValue("dependent", Duration.ofHours(1L), List.of(dependency));
        var entity = new TestdataDependencyEntity(LocalDateTime.MIN);

        var solution = new TestdataDependencySolution();
        solution.setEntities(List.of(entity));
        solution.setValues(List.of(dependency, dependent));

        entity.setValues(List.of(dependent, dependency));
        dependent.setEntity(entity);
        dependency.setEntity(entity);

        dependent.setPreviousValue(null);
        dependency.setPreviousValue(dependent);

        dependent.setIsInvalid(null);
        dependency.setIsInvalid(null);

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .givenSolution(solution)
                .penalizes(0);

        assertThat(dependent.getIsInvalid()).isNull();
        assertThat(dependency.getIsInvalid()).isNull();

        entity.setValues(List.of(dependency, dependent));
        dependent.setPreviousValue(dependency);
        dependency.setPreviousValue(null);
        dependent.setStartTime(LocalDateTime.MIN);
        dependent.setEndTime(LocalDateTime.MIN.plusHours(1));
        dependency.setStartTime(LocalDateTime.MIN.plusHours(1));
        dependency.setEndTime(LocalDateTime.MIN.plusHours(2));

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .givenSolution(solution)
                // 60 + 120 = 180
                .penalizesBy(180);

        assertThat(dependent.getIsInvalid()).isNull();
        assertThat(dependency.getIsInvalid()).isNull();
    }

    @Test
    void shouldUpdateInternalConsistencyStateIfInconsistentIsSpecifiedGivenSolution() {
        var dependency = new TestdataDependencyValue("dependency", Duration.ofHours(1L));
        var dependent = new TestdataDependencyValue("dependent", Duration.ofHours(1L), List.of(dependency));
        var entity = new TestdataDependencyEntity(LocalDateTime.MIN);

        var solution = new TestdataDependencySolution();
        solution.setEntities(List.of(entity));
        solution.setValues(List.of(dependency, dependent));

        dependent.setEntity(entity);
        dependency.setEntity(entity);

        dependent.setPreviousValue(null);
        dependency.setPreviousValue(dependent);

        dependency.setIsInvalid(false);
        dependency.setStartTime(LocalDateTime.MIN.plusHours(1));
        dependency.setEndTime(LocalDateTime.MIN.plusHours(2));

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::finishTasksAsSoonAsPossible)
                .givenSolution(solution)
                .penalizesBy(120);
    }

    @Test
    void shouldUpdateInternalConsistencyStateIfInconsistentIsSpecifiedGivenSolutionUnfiltered() {
        var entity = new TestdataDependencyEntity(LocalDateTime.MIN);

        var solution = new TestdataDependencySolution();
        var singleValue = new TestdataDependencyValue("single", Duration.ofHours(1L));

        solution.setEntities(List.of(entity));
        solution.setValues(List.of(singleValue));

        singleValue.setIsInvalid(false);
        singleValue.setEntity(entity);

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::penalizeInconsistentTasks)
                .givenSolution(solution)
                .penalizes(0);

        singleValue.setIsInvalid(true);

        constraintVerifierForConsistency.verifyThat(TestdataDependencyConstraintProvider::penalizeInconsistentTasks)
                .givenSolution(solution)
                .penalizes(1);
    }
}
