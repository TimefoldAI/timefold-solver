package ai.timefold.solver.core.impl.localsearch.decider.acceptor.hillclimbing;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class HillClimbingAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        InnerScore moveScore = moveScope.getScore();
        InnerScore lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        return moveScore.compareTo(lastStepScore) >= 0;
    }

}
