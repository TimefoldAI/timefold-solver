package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase.DefaultConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.PhaseToSolverTerminationBridge;
import ai.timefold.solver.core.impl.solver.termination.Termination;

final class RuinRecreateConstructionHeuristicPhaseFactory<Solution_>
        extends DefaultConstructionHeuristicPhaseFactory<Solution_> {

    RuinRecreateConstructionHeuristicPhaseFactory(ConstructionHeuristicPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    protected DefaultConstructionHeuristicPhaseBuilder<Solution_> createBuilder(
            HeuristicConfigPolicy<Solution_> phaseConfigPolicy,
            Termination<Solution_> solverTermination, int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
            EntityPlacer<Solution_> entityPlacer) {
        var phaseTermination = new PhaseToSolverTerminationBridge<>(new BasicPlumbingTermination<Solution_>(false));
        return new RuinRecreateConstructionHeuristicPhaseBuilder<>(phaseTermination, entityPlacer,
                buildDecider(phaseConfigPolicy, phaseTermination));

    }

    @Override
    protected ConstructionHeuristicDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            Termination<Solution_> termination) {
        return new RuinRecreateConstructionHeuristicDecider<>(termination, buildForager(configPolicy));
    }

}
