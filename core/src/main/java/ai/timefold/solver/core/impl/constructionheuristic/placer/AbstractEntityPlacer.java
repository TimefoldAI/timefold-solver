package ai.timefold.solver.core.impl.constructionheuristic.placer;

import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleSupport;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link EntityPlacer}.
 *
 * @see EntityPlacer
 */
public abstract class AbstractEntityPlacer<Solution_> implements EntityPlacer<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final EntityPlacerFactory<Solution_> factory;
    protected final HeuristicConfigPolicy<Solution_> configPolicy;

    protected PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    AbstractEntityPlacer(EntityPlacerFactory<Solution_> factory, HeuristicConfigPolicy<Solution_> configPolicy) {
        this.factory = factory;
        this.configPolicy = configPolicy;
    }

    @Override
    public EntityPlacer<Solution_> copy() {
        if (factory == null || configPolicy == null) {
            throw new IllegalStateException("The entity placer cannot be copied.");
        }
        return factory.buildEntityPlacer(configPolicy.copyConfigPolicy());
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
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
    }

}
