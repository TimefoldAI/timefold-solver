package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.preview.api.move.Move;

public abstract class AbstractAcceptorTest {

    protected <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(
            LocalSearchStepScope<Solution_> stepScope, int score) {
        Move<Solution_> move = mock(Move.class);
        var moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setInitializedScore(SimpleScore.of(score));
        return moveScope;
    }

}
