package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * An Acceptor accepts or rejects a selected {@link Move}.
 * Note that the {@link LocalSearchForager} can still ignore the advice of the {@link Acceptor}.
 *
 * @see AbstractAcceptor
 */
public interface Acceptor<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * @param moveScope not null
     * @return true if accepted
     */
    boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope);

    /**
     * The method validates whether the acceptor has reached a state that requires reconfiguration.
     * One example of such a state is when the solver gets stuck at a local optimum
     * and needs some reconfiguration to help it escape from it and continue evaluating the solution space.
     */
    default boolean needReconfiguration(LocalSearchStepScope<Solution_> stepScope) {
        // By default, the acceptors don't implement the reconfiguration strategy
        return false;
    }

    /**
     * Apply a reconfiguration that influences how the solver explores the solution space.
     * The reconfiguration can be different for each of the available metaheuristics.
     * <p>
     * One typical example is the reheat operation used in the Simulated Annealing method.
     */
    default void applyReconfiguration(LocalSearchStepScope<Solution_> stepScope) {
        // By default, executes nothing
    }
}
