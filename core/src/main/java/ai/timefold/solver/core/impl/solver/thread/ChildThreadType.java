package ai.timefold.solver.core.impl.solver.thread;

import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;

public enum ChildThreadType {
    /**
     * Used by {@link PartitionedSearchPhase}.
     */
    PART_THREAD,
    /**
     * Used by multithreaded incremental solving.
     */
    MOVE_THREAD,
    /**
     * Used by multithreaded evolutionary algorithm.
     */
    EVOLUTIONARY_AGENT_THREAD;
}
