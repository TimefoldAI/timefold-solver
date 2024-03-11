package ai.timefold.solver.core.impl.localsearch;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing.SimulatedAnnealingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.AbstractTabuAcceptor;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.Phase;

/**
 * A {@link LocalSearchPhase} is a {@link Phase} which uses a Local Search algorithm,
 * such as {@link AbstractTabuAcceptor Tabu Search}, {@link SimulatedAnnealingAcceptor Simulated Annealing},
 * {@link LateAcceptanceAcceptor Late Acceptance}, ...
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Phase
 * @see AbstractPhase
 * @see DefaultLocalSearchPhase
 */
public interface LocalSearchPhase<Solution_> extends Phase<Solution_> {

}
