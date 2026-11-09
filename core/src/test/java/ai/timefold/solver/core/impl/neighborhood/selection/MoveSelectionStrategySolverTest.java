package ai.timefold.solver.core.impl.neighborhood.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.SwapMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.FixedWeightMoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.selection.PhaseContext;
import ai.timefold.solver.core.preview.api.neighborhood.selection.StepContext;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * The strategies record into a static list, because the solver creates them.
 */
@Execution(ExecutionMode.SAME_THREAD)
class MoveSelectionStrategySolverTest {

    private static final List<RecordingStrategy> RECORDING_LIST = new ArrayList<>();

    @BeforeEach
    void clear() {
        RECORDING_LIST.clear();
    }

    @Test
    void lifecycle() {
        var solverConfig = baseConfig()
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withPhases(new LocalSearchPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(20)));
        solve(solverConfig, solution(4, 6));

        assertThat(RECORDING_LIST).hasSize(1);
        var recording = RECORDING_LIST.getFirst();
        var eventList = recording.eventList;
        assertThat(eventList.getFirst()).isEqualTo("phaseStarted");
        assertThat(eventList).filteredOn("stepStarted"::equals).hasSize(20);
        assertThat(eventList.get(1)).isEqualTo("stepStarted");
        assertThat(recording.threadSet).containsExactly(Thread.currentThread());
        assertThat(recording.stepContextSet).hasSize(1);
        assertThat(recording.solutionSet).hasSize(1);
        assertThat(recording.scoreMismatchList).isEmpty();
    }

    @Test
    void newInstancePerSolve() {
        var solverConfig = baseConfig()
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withPhases(new LocalSearchPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(3)));
        solve(solverConfig, solution(3, 3));
        solve(solverConfig, solution(3, 3));

        assertThat(RECORDING_LIST).hasSize(2);
        assertThat(RECORDING_LIST.get(0)).isNotSameAs(RECORDING_LIST.get(1));
    }

    @Test
    void zeroWeightProviderNeverUsed() {
        // Swap moves never change the number of distinct values; only change moves can improve the score.
        var problem = solution(3, 6);
        var initialScore = new DistinctValueScoreCalculator().calculateScore(problem);
        var solverConfig = baseConfig()
                .withNeighborhoodProviderClass(ChangeAndSwapNeighborhoodProvider.class)
                .withMoveSelectionStrategyClass(NoChangeStrategy.class)
                .withPhases(new LocalSearchPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(50)));

        var result = solve(solverConfig, problem);
        assertThat(result.getScore()).isEqualTo(initialScore);
    }

    @Test
    void solverLevelForGeneratedAndExplicitPhases() {
        var generated = baseConfig()
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withMoveSelectionStrategyCustomProperties(Map.of("label", "solver"))
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(3));
        solve(generated, solution(3, 3));
        assertThat(RECORDING_LIST).extracting(r -> r.label).containsExactly("solver");

        RECORDING_LIST.clear();
        var explicit = baseConfig()
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withMoveSelectionStrategyCustomProperties(Map.of("label", "solver"))
                .withPhases(new LocalSearchPhaseConfig()
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(3)));
        solve(explicit, solution(3, 3));
        assertThat(RECORDING_LIST).extracting(r -> r.label).containsExactly("solver");
    }

    @Test
    void phaseOverridesAndPropertiesNeverMixLevels() {
        var solverConfig = baseConfig()
                .withMoveSelectionStrategyClass(NoChangeStrategy.class)
                .withMoveSelectionStrategyCustomProperties(Map.of("unused", "solver"))
                .withPhases(
                        new LocalSearchPhaseConfig()
                                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                                .withMoveSelectionStrategyCustomProperties(Map.of("label", "phase"))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(3)),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(3)));
        solve(solverConfig, solution(3, 3));

        // NoChangeStrategy has no setter for "unused"; it would fail if the solver-level properties leaked.
        assertThat(RECORDING_LIST).extracting(r -> r.label).containsExactly("phase", null);
    }

    @Test
    void stepEndedWithOnlyRejectedMovesTakesBestRejectedMove() {
        // All entities on one value is optimal; every change move is worse, so Hill Climbing accepts none.
        var problem = solution(2, 3);
        var value = problem.getValueList().getFirst();
        problem.getEntityList().forEach(entity -> entity.setValue(value));
        var solverConfig = baseConfig()
                .withNeighborhoodProviderClass(ChangeOnlyNeighborhoodProvider.class)
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withMoveSelectionStrategyCustomProperties(Map.of("movesPerStep", "3"))
                .withPhases(new LocalSearchPhaseConfig()
                        .withLocalSearchType(LocalSearchType.HILL_CLIMBING)
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(10)));

        var result = solve(solverConfig, problem);
        assertThat(result.getScore()).isEqualTo(SimpleScore.of(-1));
        // Every step still had a winning move, so all 10 steps ran.
        assertThat(RECORDING_LIST.getFirst().eventList).filteredOn("stepStarted"::equals).hasSize(10);
    }

    @Test
    void settingsWithoutNeighborhoodsFail() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DistinctValueScoreCalculator.class)
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withPhases(new LocalSearchPhaseConfig());
        assertThatThrownBy(() -> SolverFactory.create(solverConfig).buildSolver())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("moveSelectionStrategyClass")
                .hasMessageContaining("NEIGHBORHOODS");
    }

    @Test
    void propertiesWithoutClassFail() {
        var solverConfig = baseConfig()
                .withPhases(new LocalSearchPhaseConfig()
                        .withMoveSelectionStrategyCustomProperties(Map.of("label", "phase")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SolverFactory.create(solverConfig).buildSolver())
                .withMessageContaining("phase-level moveSelectionStrategyCustomProperties");
    }

    @Test
    void strategyWithMoveSelectorsFails() {
        var solverConfig = baseConfig()
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withPhases(new LocalSearchPhaseConfig().withMoveSelectorConfig(new ChangeMoveSelectorConfig()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SolverFactory.create(solverConfig).buildSolver())
                .withMessageContaining("cannot be used in a phase with a move selector")
                .withMessageContaining("remove the strategy class or the move selectors");
    }

    @Test
    void unknownCustomPropertyFailsAtBuild() {
        var solverConfig = baseConfig()
                .withMoveSelectionStrategyClass(RecordingStrategy.class)
                .withMoveSelectionStrategyCustomProperties(Map.of("unknown", "x"))
                .withPhases(new LocalSearchPhaseConfig());
        assertThatIllegalStateException()
                .isThrownBy(() -> SolverFactory.create(solverConfig).buildSolver())
                .withMessageContaining("custom property unknown");
    }

    private static TestdataSolution solve(SolverConfig solverConfig, TestdataSolution problem) {
        return PlannerTestUtils.solve(solverConfig, problem, events -> {
            // Runs may or may not improve.
        });
    }

    private static SolverConfig baseConfig() {
        return new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DistinctValueScoreCalculator.class);
    }

    private static TestdataSolution solution(int valueCount, int entityCount) {
        var solution = TestdataSolution.generateSolution(valueCount, entityCount);
        var valueList = solution.getValueList();
        var entityList = solution.getEntityList();
        for (var i = 0; i < entityList.size(); i++) {
            entityList.get(i).setValue(valueList.get(i % valueList.size()));
        }
        return solution;
    }

    @NullMarked
    public static final class DistinctValueScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public SimpleScore calculateScore(TestdataSolution solution) {
            var valueSet = new HashSet<TestdataValue>();
            for (var entity : solution.getEntityList()) {
                if (entity.getValue() != null) {
                    valueSet.add(entity.getValue());
                }
            }
            return SimpleScore.of(-valueSet.size());
        }

    }

    @NullMarked
    public static final class ChangeOnlyNeighborhoodProvider implements NeighborhoodProvider<TestdataSolution> {

        @Override
        public Neighborhood defineNeighborhood(NeighborhoodBuilder<TestdataSolution> builder) {
            var variable = builder.getSolutionMetaModel().genuineEntity(TestdataEntity.class).basicVariable();
            return builder.add(new ChangeMoveProvider<>(variable)).build();
        }

    }

    @NullMarked
    public static final class ChangeAndSwapNeighborhoodProvider implements NeighborhoodProvider<TestdataSolution> {

        @Override
        public Neighborhood defineNeighborhood(NeighborhoodBuilder<TestdataSolution> builder) {
            var entity = builder.getSolutionMetaModel().genuineEntity(TestdataEntity.class);
            return builder.add("change", new ChangeMoveProvider<>(entity.basicVariable()))
                    .add("swap", new SwapMoveProvider<>(entity))
                    .build();
        }

    }

    @NullMarked
    public static final class NoChangeStrategy extends FixedWeightMoveSelectionStrategy<TestdataSolution> {

        @Override
        protected double getWeight(MoveSource<TestdataSolution> moveSource, PhaseContext<TestdataSolution> context) {
            return moveSource.id().equals("change") ? 0.0 : 1.0;
        }

    }

    /**
     * Uses the default strategy, records the calls, and ends each step after movesPerStep moves if set.
     */
    @NullMarked
    public static final class RecordingStrategy implements MoveSelectionStrategy<TestdataSolution> {

        private final FixedWeightMoveSelectionStrategy<TestdataSolution> delegate =
                new FixedWeightMoveSelectionStrategy<>();
        private final List<String> eventList = new ArrayList<>();
        private final Set<Thread> threadSet = new HashSet<>();
        private final Set<Object> stepContextSet = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Object> solutionSet = Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<String> scoreMismatchList = new ArrayList<>();
        private @Nullable String label = null;
        private int movesPerStep = Integer.MAX_VALUE;
        private int movesInStep = 0;

        public void setLabel(String label) {
            this.label = label;
        }

        public void setMovesPerStep(int movesPerStep) {
            this.movesPerStep = movesPerStep;
        }

        private void record(String event, TestdataSolution solution) {
            eventList.add(event);
            threadSet.add(Thread.currentThread());
            solutionSet.add(solution);
            var expected = new DistinctValueScoreCalculator().calculateScore(solution);
            if (!expected.equals(solution.getScore())) {
                scoreMismatchList.add(event + ": " + solution.getScore() + " != " + expected);
            }
        }

        @Override
        public void phaseStarted(PhaseContext<TestdataSolution> context) {
            RECORDING_LIST.add(this);
            record("phaseStarted", context.solution());
            delegate.phaseStarted(context);
        }

        @Override
        public void stepStarted(StepContext<TestdataSolution> context) {
            record("stepStarted", context.solution());
            stepContextSet.add(context);
            movesInStep = 0;
            delegate.stepStarted(context);
        }

        @Override
        public void moveSourceExhausted(MoveSource<TestdataSolution> moveSource) {
            delegate.moveSourceExhausted(moveSource);
        }

        @Override
        public @Nullable MoveSource<TestdataSolution> nextMoveSource(RandomGenerator random) {
            if (movesInStep++ >= movesPerStep) {
                return null;
            }
            return delegate.nextMoveSource(random);
        }

    }

}
