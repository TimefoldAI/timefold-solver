package ai.timefold.solver.core.api.solver.phase;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;

/**
 * The type of phase (for example, a Construction Heuristic).
 */
public enum PhaseType {

    /**
     * The phase configured with {@link ConstructionHeuristicPhaseConfig}.
     */
    CONSTRUCTION_HEURISTIC("Construction Heuristic"),
    /**
     * The phase configured with {@link LocalSearchPhaseConfig}.
     */
    LOCAL_SEARCH("Local Search"),
    /**
     * The phase configured with {@link ExhaustiveSearchPhaseConfig}.
     */
    EXHAUSTIVE_SEARCH("Exhaustive Search"),
    /**
     * The phase configured with {@link PartitionedSearchPhaseConfig}.
     */
    PARTITIONED_SEARCH("Partitioned Search"),
    /**
     * The phase configured with {@link CustomPhaseConfig}.
     */
    CUSTOM_PHASE("Custom Phase");

    private final String phaseName;

    PhaseType(String phaseName) {
        this.phaseName = phaseName;
    }

    /**
     * @return a label for messages, such as {@code "Local Search"}
     */
    public String getPhaseName() {
        return phaseName;
    }

}
