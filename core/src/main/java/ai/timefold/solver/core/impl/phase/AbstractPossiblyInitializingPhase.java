package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.localsearch.LocalSearchPhase;
import ai.timefold.solver.core.impl.phase.custom.CustomPhase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Describes a phase that can be used to initialize a solution.
 * {@link ConstructionHeuristicPhase} is automatically an initializing phase.
 * {@link CustomPhase} can be an initializing phase, if it comes before the first {@link LocalSearchPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractPossiblyInitializingPhase<Solution_>
        extends AbstractPhase<Solution_>
        implements PossiblyInitializingPhase<Solution_> {

    private final boolean lastInitializingPhase;

    protected AbstractPossiblyInitializingPhase(AbstractPossiblyInitializingPhaseBuilder<Solution_> builder) {
        super(builder);
        this.lastInitializingPhase = builder.isLastInitializingPhase();
    }

    @Override
    public final boolean isLastInitializingPhase() {
        return lastInitializingPhase;
    }

    protected static TerminationStatus translateEarlyTermination(AbstractPhaseScope<?> phaseScope,
            @Nullable TerminationStatus earlyTerminationStatus, boolean hasMoreSteps) {
        if (earlyTerminationStatus == null || !hasMoreSteps) {
            // We need to set the termination status to indicate that the phase has ended successfully.
            // This happens in two situations:
            // 1. The phase is over, and early termination did not happen.
            // 2. Early termination happened at the end of the last step, meaning a success anyway.
            //    This happens when BestScore termination is set to the same score that the last step ends with.
            return TerminationStatus.regular(phaseScope.getNextStepIndex());
        } else {
            return earlyTerminationStatus;
        }
    }

    protected void ensureCorrectTermination(AbstractPhaseScope<Solution_> phaseScope, Logger logger) {
        var terminationStatus = getTerminationStatus();
        if (!terminationStatus.terminated()) {
            throw new IllegalStateException("Impossible state: construction heuristic phase (%d) ended, but not terminated."
                    .formatted(phaseScope.getPhaseIndex()));
        } else if (terminationStatus.early()) {
            var advice = this instanceof CustomPhase<?>
                    ? "If the phase was used to initialize the solution, the solution may not be fully initialized."
                    : "The solution may not be fully initialized.";
            logger.warn("""
                    {} terminated early with step count ({}).
                    {}""",
                    this.getClass().getSimpleName(), terminationStatus.stepCount(), advice);
        }
    }

    public static abstract class AbstractPossiblyInitializingPhaseBuilder<Solution_>
            extends AbstractPhaseBuilder<Solution_> {

        private final boolean lastInitializingPhase;

        protected AbstractPossiblyInitializingPhaseBuilder(int phaseIndex, boolean lastInitializingPhase,
                String phaseName, Termination<Solution_> phaseTermination) {
            super(phaseIndex, phaseName, phaseTermination);
            this.lastInitializingPhase = lastInitializingPhase;
        }

        public boolean isLastInitializingPhase() {
            return lastInitializingPhase;
        }

    }

}
