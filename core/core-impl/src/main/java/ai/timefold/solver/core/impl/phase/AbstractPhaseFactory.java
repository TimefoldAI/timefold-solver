package ai.timefold.solver.core.impl.phase;

import java.util.Objects;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.termination.PhaseToSolverTerminationBridge;
import ai.timefold.solver.core.impl.solver.termination.Termination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;

public abstract class AbstractPhaseFactory<Solution_, PhaseConfig_ extends PhaseConfig<PhaseConfig_>>
        implements PhaseFactory<Solution_> {

    protected final PhaseConfig_ phaseConfig;

    public AbstractPhaseFactory(PhaseConfig_ phaseConfig) {
        this.phaseConfig = phaseConfig;
    }

    protected Termination<Solution_> buildPhaseTermination(HeuristicConfigPolicy<Solution_> configPolicy,
            Termination<Solution_> solverTermination) {
        TerminationConfig terminationConfig_ =
                Objects.requireNonNullElseGet(phaseConfig.getTerminationConfig(), TerminationConfig::new);
        // In case of childThread PART_THREAD, the solverTermination is actually the parent phase's phaseTermination
        // with the bridge removed, so it's ok to add it again
        Termination<Solution_> phaseTermination = new PhaseToSolverTerminationBridge<>(solverTermination);
        return TerminationFactory.<Solution_> create(terminationConfig_).buildTermination(configPolicy, phaseTermination);
    }
}
