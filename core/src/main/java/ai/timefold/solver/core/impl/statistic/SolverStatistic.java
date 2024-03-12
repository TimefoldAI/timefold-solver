package ai.timefold.solver.core.impl.statistic;

import ai.timefold.solver.core.api.solver.Solver;

public interface SolverStatistic<Solution_> {
    void unregister(Solver<Solution_> solver);

    void register(Solver<Solution_> solver);
}
