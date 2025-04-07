package ai.timefold.solver.core.impl.localsearch.decider.forager;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchPickEarlyType;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.finalist.FinalistPodium;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * A {@link LocalSearchForager} which forages accepted moves and ignores unaccepted moves.
 *
 * @see LocalSearchForager
 * @see Acceptor
 */
public final class AcceptedLocalSearchForager<Solution_> extends AbstractLocalSearchForager<Solution_> {

    private final FinalistPodium<Solution_> finalistPodium;
    private final LocalSearchPickEarlyType pickEarlyType;
    private final int acceptedCountLimit;
    private final boolean breakTieRandomly;

    private long selectedMoveCount;
    private long acceptedMoveCount;
    private LocalSearchMoveScope<Solution_> earlyPickedMoveScope;

    public AcceptedLocalSearchForager(FinalistPodium<Solution_> finalistPodium,
            LocalSearchPickEarlyType pickEarlyType, int acceptedCountLimit, boolean breakTieRandomly) {
        this.finalistPodium = finalistPodium;
        this.pickEarlyType = pickEarlyType;
        this.acceptedCountLimit = acceptedCountLimit;
        if (acceptedCountLimit < 1) {
            throw new IllegalArgumentException("The acceptedCountLimit (" + acceptedCountLimit
                    + ") cannot be negative or zero.");
        }
        this.breakTieRandomly = breakTieRandomly;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        finalistPodium.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        finalistPodium.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        finalistPodium.stepStarted(stepScope);
        selectedMoveCount = 0L;
        acceptedMoveCount = 0L;
        earlyPickedMoveScope = null;
    }

    @Override
    public boolean supportsNeverEndingMoveSelector() {
        // TODO FIXME magical value Integer.MAX_VALUE coming from ForagerConfig
        return acceptedCountLimit < Integer.MAX_VALUE;
    }

    @Override
    public void addMove(LocalSearchMoveScope<Solution_> moveScope) {
        selectedMoveCount++;
        moveScope.getStepScope().getPhaseScope().addMoveEvaluationCount(moveScope.getMove(), 1);
        if (moveScope.getAccepted()) {
            acceptedMoveCount++;
            checkPickEarly(moveScope);
        }
        finalistPodium.addMove(moveScope);
    }

    private <Score_ extends Score<Score_>> void checkPickEarly(LocalSearchMoveScope<Solution_> moveScope) {
        switch (pickEarlyType) {
            case NEVER:
                break;
            case FIRST_BEST_SCORE_IMPROVING:
                var bestScore = moveScope.getStepScope().getPhaseScope().<Score_> getBestScore();
                if (moveScope.<Score_> getScore().compareTo(bestScore) > 0) {
                    earlyPickedMoveScope = moveScope;
                }
                break;
            case FIRST_LAST_STEP_SCORE_IMPROVING:
                var lastStepScore = moveScope.getStepScope().getPhaseScope()
                        .getLastCompletedStepScope().<Score_> getScore();
                if (moveScope.<Score_> getScore().compareTo(lastStepScore) > 0) {
                    earlyPickedMoveScope = moveScope;
                }
                break;
            default:
                throw new IllegalStateException("The pickEarlyType (" + pickEarlyType + ") is not implemented.");
        }
    }

    @Override
    public boolean isQuitEarly() {
        return earlyPickedMoveScope != null || acceptedMoveCount >= acceptedCountLimit;
    }

    @Override
    public LocalSearchMoveScope<Solution_> pickMove(LocalSearchStepScope<Solution_> stepScope) {
        stepScope.setSelectedMoveCount(selectedMoveCount);
        stepScope.setAcceptedMoveCount(acceptedMoveCount);
        if (earlyPickedMoveScope != null) {
            return earlyPickedMoveScope;
        }
        var finalistList = finalistPodium.getFinalistList();
        if (finalistList.isEmpty()) {
            return null;
        }
        if (finalistList.size() == 1 || !breakTieRandomly) {
            return finalistList.get(0);
        }
        int randomIndex = stepScope.getWorkingRandom().nextInt(finalistList.size());
        return finalistList.get(randomIndex);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        finalistPodium.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        finalistPodium.phaseEnded(phaseScope);
        selectedMoveCount = 0L;
        acceptedMoveCount = 0L;
        earlyPickedMoveScope = null;
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        finalistPodium.solvingEnded(solverScope);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + pickEarlyType + ", " + acceptedCountLimit + ")";
    }

}
