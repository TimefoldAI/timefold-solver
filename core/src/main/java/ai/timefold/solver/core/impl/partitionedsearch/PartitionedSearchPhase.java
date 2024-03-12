package ai.timefold.solver.core.impl.partitionedsearch;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.Phase;

/**
 * A {@link PartitionedSearchPhase} is a {@link Phase} which uses a Partition Search algorithm.
 * It splits the {@link PlanningSolution} into pieces and solves those separately with other {@link Phase}s.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Phase
 * @see AbstractPhase
 */
public interface PartitionedSearchPhase<Solution_> extends Phase<Solution_> {

}
