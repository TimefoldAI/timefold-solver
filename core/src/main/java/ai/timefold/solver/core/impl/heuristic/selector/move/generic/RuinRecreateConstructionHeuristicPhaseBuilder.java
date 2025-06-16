package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase.DefaultConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;

public final class RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>
        extends DefaultConstructionHeuristicPhaseBuilder<Solution_> {

    public static <Solution_> RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>
            create(HeuristicConfigPolicy<Solution_> solverConfigPolicy, EntitySelectorConfig entitySelectorConfig) {
        var queuedEntityPlacerConfig = new QueuedEntityPlacerConfig()
                .withEntitySelectorConfig(entitySelectorConfig);
        var constructionHeuristicConfig = new ConstructionHeuristicPhaseConfig()
                .withEntityPlacerConfig(queuedEntityPlacerConfig);
        return create(solverConfigPolicy, constructionHeuristicConfig);
    }

    public static <Solution_> RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> create(
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, ConstructionHeuristicPhaseConfig constructionHeuristicConfig) {
        var constructionHeuristicPhaseFactory =
                new RuinRecreateConstructionHeuristicPhaseFactory<Solution_>(constructionHeuristicConfig);
        var builder = (RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>) constructionHeuristicPhaseFactory.getBuilder(0,
                false,
                solverConfigPolicy, (SolverTermination<Solution_>) TerminationFactory
                        .<Solution_> create(new TerminationConfig()).buildTermination(solverConfigPolicy));
        if (solverConfigPolicy.getMoveThreadCount() != null && solverConfigPolicy.getMoveThreadCount() >= 1) {
            builder.multithreaded = true;
        }
        return builder;
    }

    private final HeuristicConfigPolicy<Solution_> configPolicy;
    private final RuinRecreateConstructionHeuristicPhaseFactory<Solution_> constructionHeuristicPhaseFactory;
    private final PhaseTermination<Solution_> phaseTermination;

    Set<Object> elementsToRuin;
    List<Object> elementsToRecreate;
    private boolean multithreaded = false;

    RuinRecreateConstructionHeuristicPhaseBuilder(HeuristicConfigPolicy<Solution_> configPolicy,
            RuinRecreateConstructionHeuristicPhaseFactory<Solution_> constructionHeuristicPhaseFactory,
            PhaseTermination<Solution_> phaseTermination, EntityPlacer<Solution_> entityPlacer,
            ConstructionHeuristicDecider<Solution_> decider) {
        super(0, false, "", phaseTermination, entityPlacer, decider);
        this.configPolicy = configPolicy;
        this.constructionHeuristicPhaseFactory = constructionHeuristicPhaseFactory;
        this.phaseTermination = phaseTermination;
    }

    /**
     * In a multithreaded environment, the builder will be shared among all moves and threads.
     * Consequently, the list {@code elementsToRecreate} used by {@code getEntityPlacer} or the {@code decider},
     * will be shared between the main and move threads.
     * This sharing can lead to race conditions.
     * The method creates a new copy of the builder and the decider to avoid race conditions.
     */
    public RuinRecreateConstructionHeuristicPhaseBuilder<Solution_>
            ensureThreadSafe(InnerScoreDirector<Solution_, ?> scoreDirector) {
        if (multithreaded && scoreDirector.isDerived()) {
            return new RuinRecreateConstructionHeuristicPhaseBuilder<>(configPolicy, constructionHeuristicPhaseFactory,
                    phaseTermination, super.getEntityPlacer().copy(),
                    constructionHeuristicPhaseFactory.buildDecider(configPolicy, phaseTermination));
        }
        return this;
    }

    public RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> withElementsToRecreate(List<Object> elements) {
        this.elementsToRecreate = elements;
        return this;
    }

    public RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> withElementsToRuin(Set<Object> elements) {
        this.elementsToRuin = elements;
        return this;
    }

    @Override
    public EntityPlacer<Solution_> getEntityPlacer() {
        var placer = super.getEntityPlacer();
        if (elementsToRecreate == null || elementsToRecreate.isEmpty()) {
            return placer;
        }
        return placer.rebuildWithFilter((scoreDirector, selection) -> {
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
