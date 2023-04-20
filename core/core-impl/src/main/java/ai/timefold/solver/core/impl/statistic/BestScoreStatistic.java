package ai.timefold.solver.core.impl.statistic;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.SolverEventListener;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Tags;

public class BestScoreStatistic<Solution_> implements SolverStatistic<Solution_> {
    private final Map<Tags, List<AtomicReference<Number>>> tagsToBestScoreMap = new ConcurrentHashMap<>();

    private final Map<Solver<Solution_>, SolverEventListener<Solution_>> solverToEventListenerMap = new WeakHashMap<>();

    @Override
    public void unregister(Solver<Solution_> solver) {
        SolverEventListener<Solution_> listener = solverToEventListenerMap.remove(solver);
        if (listener != null) {
            solver.removeEventListener(listener);
        }
    }

    @Override
    public void register(Solver<Solution_> solver) {
        DefaultSolver<Solution_> defaultSolver = (DefaultSolver<Solution_>) solver;
        ScoreDefinition<?> scoreDefinition = defaultSolver.getSolverScope().getScoreDefinition();
        SolverEventListener<Solution_> listener = event -> SolverMetric.registerScoreMetrics(SolverMetric.BEST_SCORE,
                defaultSolver.getSolverScope().getMonitoringTags(),
                scoreDefinition, tagsToBestScoreMap, event.getNewBestScore());
        solverToEventListenerMap.put(defaultSolver, listener);
        defaultSolver.addEventListener(listener);
    }
}
