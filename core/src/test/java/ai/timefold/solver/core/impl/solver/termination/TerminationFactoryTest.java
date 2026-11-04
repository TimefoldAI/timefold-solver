package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.solver.termination.TerminationType;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.definition.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class TerminationFactoryTest {

    private static final DefaultTerminationId ROOT_ID = new DefaultTerminationId(TerminationType.OR, null, 0, null);
    private static final DefaultTerminationId R_ID = ROOT_ID;
    private static final DefaultTerminationId A_ID = new DefaultTerminationId(TerminationType.OR, R_ID, 0, null);
    private static final DefaultTerminationId B_ID = new DefaultTerminationId(TerminationType.AND, R_ID, 1, null);

    @Test
    void spentLimit() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setSpentLimit(Duration.ofMillis(5L)
                .plusSeconds(4L)
                .plusMinutes(3L)
                .plusHours(2L)
                .plusDays(1L));
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(TimeMillisSpentTermination.class);
        assertThat(((TimeMillisSpentTermination<?>) termination).getTimeMillisSpentLimit()).isEqualTo(93784005L);
    }

    @Test
    void spentLimitWithoutJavaTime() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setMillisecondsSpentLimit(5L);
        terminationConfig.setSecondsSpentLimit(4L);
        terminationConfig.setMinutesSpentLimit(3L);
        terminationConfig.setHoursSpentLimit(2L);
        terminationConfig.setDaysSpentLimit(1L);
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(TimeMillisSpentTermination.class);
        assertThat(((TimeMillisSpentTermination<?>) termination).getTimeMillisSpentLimit()).isEqualTo(93784005L);
    }

    @Test
    void unimprovedSpentLimit() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setUnimprovedSpentLimit(Duration.ofMillis(5L)
                .plusSeconds(4L)
                .plusMinutes(3L)
                .plusHours(2L)
                .plusDays(1L));
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(UnimprovedTimeMillisSpentTermination.class);
        assertThat(((UnimprovedTimeMillisSpentTermination<?>) termination).getUnimprovedTimeMillisSpentLimit())
                .isEqualTo(93784005L);
    }

    @Test
    void unimprovedSpentLimitWithoutJavaTime() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setUnimprovedMillisecondsSpentLimit(5L);
        terminationConfig.setUnimprovedSecondsSpentLimit(4L);
        terminationConfig.setUnimprovedMinutesSpentLimit(3L);
        terminationConfig.setUnimprovedHoursSpentLimit(2L);
        terminationConfig.setUnimprovedDaysSpentLimit(1L);
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(UnimprovedTimeMillisSpentTermination.class);
        assertThat(((UnimprovedTimeMillisSpentTermination<?>) termination).getUnimprovedTimeMillisSpentLimit())
                .isEqualTo(93784005L);
    }

    @Test
    void buildTerminationFromList_single() {
        Termination<TestdataSolution> termination = new TimeMillisSpentTermination<>(1000L);
        assertThat(TerminationFactory.<TestdataSolution> create(new TerminationConfig())
                .buildTerminationFromList(Collections.singletonList(termination), ROOT_ID)).isSameAs(termination);
    }

    @Test
    void buildTerminationFromList_withAndComposition() {
        Termination<TestdataSolution> timeBasedTermination = new TimeMillisSpentTermination<>(1000L);
        Termination<TestdataSolution> stepCountTermination = new StepCountTermination<>(5);
        List<Termination<TestdataSolution>> terminationList = new ArrayList<>();
        terminationList.add(timeBasedTermination);
        terminationList.add(stepCountTermination);

        var resultingTermination =
                TerminationFactory.<TestdataSolution> create(new TerminationConfig()
                        .withTerminationCompositionStyle(TerminationCompositionStyle.AND))
                        .buildTerminationFromList(terminationList, ROOT_ID);
        assertThat(resultingTermination).isExactlyInstanceOf(AndCompositeTermination.class);
        var andCompositeTermination =
                (AndCompositeTermination<TestdataSolution>) resultingTermination;
        assertThat(andCompositeTermination.terminationList).containsExactly(timeBasedTermination, stepCountTermination);
    }

    @Test
    void buildTerminationFromList_withDefaultComposition() {
        Termination<TestdataSolution> timeBasedTermination = new TimeMillisSpentTermination<>(1000L);
        Termination<TestdataSolution> stepCountTermination = new StepCountTermination<>(5);
        List<Termination<TestdataSolution>> terminationList = new ArrayList<>();
        terminationList.add(timeBasedTermination);
        terminationList.add(stepCountTermination);

        var resultingTermination =
                TerminationFactory.<TestdataSolution> create(new TerminationConfig())
                        .buildTerminationFromList(terminationList, ROOT_ID);
        assertThat(resultingTermination).isExactlyInstanceOf(OrCompositeTermination.class);
        var andCompositeTermination =
                (OrCompositeTermination<TestdataSolution>) resultingTermination;
        assertThat(andCompositeTermination.terminationList).containsExactly(timeBasedTermination, stepCountTermination);
    }

    @Test
    void buildInnerTermination() {
        var innerTerminationConfig = new TerminationConfig().withSecondsSpentLimit(1L);
        var outerTerminationConfig = new TerminationConfig()
                .withTerminationConfigList(Collections.singletonList(innerTerminationConfig));
        List<Termination<?>> terminationList =
                TerminationFactory.create(outerTerminationConfig).buildInnerTermination(mock(HeuristicConfigPolicy.class),
                        ROOT_ID);
        assertThat(terminationList)
                .hasSize(1)
                .hasOnlyElementsOfType(TimeMillisSpentTermination.class);
        var timeMillisSpentTermination = (TimeMillisSpentTermination<?>) terminationList.getFirst();
        assertThat(timeMillisSpentTermination.getTimeMillisSpentLimit()).isEqualTo(1000L);
    }

    @Test
    void buildTimeBasedTermination_withScoreDifferenceThreshold() {
        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);
        when(heuristicConfigPolicy.getScoreDefinition()).thenReturn(new HardSoftScoreDefinition());

        var terminationConfig = new TerminationConfig()
                .withMillisecondsSpentLimit(1000L)
                .withUnimprovedMillisecondsSpentLimit(1000L)
                .withUnimprovedScoreDifferenceThreshold("1hard/0soft");
        var terminationList =
                TerminationFactory.<TestdataSolution> create(terminationConfig)
                        .buildTimeBasedTermination(heuristicConfigPolicy);
        assertThat(terminationList).hasOnlyElementsOfTypes(TimeMillisSpentTermination.class,
                UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination.class);
    }

    @Test
    void buildTimeBasedTermination_withoutScoreDifferenceThreshold() {
        var terminationConfig = new TerminationConfig()
                .withMillisecondsSpentLimit(1000L)
                .withUnimprovedMillisecondsSpentLimit(1000L);
        var terminationList =
                TerminationFactory.<TestdataSolution> create(terminationConfig)
                        .buildTimeBasedTermination(mock(HeuristicConfigPolicy.class));
        assertThat(terminationList).hasOnlyElementsOfTypes(TimeMillisSpentTermination.class,
                UnimprovedTimeMillisSpentTermination.class);
    }

    @Test
    void buildWithMoveCount() {
        var terminationConfig = new TerminationConfig()
                .withMoveCountLimit(1L);
        var terminationList =
                TerminationFactory.<TestdataSolution> create(terminationConfig)
                        .buildTimeBasedTermination(mock(HeuristicConfigPolicy.class));
        assertThat(terminationList).hasOnlyElementsOfTypes(MoveCountTermination.class);
    }

    @Test
    void scoreDifferenceThreshold_mustBeUsedWithUnimprovedTimeSpent() {
        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);

        var terminationConfig = new TerminationConfig();
        terminationConfig.withUnimprovedScoreDifferenceThreshold("1hard/0soft");

        TerminationFactory<TestdataSolution> terminationFactory = TerminationFactory.create(terminationConfig);
        assertThatIllegalStateException()
                .isThrownBy(() -> terminationFactory.buildTimeBasedTermination(heuristicConfigPolicy))
                .withMessageContaining("can only be used if an unimproved*SpentLimit");
    }

    @Test
    void bestScoreFeasible_requiresAtLeastOneFeasibleLevel() {
        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);
        when(heuristicConfigPolicy.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        var terminationConfig = new TerminationConfig().withBestScoreFeasible(true);

        TerminationFactory<TestdataSolution> terminationFactory = TerminationFactory.create(terminationConfig);
        assertThatIllegalStateException()
                .isThrownBy(() -> terminationFactory.buildTermination(heuristicConfigPolicy))
                .withMessageContaining("can only be used with a score type that has at least 1 feasible level");
    }

    @Test
    void diminishedReturns_default() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig());
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(DiminishedReturnsTermination.class);
        assertThat((DiminishedReturnsTermination<?, ?>) termination)
                .extracting(DiminishedReturnsTermination::getSlidingWindowNanos)
                .isEqualTo(Duration.ofSeconds(30).toNanos());
        assertThat((DiminishedReturnsTermination<?, ?>) termination)
                .extracting(DiminishedReturnsTermination::getMinimumImprovementRatio)
                .isEqualTo(0.0001);
    }

    @Test
    void diminishedReturns_custom() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig()
                .withSlidingWindowMilliseconds(1L)
                .withSlidingWindowSeconds(2L)
                .withSlidingWindowMinutes(3L)
                .withSlidingWindowHours(4L)
                .withSlidingWindowDays(5L)
                .withMinimumImprovementRatio(0.5));
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(DiminishedReturnsTermination.class);
        assertThat((DiminishedReturnsTermination<?, ?>) termination)
                .extracting(DiminishedReturnsTermination::getSlidingWindowNanos)
                .isEqualTo(Duration.ofMillis(1)
                        .plusSeconds(2)
                        .plusMinutes(3)
                        .plusHours(4)
                        .plusDays(5).toNanos());
        assertThat((DiminishedReturnsTermination<?, ?>) termination)
                .extracting(DiminishedReturnsTermination::getMinimumImprovementRatio)
                .isEqualTo(0.5);
    }

    @Test
    void diminishedReturns_customDuration() {
        var terminationConfig = new TerminationConfig();
        terminationConfig.setDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig()
                .withSlidingWindowDuration(Duration.ofMinutes(123))
                .withMinimumImprovementRatio(0.5));
        Termination<?> termination = TerminationFactory.create(terminationConfig)
                .buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat(termination)
                .isInstanceOf(DiminishedReturnsTermination.class);
        assertThat((DiminishedReturnsTermination<?, ?>) termination)
                .extracting(DiminishedReturnsTermination::getSlidingWindowNanos)
                .isEqualTo(Duration.ofMinutes(123).toNanos());
        assertThat((DiminishedReturnsTermination<?, ?>) termination)
                .extracting(DiminishedReturnsTermination::getMinimumImprovementRatio)
                .isEqualTo(0.5);
    }

    @Test
    void terminationIdsOfLevelsAndLeaves() {
        var termination = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(buildRabConfig())
                .buildTermination(mockSimpleScoreConfigPolicy());

        assertThat(termination.getTerminationId()).isNull(); // A built OR has no id.
        assertThat(termination.terminationList).hasSize(3);
        var spentLimit = (TimeMillisSpentTermination<TestdataSolution>) termination.terminationList.getFirst();
        assertThat(spentLimit.getTimeMillisSpentLimit()).isEqualTo(90_000L);
        assertThat(spentLimit.getTerminationId())
                .isEqualTo(new DefaultTerminationId(TerminationType.SPENT_LIMIT, R_ID, 0, null));
        // A builds no OR for its single leaf, but the leaf still has A as its parent.
        var bestScoreLimit = termination.terminationList.get(1);
        assertThat(bestScoreLimit).isInstanceOf(BestScoreTermination.class);
        assertThat(bestScoreLimit.getTerminationId())
                .isEqualTo(new DefaultTerminationId(TerminationType.BEST_SCORE_LIMIT, A_ID, 0, null));
        var and = (AndCompositeTermination<TestdataSolution>) termination.terminationList.get(2);
        assertThat(and.getTerminationId()).isEqualTo(B_ID);
        assertThat(and.terminationList)
                .map(Termination::getTerminationId)
                .containsExactly(new DefaultTerminationId(TerminationType.STEP_COUNT_LIMIT, B_ID, 0, null),
                        new DefaultTerminationId(TerminationType.UNIMPROVED_STEP_COUNT_LIMIT, B_ID, 0, null));
    }

    @Test
    void emptyNestedLevelShiftsNoOrdinal() {
        var config = new TerminationConfig()
                .withTerminationConfigList(List.of(
                        new TerminationConfig(),
                        new TerminationConfig()
                                .withTerminationCompositionStyle(TerminationCompositionStyle.AND)
                                .withStepCountLimit(100)
                                .withUnimprovedStepCountLimit(10)));
        var termination = TerminationFactory.<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy());

        assertThat(termination).isInstanceOf(AndCompositeTermination.class);
        assertThat(termination.getTerminationId()).isEqualTo(B_ID);
    }

    @Test
    void phaseIndexOnWholeChain() {
        var termination = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(buildRabConfig())
                .buildTermination(mockSimpleScoreConfigPolicy(), null, 0, 2);

        var rootId = new DefaultTerminationId(TerminationType.OR, null, 0, 2);
        var aId = new DefaultTerminationId(TerminationType.OR, rootId, 0, 2);
        var bestScoreLimitId = termination.terminationList.get(1).getTerminationId();
        assertThat(bestScoreLimitId)
                .isEqualTo(new DefaultTerminationId(TerminationType.BEST_SCORE_LIMIT, aId, 0, 2));
        assertThat(bestScoreLimitId.phaseIndex()).hasValue(2);
        assertThat(bestScoreLimitId.parent().phaseIndex()).hasValue(2);
        assertThat(bestScoreLimitId.parent().parent().phaseIndex()).hasValue(2);
        assertThat(bestScoreLimitId.parent().parent().parent()).isNull();
    }

    @Test
    void equalIdsFromTwoFactoriesOfOneConfig() {
        var config = buildRabConfig();
        var termination1 = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy());
        var termination2 = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy());

        assertThat(termination1.terminationList).isNotEqualTo(termination2.terminationList);
        assertThat(termination1.terminationList)
                .map(Termination::getTerminationId)
                .containsExactlyElementsOf(termination2.terminationList.stream()
                        .map(Termination::getTerminationId)
                        .toList());
    }

    @Test
    void orBelowAndRecordsNothing() {
        // B(AND){stepCountLimit, [C{spentLimit, bestScoreLimit}]}
        var config = new TerminationConfig()
                .withTerminationCompositionStyle(TerminationCompositionStyle.AND)
                .withStepCountLimit(5)
                .withTerminationConfigList(List.of(new TerminationConfig()
                        .withMillisecondsSpentLimit(10L)
                        .withBestScoreLimit("0")));
        Termination<TestdataSolution> chainedTermination = new StepCountTermination<>(1000);
        var termination = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy(), chainedTermination);
        var and = (AndCompositeTermination<TestdataSolution>) termination.terminationList.get(1);
        var or = (OrCompositeTermination<TestdataSolution>) and.terminationList.get(1);
        var solverScope = new SolverScope<TestdataSolution>();
        var phaseScope = mockPhaseScope(solverScope);
        when(phaseScope.calculatePhaseTimeMillisSpentUpToNow()).thenReturn(10L);

        when(phaseScope.getNextStepIndex()).thenReturn(0);
        assertThat(or.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(solverScope.getLastTerminationId()).isNull();

        when(phaseScope.getNextStepIndex()).thenReturn(5);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(solverScope.getLastTerminationId())
                .isEqualTo(new DefaultTerminationId(TerminationType.AND, null, 0, null));
    }

    @Test
    void orBelowSingleChildAndRecordsLeaf() {
        // B(AND){[C{spentLimit, bestScoreLimit}]}: no And is built, so C records its leaf.
        var config = new TerminationConfig()
                .withTerminationCompositionStyle(TerminationCompositionStyle.AND)
                .withTerminationConfigList(List.of(new TerminationConfig()
                        .withMillisecondsSpentLimit(10L)
                        .withBestScoreLimit("0")));
        Termination<TestdataSolution> chainedTermination = new StepCountTermination<>(1000);
        var termination = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy(), chainedTermination);
        var solverScope = new SolverScope<TestdataSolution>();
        var phaseScope = mockPhaseScope(solverScope);
        when(phaseScope.calculatePhaseTimeMillisSpentUpToNow()).thenReturn(10L);
        when(phaseScope.getNextStepIndex()).thenReturn(0);

        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        var bId = new DefaultTerminationId(TerminationType.AND, null, 0, null);
        var cId = new DefaultTerminationId(TerminationType.OR, bId, 0, null);
        assertThat(solverScope.getLastTerminationId())
                .isEqualTo(new DefaultTerminationId(TerminationType.SPENT_LIMIT, cId, 0, null));
    }

    @Test
    void lastRecordedIdWins() {
        // An unimproved* termination can be true in the middle of a step and false at the phase loop check.
        var config = new TerminationConfig()
                .withMillisecondsSpentLimit(10L)
                .withStepCountLimit(5);
        var termination = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy());
        var solverScope = new SolverScope<TestdataSolution>();
        var phaseScope = mockPhaseScope(solverScope);

        when(phaseScope.calculatePhaseTimeMillisSpentUpToNow()).thenReturn(0L);
        when(phaseScope.getNextStepIndex()).thenReturn(5);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        var stepCountLimitId = new DefaultTerminationId(TerminationType.STEP_COUNT_LIMIT, ROOT_ID, 0, null);
        assertThat(solverScope.getLastTerminationId()).isEqualTo(stepCountLimitId);

        when(phaseScope.calculatePhaseTimeMillisSpentUpToNow()).thenReturn(10L);
        when(phaseScope.getNextStepIndex()).thenReturn(0);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(solverScope.getLastTerminationId())
                .isEqualTo(new DefaultTerminationId(TerminationType.SPENT_LIMIT, ROOT_ID, 0, null));
    }

    @Test
    void phaseRootClearsIdWhenFalse() {
        // A termination true in the middle of a step, then false at the phase loop check, did not end the phase.
        var config = new TerminationConfig().withStepCountLimit(5);
        Termination<TestdataSolution> chainedTermination = new StepCountTermination<>(1000);
        var phaseRoot = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(config)
                .buildTermination(mockSimpleScoreConfigPolicy(), chainedTermination, 1);
        var solverScope = new SolverScope<TestdataSolution>();
        var phaseScope = mockPhaseScope(solverScope);

        when(phaseScope.getNextStepIndex()).thenReturn(5);
        assertThat(phaseRoot.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(solverScope.getLastTerminationId()).isNotNull();

        when(phaseScope.getNextStepIndex()).thenReturn(0);
        assertThat(phaseRoot.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(solverScope.getLastTerminationId()).isNull();
    }

    @Test
    void phaseRootWithoutPhaseTerminationClearsIdWhenFalse() {
        Termination<TestdataSolution> chainedTermination = new StepCountTermination<>(1000);
        var phaseRoot = (OrCompositeTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(new TerminationConfig())
                .buildTermination(mockSimpleScoreConfigPolicy(), chainedTermination, 1);
        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setLastTerminationId(new DefaultTerminationId(TerminationType.STEP_COUNT_LIMIT, null, 0, null));
        var phaseScope = mockPhaseScope(solverScope);

        when(phaseScope.getNextStepIndex()).thenReturn(0);
        assertThat(phaseRoot.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(solverScope.getLastTerminationId()).isNull();
    }

    // R{spentLimit, [A{bestScoreLimit}, B(AND){stepCountLimit, unimprovedStepCountLimit}]}
    private static TerminationConfig buildRabConfig() {
        return new TerminationConfig()
                .withSecondsSpentLimit(30L)
                .withMinutesSpentLimit(1L)
                .withTerminationConfigList(List.of(
                        new TerminationConfig().withBestScoreLimit("0"),
                        new TerminationConfig()
                                .withTerminationCompositionStyle(TerminationCompositionStyle.AND)
                                .withStepCountLimit(100)
                                .withUnimprovedStepCountLimit(10)));
    }

    private static HeuristicConfigPolicy<TestdataSolution> mockSimpleScoreConfigPolicy() {
        HeuristicConfigPolicy<TestdataSolution> configPolicy = mock(HeuristicConfigPolicy.class);
        when(configPolicy.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        return configPolicy;
    }

    private static AbstractPhaseScope<TestdataSolution> mockPhaseScope(SolverScope<TestdataSolution> solverScope) {
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        return phaseScope;
    }

}
