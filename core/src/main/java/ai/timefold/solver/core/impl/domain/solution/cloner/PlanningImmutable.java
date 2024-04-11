package ai.timefold.solver.core.impl.domain.solution.cloner;

/**
 * Opposite to {@link PlanningCloneable}, this interface is used to mark an object as immutable.
 * It will never be cloned, and multi-threaded solving will assume that it does not change.
 * Using this interface together with @{@link PlanningCloneable} is not allowed and will throw exceptions.
 */
public interface PlanningImmutable {

}
