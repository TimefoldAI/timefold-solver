package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

final class RuinRecreateConstructionHeuristicPhaseFactory<Solution_>
        extends DefaultConstructionHeuristicPhaseFactory<Solution_> {

    RuinRecreateConstructionHeuristicPhaseFactory(ConstructionHeuristicPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    protected RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> createBuilder(
            HeuristicConfigPolicy<Solution_> phaseConfigPolicy, SolverTermination<Solution_> solverTermination, int phaseIndex,
            boolean lastInitializingPhase, EntityPlacer<Solution_> entityPlacer) {
        var phaseTermination = PhaseTermination.bridge(new BasicPlumbingTermination<Solution_>(false));
        // The R&R decider uses the root solver environment mode by default
        return new RuinRecreateConstructionHeuristicPhaseBuilder<>(phaseConfigPolicy, this, phaseTermination, entityPlacer,
                buildDecider(phaseConfigPolicy, phaseConfigPolicy.getEnvironmentMode(), phaseTermination));

    }

    @Override
    protected RuinRecreateConstructionHeuristicDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            EnvironmentMode environmentMode, PhaseTermination<Solution_> termination) {
        return new RuinRecreateConstructionHeuristicDecider<>(termination, buildForager(configPolicy));
    }

}
