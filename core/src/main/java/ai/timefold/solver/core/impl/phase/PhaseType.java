package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.exhaustivesearch.ExhaustiveSearchPhase;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.localsearch.LocalSearchPhase;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.phase.custom.CustomPhase;

/**
 * The type of phase (for example, a Construction Heuristic).
 */
public enum PhaseType {

    /**
     * The type of phase associated with {@link ConstructionHeuristicPhase}.
     */
    CONSTRUCTION_HEURISTIC("Construction Heuristic"),
    /**
     * The type of phase associated with {@link RuinRecreateConstructionHeuristicPhase}
     */
    RUIN_AND_RECREATE_CONSTRUCTION_HEURISTIC("Ruin & Recreate Construction Heuristic"),
    /**
     * The type of phase associated with {@link LocalSearchPhase}.
     */
    LOCAL_SEARCH("Local Search"),
    /**
     * The type of phase associated with {@link ExhaustiveSearchPhase}.
     */
    EXHAUSTIVE_SEARCH("Exhaustive Search"),
    /**
     * The type of phase associated with {@link PartitionedSearchPhase}.
     */
    PARTITIONED_SEARCH("Partitioned Search"),
    /**
     * The type of phase associated with {@link CustomPhase}.
     */
    CUSTOM_PHASE("Custom Phase");

    private final String phaseName;

    PhaseType(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getPhaseName() {
        return phaseName;
    }

    @Override
    public String toString() {
        return phaseName;
    }
}
