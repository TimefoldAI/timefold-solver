package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import static java.util.stream.Collectors.joining;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.RestartableAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class LateAcceptanceAcceptor<Solution_> extends RestartableAcceptor<Solution_> {

    protected static final double MIN_DIVERSITY_RATIO = 0.05;
    protected static final int GEOMETRIC_FACTOR = 3;

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    private final boolean enableStepIntensification;
    private Score<?> currentBestScore;
    // Keep track of the best scores accumulated so far. This list will be used to reseed the later elements list.
    protected Deque<Score<?>> bestScoreQueue;
    protected int defaultLateAcceptanceSize;
    protected int maxBestScoreSize;
    protected int coefficient;
    private boolean allowDecrease;

    public LateAcceptanceAcceptor(boolean enableRestart, StuckCriterion<Solution_> stuckCriterionDetection) {
        this(true, enableRestart, stuckCriterionDetection);
    }

    public LateAcceptanceAcceptor(boolean enableStepIntensification, boolean enableRestart,
            StuckCriterion<Solution_> stuckCriterionDetection) {
        super(enableRestart, stuckCriterionDetection);
        this.enableStepIntensification = enableStepIntensification;
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
        coefficient = 1;
        allowDecrease = false;
        maxBestScoreSize = 50;
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
    public boolean accept(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        var lateScore = previousScores[lateScoreIndex];
        var lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        var accept = moveScore.compareTo(lateScore) >= 0 || moveScore.compareTo(lastStepScore) >= 0;
        if (enableStepIntensification && accept) {
            previousScores[lateScoreIndex] = moveScore;
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        } else {
            if (accept) {
                previousScores[lateScoreIndex] = moveScore;
            } else {
                previousScores[lateScoreIndex] = lastStepScore;
            }
            lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        }
        return accept;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (enableRestart && currentBestScore != null && ((Score) currentBestScore).compareTo(stepScope.getScore()) < 0) {
            var levels = stepScope.getScore().toLevelDoubles();
            var currentLevels = currentBestScore.toLevelDoubles();
            if (levels.length == 3 && levels[1] != currentLevels[1]) {
                logger.info("New best score ({}), Previous best score ({}), index ({}), late elements [{}]",
                        stepScope.getScore(), currentBestScore, lateScoreIndex,
                        Arrays.asList(previousScores).stream().map(Objects::toString).collect(joining(", ")));
            }
            if (allowDecrease) {
                // We decrease the coefficient
                // if there is an improvement
                // to avoid altering the late element size in the next restart event.
                // This action is performed only once after a restart event
                coefficient /= GEOMETRIC_FACTOR;
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
            logger.info(
                    "Restart event delayed. Diversity ({}), Count best scores ({}), Distinct Elements ({})",
                    diversity, bestScoreQueue.size(), distinctElements);
        }
        return reject;
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        coefficient *= GEOMETRIC_FACTOR;
        allowDecrease = true;
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
        var scoreQueueSize = bestScoreQueue.size();
        if (scoreQueueSize > 2) {
            scoreQueueSize--;
        }
        var countPerScore = newLateAcceptanceSize / scoreQueueSize + 1;
        var count = newLateAcceptanceSize - 1;
        var iterator = bestScoreQueue.descendingIterator();
        if (scoreQueueSize > 2) {
            // We discard the current best score
            iterator.next();
        }
        while (count >= 0 && iterator.hasNext()) {
            var score = iterator.next();
            for (var i = 0; i < countPerScore; i++) {
                newPreviousScores[count] = score;
                count--;
                if (count < 0) {
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

    public <Score_ extends Score<Score_>> void resetLateElementsScore(int size, Score_ score) {
        this.lateScoreIndex = 0;
        this.lateAcceptanceSize = size;
        this.previousScores = new Score[size];
        Arrays.fill(previousScores, score);
    }

}
