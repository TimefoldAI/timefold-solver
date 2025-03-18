package ai.timefold.solver.core.impl.localsearch.decider.acceptor.iteratedlocalsearch;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.RestartableAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.UnimprovedMoveCountStuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class AdaptiveLateAcceptanceAcceptor<Solution_> extends RestartableAcceptor<Solution_> {

    private static final int[] LATE_ELEMENTS_SIZE =
            new int[] { 2_500, 5_000, 12_500, 25_000, 25_000 };
    private static final int[] LATE_ELEMENTS_MAX_REJECTIONS =
            new int[] { 5_000, 5_000, 12_500, 25_000, 50_000 };
    private final LateAcceptanceAcceptor<Solution_> lateAcceptanceAcceptor;
    private int lateIndex;
    private Score<?> initialScore;

    public AdaptiveLateAcceptanceAcceptor(StuckCriterion<Solution_> stuckCriterion) {
        super(true, stuckCriterion);
        this.lateAcceptanceAcceptor = new LateAcceptanceAcceptor<>(false, false, null);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        initialScore = phaseScope.getBestScore();
        lateIndex = 0;
        lateAcceptanceAcceptor.setLateAcceptanceSize(LATE_ELEMENTS_SIZE[lateIndex]);
        lateAcceptanceAcceptor.phaseStarted(phaseScope);
        stuckCriterion.reset(phaseScope);
        if (stuckCriterion instanceof UnimprovedMoveCountStuckCriterion<Solution_> stepCountStuckCriterion) {
            stepCountStuckCriterion.setMaxRejected(LATE_ELEMENTS_MAX_REJECTIONS[lateIndex]);
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.lateAcceptanceAcceptor.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        lateAcceptanceAcceptor.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        lateAcceptanceAcceptor.stepEnded(stepScope);
    }

    @Override
    public boolean rejectRestartEvent() {
        // The restart events are always accepted
        return false;
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        lateIndex = (lateIndex + 1) % LATE_ELEMENTS_SIZE.length;
        var phaseScope = stepScope.getPhaseScope();
        var decider = phaseScope.getDecider();
        // Restore the current best solution
        decider.restoreCurrentBestSolution(stepScope);
        logger.info(
                "Restart event triggered, step count ({}), late elements size ({}), max rejections ({}), best score ({}), new perturbation score ({}),",
                stepScope.getStepIndex(), LATE_ELEMENTS_SIZE[lateIndex], LATE_ELEMENTS_MAX_REJECTIONS[lateIndex],
                stepScope.getPhaseScope().getBestScore(), initialScore);
        lateAcceptanceAcceptor.resetLateElementsScore(LATE_ELEMENTS_SIZE[lateIndex], (Score) initialScore);
        if (stuckCriterion instanceof UnimprovedMoveCountStuckCriterion<Solution_> stepCountStuckCriterion) {
            stepCountStuckCriterion.setMaxRejected(LATE_ELEMENTS_MAX_REJECTIONS[lateIndex]);
        }
        stuckCriterion.reset(stepScope.getPhaseScope());
    }

    @Override
    public boolean accept(LocalSearchMoveScope<Solution_> moveScope) {
        return lateAcceptanceAcceptor.isAccepted(moveScope);
    }
}
