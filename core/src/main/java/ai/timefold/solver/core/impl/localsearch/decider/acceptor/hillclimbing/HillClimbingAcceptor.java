package ai.timefold.solver.core.impl.localsearch.decider.acceptor.hillclimbing;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.bound.MinimumScoreBound;
import ai.timefold.solver.core.impl.score.bound.StepBasedMinimumScoreBound;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class HillClimbingAcceptor<Solution_> extends AbstractAcceptor<Solution_> {
    private final StepBasedMinimumScoreBound minimumScoreBound = new StepBasedMinimumScoreBound<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isStructurallyValidSolutionAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        InnerScore moveScore = moveScope.getScore();
        InnerScore lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        return moveScore.compareTo(lastStepScore) >= 0;
    }

    @Override
    public <Score_ extends Score<Score_>> MinimumScoreBound<Score_> getMinimumScoreBound() {
        return minimumScoreBound;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        Score lastStepScore = stepScope.getPhaseScope().getLastCompletedStepScope().getScore().raw();
        minimumScoreBound.setMinimumRequiredScoreForStep(lastStepScore);
    }
}
