package ai.timefold.solver.core.impl.statistic;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

public class MoveCountPerTypeStatistic<Solution_> implements SolverStatistic<Solution_> {

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
    public void register(Solver<Solution_> solver) {
        var defaultSolver = (DefaultSolver<Solution_>) solver;
        var listener = new MoveCountPerTypeStatistic.MoveCountPerTypeStatisticListener<Solution_>();
        solverToPhaseLifecycleListenerMap.put(solver, listener);
        defaultSolver.addPhaseLifecycleListener(listener);
    }

    private static class MoveCountPerTypeStatisticListener<Solution_> extends PhaseLifecycleListenerAdapter<Solution_> {
        private final Map<Tags, Map<String, AtomicLong>> tagsToMoveCountMap = new ConcurrentHashMap<>();

        @Override
        public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
            // The metric must be collected when the phase ends instead of when the solver ends
            // because there is no guarantee this listener will run the phase event before the StatisticRegistry listener
            var moveCountPerType = phaseScope.getSolverScope().getMoveEvaluationCountPerType();
            var tags = phaseScope.getSolverScope().getMonitoringTags();
            moveCountPerType.forEach((type, count) -> {
                var key = SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + type;
                var counter = Metrics.gauge(key, tags, new AtomicLong(0L));
                if (counter != null) {
                    counter.set(count);
                }
                registerMoveCountPerType(tags, key, counter);
            });
        }

        private void registerMoveCountPerType(Tags tag, String key, AtomicLong count) {
            tagsToMoveCountMap.compute(tag, (tags, countMap) -> {
                if (countMap == null) {
                    countMap = new HashMap<>();
                }
                countMap.put(key, count);
                return countMap;
            });
        }
    }

}
