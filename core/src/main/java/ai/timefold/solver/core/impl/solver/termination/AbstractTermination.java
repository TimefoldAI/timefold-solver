package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract sealed class AbstractTermination<Solution_>
        extends PhaseLifecycleListenerAdapter<Solution_>
        implements Termination<Solution_>
        permits AbstractCompositeTermination, BasicPlumbingTermination, BestScoreFeasibleTermination, BestScoreTermination,
        ChildThreadPlumbingTermination, PhaseToSolverTerminationBridge, ScoreCalculationCountTermination, StepCountTermination,
        TimeMillisSpentTermination, UnimprovedStepCountTermination,
        UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination, UnimprovedTimeMillisSpentTermination {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        throw new UnsupportedOperationException(
                "This terminationClass (%s) does not yet support being used in child threads of type (%s)."
                        .formatted(getClass().getSimpleName(), childThreadType));
    }

}
