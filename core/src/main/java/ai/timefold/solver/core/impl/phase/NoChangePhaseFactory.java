package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.config.phase.NoChangePhaseConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public class NoChangePhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, NoChangePhaseConfig> {

    public NoChangePhaseFactory(NoChangePhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public NoChangePhase<Solution_> buildPhase(int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> solverTermination) {
        HeuristicConfigPolicy<Solution_> phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        return new NoChangePhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(),
                buildPhaseTermination(phaseConfigPolicy, solverTermination)).build();
    }
}
