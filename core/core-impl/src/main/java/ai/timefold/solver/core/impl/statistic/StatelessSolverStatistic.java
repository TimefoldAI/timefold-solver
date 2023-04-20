package ai.timefold.solver.core.impl.statistic;

import ai.timefold.solver.core.api.solver.Solver;

/**
 * A {@link SolverStatistic} that has no state or event listener
 */
public class StatelessSolverStatistic<Solution_> implements SolverStatistic<Solution_> {

    @Override
    public void unregister(Solver<Solution_> solver) {
        // intentionally empty
    }

    @Override
    public void register(Solver<Solution_> solver) {
        // intentionally empty
    }
}
