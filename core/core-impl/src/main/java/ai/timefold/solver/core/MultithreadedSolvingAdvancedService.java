package ai.timefold.solver.core;

import java.util.Iterator;
import java.util.ServiceLoader;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public interface MultithreadedSolvingAdvancedService {

    static MultithreadedSolvingAdvancedService load(Integer moveThreadCount) {
        ServiceLoader<MultithreadedSolvingAdvancedService> serviceLoader =
                ServiceLoader.load(MultithreadedSolvingAdvancedService.class);
        Iterator<MultithreadedSolvingAdvancedService> iterator = serviceLoader.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException(
                    "Multi-threaded solving requested with moveThreadCount (" + moveThreadCount
                            + ") but Timefold Solver Advanced Edition not found on classpath.\n" +
                            "Either add the ai.timefold.solver.advanced:timefold-solver-advanced-core dependency, " +
                            "or remove moveThreadCount from solver configuration.\n" +
                            "Note: Timefold Solver Advanced Edition is a commercial product.");
        }
        return iterator.next();
    }

    <Solution_> ConstructionHeuristicDecider<Solution_> buildConstructionHeuristic(int moveThreadCount,
            Termination<Solution_> termination, ConstructionHeuristicForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy);

    <Solution_> LocalSearchDecider<Solution_> buildLocalSearch(int moveThreadCount, Termination<Solution_> termination,
            MoveSelector<Solution_> moveSelector, Acceptor<Solution_> acceptor, LocalSearchForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy);

}
