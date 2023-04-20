package ai.timefold.solver.core.impl.exhaustivesearch;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.Phase;

/**
 * A {@link ExhaustiveSearchPhase} is a {@link Phase} which uses an exhaustive algorithm, such as Brute Force.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Phase
 * @see AbstractPhase
 * @see DefaultExhaustiveSearchPhase
 */
public interface ExhaustiveSearchPhase<Solution_> extends Phase<Solution_> {

}
