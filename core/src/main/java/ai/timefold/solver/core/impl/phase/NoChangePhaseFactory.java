package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.config.phase.NoChangePhaseConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

/**
 *
 * @param <Solution_>
 * @deprecated Deprecated on account of deprecating {@link NoChangePhase}.
 */
@Deprecated(forRemoval = true, since = "1.20.0")
public class NoChangePhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, NoChangePhaseConfig> {

    public NoChangePhaseFactory(NoChangePhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public NoChangePhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        HeuristicConfigPolicy<Solution_> phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        return new NoChangePhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(),
                buildPhaseTermination(phaseConfigPolicy, solverTermination)).build();
    }
}
