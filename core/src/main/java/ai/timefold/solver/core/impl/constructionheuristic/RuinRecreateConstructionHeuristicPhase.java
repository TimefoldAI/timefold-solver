package ai.timefold.solver.core.impl.constructionheuristic;

import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public final class RuinRecreateConstructionHeuristicPhase<Solution_>
        extends DefaultConstructionHeuristicPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    public RuinRecreateConstructionHeuristicPhase(RuinRecreateBuilder<Solution_> builder) {
        super(builder);
    }

    @Override
    protected boolean isNested() {
        return true;
    }

    public static final class RuinRecreateBuilder<Solution_> extends Builder<Solution_> {

        public RuinRecreateBuilder(Termination<Solution_> phaseTermination, EntityPlacer<Solution_> entityPlacer,
                ConstructionHeuristicDecider<Solution_> decider) {
            super(0, false, "", phaseTermination, entityPlacer, decider);
        }

        @Override
        public DefaultConstructionHeuristicPhase<Solution_> build() {
            return new RuinRecreateConstructionHeuristicPhase<>(this);
        }
    }

}
