package ai.timefold.solver.core.impl.solver.monitoring.statistic;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.monitoring.ScoreLevels;
import ai.timefold.solver.core.impl.solver.monitoring.SolverMetricUtil;

import io.micrometer.core.instrument.Tags;

public class PickedMoveBestScoreDiffStatistic<Solution_, Score_ extends Score<Score_>> implements SolverStatistic<Solution_> {

    private final Map<Solver<Solution_>, PhaseLifecycleListenerAdapter<Solution_>> solverToPhaseLifecycleListenerMap =
            new WeakHashMap<>();

    @Override
    public void unregister(Solver<Solution_> solver) {
        var listener = solverToPhaseLifecycleListenerMap.remove(solver);
        if (listener != null) {
            ((DefaultSolver<Solution_>) solver).removePhaseLifecycleListener(listener);
        }
    }

    @Override
    public void register(Solver<Solution_> solver) {
        var defaultSolver = (DefaultSolver<Solution_>) solver;
        var scoreDirectorFactory = defaultSolver.getScoreDirectorFactory();
        var solutionDescriptor = scoreDirectorFactory.getSolutionDescriptor();
        var listener = new PickedMoveBestScoreDiffStatisticListener<Solution_, Score_>(solutionDescriptor.getScoreDefinition());
        solverToPhaseLifecycleListenerMap.put(solver, listener);
        defaultSolver.addPhaseLifecycleListener(listener);
    }

    private static class PickedMoveBestScoreDiffStatisticListener<Solution_, Score_ extends Score<Score_>>
            extends PhaseLifecycleListenerAdapter<Solution_> {

        private Score_ oldBestScore = null; // Guaranteed local search; no need for InnerScore.
        private final ScoreDefinition<Score_> scoreDefinition;
        private final Map<Tags, ScoreLevels> tagsToMoveScoreMap = new ConcurrentHashMap<>();

        public PickedMoveBestScoreDiffStatisticListener(ScoreDefinition<Score_> scoreDefinition) {
            this.scoreDefinition = scoreDefinition;
        }

        @Override
        public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
            if (phaseScope instanceof LocalSearchPhaseScope) {
                oldBestScore = phaseScope.<Score_> getBestScore().raw();
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

        private void localSearchStepEnded(LocalSearchStepScope<Solution_> stepScope) {
            if (stepScope.getBestScoreImproved()) {
                var moveType = stepScope.getStep().describe();
                var newBestScore = stepScope.<Score_> getScore().raw();
                var bestScoreDiff = newBestScore.subtract(oldBestScore);
                oldBestScore = newBestScore;
                var tags = stepScope.getPhaseScope().getSolverScope().getMonitoringTags()
                        .and("move.type", moveType);
                SolverMetricUtil.registerScore(SolverMetric.PICKED_MOVE_TYPE_BEST_SCORE_DIFF, tags, scoreDefinition,
                        tagsToMoveScoreMap, InnerScore.fullyAssigned(bestScoreDiff));
            }
        }
    }
}
