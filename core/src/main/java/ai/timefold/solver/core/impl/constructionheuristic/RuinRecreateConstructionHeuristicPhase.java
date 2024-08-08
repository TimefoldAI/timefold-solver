package ai.timefold.solver.core.impl.constructionheuristic;

import java.util.List;

import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public final class RuinRecreateConstructionHeuristicPhase<Solution_>
        extends DefaultConstructionHeuristicPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    public RuinRecreateConstructionHeuristicPhase(RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
    }

    @Override
    protected boolean isNested() {
        return true;
    }

    public static final class RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_>
            extends DefaultConstructionHeuristicPhaseBuilder<Solution_> {

        private List<Object> elementsToRecreate;

        public RuinRecreateBuilderConstructionHeuristicPhaseBuilder(Termination<Solution_> phaseTermination,
                EntityPlacer<Solution_> entityPlacer, ConstructionHeuristicDecider<Solution_> decider) {
            super(0, false, "", phaseTermination, entityPlacer, decider);
        }

        public RuinRecreateBuilderConstructionHeuristicPhaseBuilder<Solution_> setElementsToRecreate(List<Object> elements) {
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
        public RuinRecreateConstructionHeuristicPhase<Solution_> build() {
            return new RuinRecreateConstructionHeuristicPhase<>(this);
        }
    }

}
