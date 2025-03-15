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

    protected static final double MIN_DIVERSITY_RATIO = 0.05;
    protected static final int SCALE_FACTOR = 3;
    private static final int MAX_REJECTED_EVENTS = 3;

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    private Score<?> bestStepScore;
    private Score<?> currentBestScore;
    // Keep track of the best scores accumulated so far. This list will be used to reseed the later elements list.
    protected Deque<Score<?>> bestScoreQueue;
    protected int defaultLateAcceptanceSize;
    protected int maxBestScoreSize;
    protected int coefficient;
    private boolean allowDecrease;
    private int countRejected;

    public LateAcceptanceAcceptor(boolean enableRestart, StuckCriterion<Solution_> stuckCriterionDetection) {
        super(enableRestart, stuckCriterionDetection);
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
        bestStepScore = null;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        previousScores = new Score[lateAcceptanceSize];
        var initialScore = phaseScope.getBestScore();
        Arrays.fill(previousScores, initialScore);
        lateScoreIndex = 0;
        coefficient = 1;
        allowDecrease = false;
        countRejected = 0;
        maxBestScoreSize = lateAcceptanceSize * 3;
        bestScoreQueue = new ArrayDeque<>(maxBestScoreSize);
        bestScoreQueue.addLast(initialScore);
        defaultLateAcceptanceSize = lateAcceptanceSize;
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        if (moveScore.compareTo(moveScope.getStepScope().getPhaseScope().getBestScore()) != 0
                && (bestStepScore == null || moveScore.compareTo(bestStepScore) > 0)) {
            bestStepScore = moveScore;
        }
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
            if (allowDecrease) {
                // We decrease the coefficient
                // if there is an improvement
                // to avoid altering the late element size in the next restart event.
                // This action is performed only once after a restart event
                coefficient /= SCALE_FACTOR;
                allowDecrease = false;
            }
            if (bestScoreQueue.size() < maxBestScoreSize) {
                bestScoreQueue.addLast(stepScope.getScore());
            } else {
                // When the collection is full, we remove the lowest score and add the new best value.
                bestScoreQueue.poll();
                bestScoreQueue.addLast(stepScope.getScore());
            }
        }
    }

    @Override
    public boolean rejectRestartEvent() {
        var distinctElements = Arrays.stream(previousScores).distinct().count();
        var diversity = distinctElements == 1 ? 0 : distinctElements / (double) lateAcceptanceSize;
        // We prefer not to restart until it is really necessary to keep the default behavior,
        // and we have observed that this approach works better for more complex datasets.
        // However, when the diversity is low, it indicates that the LA may be stuck in a local minimum,
        // and in such cases, we should not reject the event.
        // Additionally, when there is only one best score,
        // it does not make sense to restart as nothing would change
        // and the proposed approach requires some diversity to reseed the scores.
        var reject = diversity > MIN_DIVERSITY_RATIO || bestScoreQueue.size() == 1;
        if (reject) {
            countRejected++;
            if (countRejected > MAX_REJECTED_EVENTS && bestScoreQueue.size() > 1) {
                logger.info(
                        "Restart event not rejected. Diversity ({}), Count best scores ({}), Distinct Elements ({}), Rejection Count ({})",
                        diversity, bestScoreQueue.size(), distinctElements, countRejected);
                return false;
            }
            logger.info(
                    "Restart event delayed. Diversity ({}), Count best scores ({}), Distinct Elements ({}), Rejection Count ({})",
                    diversity, bestScoreQueue.size(), distinctElements, countRejected);
        }
        return reject;
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        coefficient *= SCALE_FACTOR;
        allowDecrease = true;
        countRejected = 0;
        var newLateAcceptanceSize = defaultLateAcceptanceSize * coefficient;
        if (logger.isInfoEnabled()) {
            if (lateAcceptanceSize == newLateAcceptanceSize) {
                logger.info("Keeping the lateAcceptanceSize as {}.", lateAcceptanceSize);
            } else {
                logger.info("Changing the lateAcceptanceSize from {} to {}.", lateAcceptanceSize, newLateAcceptanceSize);
            }
        }
        rebuildLateElementsList(newLateAcceptanceSize);
    }

    /**
     * The method recreates the late elements list with the new size {@code newLateAcceptanceSize}.
     * The aim is to recreate elements using previous best scores.
     * This method provides diversity
     * while avoiding the initialization phase
     * that occurs when the later elements are set to the most recent best score.
     * <p>
     * The approach is based on the work:
     * Parameter-less Late Acceptance Hill-climbing: Foundations & Applications by Mosab Bazargani.
     */
    private void rebuildLateElementsList(int newLateAcceptanceSize) {
        var newPreviousScores = new Score[newLateAcceptanceSize];
        var countPerScore = Math.min(newLateAcceptanceSize / 2, (newLateAcceptanceSize / bestScoreQueue.size()) * 2);
        var count = new MutableInt(0);
        var iterator = bestScoreQueue.iterator();
        while (count.intValue() < newLateAcceptanceSize && iterator.hasNext()) {
            var score = iterator.next();
            for (var i = 0; i < countPerScore; i++) {
                newPreviousScores[count.intValue()] = score;
                count.increment();
                if (count.intValue() == newLateAcceptanceSize) {
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
