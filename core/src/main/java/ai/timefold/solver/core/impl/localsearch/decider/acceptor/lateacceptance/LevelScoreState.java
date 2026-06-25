package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * Tracks whether the non-dominated score levels (hard or medium) have changed,
 * so {@link LateAcceptanceAcceptor} can decide when to reset the {@link LateAcceptanceScoreBuffer}.
 * <p>
 * {@link DefaultLevelScoreState} is used when the score has more than one level and non-dominated
 * level tracking is meaningful.
 * {@link NoOpLevelScoreState} is used for single-level scores, where no reset is ever needed.
 * 
 * @see DefaultLevelScoreState
 * @see NoOpLevelScoreState
 */
sealed interface LevelScoreState<Solution_> permits DefaultLevelScoreState, NoOpLevelScoreState {

    void update(LocalSearchStepScope<Solution_> stepScope);

    boolean isNonDominatedLevelChanged(LocalSearchStepScope<Solution_> stepScope);
}
