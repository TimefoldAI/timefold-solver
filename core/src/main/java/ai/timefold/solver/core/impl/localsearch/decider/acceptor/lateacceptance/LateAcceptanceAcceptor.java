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
    // The goal is to increase from hundreds to thousands in the first restart event and then increment it linearly
    protected static final int SCALING_FACTOR = 10;

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    private int maxBestScoreSize;
    private Score<?> bestStepScore;
    private Score<?> currentBestScore;
    // Keep track of the best scores accumulated so far. This list will be used to reseed the later elements list.
    private Deque<Score<?>> bestScoreQueue;
    protected int defaultLateAcceptanceSize;
    protected int coefficient;
    protected int countRestartWithoutImprovement;

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
        coefficient = 0;
        countRestartWithoutImprovement = 0;
        defaultLateAcceptanceSize = lateAcceptanceSize;
        // The maximum size is three times the size of the initial element list
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
            if (countRestartWithoutImprovement > 0 && coefficient > 0) {
                // We decrease the coefficient
                // if there is an improvement
                // to avoid altering the late element size in the next restart event.
                // This action is performed only once after a restart event
                coefficient--;
            }
            countRestartWithoutImprovement = 0;
            if (bestScoreQueue.size() < maxBestScoreSize) {
                bestScoreQueue.addLast(stepScope.getScore());
            } else {
                // // When the collection is full, we remove the lowest score and add the new best value.
                bestScoreQueue.poll();
                bestScoreQueue.addLast(stepScope.getScore());
            }
        }
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        countRestartWithoutImprovement++;
        var distinctElements = Arrays.stream(previousScores).distinct().count();
        var diversity = distinctElements == 1 ? 0 : distinctElements / (double) lateAcceptanceSize;
        if (coefficient == 0 && (diversity > MIN_DIVERSITY_RATIO || bestScoreQueue.size() == 1)) {
            // We prefer not to restart until the first event has passed (about 10:30 minutes).
            // We have observed that this approach works better for more complex datasets.
            // However, when the diversity is zero, it indicates that the LA may be stuck in a local minimum,
            // and in such cases, we should restart before the first event.
            // Additionally, when there is only one best score,
            // it does not make sense to restart at the first event as nothing would change.
            logger.info("Restart event delayed. Diversity ({}), Count best scores ({}), Distinct Elements ({}), Restart without Improvement ({})",
                    bestScoreQueue.size(), diversity, distinctElements, countRestartWithoutImprovement);
            return;
        }
        coefficient++;
        var newLateAcceptanceSize = defaultLateAcceptanceSize * coefficient * SCALING_FACTOR;
        if (logger.isInfoEnabled()) {
            if (lateAcceptanceSize == newLateAcceptanceSize) {
                logger.info("Keeping the lateAcceptanceSize as {}. Diversity ({}), Distinct Elements ({})", lateAcceptanceSize,
                        diversity, distinctElements);
            } else {
                logger.info(
                        "Changing the lateAcceptanceSize from {} to {}. Diversity ({}), Distinct Elements ({})",
                        lateAcceptanceSize, newLateAcceptanceSize, diversity, distinctElements);
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
        if (bestScoreQueue.size() == 1) {
            logger.info("No diversity. Using {} and {}", bestStepScore, bestScoreQueue.getFirst());
            // There is only one best score, which is the one found by the previous phase.
            Arrays.fill(newPreviousScores, 0, newLateAcceptanceSize / 2, bestStepScore);
            Arrays.fill(newPreviousScores, newLateAcceptanceSize / 2, newLateAcceptanceSize, bestScoreQueue.getFirst());
        } else {
            // It is possible to add some variability
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
