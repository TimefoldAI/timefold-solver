package ai.timefold.solver.core.impl.solver.monitoring.statistic;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.event.DefaultBestSolutionChangedEvent;
import ai.timefold.solver.core.impl.solver.monitoring.ScoreLevels;
import ai.timefold.solver.core.impl.solver.monitoring.SolverMetricUtil;

import io.micrometer.core.instrument.Tags;

public class BestScoreStatistic<Solution_> implements SolverStatistic<Solution_> {

    private final Map<Tags, ScoreLevels> tagsToBestScoreMap = new ConcurrentHashMap<>();
    private final Map<Solver<Solution_>, SolverEventListener<Solution_>> solverToEventListenerMap = new WeakHashMap<>();

    @Override
    public void unregister(Solver<Solution_> solver) {
        SolverEventListener<Solution_> listener = solverToEventListenerMap.remove(solver);
        if (listener != null) {
            solver.removeEventListener(listener);
        }
        tagsToBestScoreMap.remove(extractTags(solver));
    }

    private static Tags extractTags(Solver<?> solver) {
        var defaultSolver = (DefaultSolver<?>) solver;
        return defaultSolver.getSolverScope().getMonitoringTags();
    }

    @Override
    public void register(Solver<Solution_> solver) {
        var defaultSolver = (DefaultSolver<Solution_>) solver;
        var scoreDefinition = defaultSolver.getSolverScope().getScoreDefinition();
        var tags = extractTags(solver);
        SolverEventListener<Solution_> listener =
                event -> {
                    var castEvent = (DefaultBestSolutionChangedEvent<Solution_>) event;
                    SolverMetricUtil.registerScore(SolverMetric.BEST_SCORE, tags, scoreDefinition, tagsToBestScoreMap,
                            InnerScore.withUnassignedCount(event.getNewBestScore(), castEvent.getUnassignedCount()));
                };
        solverToEventListenerMap.put(defaultSolver, listener);
        defaultSolver.addEventListener(listener);
    }
}
