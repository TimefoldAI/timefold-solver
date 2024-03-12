package ai.timefold.solver.core.impl.localsearch.decider.acceptor.hillclimbing;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

public class HillClimbingAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        Score moveScore = moveScope.getScore();
        Score lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        return moveScore.compareTo(lastStepScore) >= 0;
    }

}
