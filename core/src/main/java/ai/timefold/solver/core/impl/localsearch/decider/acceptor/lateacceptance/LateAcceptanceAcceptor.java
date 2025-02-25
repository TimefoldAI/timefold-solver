package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.RestartableAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.util.MutableInt;

public class LateAcceptanceAcceptor<Solution_> extends RestartableAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    private int maxBestScoreSize;
    private Score<?> currentBestScore;
    private Deque<Score<?>> bestScoreQueue;
    protected static final int SCALING_FACTOR = 10;
    protected int defaultLateAcceptanceSize;
    protected int coefficient;

    public LateAcceptanceAcceptor(StuckCriterion<Solution_> stuckCriterionDetection) {
        super(stuckCriterionDetection);
    }

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
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        currentBestScore = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        previousScores = new Score[lateAcceptanceSize];
        var initialScore = phaseScope.getBestScore();
        Arrays.fill(previousScores, initialScore);
        lateScoreIndex = 0;
        coefficient = 0;
        defaultLateAcceptanceSize = lateAcceptanceSize;
        maxBestScoreSize = defaultLateAcceptanceSize * 3 * SCALING_FACTOR;
        bestScoreQueue = new ArrayDeque<>(maxBestScoreSize);
        bestScoreQueue.addLast(initialScore);
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean accept(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        var lateScore = previousScores[lateScoreIndex];
        if (moveScore.compareTo(lateScore) >= 0) {
            return true;
        }
        if (hillClimbingEnabled) {
            var lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
            return moveScore.compareTo(lastStepScore) >= 0;
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        previousScores[lateScoreIndex] = stepScope.getScore();
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        if (((Score) currentBestScore).compareTo(stepScope.getScore()) < 0) {
            if (bestScoreQueue.size() < maxBestScoreSize) {
                bestScoreQueue.addLast(stepScope.getScore());
            } else {
                bestScoreQueue.poll();
                bestScoreQueue.addLast(stepScope.getScore());
            }
        }
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        coefficient++;
        var newLateAcceptanceSize = defaultLateAcceptanceSize * coefficient * SCALING_FACTOR;
        rebuildLateElementsList(newLateAcceptanceSize);
    }

    private void rebuildLateElementsList(int newLateAcceptanceSize) {
        var newPreviousScores = new Score[newLateAcceptanceSize];
        if (logger.isInfoEnabled()) {
            logger.info("Changing the lateAcceptanceSize from %d to %d.".formatted(lateAcceptanceSize, newLateAcceptanceSize));
        }
        var countPerScore = newLateAcceptanceSize / bestScoreQueue.size() + 1;
        var count = new MutableInt(newLateAcceptanceSize - 1);
        var iterator = bestScoreQueue.descendingIterator();
        while (count.intValue() >= 0 && iterator.hasNext()) {
            var score = iterator.next();
            for (var i = 0; i < countPerScore; i++) {
                newPreviousScores[count.intValue()] = score;
                count.decrement();
                if (count.intValue() < 0) {
                    break;
                }
            }
        }
        this.previousScores = newPreviousScores;
        this.lateAcceptanceSize = newLateAcceptanceSize;
        this.lateScoreIndex = 0;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
    }

}
