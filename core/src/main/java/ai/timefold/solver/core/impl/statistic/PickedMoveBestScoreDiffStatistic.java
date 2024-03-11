package ai.timefold.solver.core.impl.statistic;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Tags;

public class PickedMoveBestScoreDiffStatistic<Solution_, Score_ extends Score<Score_>> implements SolverStatistic<Solution_> {

    private final Map<Solver<Solution_>, PhaseLifecycleListenerAdapter<Solution_>> solverToPhaseLifecycleListenerMap =
            new WeakHashMap<>();

    @Override
    public void unregister(Solver<Solution_> solver) {
        PhaseLifecycleListenerAdapter<Solution_> listener = solverToPhaseLifecycleListenerMap.remove(solver);
        if (listener != null) {
            ((DefaultSolver<Solution_>) solver).removePhaseLifecycleListener(listener);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(Solver<Solution_> solver) {
        DefaultSolver<Solution_> defaultSolver = (DefaultSolver<Solution_>) solver;
        InnerScoreDirectorFactory<Solution_, ?> innerScoreDirectorFactory = defaultSolver.getScoreDirectorFactory();
        SolutionDescriptor<Solution_> solutionDescriptor = innerScoreDirectorFactory.getSolutionDescriptor();
        PickedMoveBestScoreDiffStatisticListener<Solution_, Score_> listener =
                new PickedMoveBestScoreDiffStatisticListener<Solution_, Score_>(solutionDescriptor.getScoreDefinition());
        solverToPhaseLifecycleListenerMap.put(solver, listener);
        defaultSolver.addPhaseLifecycleListener(listener);
    }

    private static class PickedMoveBestScoreDiffStatisticListener<Solution_, Score_ extends Score<Score_>>
            extends PhaseLifecycleListenerAdapter<Solution_> {

        private Score_ oldBestScore = null;
        private final ScoreDefinition<Score_> scoreDefinition;
        private final Map<Tags, List<AtomicReference<Number>>> tagsToMoveScoreMap = new ConcurrentHashMap<>();

        public PickedMoveBestScoreDiffStatisticListener(ScoreDefinition<Score_> scoreDefinition) {
            this.scoreDefinition = scoreDefinition;
        }

        @Override
        public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
            if (phaseScope instanceof LocalSearchPhaseScope) {
                oldBestScore = phaseScope.getBestScore();
            }
        }

        @Override
        public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
            if (phaseScope instanceof LocalSearchPhaseScope) {
                oldBestScore = null;
            }
        }

        @Override
        public void stepEnded(AbstractStepScope<Solution_> stepScope) {
            if (stepScope instanceof LocalSearchStepScope) {
                localSearchStepEnded((LocalSearchStepScope<Solution_>) stepScope);
            }
        }

        @SuppressWarnings("unchecked")
        private void localSearchStepEnded(LocalSearchStepScope<Solution_> stepScope) {
            if (stepScope.getBestScoreImproved()) {
                String moveType = stepScope.getStep().getSimpleMoveTypeDescription();
                Score_ newBestScore = (Score_) stepScope.getScore();
                Score_ bestScoreDiff = newBestScore.subtract(oldBestScore);
                oldBestScore = newBestScore;
                SolverMetric.registerScoreMetrics(SolverMetric.PICKED_MOVE_TYPE_BEST_SCORE_DIFF,
                        stepScope.getPhaseScope().getSolverScope().getMonitoringTags()
                                .and("move.type", moveType),
                        scoreDefinition,
                        tagsToMoveScoreMap,
                        bestScoreDiff);
            }
        }
    }
}
