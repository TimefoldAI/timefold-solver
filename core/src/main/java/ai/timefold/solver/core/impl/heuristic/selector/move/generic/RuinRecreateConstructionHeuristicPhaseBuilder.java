package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.List;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.solver.termination.Termination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;

public final class RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>
        extends DefaultConstructionHeuristicPhase.DefaultConstructionHeuristicPhaseBuilder<Solution_> {

    public static <Solution_> RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>
            create(HeuristicConfigPolicy<Solution_> solverConfigPolicy) {
        var constructionHeuristicConfig = new ConstructionHeuristicPhaseConfig();
        return create(solverConfigPolicy, constructionHeuristicConfig);
    }

    public static <Solution_> RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> create(
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, ConstructionHeuristicPhaseConfig constructionHeuristicConfig) {
        var constructionHeuristicPhaseFactory =
                new RuinRecreateConstructionHeuristicPhaseFactory<Solution_>(constructionHeuristicConfig);
        return (RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>) constructionHeuristicPhaseFactory.getBuilder(0, false,
                solverConfigPolicy, TerminationFactory.<Solution_> create(new TerminationConfig())
                        .buildTermination(solverConfigPolicy));
    }

    private List<Object> elementsToRecreate;

    RuinRecreateConstructionHeuristicPhaseBuilder(Termination<Solution_> phaseTermination,
            EntityPlacer<Solution_> entityPlacer, ConstructionHeuristicDecider<Solution_> decider) {
        super(0, false, "", phaseTermination, entityPlacer, decider);
    }

    public RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> withElementsToRecreate(List<Object> elements) {
        this.elementsToRecreate = elements;
        return this;
    }

    @Override
    public EntityPlacer<Solution_> getEntityPlacer() {
        if (elementsToRecreate == null || elementsToRecreate.isEmpty()) {
            return super.getEntityPlacer();
        }
        return super.getEntityPlacer().rebuildWithFilter((scoreDirector, selection) -> {
            for (var element : elementsToRecreate) {
                if (selection == element) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public DefaultConstructionHeuristicPhase<Solution_> build() {
        return new RuinRecreateConstructionHeuristicPhase<>(this);
    }
}
