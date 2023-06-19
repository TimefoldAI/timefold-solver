package ai.timefold.solver.core.impl.partitionedsearch;

import ai.timefold.solver.core.enterprise.PartitionedSearchEnterpriseService;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public class DefaultPartitionedSearchPhaseFactory<Solution_>
        extends AbstractPhaseFactory<Solution_, PartitionedSearchPhaseConfig> {

    public DefaultPartitionedSearchPhaseFactory(PartitionedSearchPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public PartitionedSearchPhase<Solution_> buildPhase(int phaseIndex, HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, Termination<Solution_> solverTermination) {
        return PartitionedSearchEnterpriseService.load()
                .buildPartitionedSearch(phaseIndex, phaseConfig, solverConfigPolicy, solverTermination,
                        this::buildPhaseTermination);
    }

}
