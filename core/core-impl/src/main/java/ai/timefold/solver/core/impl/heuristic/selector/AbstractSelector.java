package ai.timefold.solver.core.impl.heuristic.selector;

import java.util.Random;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleSupport;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link Selector}.
 *
 * @see AbstractDemandEnabledSelector
 */
public abstract class AbstractSelector<Solution_> implements Selector<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    protected Random workingRandom = null;

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        workingRandom = solverScope.getWorkingRandom();
        phaseLifecycleSupport.fireSolvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseLifecycleSupport.firePhaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        phaseLifecycleSupport.fireStepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        phaseLifecycleSupport.fireStepEnded(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        phaseLifecycleSupport.firePhaseEnded(phaseScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        phaseLifecycleSupport.fireSolvingEnded(solverScope);
        workingRandom = null;
    }

    @Override
    public SelectionCacheType getCacheType() {
        return SelectionCacheType.JUST_IN_TIME;
    }

}
