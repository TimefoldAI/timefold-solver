package ai.timefold.solver.core.impl.enterprise;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public interface PartitionedSearchEnterpriseService {

    static PartitionedSearchEnterpriseService load() {
        ServiceLoader<PartitionedSearchEnterpriseService> serviceLoader =
                ServiceLoader.load(PartitionedSearchEnterpriseService.class);
        Iterator<PartitionedSearchEnterpriseService> iterator = serviceLoader.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException(
                    "Partitioned search requested  but Timefold Solver Enterprise not found on classpath.\n" +
                            "Either ddd the ai.timefold.solver:timefold-solver-enterprise dependency, " +
                            "or remove partitioned search from solver configuration.\n" +
                            "Note: Timefold Solver Enterprise is a commercial product.");
        }
        return iterator.next();
    }

    <Solution_> PartitionedSearchPhase<Solution_> buildPartitionedSearch(int phaseIndex,
            PartitionedSearchPhaseConfig phaseConfig, HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            Termination<Solution_> solverTermination,
            BiFunction<HeuristicConfigPolicy<Solution_>, Termination<Solution_>, Termination<Solution_>> phaseTerminationFunction);

}
