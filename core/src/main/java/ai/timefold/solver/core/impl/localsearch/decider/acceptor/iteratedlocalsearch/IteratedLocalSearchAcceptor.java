package ai.timefold.solver.core.impl.localsearch.decider.acceptor.iteratedlocalsearch;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.RestartableAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.UnimprovedMoveCountStuckCriterion;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class IteratedLocalSearchAcceptor<Solution_> extends RestartableAcceptor<Solution_> {

    private static final int[] LATE_ELEMENTS_SIZE =
            new int[] { 12_500, 25_000, 25_000, 50_000, 50_000 };
    private static final int[] LATE_ELEMENTS_MAX_REJECTIONS =
            new int[] { 12_500, 25_000, 50_000, 50_000, 75_000 };
    private final LateAcceptanceAcceptor<Solution_> lateAcceptanceAcceptor;
    private MoveSelector<Solution_> perturbationMoveSelector;
    private final int maxPerturbationCount;
    private int lateIndex;
    private int perturbationCount;
    private Score<?> currentBestScore;

    public IteratedLocalSearchAcceptor(int maxPerturbationCount, StuckCriterion<Solution_> stuckCriterion) {
        super(true, stuckCriterion);
        this.maxPerturbationCount = maxPerturbationCount;
        this.lateAcceptanceAcceptor = new LateAcceptanceAcceptor<>(false, false, null);
    }

    public void setPerturbationMoveSelector(MoveSelector<Solution_> perturbationMoveSelector) {
        this.perturbationMoveSelector = perturbationMoveSelector;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        perturbationCount = 1;
        lateIndex = 0;
        lateAcceptanceAcceptor.setLateAcceptanceSize(LATE_ELEMENTS_SIZE[perturbationCount]);
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
        currentBestScore = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        lateAcceptanceAcceptor.stepEnded(stepScope);
        if (((Score) stepScope.getScore()).compareTo(currentBestScore) > 0) {
            perturbationCount = 1;
        }
    }

    @Override
    public boolean rejectRestartEvent() {
        // The restart events are always accepted
        return false;
    }

    @Override
    public void restart(LocalSearchStepScope<Solution_> stepScope) {
        lateIndex = (lateIndex + 1) % LATE_ELEMENTS_SIZE.length;
        var lastCompletedStepScore = stepScope.getPhaseScope().getLastCompletedStepScope().getScore();
        var phaseScope = stepScope.getPhaseScope();
        var decider = phaseScope.getDecider();
        // Restore the current best solution
        decider.restoreCurrentBestSolution(phaseScope);
        // Apply the perturbation with perturbation move selector
        for (int i = 0; i < perturbationCount; i++) {
            perturbationMoveSelector.phaseStarted(phaseScope);
            var iterator = perturbationMoveSelector.iterator();
            if (iterator.hasNext()) {
                decider.doMoveOnly(phaseScope, iterator.next());
            }
        }
        // Reset cached entity list
        decider.moveSelectorPhaseStarted(phaseScope);
        logger.info(
                "Restart event triggered, step count ({}), perturbation count ({}), late elements size ({}), max rejections ({}), best score ({}), last completed score ({}), new perturbation score ({}),",
                stepScope.getStepIndex(), perturbationCount, LATE_ELEMENTS_SIZE[lateIndex],
                LATE_ELEMENTS_MAX_REJECTIONS[lateIndex], stepScope.getPhaseScope().getBestScore(),
                lastCompletedStepScore,
                stepScope.getPhaseScope().getLastCompletedStepScope().getScore());
        lateAcceptanceAcceptor.resetLateElementsScore(LATE_ELEMENTS_SIZE[lateIndex],
                (Score) stepScope.getPhaseScope().getLastCompletedStepScope().getScore());
        if (stuckCriterion instanceof UnimprovedMoveCountStuckCriterion<Solution_> stepCountStuckCriterion) {
            stepCountStuckCriterion.setMaxRejected(LATE_ELEMENTS_MAX_REJECTIONS[lateIndex]);
        }
        stuckCriterion.reset(stepScope.getPhaseScope());
        perturbationCount = (perturbationCount % maxPerturbationCount) + 1;
    }

    @Override
    public boolean accept(LocalSearchMoveScope<Solution_> moveScope) {
        return lateAcceptanceAcceptor.isAccepted(moveScope);
    }
}
