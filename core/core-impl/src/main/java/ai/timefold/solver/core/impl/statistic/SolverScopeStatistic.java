package ai.timefold.solver.core.impl.statistic;

import java.util.function.ToDoubleFunction;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;

public class SolverScopeStatistic<Solution_> implements SolverStatistic<Solution_> {
    private final String meterId;
    private final ToDoubleFunction<SolverScope<Solution_>> metricFunction;

    public SolverScopeStatistic(String meterId, ToDoubleFunction<SolverScope<Solution_>> metricFunction) {
        this.meterId = meterId;
        this.metricFunction = metricFunction;
    }

    @Override
    public void register(Solver<Solution_> solver) {
        SolverScope<Solution_> solverScope = ((DefaultSolver<Solution_>) solver).getSolverScope();
        Metrics.gauge(meterId, solverScope.getMonitoringTags(),
                solverScope, metricFunction);
    }

    @Override
    public void unregister(Solver<Solution_> solver) {
        SolverScope<Solution_> solverScope = ((DefaultSolver<Solution_>) solver).getSolverScope();
        Metrics.globalRegistry.remove(new Meter.Id(meterId,
                solverScope.getMonitoringTags(),
                null,
                null,
                Meter.Type.GAUGE));
    }
}
