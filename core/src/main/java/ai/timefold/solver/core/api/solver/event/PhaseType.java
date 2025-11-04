package ai.timefold.solver.core.api.solver.event;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.NoChangePhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.impl.phase.NoChangePhase;

/**
 * The type of phase (for example, a Construction Heuristic).
 */
public enum PhaseType {
    /**
     * The type of phase associated with {@link NoChangePhaseConfig}.
     * 
     * @deprecated Deprecated on account of {@link NoChangePhase} having no use.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    NO_CHANGE("No Change"),
    /**
     * The type of phase associated with {@link ConstructionHeuristicPhaseConfig}.
     */
    CONSTRUCTION_HEURISTIC("Construction Heuristics"),
    /**
     * The type of phase associated with {@link LocalSearchPhaseConfig}.
     */
    LOCAL_SEARCH("Local Search"),
    /**
     * The type of phase associated with {@link ExhaustiveSearchPhaseConfig}.
     */
    EXHAUSTIVE_SEARCH("Exhaustive Search"),
    /**
     * The type of phase associated with {@link PartitionedSearchPhaseConfig}.
     */
    PARTITIONED_SEARCH("Partitioned Search"),
    /**
     * The type of phase associated with {@link CustomPhaseConfig}.
     */
    CUSTOM_PHASE("Custom Phase");

    private final String phaseName;

    PhaseType(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getPhaseName() {
        return phaseName;
    }
}
