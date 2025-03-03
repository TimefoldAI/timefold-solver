package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

final class RuinRecreateConstructionHeuristicDecider<Solution_>
        extends ConstructionHeuristicDecider<Solution_> {

    RuinRecreateConstructionHeuristicDecider(PhaseTermination<Solution_> termination,
            ConstructionHeuristicForager<Solution_> forager) {
        super("", termination, forager);
    }

    @Override
    public boolean isLoggingEnabled() {
        return false;
    }

    @Override
    public void enableAssertions(EnvironmentMode environmentMode) {
        throw new UnsupportedOperationException(
                "Impossible state: Construction heuristics inside Ruin and Recreate moves cannot be asserted.");
    }
}
