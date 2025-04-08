package ai.timefold.solver.core.impl.phase.custom;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.phase.AbstractPossiblyInitializingPhase;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomPhaseScope;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomStepScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

import org.jspecify.annotations.NullMarked;

/**
 * Default implementation of {@link CustomPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class DefaultCustomPhase<Solution_>
        extends AbstractPossiblyInitializingPhase<Solution_>
        implements CustomPhase<Solution_> {

    private final List<PhaseCommand<Solution_>> customPhaseCommandList;
    private TerminationStatus terminationStatus = TerminationStatus.NOT_TERMINATED;

    private DefaultCustomPhase(DefaultCustomPhaseBuilder<Solution_> builder) {
        super(builder);
        this.customPhaseCommandList = builder.customPhaseCommandList;
    }

    @Override
    public TerminationStatus getTerminationStatus() {
        return terminationStatus;
    }

    @Override
    public String getPhaseTypeString() {
        return "Custom";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var phaseScope = new CustomPhaseScope<>(solverScope, phaseIndex);
        phaseStarted(phaseScope);
        TerminationStatus earlyTerminationStatus = null;
        var iterator = customPhaseCommandList.iterator();
        while (iterator.hasNext()) {
            var customPhaseCommand = iterator.next();
            solverScope.checkYielding();
            if (phaseTermination.isPhaseTerminated(phaseScope)) {
                earlyTerminationStatus = TerminationStatus.early(phaseScope.getNextStepIndex());
                break;
            }
            var stepScope = new CustomStepScope<>(phaseScope);
            stepStarted(stepScope);
            doStep(stepScope, customPhaseCommand);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        // We only store the termination status, which is exposed to the outside, when the phase has ended.
        terminationStatus = translateEarlyTermination(phaseScope, earlyTerminationStatus, iterator.hasNext());
        phaseEnded(phaseScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        terminationStatus = TerminationStatus.NOT_TERMINATED;
    }

    private void doStep(CustomStepScope<Solution_> stepScope, PhaseCommand<Solution_> customPhaseCommand) {
        var scoreDirector = stepScope.getScoreDirector();
        customPhaseCommand.changeWorkingSolution(scoreDirector,
                () -> phaseTermination.isPhaseTerminated(stepScope.getPhaseScope()));
        calculateWorkingStepScore(stepScope, customPhaseCommand);
        var solver = stepScope.getPhaseScope().getSolverScope().getSolver();
        solver.getBestSolutionRecaller().processWorkingSolutionDuringStep(stepScope);
    }

    public void stepEnded(CustomStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        var phaseScope = stepScope.getPhaseScope();
        if (logger.isDebugEnabled()) {
            logger.debug("{}    Custom step ({}), time spent ({}), score ({}), {} best score ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getScore().raw(),
                    stepScope.getBestScoreImproved() ? "new" : "   ",
                    phaseScope.getBestScore().raw());
        }
    }

    public void phaseEnded(CustomPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        ensureCorrectTermination(phaseScope, logger);
        phaseScope.endingNow();
        logger.info("{}Custom phase ({}) ended: time spent ({}), best score ({}),"
                + " move evaluation speed ({}/sec), step total ({}).",
                logIndentation,
                phaseIndex,
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore().raw(),
                phaseScope.getPhaseMoveEvaluationSpeed(),
                phaseScope.getNextStepIndex());
    }

    public static final class DefaultCustomPhaseBuilder<Solution_>
            extends AbstractPossiblyInitializingPhaseBuilder<Solution_> {

        private final List<PhaseCommand<Solution_>> customPhaseCommandList;

        public DefaultCustomPhaseBuilder(int phaseIndex, boolean lastInitializingPhase, String logIndentation,
                PhaseTermination<Solution_> phaseTermination, List<PhaseCommand<Solution_>> customPhaseCommandList) {
            super(phaseIndex, lastInitializingPhase, logIndentation, phaseTermination);
            this.customPhaseCommandList = List.copyOf(customPhaseCommandList);
        }

        @Override
        public DefaultCustomPhaseBuilder<Solution_> enableAssertions(EnvironmentMode environmentMode) {
            super.enableAssertions(environmentMode);
            return this;
        }

        @Override
        public DefaultCustomPhase<Solution_> build() {
            return new DefaultCustomPhase<>(this);
        }
    }
}
