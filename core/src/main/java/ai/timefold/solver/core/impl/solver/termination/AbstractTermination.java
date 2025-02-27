package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract sealed class AbstractTermination<Solution_>
        extends PhaseLifecycleListenerAdapter<Solution_>
        implements Termination<Solution_>
        permits AbstractCompositeTermination, BasicPlumbingTermination, BestScoreFeasibleTermination, BestScoreTermination,
        ChildThreadPlumbingTermination, DiminishedReturnsTermination, MoveCountTermination, PhaseToSolverTerminationBridge,
        ScoreCalculationCountTermination, StepCountTermination, TimeMillisSpentTermination, UnimprovedStepCountTermination,
        UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination, UnimprovedTimeMillisSpentTermination {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

}
