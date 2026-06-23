package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    LateAcceptanceScoreBuffer scoreBuffer;
    private LevelScoreState<Solution_> bestScoreState;

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        var initialScore = phaseScope.getBestScore();
        scoreBuffer = new LateAcceptanceScoreBuffer(lateAcceptanceSize, initialScore);
        var scoreDefinition = phaseScope.getSolverScope().getScoreDefinition();
        if (scoreDefinition.getLevelsSize() > 1) {
            bestScoreState = new DefaultLevelScoreState<>(initialScore, scoreDefinition);
        } else {
            bestScoreState = new NoOpLevelScoreState<>();
        }
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = (InnerScore) moveScope.getScore();
        var lateScore = scoreBuffer.getCurrent();
        if (moveScore.compareTo(lateScore) >= 0) {
            return true;
        }
        if (hillClimbingEnabled) {
            var lastStepScore = moveScope.getStepScope().getPhaseScope()
                    .getLastCompletedStepScope().getScore();
            return moveScore.compareTo(lastStepScore) >= 0;
        }
        return false;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        bestScoreState.update(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        scoreBuffer.update(stepScope.getScore());
        if (bestScoreState.isNonDominatedLevelChanged(stepScope)) {
            scoreBuffer.tryReset(stepScope.getPhaseScope().getBestScore());
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        scoreBuffer = null;
        bestScoreState = null;
    }

}
