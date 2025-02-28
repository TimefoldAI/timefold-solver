package ai.timefold.solver.core.impl.phase;

import java.util.Objects;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;

public abstract class AbstractPhaseFactory<Solution_, PhaseConfig_ extends PhaseConfig<PhaseConfig_>>
        implements PhaseFactory<Solution_> {

    protected final PhaseConfig_ phaseConfig;

    public AbstractPhaseFactory(PhaseConfig_ phaseConfig) {
        this.phaseConfig = phaseConfig;
    }

    protected PhaseTermination<Solution_> buildPhaseTermination(HeuristicConfigPolicy<Solution_> configPolicy,
            SolverTermination<Solution_> solverTermination) {
        var terminationConfig_ = Objects.requireNonNullElseGet(phaseConfig.getTerminationConfig(), TerminationConfig::new);
        return PhaseTermination.bridge(TerminationFactory.<Solution_> create(terminationConfig_)
                .buildTermination(configPolicy, solverTermination));
    }
}
