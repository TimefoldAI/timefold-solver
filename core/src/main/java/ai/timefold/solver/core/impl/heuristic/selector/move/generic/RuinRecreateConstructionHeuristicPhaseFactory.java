package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase.DefaultConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

final class RuinRecreateConstructionHeuristicPhaseFactory<Solution_>
        extends DefaultConstructionHeuristicPhaseFactory<Solution_> {

    RuinRecreateConstructionHeuristicPhaseFactory(ConstructionHeuristicPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    protected DefaultConstructionHeuristicPhaseBuilder<Solution_> createBuilder(
            HeuristicConfigPolicy<Solution_> phaseConfigPolicy, SolverTermination<Solution_> solverTermination, int phaseIndex,
            boolean lastInitializingPhase, EntityPlacer<Solution_> entityPlacer) {
        var phaseTermination = UniversalTermination.bridge(new BasicPlumbingTermination<Solution_>(false));
        return new RuinRecreateConstructionHeuristicPhaseBuilder<>(phaseConfigPolicy, this, phaseTermination, entityPlacer,
                buildDecider(phaseConfigPolicy, phaseTermination));

    }

    @Override
    protected ConstructionHeuristicDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination) {
        return new RuinRecreateConstructionHeuristicDecider<>(termination, buildForager(configPolicy));
    }

}
