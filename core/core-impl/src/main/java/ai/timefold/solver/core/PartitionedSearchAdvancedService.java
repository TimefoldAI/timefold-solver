package ai.timefold.solver.core;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public interface PartitionedSearchAdvancedService {

    static PartitionedSearchAdvancedService load() {
        ServiceLoader<PartitionedSearchAdvancedService> serviceLoader =
                ServiceLoader.load(PartitionedSearchAdvancedService.class);
        Iterator<PartitionedSearchAdvancedService> iterator = serviceLoader.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException(
                    "Partitioned search requested  but Timefold Enterprise not found on classpath.\n" +
                            "Either add the ai.timefold.solver:timefold-solver-enterprise dependency, " +
                            "or remove partitioned search from solver configuration.\n" +
                            "Note: Timefold Enterprise is a commercial product.");
        }
        return iterator.next();
    }

    <Solution_> PartitionedSearchPhase<Solution_> buildPartitionedSearch(int phaseIndex,
            PartitionedSearchPhaseConfig phaseConfig, HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            Termination<Solution_> solverTermination,
            BiFunction<HeuristicConfigPolicy<Solution_>, Termination<Solution_>, Termination<Solution_>> phaseTerminationFunction);

}
